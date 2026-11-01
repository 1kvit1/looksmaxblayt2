package com.looksmax.client;

import com.looksmax.LooksMax;
import com.looksmax.SyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Применяет данные от сервера к клиентской копии игрока (для надписи над ником). */
public final class ClientSync {
    private ClientSync() {}

    public static void apply(SyncPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p.entityId());
        if (e instanceof Player pl) {
            pl.setData(LooksMax.RATING.get(), p.rating());
            pl.setData(LooksMax.FARMING.get(), p.farming());
        }
    }
}
