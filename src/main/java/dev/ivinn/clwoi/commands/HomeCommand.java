package dev.ivinn.clwoi.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class HomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                // Go to main home
                Commands.literal("home")
                        .executes(HomeCommand::goToHome)

                        // Set main home
                        .then(Commands.literal("set")
                                .executes(HomeCommand::setHome)

                                // Set named home
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> setNamedHome(ctx, StringArgumentType.getString(ctx, "name")))))

                        // Remove homes
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> removeHome(ctx, StringArgumentType.getString(ctx, "name")))))

                        // List all homes
                        .then(Commands.literal("listall").executes(HomeCommand::listAllHomes))

                        // Go to specific home
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> goToNamedHome(ctx, StringArgumentType.getString(ctx, "name"))))
        );
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return setNamedHome(ctx, "home");
    }

    private static int setNamedHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (homes.contains(name)) {
            player.sendSystemMessage(Component.literal("Home '" + name + "' already exists."));
            return 1;
        }

        CompoundTag pos = new CompoundTag();
        pos.putInt("x", player.blockPosition().getX());
        pos.putInt("y", player.blockPosition().getY());
        pos.putInt("z", player.blockPosition().getZ());

        homes.put(name, pos);
        data.put("homes", homes);

        player.sendSystemMessage(Component.literal("Home set: '" + name + "'"));

        return Command.SINGLE_SUCCESS;
    }

    private static int goToHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return goToNamedHome(ctx, "home");
    }

    private static int goToNamedHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (!homes.contains(name)) {
            player.sendSystemMessage(Component.literal("Home '" + name + "' does not exist."));
            return 1;
        }

        CompoundTag pos = homes.getCompound(name);
        int x = pos.getInt("x");
        int y = pos.getInt("y");
        int z = pos.getInt("z");

        player.teleportTo(x + 0.5, y, z + 0.5);
//        player.sendSystemMessage(Component.literal("Teleported to " + name + "!"));

        return Command.SINGLE_SUCCESS;
    }

    private static int removeHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (!homes.contains(name)) {
            player.sendSystemMessage(Component.literal("Home '" + name + "' does not exist."));
            return 1;
        }

        homes.remove(name);
        player.sendSystemMessage(Component.literal("Removed home: '" + name + "'"));

        return Command.SINGLE_SUCCESS;
    }

    private static int listAllHomes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();
        CompoundTag homes = data.getCompound("homes");

        if (homes.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no homes set."));
            return 1;
        }

        for (String homeName : homes.getAllKeys()) {
            player.sendSystemMessage(Component.literal("- " + homeName));
        }

        return Command.SINGLE_SUCCESS;
    }
}
