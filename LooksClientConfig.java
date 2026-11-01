package com.looksmax;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Клиентские настройки (меню на клавишу K, файл config/looksmax-client.toml). */
public final class LooksClientConfig {
    private LooksClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> TRACK;
    public static final ModConfigSpec.BooleanValue MUSIC_ON;
    public static final ModConfigSpec.IntValue VOLUME;
    public static final ModConfigSpec.BooleanValue SHOW_TAG;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        TRACK = b.comment("Мой трек для мог-батлов: random или gozalo/revolver/aura/y120/beamer/s503/shawel")
                .define("track", Tracks.RANDOM);
        MUSIC_ON = b.comment("Включить музыку мог-батлов").define("musicEnabled", true);
        VOLUME = b.comment("Громкость музыки, %").defineInRange("volume", 80, 0, 100);
        SHOW_TAG = b.comment("Показывать рейтинг и 'фармит ауру' над ником").define("showTag", true);
        SPEC = b.build();
    }
}
