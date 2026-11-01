package com.looksmax;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: рейтинг (в десятых) и уровень скул игрока с данным entityId. */
public record SyncPayload(int entityId, int rating, int cheek) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "sync"));
    public static final StreamCodec<ByteBuf, SyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncPayload::entityId,
            ByteBufCodecs.VAR_INT, SyncPayload::rating,
            ByteBufCodecs.VAR_INT, SyncPayload::cheek,
            SyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
