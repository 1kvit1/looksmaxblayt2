package com.looksmax;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
                .then(net.minecraft.commands.Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                                .then(net.minecraft.commands.Commands.argument("rating", DoubleArgumentType.doubleArg(1.0, 10.0))
                                        .executes(c -> {
                                            ServerPlayer t = EntityArgument.getPlayer(c, "player");
                                            LooksUtil.set(t, (int) Math.round(DoubleArgumentType.getDouble(c, "rating") * 100));
                                            return rate(c.getSource(), t);
                                        }))))
                .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                        .executes(c -> rate(c.getSource(), EntityArgument.getPlayer(c, "player")))));
        d.register(net.minecraft.commands.Commands.literal("mog")
                .then(net.minecraft.commands.Commands.literal("accept")
                        .executes(c -> MogManager.accept(c.getSource().getPlayerOrException())))
                .then(net.minecraft.commands.Commands.literal("solo")
                        .executes(c -> MogManager.solo(c.getSource().getPlayerOrException())))
                .then(net.minecraft.commands.Commands.literal("mob")
                        .executes(c -> MogManager.mob(c.getSource().getPlayerOrException())))
                .then(net.minecraft.commands.Commands.argument("player", EntityArgument.player())
                        .executes(c -> MogManager.challenge(c.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(c, "player")))));
    }

    private static int rate(CommandSourceStack src, ServerPlayer target) {
        int v = LooksUtil.get(target);
        double s = LooksUtil.score(v);
        Component msg = Component.literal(target.getGameProfile().getName() + ": " + LooksUtil.fmt(v) + "/10 - ")
                .append(Component.literal(LooksUtil.title(s)).withStyle(LooksUtil.color(s), ChatFormatting.BOLD));
        src.sendSuccess(() -> msg, false);
        return 1;
    }
}
