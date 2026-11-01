package com.looksmax;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class Commands {
    private Commands() {}

    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(net.minecraft.commands.Commands.literal("looks")
                .executes(c -> rate(c.getSource(), c.getSource().getPlayerOrException()))
                .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                        .executes(c -> rate(c.getSource(), EntityArgument.getPlayer(c, "player")))));
        d.register(net.minecraft.commands.Commands.literal("mog")
                .then(net.minecraft.commands.Commands.literal("accept")
                        .executes(c -> MogManager.accept(c.getSource().getPlayerOrException())))
                .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                        .executes(c -> MogManager.challenge(c.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(c, "player")))));
    }

    private static int rate(CommandSourceStack src, ServerPlayer target) {
        double s = LooksUtil.score(LooksUtil.get(target));
        Component msg = Component.literal(target.getGameProfile().getName() + ": " + s + "/10 — ")
                .append(Component.literal(LooksUtil.title(s)).withStyle(LooksUtil.color(s), ChatFormatting.BOLD));
        src.sendSuccess(() -> msg, false);
        if (src.getEntity() instanceof ServerPlayer sp) CpmCompat.play(sp, "looksmax_rate");
        return 1;
    }
}
