package com.looksmax.client;

import com.looksmax.LooksClientConfig;
import com.looksmax.LooksMax;
import com.looksmax.LooksUtil;
import com.looksmax.PrefPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = LooksMax.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    private static boolean sent;

    public static void sendPrefs() {
        PacketDistributor.sendToServer(new PrefPayload(LooksClientConfig.TRACK.get()));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { sent = false; return; }
        if (!sent) { sendPrefs(); sent = true; } // сообщаем серверу выбранный трек
        while (ClientModEvents.MENU.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new SettingsScreen(null));
        }
    }

    /** Над ником: [рейтинг титул] и надпись, что игрок фармит ауру. */
    @SubscribeEvent
    public static void nameTag(RenderNameTagEvent e) {
        if (!LooksClientConfig.SHOW_TAG.get()) return;
        if (!(e.getEntity() instanceof Player p)) return;
        int r = p.getData(LooksMax.RATING.get());
        if (r <= 0) return;
        double s = LooksUtil.score(r);
        MutableComponent c = e.getContent().copy();
        c.append(Component.literal(" [" + LooksUtil.fmt(r) + " " + LooksUtil.title(s) + "]").withStyle(LooksUtil.color(s)));
        if (p.getData(LooksMax.FARMING.get()) == 1) {
            c.append(Component.literal("  ✦ фармит ауру ✦").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        e.setContent(c);
    }
}
