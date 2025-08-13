package dev.ivinn.clwoi.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackCommand {
    public static class BackData {
        public ResourceKey<Level> dimension;
        public BlockPos pos;
        public float yaw;
        public float pitch;
        public long timestamp; // time in milliseconds

        public BackData(ResourceKey<Level> dimension, BlockPos pos, float yaw, float pitch, long timestamp) {
            this.dimension = dimension;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
        }
    }

    public static final Map<UUID, BackData> lastTeleport = new HashMap<>();
    public static final Map<UUID, BackData> lastDeath = new HashMap<>();

    public static void setLastTeleport(ServerPlayer player) {
        lastTeleport.put(player.getUUID(), new BackData(
                player.level().dimension(),
                player.blockPosition(),
                player.getYRot(),
                player.getXRot(),
                System.currentTimeMillis()
        ));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("back")
                    .executes(BackCommand::goBack)
        );
    }

    private static int goBack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        UUID id = player.getUUID();

        BackData teleportLoc = lastTeleport.get(id);
        BackData deathLoc = lastDeath.get(id);

        BackData backLoc = null;

        if (teleportLoc != null && deathLoc != null) {
            backLoc = teleportLoc.timestamp > deathLoc.timestamp ? teleportLoc : deathLoc;
        } else if (teleportLoc != null) {
            backLoc = teleportLoc;
        } else if (deathLoc != null) {
            backLoc = deathLoc;
        }

        if (backLoc == null) {
            player.sendSystemMessage(Component.literal("No location to return to."));
            return 0;
        }

        // Save current location as lastTeleport before moving (so /back can chain)
        setLastTeleport(player);

        ServerLevel targetWorld = player.server.getLevel(backLoc.dimension);
        if (targetWorld == null) {
            player.sendSystemMessage(Component.literal("That dimension no longer exists."));
            return 0;
        }

        player.teleportTo(
                targetWorld,
                backLoc.pos.getX() + 0.5,
                backLoc.pos.getY(),
                backLoc.pos.getZ() + 0.5,
                backLoc.yaw,
                backLoc.pitch
        );

        return Command.SINGLE_SUCCESS;
    }
}
