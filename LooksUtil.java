package com.looksmax;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LooksUtil {
    private LooksUtil() {}

    /** Рейтинг хранится в СОТЫХ: 100 = 1.00, 1000 = 10.00 */
    public static final int MIN = 100;
    public static final int MAX = 1000;
    /** Предметы, фарм и соло-мог поднимают рейтинг максимум до 9.00. Выше - только мог-батлы. */
    public static final int ITEM_CAP = 900;

    // --- Баланс (в сотых) ---
    public static final int PVP_STEP = 5;    // победа/поражение против игрока: +-0.05
    public static final int MOB_STEP = 2;    // мог моба: +-0.02
    public static final int SOLO_STEP = 1;   // соло-мог: +0.01
    public static final int DECAY_STEP = 2;  // распад за период бездействия: -0.02

    private static final String[] TITLES = {
            "Субхуман", "LTN", "Нормис", "HTN", "Чедлайт", "Чад", "Гигачад", "Адонис", "Тру Адам"};

    private static int fallback(Player p) {
        return 200 + Math.floorMod(p.getUUID().hashCode(), 350); // стартовый ролл 2.00..5.49
    }

    /** Рейтинг в сотых. На сервере инициализирует значение. На клиенте берёт синхронизированное. */
    public static int get(Player p) {
        int v = p.getData(LooksMax.RATING.get());
        if (v <= 0) {
            v = fallback(p);
            if (!p.level().isClientSide) p.setData(LooksMax.RATING.get(), v);
        }
        return v;
    }

    public static void set(Player p, int v) {
        v = Mth.clamp(v, MIN, MAX);
        p.setData(LooksMax.RATING.get(), v);
        if (p instanceof ServerPlayer sp) {
            sync(sp);
            CpmCompat.updateFace(sp);
        }
    }

    /** Добавляет delta. Положительная дельта не поднимает выше cap (но и не опускает, если уже выше). */
    public static int add(Player p, int delta, int cap) {
        int cur = get(p);
        int n = delta >= 0 ? Math.max(cur, Math.min(cap, cur + delta)) : cur + delta;
        set(p, n);
        return Mth.clamp(n, MIN, MAX);
    }

    public static SyncPayload payload(ServerPlayer sp) {
        return new SyncPayload(sp.getId(), get(sp), AuraManager.isFarming(sp.getUUID()) ? 1 : 0);
    }

    /** В 1.21.1 у вложений нет авто-синхронизации: шлём рейтинг игроку и всем, кто его видит. */
    public static void sync(ServerPlayer sp) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp, payload(sp));
    }

    public static void touch(ServerPlayer p) {
        p.setData(LooksMax.LAST_ACTIVE.get(), p.level().getGameTime());
    }

    public static long decayTicks() {
        return LooksConfig.DECAY_MINUTES.get() * 1200L;
    }

    public static double score(int hundredths) {
        return hundredths / 100.0;
    }

    public static String fmt(int hundredths) {
        return String.format(Locale.ROOT, "%.2f", hundredths / 100.0);
    }

    public static String fmtDelta(int delta) {
        return String.format(Locale.ROOT, "%+.2f", delta / 100.0);
    }

    /** 0..8, по ступеням титулов. */
    public static int tier(double s) {
        if (s < 2) return 0;
        if (s < 3) return 1;
        if (s < 4) return 2;
        if (s < 5) return 3;
        if (s < 6) return 4;
        if (s < 7) return 5;
        if (s < 8) return 6;
        if (s < 9) return 7;
        return 8;
    }

    public static String title(double s) {
        return TITLES[tier(s)];
    }

    public static ChatFormatting color(double s) {
        if (s < 3) return ChatFormatting.DARK_RED;
        if (s < 5) return ChatFormatting.GOLD;
        if (s < 7) return ChatFormatting.YELLOW;
        if (s < 9) return ChatFormatting.GREEN;
        return ChatFormatting.AQUA;
    }
}
