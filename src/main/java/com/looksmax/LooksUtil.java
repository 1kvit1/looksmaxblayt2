package com.looksmax;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class LooksUtil {
    private LooksUtil() {}

    public static int get(Player p) {
        int v = p.getData(LooksMax.POINTS.get());
        if (v < 0) { // стартовый "генетический ролл" 20..54, стабильный для игрока
            v = 20 + Math.floorMod(p.getUUID().hashCode(), 35);
            p.setData(LooksMax.POINTS.get(), v);
        }
        return v;
    }

    public static void set(Player p, int v) {
        p.setData(LooksMax.POINTS.get(), Mth.clamp(v, 0, 100));
    }

    /** 0..100 очков -> оценка 1.0..10.0 */
    public static double score(int pts) {
        return Math.round((1 + pts * 0.09) * 10) / 10.0;
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
