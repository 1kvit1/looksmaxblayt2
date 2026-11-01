package com.looksmax.client;

import com.looksmax.LooksClientConfig;
import com.looksmax.LooksMax;
import com.looksmax.MusicPayload;
import com.looksmax.Tracks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Воспроизведение музыки мога на клиенте (с учётом настроек игрока). */
public final class ClientMusic {
    private ClientMusic() {}

    private static SoundInstance current;

    private static SoundEvent event(String track) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "mog_" + track));
    }

    public static void handle(MusicPayload p) {
        if (p.stop()) { stop(); return; }
        if (!LooksClientConfig.MUSIC_ON.get() || !Tracks.isTrack(p.track())) return;
        stop();
        float vol = 3.0f * LooksClientConfig.VOLUME.get() / 100f;
        SimpleSoundInstance inst = new SimpleSoundInstance(event(p.track()), SoundSource.RECORDS, vol, 1.0f,
                RandomSource.create(), p.x(), p.y(), p.z());
        current = inst;
        Minecraft.getInstance().getSoundManager().play(inst);
    }

    /** Прослушивание трека из меню. */
    public static void preview(String track) {
        stop();
        if (!Tracks.isTrack(track)) return;
        SoundInstance inst = SimpleSoundInstance.forUI(event(track), 1.0f, LooksClientConfig.VOLUME.get() / 100f);
        current = inst;
        Minecraft.getInstance().getSoundManager().play(inst);
    }

    public static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }
}
