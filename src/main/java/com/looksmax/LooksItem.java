package com.looksmax;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LooksItem extends Item {
    private final int gain; // в десятых

    public LooksItem(Properties props, int gain) {
        super(props);
        this.gain = gain;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int cur = LooksUtil.get(sp);
            if (cur >= LooksUtil.ITEM_CAP) {
                sp.displayClientMessage(Component.literal("Выше 9.0 предметы не работают. Доказывай в /mog батлах!")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            int now = Math.min(LooksUtil.ITEM_CAP, cur + gain);
            LooksUtil.set(sp, now);
            LooksUtil.touch(sp);
            double s = LooksUtil.score(now);
            sp.displayClientMessage(Component.literal("Лукс-апгрейд! " + s + "/10 — " + LooksUtil.title(s))
                    .withStyle(LooksUtil.color(s), ChatFormatting.BOLD), true);
            if (!sp.getAbilities().instabuild) stack.shrink(1);
            sp.getCooldowns().addCooldown(this, 40);
            CpmCompat.play(sp, "looksmax_rate", 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
