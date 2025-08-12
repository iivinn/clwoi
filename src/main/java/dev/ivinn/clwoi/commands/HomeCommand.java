package dev.ivinn.clwoi.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

public class HomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("home")
                        .executes(HomeCommand::goToHome)

                        .then(Commands.argument("homeName", StringArgumentType.word())
                                .executes(ctx -> goToNamedHome(ctx, StringArgumentType.getString(ctx, "homeName"))))
        );

        dispatcher.register(
                Commands.literal("homes")
                        .executes(HomeCommand::listAllHomes)
        );

        dispatcher.register(
                Commands.literal("sethome")
                        .executes(HomeCommand::setHome)

                        .then(Commands.argument("homeName", StringArgumentType.word())
                                .executes(ctx -> setNamedHome(ctx, StringArgumentType.getString(ctx, "homeName"))))
        );

        dispatcher.register(
                Commands.literal("delhome")
                        .then(Commands.argument("homeName", StringArgumentType.word())
                                .executes(ctx -> deleteHome(ctx, StringArgumentType.getString(ctx, "homeName"))))
        );

        dispatcher.register(
                Commands.literal("renamehome")
                        .then(Commands.argument("homeName", StringArgumentType.word())
                                .then(Commands.argument("newHomeName", StringArgumentType.word())
                                        .executes(ctx -> renameHome(ctx, StringArgumentType.getString(ctx, "homeName"), StringArgumentType.getString(ctx, "newHomeName")))))
        );


    }

    private static int setHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setNamedHome(ctx, "home");
    }

    private static int setNamedHome(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (homes.contains(homeName)) {
            player.sendSystemMessage(Component.literal("Home '" + homeName + "' already exists."));
            return 0;
        }

        CompoundTag pos = new CompoundTag();
        pos.putInt("x", player.blockPosition().getX());
        pos.putInt("y", player.blockPosition().getY());
        pos.putInt("z", player.blockPosition().getZ());

        homes.put(homeName, pos);
        data.put("homes", homes);

        player.sendSystemMessage(Component.literal("Home set: '" + homeName + "'"));

        return Command.SINGLE_SUCCESS;
    }

    private static int goToHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return goToNamedHome(ctx, "home");
    }

    private static int goToNamedHome(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (!homes.contains(homeName)) {
            player.sendSystemMessage(Component.literal("Home '" + homeName + "' does not exist."));
            return 0;
        }

        CompoundTag pos = homes.getCompound(homeName);
        int x = pos.getInt("x");
        int y = pos.getInt("y");
        int z = pos.getInt("z");

        player.teleportTo(x + 0.5, y, z + 0.5);
//        player.sendSystemMessage(Component.literal("Teleported to " + homeName + "!"));

        return Command.SINGLE_SUCCESS;
    }

    private static int deleteHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (!homes.contains(name)) {
            player.sendSystemMessage(Component.literal("Home '" + name + "' does not exist."));
            return 0;
        }

        homes.remove(name);
        player.sendSystemMessage(Component.literal("Removed home: '" + name + "'"));

        return Command.SINGLE_SUCCESS;
    }


    private static int renameHome(CommandContext<CommandSourceStack> ctx, String homeName, String newHomeName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (!homes.contains(homeName)) {
            player.sendSystemMessage(Component.literal("Home '" + homeName + "' does not exist."));
            return 0;
        }

        CompoundTag homeData = homes.getCompound(homeName);
        homes.remove(homeName);
        homes.put(newHomeName, homeData);
        data.put("homes", homes);

        player.sendSystemMessage(Component.literal("Home '" + homeName + "' renamed to '" + newHomeName + "'."));

        return Command.SINGLE_SUCCESS;
    }

    private static int listAllHomes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        for (String homeName : homes.getAllKeys()) {
            MutableComponent msg = Component.literal("- " + homeName + " ")
                    .append(
                            Component.literal("[Teleport]")
                                    .setStyle(Style.EMPTY
                                            .withColor(ChatFormatting.GREEN)
                                            .withUnderlined(true)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport!")))
                                    )
                    );

            player.sendSystemMessage(msg);
        }

        return Command.SINGLE_SUCCESS;
    }
}
