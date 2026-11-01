package com.looksmax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Мог-батлы: игрок vs игрок, игрок vs моб, соло. Длятся battleSeconds (по умолчанию 30). */
public final class MogManager {
    private MogManager() {}

    public enum Kind { PVP, MOB, SOLO }

    private record Req(UUID from, int expire) {}

    private static final class Session {
        final Kind kind;
        final UUID a;
        final UUID b; // второй игрок или моб (null для соло)
        final int end;
        int nextEmote;
        final ServerLevel level;
        final double x, y, z;
        final String track;
        boolean mobWasNoAi;

        Session(Kind kind, UUID a, UUID b, int start, int duration, ServerLevel level, Vec3 pos, String track) {
            this.kind = kind; this.a = a; this.b = b;
            this.end = start + duration;
            this.nextEmote = start + EMOTE_EVERY;
            this.level = level; this.x = pos.x; this.y = pos.y; this.z = pos.z;
            this.track = track;
        }
    }

    private static final int EMOTE_EVERY = 100;
    private static final int COOLDOWN_TICKS = 600; // 30 секунд после мога

    private static final Map<UUID, Req> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final Map<UUID, String> PREFS = new HashMap<>();
    private static final List<Session> SESSIONS = new ArrayList<>();
    private static final Random RNG = new Random();

    // ---------- вспомогательное ----------

    private static void err(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED));
    }

    private static int duration() {
        return LooksConfig.BATTLE_SECONDS.get() * 20;
    }

    public static boolean inSession(UUID id) {
        for (Session s : SESSIONS) if (id.equals(s.a) || id.equals(s.b)) return true;
        return false;
    }

    public static void setPref(UUID id, String track) {
        if (Tracks.isValid(track)) PREFS.put(id, track);
    }

    public static void forget(UUID id) {
        PREFS.remove(id);
        PENDING.remove(id);
        COOLDOWN.remove(id);
    }

    private static boolean onCooldown(ServerPlayer p) {
        int now = p.getServer().getTickCount();
        int until = COOLDOWN.getOrDefault(p.getUUID(), 0);
        if (until > now) {
            err(p, "Кулдаун мога: ещё " + ((until - now) / 20 + 1) + " сек.");
            return true;
        }
        return false;
    }

    /** Трек: у каждого участника свой выбор, а чей из них играет - решает рандом. */
    private static String pickTrack(ServerPlayer... players) {
        ServerPlayer owner = players[RNG.nextInt(players.length)];
        String t = PREFS.getOrDefault(owner.getUUID(), Tracks.RANDOM);
        if (!Tracks.isTrack(t)) t = Tracks.randomId(RNG);
        owner.sendSystemMessage(Component.literal("Играет твой трек: " + Tracks.name(t)).withStyle(ChatFormatting.GRAY));
        if (players.length > 1) {
            for (ServerPlayer p : players) {
                if (p != owner) {
                    p.sendSystemMessage(Component.literal("Играет трек игрока " + owner.getGameProfile().getName()
                            + ": " + Tracks.name(t)).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        return t;
    }

    private static void sendMusic(ServerLevel level, double x, double y, double z, String track, boolean stop) {
        MusicPayload payload = new MusicPayload(track, x, y, z, stop);
        for (ServerPlayer pl : level.players()) {
            if (pl.distanceToSqr(x, y, z) < 80 * 80) PacketDistributor.sendToPlayer(pl, payload);
        }
    }

    private static void freeze(ServerPlayer p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false, false));
        PacketDistributor.sendToPlayer(p, new EmotePayload());
        CpmCompat.play(p, "looksmax_battle", 1);
    }

    // ---------- старт ----------

    public static int challenge(ServerPlayer from, ServerPlayer to) {
        int now = from.getServer().getTickCount();
        if (from == to) { err(from, "Себя мог не получится. Попробуй /mog solo."); return 0; }
        if (inSession(from.getUUID()) || inSession(to.getUUID())) { err(from, "Кто-то из вас уже могает."); return 0; }
        if (onCooldown(from)) return 0;
        PENDING.put(to.getUUID(), new Req(from.getUUID(), now + 600));
        from.sendSystemMessage(Component.literal("Вызов отправлен: " + to.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW));
        to.sendSystemMessage(Component.literal(from.getGameProfile().getName()
                + " вызывает тебя на МОГ-БАТЛ! Напиши /mog accept (30 сек)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return 1;
    }

    public static int accept(ServerPlayer to) {
        int now = to.getServer().getTickCount();
        Req r = PENDING.remove(to.getUUID());
        if (r == null || now > r.expire()) { err(to, "Нет активных вызовов."); return 0; }
        ServerPlayer from = to.getServer().getPlayerList().getPlayer(r.from());
        if (from == null) { err(to, "Соперник ушёл с сервера."); return 0; }
        if (inSession(from.getUUID()) || inSession(to.getUUID())) { err(to, "Кто-то уже могает."); return 0; }
        if (onCooldown(to)) return 0;
        Vec3 mid = from.position().add(to.position()).scale(0.5);
        String track = pickTrack(from, to);
        Session s = new Session(Kind.PVP, from.getUUID(), to.getUUID(), now, duration(), from.serverLevel(), mid, track);
        SESSIONS.add(s);
        freeze(from, duration());
        freeze(to, duration());
        sendMusic(s.level, s.x, s.y, s.z, track, false);
        broadcast(from, "[MOG] " + from.getGameProfile().getName() + " vs " + to.getGameProfile().getName()
                + " - МОГ-БАТЛ начался! " + LooksConfig.BATTLE_SECONDS.get() + " секунд!");
        return 1;
    }

    public static int solo(ServerPlayer p) {
        int now = p.getServer().getTickCount();
        if (inSession(p.getUUID())) { err(p, "Ты уже могаешь."); return 0; }
        if (onCooldown(p)) return 0;
        String track = pickTrack(p);
        Session s = new Session(Kind.SOLO, p.getUUID(), null, now, duration(), p.serverLevel(), p.position(), track);
        SESSIONS.add(s);
        freeze(p, duration());
        sendMusic(s.level, s.x, s.y, s.z, track, false);
        broadcast(p, "[MOG] " + p.getGameProfile().getName() + " могает в одиночку. " + LooksConfig.BATTLE_SECONDS.get() + " секунд!");
        return 1;
    }

    public static int mob(ServerPlayer p) {
        if (!LooksConfig.MOB_MOG.get()) { err(p, "Мог мобов выключен в настройках."); return 0; }
        int now = p.getServer().getTickCount();
        if (inSession(p.getUUID())) { err(p, "Ты уже могаешь."); return 0; }
        if (onCooldown(p)) return 0;

        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = p.getBoundingBox().inflate(7);
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && !inSession(e.getUUID()))) {
            Vec3 to = m.getEyePosition().subtract(eye);
            double d = to.length();
            if (d < 0.5) continue;
            if (to.normalize().dot(look) > 0.85 && d < bestDist) { best = m; bestDist = d; }
        }
        if (best == null) { err(p, "Посмотри на моба рядом (до 7 блоков) и повтори."); return 0; }

        String track = pickTrack(p);
        Session s = new Session(Kind.MOB, p.getUUID(), best.getUUID(), now, duration(), p.serverLevel(),
                p.position().add(best.position()).scale(0.5), track);
        s.mobWasNoAi = best.isNoAi();
        best.setNoAi(true);
        SESSIONS.add(s);
        freeze(p, duration());
        sendMusic(s.level, s.x, s.y, s.z, track, false);
        broadcast(p, "[MOG] " + p.getGameProfile().getName() + " могает моба: " + best.getName().getString()
                + " (" + String.format(Locale.ROOT, "%.2f", MobRatings.of(best)) + ")");
        return 1;
    }

    private static void broadcast(ServerPlayer any, String text) {
        any.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
    }

    // ---------- тик ----------

    public static void tick(ServerTickEvent.Post e) {
        if (SESSIONS.isEmpty()) return;
        int now = e.getServer().getTickCount();
        Iterator<Session> it = SESSIONS.iterator();
        while (it.hasNext()) {
            Session s = it.next();
            ServerPlayer pa = e.getServer().getPlayerList().getPlayer(s.a);
            ServerPlayer pb = s.kind == Kind.PVP ? e.getServer().getPlayerList().getPlayer(s.b) : null;
            Entity mob = s.kind == Kind.MOB ? s.level.getEntity(s.b) : null;

            boolean broken = pa == null || pa.isDeadOrDying()
                    || (s.kind == Kind.PVP && (pb == null || pb.isDeadOrDying()))
                    || (s.kind == Kind.MOB && (mob == null || !mob.isAlive()));
            if (broken) { it.remove(); cleanup(s, mob); continue; }

            if (now >= s.end) {
                it.remove();
                cleanup(s, mob);
                finish(s, pa, pb, mob, now);
                continue;
            }
            if (now >= s.nextEmote) {
                PacketDistributor.sendToPlayer(pa, new EmotePayload());
                if (pb != null) PacketDistributor.sendToPlayer(pb, new EmotePayload());
                s.nextEmote += EMOTE_EVERY;
            }
        }
    }

    private static void cleanup(Session s, Entity mob) {
        if (mob instanceof Mob m) m.setNoAi(s.mobWasNoAi);
        sendMusic(s.level, s.x, s.y, s.z, s.track, true);
    }

    // ---------- итоги ----------

    private static void finish(Session s, ServerPlayer a, ServerPlayer b, Entity mob, int now) {
        int cd = now + COOLDOWN_TICKS;
        COOLDOWN.put(a.getUUID(), cd);
        LooksUtil.touch(a);
        switch (s.kind) {
            case SOLO -> {
                int after = LooksUtil.add(a, LooksUtil.SOLO_STEP, LooksUtil.ITEM_CAP);
                a.sendSystemMessage(Component.literal("Соло-мог завершён: " + LooksUtil.fmtDelta(LooksUtil.SOLO_STEP)
                        + " -> " + LooksUtil.fmt(after) + "/10").withStyle(ChatFormatting.GREEN));
                CpmCompat.play(a, "looksmax_win", 1);
            }
            case MOB -> {
                double sa = LooksUtil.score(LooksUtil.get(a)) + RNG.nextGaussian() * 0.6;
                double sm = MobRatings.of(mob) + RNG.nextGaussian() * 0.6;
                boolean win = sa >= sm;
                int delta = win ? LooksUtil.MOB_STEP : -LooksUtil.MOB_STEP;
                int after = LooksUtil.add(a, delta, LooksUtil.MAX);
                broadcast(a, String.format(Locale.ROOT, "[MOG] %s (%.2f) %s %s (%.2f)!", a.getGameProfile().getName(), sa,
                        win ? "замогал моба" : "был замоган мобом", mob.getName().getString(), sm));
                a.sendSystemMessage(Component.literal(LooksUtil.fmtDelta(delta) + " -> " + LooksUtil.fmt(after) + "/10")
                        .withStyle(win ? ChatFormatting.GREEN : ChatFormatting.RED));
                CpmCompat.play(a, win ? "looksmax_win" : "looksmax_lose", 1);
            }
            case PVP -> {
                COOLDOWN.put(b.getUUID(), cd);
                LooksUtil.touch(b);
                double ra = LooksUtil.score(LooksUtil.get(a)) + RNG.nextGaussian() * 0.6;
                double rb = LooksUtil.score(LooksUtil.get(b)) + RNG.nextGaussian() * 0.6;
                ServerPlayer win = ra >= rb ? a : b;
                ServerPlayer lose = win == a ? b : a;
                double margin = Math.abs(ra - rb);
                String phrase = margin > 2.5 ? "ТОТАЛЬНО ЗАМОГАЛ" : margin > 1 ? "замогал" : "еле-еле замогал";
                int w = LooksUtil.add(win, LooksUtil.PVP_STEP, LooksUtil.MAX);
                int l = LooksUtil.add(lose, -LooksUtil.PVP_STEP, LooksUtil.MAX);
                broadcast(a, String.format(Locale.ROOT, "[MOG] %s (%.2f) %s %s (%.2f)!",
                        win.getGameProfile().getName(), win == a ? ra : rb, phrase,
                        lose.getGameProfile().getName(), win == a ? rb : ra));
                win.sendSystemMessage(Component.literal(LooksUtil.fmtDelta(LooksUtil.PVP_STEP) + " -> " + LooksUtil.fmt(w) + "/10")
                        .withStyle(ChatFormatting.GREEN));
                lose.sendSystemMessage(Component.literal(LooksUtil.fmtDelta(-LooksUtil.PVP_STEP) + " -> " + LooksUtil.fmt(l) + "/10")
                        .withStyle(ChatFormatting.RED));
                CpmCompat.play(win, "looksmax_win", 1);
                CpmCompat.play(lose, "looksmax_lose", 1);
            }
        }
    }
}
