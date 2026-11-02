package com.looksmax;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Серверные/общие настройки (файл config/looksmax-common.toml). */
public final class LooksConfig {
    private LooksConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AURA_PARTICLES;
    public static final ModConfigSpec.BooleanValue MOB_MOG;
    public static final ModConfigSpec.IntValue BATTLE_SECONDS;
    public static final ModConfigSpec.IntValue FARM_IDLE_SECONDS;
    public static final ModConfigSpec.IntValue FARM_INTERVAL_SECONDS;
    public static final ModConfigSpec.IntValue FARM_GAIN;
    public static final ModConfigSpec.IntValue DECAY_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        AURA_PARTICLES = b.comment("Частицы ауры вокруг игроков").define("auraParticles", true);
        MOB_MOG = b.comment("Разрешить /mog mob (мог мобов)").define("mobMog", true);
        BATTLE_SECONDS = b.comment("Длительность мога в секундах (трек - 30 секунд)")
                .defineInRange("battleSeconds", 30, 10, 120);
        FARM_IDLE_SECONDS = b.comment("Сколько секунд надо простоять, чтобы начать фармить ауру")
                .defineInRange("farmIdleSeconds", 60, 5, 3600);
        FARM_INTERVAL_SECONDS = b.comment("Раз в сколько секунд фарм даёт очки")
                .defineInRange("farmIntervalSeconds", 20, 5, 600);
        FARM_GAIN = b.comment("Сколько сотых рейтинга даёт один тик фарма (2 = +0.02)")
                .defineInRange("farmGain", 2, 0, 100);
        DECAY_MINUTES = b.comment("Через сколько минут бездействия рейтинг падает на 0.02")
                .defineInRange("decayMinutes", 10, 1, 600);
        SPEC = b.build();
    }
}
