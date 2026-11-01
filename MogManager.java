package com.looksmax;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MogManager {
    private MogManager() {}

    private record Req(UUID from, int expire) {}

    private static final Map<UUID, Req> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> LAST = new HashMap<>();
    private static final Random RNG = new Random();

    private static void err(ServerPlayer p, String s) {
        p.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.RED));
    }

    public static int challenge(ServerPlayer from, ServerPlayer to) {
        int now = from.getServer().getTickCount();
        if (from == to) { err(from, "Себя мог не получится."); return 0; }
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
        battle(from, to, now);
        return 1;
    }

    private static void battle(ServerPlayer a, ServerPlayer b, int now) {
        double sa = LooksUtil.score(LooksUtil.get(a));
        double sb = LooksUtil.score(LooksUtil.get(b));
        double ra = sa + RNG.nextGaussian() * 0.6;
        double rb = sb + RNG.nextGaussian() * 0.6;
        ServerPlayer win = ra >= rb ? a : b;
        ServerPlayer lose = win == a ? b : a;
        double margin = Math.abs(ra - rb);
        String phrase = margin > 2.5 ? "ТОТАЛЬНО ЗАМОГАЛ" : margin > 1 ? "замогал" : "еле-еле замогал";

        LooksUtil.set(win, LooksUtil.get(win) + 2);
        LooksUtil.set(lose, LooksUtil.get(lose) - 1);
        LAST.put(a.getUUID(), now);
        LAST.put(b.getUUID(), now);

        String line = String.format("%s (%.1f) %s %s (%.1f)!", win.getGameProfile().getName(), win == a ? ra : rb,
                phrase, lose.getGameProfile().getName(), win == a ? rb : ra);
        a.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("[MOG] " + line).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        win.sendSystemMessage(Component.literal("+2 к лукс-очкам. Теперь: " + LooksUtil.score(LooksUtil.get(win)) + "/10").withStyle(ChatFormatting.GREEN));
        lose.sendSystemMessage(Component.literal("-1 к лукс-очкам. Теперь: " + LooksUtil.score(LooksUtil.get(lose)) + "/10").withStyle(ChatFormatting.RED));
        CpmCompat.play(win, "looksmax_win");
        CpmCompat.play(lose, "looksmax_lose");
    }
}
