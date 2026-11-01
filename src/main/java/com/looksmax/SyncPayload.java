package com.looksmax;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: рейтинг (в сотых) и состояние "фармит ауру" игрока с данным entityId. */
public record SyncPayload(int entityId, int rating, int farming) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LooksMax.MODID, "sync"));
    public static final StreamCodec<ByteBuf, SyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncPayload::entityId,
            ByteBufCodecs.VAR_INT, SyncPayload::rating,
            ByteBufCodecs.VAR_INT, SyncPayload::farming,
            SyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
