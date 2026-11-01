package com.looksmax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MogManager {
    private MogManager() {}

    /** Длительность батла: 200 тиков = 10 секунд. */
    public static final int DURATION = 200;
    /** Как часто менять эмоцию (тики). */
    private static final int EMOTE_EVERY = 100;

    private record Req(UUID from, int expire) {}

    private static final class Battle {
        final UUID a, b;
        final int end;
        int nextEmote;
        Battle(UUID a, UUID b, int start) { this.a = a; this.b = b; this.end = start + DURATION; this.nextEmote = start + EMOTE_EVERY; }
    }

    private static final Map<UUID, Req> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> LAST = new HashMap<>();
    private static final List<Battle> BATTLES = new ArrayList<>();
    private static final Random RNG = new Random();

    private static void err(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED));
    }

    private static boolean inBattle(UUID id) {
        for (Battle b : BATTLES) if (b.a.equals(id) || b.b.equals(id)) return true;
        return false;
    }

    public static int challenge(ServerPlayer from, ServerPlayer to) {
        int now = from.getServer().getTickCount();
        if (from == to) { err(from, "Себя мог не получится."); return 0; }
        if (inBattle(from.getUUID()) || inBattle(to.getUUID())) { err(from, "Кто-то из вас уже в батле."); return 0; }
        if (now - LAST.getOrDefault(from.getUUID(), -10000) < 400) { err(from, "Кулдаун мога: подожди 20 секунд."); return 0; }
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
        if (inBattle(from.getUUID()) || inBattle(to.getUUID())) { err(to, "Кто-то уже в батле."); return 0; }
        start(from, to, now);
        return 1;
    }

    private static void start(ServerPlayer a, ServerPlayer b, int now) {
        BATTLES.add(new Battle(a.getUUID(), b.getUUID(), now));
        for (ServerPlayer p : new ServerPlayer[]{a, b}) {
            // на 10 секунд игроки "стоят лицом к лицу"
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 6, false, false, false));
            PacketDistributor.sendToPlayer(p, new EmotePayload());
            CpmCompat.play(p, "looksmax_battle", 1);
        }
        // Музыка: звук играет с начала файла. Положи в mog_battle.ogg уже вырезанный дроп (10 сек).
        a.serverLevel().playSound(null, a.blockPosition(), LooksMax.MOG_BATTLE.get(), SoundSource.RECORDS, 4.0f, 1.0f);
        a.getServer().getPlayerList().broadcastSystemMessage(Component.literal("[MOG] " + a.getGameProfile().getName()
                + " vs " + b.getGameProfile().getName() + " — МОГ-БАТЛ начался! 10 секунд!")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
    }

    public static void tick(ServerTickEvent.Post e) {
        if (BATTLES.isEmpty()) return;
        int now = e.getServer().getTickCount();
        Iterator<Battle> it = BATTLES.iterator();
        while (it.hasNext()) {
            Battle bt = it.next();
            ServerPlayer pa = e.getServer().getPlayerList().getPlayer(bt.a);
            ServerPlayer pb = e.getServer().getPlayerList().getPlayer(bt.b);
            if (pa == null || pb == null || pa.isDeadOrDying() || pb.isDeadOrDying()) { it.remove(); continue; }
            if (now >= bt.end) { it.remove(); finish(pa, pb, now); continue; }
            if (now >= bt.nextEmote) {
                PacketDistributor.sendToPlayer(pa, new EmotePayload());
                PacketDistributor.sendToPlayer(pb, new EmotePayload());
                bt.nextEmote += EMOTE_EVERY;
            }
        }
    }

    private static void finish(ServerPlayer a, ServerPlayer b, int now) {
        double sa = LooksUtil.score(LooksUtil.get(a));
        double sb = LooksUtil.score(LooksUtil.get(b));
        double ra = sa + RNG.nextGaussian() * 0.6;
        double rb = sb + RNG.nextGaussian() * 0.6;
        ServerPlayer win = ra >= rb ? a : b;
        ServerPlayer lose = win == a ? b : a;
        double margin = Math.abs(ra - rb);
        String phrase = margin > 2.5 ? "ТОТАЛЬНО ЗАМОГАЛ" : margin > 1 ? "замогал" : "еле-еле замогал";

        LooksUtil.set(win, LooksUtil.get(win) + LooksUtil.MOG_STEP);
        LooksUtil.set(lose, LooksUtil.get(lose) - LooksUtil.MOG_STEP);
        LooksUtil.touch(win);
        LooksUtil.touch(lose);
        LAST.put(a.getUUID(), now);
        LAST.put(b.getUUID(), now);

        String line = String.format("%s (%.1f) %s %s (%.1f)!", win.getGameProfile().getName(), win == a ? ra : rb,
                phrase, lose.getGameProfile().getName(), win == a ? rb : ra);
        a.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("[MOG] " + line).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        win.sendSystemMessage(Component.literal("+0.2 к рейтингу. Теперь: " + LooksUtil.score(LooksUtil.get(win)) + "/10").withStyle(ChatFormatting.GREEN));
        lose.sendSystemMessage(Component.literal("-0.2 к рейтингу. Теперь: " + LooksUtil.score(LooksUtil.get(lose)) + "/10").withStyle(ChatFormatting.RED));
        CpmCompat.play(win, "looksmax_win", 1);
        CpmCompat.play(lose, "looksmax_lose", 1);
    }
}
