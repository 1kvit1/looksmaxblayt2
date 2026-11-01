package com.looksmax;

import java.util.Random;

/** Список треков для мог-батлов. Файлы: assets/looksmax/sounds/mog/<id>.ogg (по 30 секунд). */
public final class Tracks {
    private Tracks() {}

    public static final String RANDOM = "random";
    public static final String[] IDS = {"gozalo", "revolver", "aura", "y120", "beamer", "s503", "shawel"};
    public static final String[] NAMES = {
            "GOZALO - Ariis",
            "DJ ZUP RAIii - револьвер (Super Slowed)",
            "Phonk3r - Aura",
            "SXRKIN, Zmajor, Mc Lyc4n - Y120 (Slowed)",
            "predayed - beamer boy",
            "Sarkon - 503 (Slowed)",
            "QMIIR, Sek1roRem - SHAWEL FUNK"
    };

    public static boolean isTrack(String id) {
        for (String s : IDS) if (s.equals(id)) return true;
        return false;
    }

    public static boolean isValid(String id) {
        return RANDOM.equals(id) || isTrack(id);
    }

    public static String name(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return NAMES[i];
        return "Случайный";
    }

    public static String randomId(Random r) {
        return IDS[r.nextInt(IDS.length)];
    }
}
