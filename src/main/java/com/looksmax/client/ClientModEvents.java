package com.looksmax.client;

import com.looksmax.LooksMax;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = LooksMax.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    /** Клавиша K открывает меню LooksMax (можно переназначить в управлении). */
    public static final KeyMapping MENU = new KeyMapping("key.looksmax.menu", InputConstants.KEY_K, "key.categories.looksmax");

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(MENU);
    }
}
