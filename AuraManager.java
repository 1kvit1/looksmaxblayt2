package com.looksmax;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Частицы ауры (своя на каждый уровень) и фарм ауры, когда игрок стоит на месте. */
public final class AuraManager {
    private AuraManager() {}

    private static final class State {
        Vec3 pos;
        int still;     // тиков без движения
        int farmAcc;   // тиков с последнего начисления
        boolean farming;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** Частицы по ступеням: Субхуман ... Тру Адам. */
    private static final ParticleOptions[] AURA = {
            ParticleTypes.SMOKE,            // Субхуман
            ParticleTypes.ASH,              // LTN
            ParticleTypes.WHITE_ASH,        // Нормис
            ParticleTypes.CLOUD,            // HTN
            ParticleTypes.HAPPY_VILLAGER,   // Чедлайт
            ParticleTypes.END_ROD,          // Чад
            ParticleTypes.ELECTRIC_SPARK,   // Гигачад
            ParticleTypes.TOTEM_OF_UNDYING, // Адонис
            ParticleTypes.SOUL_FIRE_FLAME   // Тру Адам
    };

    public static boolean isFarming(UUID id) {
        State s = STATES.get(id);
        return s != null && s.farming;
    }

    public static void remove(UUID id) {
        STATES.remove(id);
    }

    /** Вызывается каждые 5 тиков для каждого серверного игрока. */
    public static void tick(ServerPlayer sp) {
        if (sp.isSpectator() || !sp.isAlive()) return;
        State st = STATES.computeIfAbsent(sp.getUUID(), k -> new State());
        Vec3 pos = sp.position();
        boolean moved = st.pos == null || pos.distanceToSqr(st.pos) > 0.01 || sp.isPassenger();
        st.pos = pos;

        if (moved || MogManager.inSession(sp.getUUID())) {
            st.still = 0;
            st.farmAcc = 0;
            if (st.farming) {
                st.farming = false;
                LooksUtil.sync(sp);
                sp.displayClientMessage(Component.literal("Ты перестал фармить ауру").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            st.still += 5;
            if (!st.farming && st.still >= LooksConfig.FARM_IDLE_SECONDS.get() * 20) {
                st.farming = true;
                st.farmAcc = 0;
                LooksUtil.sync(sp);
                CpmCompat.play(sp, "looksmax_farm", 1);
            }
        }

        if (st.farming) {
            st.farmAcc += 5;
            int interval = LooksConfig.FARM_INTERVAL_SECONDS.get() * 20;
            if (st.farmAcc >= interval) {
                st.farmAcc = 0;
                if (LooksUtil.get(sp) < LooksUtil.ITEM_CAP) {
                    LooksUtil.add(sp, LooksConfig.FARM_GAIN.get(), LooksUtil.ITEM_CAP);
                }
                LooksUtil.touch(sp);
            }
            if (sp.tickCount % 20 < 5) {
                sp.displayClientMessage(Component.literal("✦ Ты фармишь ауру ✦  "
                        + LooksUtil.fmt(LooksUtil.get(sp)) + "/10").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
            }
        }

        if (LooksConfig.AURA_PARTICLES.get()) spawn(sp, st.farming);
    }

    private static void spawn(ServerPlayer sp, boolean farming) {
        ServerLevel level = sp.serverLevel();
        int tier = LooksUtil.tier(LooksUtil.score(LooksUtil.get(sp)));
        int count = 1 + tier / 2;
        if (farming) count *= 3;
        level.sendParticles(AURA[tier], sp.getX(), sp.getY() + 1.0, sp.getZ(), count, 0.35, 0.6, 0.35, 0.02);
        if (farming) {
            level.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.3, sp.getZ(), 6, 0.5, 0.6, 0.5, 0.4);
            level.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 0.2, sp.getZ(), 2, 0.4, 0.1, 0.4, 0.06);
        }
    }
}
