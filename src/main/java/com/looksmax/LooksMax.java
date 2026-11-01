package com.looksmax;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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

    /** Рейтинг в десятых долях: 10..100 = 1.0..10.0. 0 = ещё не инициализирован. Синхронизируется с клиентами. */
    public static final Supplier<AttachmentType<Integer>> RATING = ATTACH.register("rating",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).sync(ByteBufCodecs.VAR_INT).copyOnDeath().build());

    /** Игровое время последней активности (предмет или мог-батл). -1 = не задано. */
    public static final Supplier<AttachmentType<Long>> LAST_ACTIVE = ATTACH.register("last_active",
            () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).copyOnDeath().build());

    /** Уровень скул от молоточка 0..5. Синхронизируется с клиентами (рисуется в FaceLayer). */
    public static final Supplier<AttachmentType<Integer>> CHEEK = ATTACH.register("cheek",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).sync(ByteBufCodecs.VAR_INT).copyOnDeath().build());

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final Supplier<SoundEvent> MOG_BATTLE = SOUNDS.register("mog_battle",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "mog_battle")));

    public static final DeferredItem<Item> HAMMER = ITEMS.register("cheek_hammer",
            () -> new CheekHammer(new Item.Properties().durability(40)));

    public static final DeferredItem<Item> GUM = ITEMS.register("mewing_gum",
            () -> new LooksItem(new Item.Properties().stacksTo(64), 1));   // +0.1
    public static final DeferredItem<Item> SERUM = ITEMS.register("serum",
            () -> new LooksItem(new Item.Properties().stacksTo(16), 4));   // +0.4
    public static final DeferredItem<Item> MIRROR = ITEMS.register("golden_mirror",
            () -> new LooksItem(new Item.Properties().stacksTo(1), 8));    // +0.8

    public LooksMax(IEventBus bus) {
        ITEMS.register(bus);
        ATTACH.register(bus);
        SOUNDS.register(bus);
        bus.addListener(this::payloads);
        bus.addListener(this::tab);
        bus.addListener(this::imc);
        NeoForge.EVENT_BUS.addListener(Commands::register);
        NeoForge.EVENT_BUS.addListener(LooksMax::onTick);
        NeoForge.EVENT_BUS.addListener(LooksMax::onLogin);
        NeoForge.EVENT_BUS.addListener(MogManager::tick);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            LooksUtil.get(sp); // инициализация + синхронизация
            CpmCompat.updateFace(sp);
        }
    }

    /** Распад рейтинга при бездействии. */
    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 200 != 0) return;
        long now = sp.level().getGameTime();
        long last = sp.getData(LAST_ACTIVE.get());
        if (last < 0) { sp.setData(LAST_ACTIVE.get(), now); return; }
        if (now - last >= LooksUtil.DECAY_TICKS) {
            int cur = LooksUtil.get(sp);
            if (cur > LooksUtil.MIN) {
                LooksUtil.set(sp, cur - LooksUtil.DECAY_STEP);
                sp.displayClientMessage(Component.literal("Ты давно не лукс-макс'ил: рейтинг падает до "
                        + LooksUtil.score(LooksUtil.get(sp)) + "/10").withStyle(ChatFormatting.RED), true);
            }
            sp.setData(LAST_ACTIVE.get(), now);
        }
    }

    private void payloads(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(EmotePayload.TYPE, EmotePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(EmoteClient::playRandom));
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
