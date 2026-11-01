package com.looksmax;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class LooksUtil {
    private LooksUtil() {}

    /** Рейтинг хранится в десятых: 10 = 1.0, 100 = 10.0 */
    public static final int MIN = 10;
    public static final int MAX = 100;
    /** Максимальный уровень скул от молоточка. */
    public static final int MAX_CHEEK = 5;
    /** Предметы поднимают рейтинг максимум до 9.0. Выше — только мог-батлы. */
    public static final int ITEM_CAP = 90;
    /** Награда/штраф за мог-батл: 0.2 */
    public static final int MOG_STEP = 2;
    /** Бездействие: через столько тиков без предметов/батлов рейтинг падает (12000 тиков = 10 минут). */
    public static final long DECAY_TICKS = 12000L;
    /** Падение за один период бездействия: 0.1 */
    public static final int DECAY_STEP = 1;

    private static int fallback(Player p) {
        return 20 + Math.floorMod(p.getUUID().hashCode(), 35); // стартовый ролл 2.0..5.4
    }

    /** Рейтинг в десятых. На сервере инициализирует значение. На клиенте берёт синхронизированное. */
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
        if (p instanceof ServerPlayer sp) CpmCompat.updateFace(sp);
    }

    public static void touch(ServerPlayer p) {
        p.setData(LooksMax.LAST_ACTIVE.get(), p.level().getGameTime());
    }

    public static double score(int tenths) {
        return tenths / 10.0;
    }

    public static String title(double s) {
        if (s < 2) return "Субхуман";
        if (s < 3) return "LTN";
        if (s < 4) return "Нормис";
        if (s < 5) return "HTN";
        if (s < 6) return "Чедлайт";
        if (s < 7) return "Чад";
        if (s < 8) return "Гигачад";
        if (s < 9) return "Адонис";
        return "Тру Адам";
    }

    public static ChatFormatting color(double s) {
        if (s < 3) return ChatFormatting.DARK_RED;
        if (s < 5) return ChatFormatting.GOLD;
        if (s < 7) return ChatFormatting.YELLOW;
        if (s < 9) return ChatFormatting.GREEN;
        return ChatFormatting.AQUA;
    }
}
