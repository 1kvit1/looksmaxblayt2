package com.looksmax;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> сервер: какой трек выбрал игрок. */
public record PrefPayload(String track) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PrefPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "pref"));
    public static final StreamCodec<ByteBuf, PrefPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PrefPayload::track, PrefPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
