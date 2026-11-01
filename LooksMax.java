package com.looksmax;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
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
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(LooksMax.MODID)
public class LooksMax {
    public static final String MODID = "looksmax";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACH =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    /** Очки внешности 0..100. -1 = ещё не инициализировано. */
    public static final Supplier<AttachmentType<Integer>> POINTS = ATTACH.register("points",
            () -> AttachmentType.builder(() -> -1).serialize(Codec.INT).copyOnDeath().build());

    public static final DeferredItem<Item> GUM = ITEMS.register("mewing_gum",
            () -> new LooksItem(new Item.Properties().stacksTo(64), 1));
    public static final DeferredItem<Item> SERUM = ITEMS.register("serum",
            () -> new LooksItem(new Item.Properties().stacksTo(16), 4));
    public static final DeferredItem<Item> MIRROR = ITEMS.register("golden_mirror",
            () -> new LooksItem(new Item.Properties().stacksTo(1), 8));

    public LooksMax(IEventBus bus) {
        ITEMS.register(bus);
        ATTACH.register(bus);
        bus.addListener(this::tab);
        bus.addListener(this::imc);
        NeoForge.EVENT_BUS.addListener(Commands::register);
    }

    private void tab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            e.accept(GUM.get());
            e.accept(SERUM.get());
            e.accept(MIRROR.get());
        }
    }

    private void imc(InterModEnqueueEvent e) {
        if (ModList.get().isLoaded("cpm")) {
            e.enqueueWork(() -> InterModComms.sendTo("cpm", "api",
                    () -> (Supplier<?>) () -> CpmCompat.create()));
        }
    }
}
