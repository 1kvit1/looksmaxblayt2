package com.looksmax;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Молоточек для формирования скул: ПКМ = +1 уровень скул (макс. 5) и +0.1 рейтинга (до потолка 9.0). */
public class CheekHammer extends Item {
    public CheekHammer(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int cheek = sp.getData(LooksMax.CHEEK.get());
            int cur = LooksUtil.get(sp);
            boolean canCheek = cheek < LooksUtil.MAX_CHEEK;
            boolean canRate = cur < LooksUtil.ITEM_CAP;
            if (!canCheek && !canRate) {
                sp.displayClientMessage(Component.literal("Скулы уже идеальные, бить больше нечего.")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            if (canCheek) sp.setData(LooksMax.CHEEK.get(), cheek + 1);
            if (canRate) LooksUtil.set(sp, Math.min(LooksUtil.ITEM_CAP, cur + 1));
            LooksUtil.touch(sp);
            level.playSound(null, sp.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
            double s = LooksUtil.score(LooksUtil.get(sp));
            sp.displayClientMessage(Component.literal("Скулы: " + sp.getData(LooksMax.CHEEK.get()) + "/"
                    + LooksUtil.MAX_CHEEK + " | рейтинг " + s + "/10").withStyle(LooksUtil.color(s), ChatFormatting.BOLD), true);
            stack.hurtAndBreak(1, sp, LivingEntity.getSlotForHand(hand));
            sp.getCooldowns().addCooldown(this, 100);
            CpmCompat.play(sp, "looksmax_rate", 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
