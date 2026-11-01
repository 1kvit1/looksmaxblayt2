package com.looksmax;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: "включи случайную эмоцию из своего списка Emotecraft". */
public record EmotePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EmotePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "emote"));
    public static final StreamCodec<ByteBuf, EmotePayload> CODEC = StreamCodec.unit(new EmotePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
