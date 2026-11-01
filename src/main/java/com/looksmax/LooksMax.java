package com.looksmax;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(LooksMax.MODID)
public class LooksMax {
    public static final String MODID = "looksmax";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACH =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /** Рейтинг в сотых: 100..1000 = 1.00..10.00. 0 = ещё не инициализирован. */
    public static final Supplier<AttachmentType<Integer>> RATING = ATTACH.register("rating",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    /** Игровое время последней активности (предмет, фарм, мог). -1 = не задано. */
    public static final Supplier<AttachmentType<Long>> LAST_ACTIVE = ATTACH.register("last_active",
            () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).copyOnDeath().build());

    /** Только для клиента: 1 = игрок сейчас фармит ауру (приходит пакетом, не сохраняется). */
    public static final Supplier<AttachmentType<Integer>> FARMING = ATTACH.register("farming",
            () -> AttachmentType.builder(() -> 0).build());

    // Предметы: чуть-чуть рейтинга (в сотых), большие кулдауны
    public static final DeferredItem<Item> GUM = ITEMS.register("mewing_gum",
            () -> new LooksItem(new Item.Properties().stacksTo(64), 2, 600));
    public static final DeferredItem<Item> SERUM = ITEMS.register("serum",
            () -> new LooksItem(new Item.Properties().stacksTo(16), 5, 1200));
    public static final DeferredItem<Item> MIRROR = ITEMS.register("golden_mirror",
            () -> new LooksItem(new Item.Properties().stacksTo(1), 10, 2400));
    public static final DeferredItem<Item> HAMMER = ITEMS.register("cheek_hammer",
            () -> new LooksItem(new Item.Properties().durability(40), 3, 1200));

    public LooksMax(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, LooksConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, LooksClientConfig.SPEC);
        ITEMS.register(bus);
        ATTACH.register(bus);
        bus.addListener(this::payloads);
        bus.addListener(this::tab);
        bus.addListener(this::imc);
        NeoForge.EVENT_BUS.addListener(Commands::register);
        NeoForge.EVENT_BUS.addListener(LooksMax::onTick);
        NeoForge.EVENT_BUS.addListener(LooksMax::onLogin);
        NeoForge.EVENT_BUS.addListener(LooksMax::onLogout);
        NeoForge.EVENT_BUS.addListener(LooksMax::onTrack);
        NeoForge.EVENT_BUS.addListener(LooksMax::onRespawn);
        NeoForge.EVENT_BUS.addListener(LooksMax::onDimension);
        NeoForge.EVENT_BUS.addListener(MogManager::tick);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            LooksUtil.get(sp); // инициализация
            LooksUtil.sync(sp);
            CpmCompat.updateFace(sp);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        AuraManager.remove(e.getEntity().getUUID());
        MogManager.forget(e.getEntity().getUUID());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) LooksUtil.sync(sp);
    }

    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) LooksUtil.sync(sp);
    }

    /** Когда игрок начинает "видеть" другого игрока, отправляем ему рейтинг и состояние фарма. */
    private static void onTrack(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, LooksUtil.payload(target));
        }
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 5 == 0) AuraManager.tick(sp);
        if (sp.tickCount % 200 != 0) return;

        // Распад рейтинга при бездействии
        long now = sp.level().getGameTime();
        long last = sp.getData(LAST_ACTIVE.get());
        if (last < 0) { sp.setData(LAST_ACTIVE.get(), now); return; }
        if (now - last >= LooksUtil.decayTicks()) {
            int cur = LooksUtil.get(sp);
            if (cur > LooksUtil.MIN) {
                LooksUtil.set(sp, cur - LooksUtil.DECAY_STEP);
                sp.displayClientMessage(Component.literal("Ты давно не лукс-макс'ил: рейтинг падает до "
                        + LooksUtil.fmt(LooksUtil.get(sp)) + "/10").withStyle(ChatFormatting.RED), true);
            }
            sp.setData(LAST_ACTIVE.get(), now);
        }
    }

    private void payloads(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(EmotePayload.TYPE, EmotePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(EmoteClient::playRandom));
        r.playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.looksmax.client.ClientSync.apply(payload)));
        r.playToClient(MusicPayload.TYPE, MusicPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.looksmax.client.ClientMusic.handle(payload)));
        r.playToServer(PrefPayload.TYPE, PrefPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> MogManager.setPref(ctx.player().getUUID(), payload.track())));
    }

    private void tab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            e.accept(GUM.get());
            e.accept(SERUM.get());
            e.accept(MIRROR.get());
            e.accept(HAMMER.get());
        }
    }

    private void imc(InterModEnqueueEvent e) {
        if (ModList.get().isLoaded("cpm")) {
            e.enqueueWork(() -> InterModComms.sendTo("cpm", "api",
                    () -> (Supplier<?>) () -> CpmCompat.create()));
        }
    }
}
