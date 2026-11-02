package com.looksmax;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: включить (или остановить) музыку мога в точке. */
public record MusicPayload(String track, double x, double y, double z, boolean stop) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MusicPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "music"));
    public static final StreamCodec<ByteBuf, MusicPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MusicPayload::track,
            ByteBufCodecs.DOUBLE, MusicPayload::x,
            ByteBufCodecs.DOUBLE, MusicPayload::y,
            ByteBufCodecs.DOUBLE, MusicPayload::z,
            ByteBufCodecs.BOOL, MusicPayload::stop,
            MusicPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
