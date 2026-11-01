package com.looksmax;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Предмет, дающий чуть-чуть рейтинга (в сотых) с большим кулдауном. Потолок 9.00. */
public class LooksItem extends Item {
    private final int gain;
    private final int cooldown;

    public LooksItem(Properties props, int gain, int cooldownTicks) {
        super(props);
        this.gain = gain;
        this.cooldown = cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (LooksUtil.get(sp) >= LooksUtil.ITEM_CAP) {
                sp.displayClientMessage(Component.literal("Выше 9.00 предметы не работают. Только мог-батлы!")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            int now = LooksUtil.add(sp, gain, LooksUtil.ITEM_CAP);
            LooksUtil.touch(sp);
            double s = LooksUtil.score(now);
            sp.displayClientMessage(Component.literal(LooksUtil.fmtDelta(gain) + "  ->  " + LooksUtil.fmt(now)
                    + "/10 - " + LooksUtil.title(s)).withStyle(LooksUtil.color(s), ChatFormatting.BOLD), true);
            if (stack.isDamageableItem()) stack.hurtAndBreak(1, sp, LivingEntity.getSlotForHand(hand));
            else if (!sp.getAbilities().instabuild) stack.shrink(1);
            sp.getCooldowns().addCooldown(this, cooldown);
            CpmCompat.play(sp, "looksmax_rate", 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
