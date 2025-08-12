package dev.ivinn.clwoi.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TpaCommand {
    public static class TpaRequest {
        public UUID sender;
        public UUID target;
        public boolean here; // true = /tpahere, false = /tpa
        public long expiresAt;

        public TpaRequest(UUID sender, UUID target, boolean here, long expiresAt) {
            this.sender = sender;
            this.target = target;
            this.here = here;
            this.expiresAt = expiresAt;
        }
    }

    private static final Map<UUID, TpaRequest> requests = new HashMap<>();

    public static void sendRequest(UUID sender, UUID target, boolean here) {
        requests.put(target, new TpaRequest(sender, target, here, System.currentTimeMillis() + 60_000)); // 60 seconds
    }

    public static TpaRequest getRequest(UUID target) {
        return requests.get(target);
    }

    public static void removeRequest(UUID target) {
        requests.remove(target);
    }

    public static boolean isExpired(TpaRequest req) {
        return System.currentTimeMillis() > req.expiresAt;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tpa")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> sendTpaRequest(ctx, EntityArgument.getPlayer(ctx, "player"))))
        );

        dispatcher.register(
                Commands.literal("tpahere")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> sendTpaHereRequest(ctx, EntityArgument.getPlayer(ctx, "player"))))
        );

        dispatcher.register(
                Commands.literal("tpaccept").executes(TpaCommand::acceptTpa)
        );

        dispatcher.register(
                Commands.literal("tpdecline").executes(TpaCommand::declineTpa)
        );
    }

    private static int sendTpaRequest(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();

        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("You cannot send a TPA request to yourself."));
            return 0;
        }

        TpaCommand.sendRequest(sender.getUUID(), target.getUUID(), false);

        sender.sendSystemMessage(Component.literal("Sent TPA request to " + target.getName().getString()));

        MutableComponent msg = Component.literal(sender.getName().getString())
                .append(Component.literal(" has requested to teleport to you. "))
                .append(
                        Component.literal("[Accept] ")
                                .setStyle(Style.EMPTY
                                        .withColor(ChatFormatting.GREEN)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to accept!")))
                                )
                )
                .append(
                        Component.literal("[Decline]")
                                .setStyle(Style.EMPTY
                                        .withColor(ChatFormatting.RED)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdecline"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to decline!")))
                                ));

        target.sendSystemMessage(msg);

        return Command.SINGLE_SUCCESS;
    }

    private static int sendTpaHereRequest(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();

        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("You cannot send a TPA request to yourself."));
            return 0;
        }

        TpaCommand.sendRequest(sender.getUUID(), target.getUUID(), true);

        sender.sendSystemMessage(Component.literal("Sent TPAHere request to " + target.getName().getString()));

        MutableComponent msg = Component.literal(sender.getName().getString())
                .append(Component.literal(" has requested you teleport to them. "))
                .append(
                        Component.literal("[Accept] ")
                                .setStyle(Style.EMPTY
                                        .withColor(ChatFormatting.GREEN)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to accept!")))
                                )
                )
                .append(
                        Component.literal("[Decline]")
                                .setStyle(Style.EMPTY
                                        .withColor(ChatFormatting.RED)
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdecline"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to decline!")))
                ));

        target.sendSystemMessage(msg);

        return Command.SINGLE_SUCCESS;
    }

    private static int acceptTpa(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        TpaCommand.TpaRequest req = TpaCommand.getRequest(target.getUUID());

        if (req == null || TpaCommand.isExpired(req)) {
            target.sendSystemMessage(Component.literal("No valid TPA request found."));
            TpaCommand.removeRequest(target.getUUID());
            return 0;
        }

        ServerPlayer sender = Objects.requireNonNull(target.getServer()).getPlayerList().getPlayer(req.sender);
        if (sender == null) {
            target.sendSystemMessage(Component.literal("The player who sent the request is no longer online."));
            TpaCommand.removeRequest(target.getUUID());
            return 0;
        }

        if (req.here) {
            target.teleportTo(sender.serverLevel(), sender.getX(), sender.getY(), sender.getZ(), sender.getYRot(), sender.getXRot());
        } else {
            sender.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        }

        sender.sendSystemMessage(Component.literal("Teleport request accepted."));
        target.sendSystemMessage(Component.literal("Teleport request accepted."));

        TpaCommand.removeRequest(target.getUUID());
        return Command.SINGLE_SUCCESS;
    }

    private static int declineTpa(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        TpaCommand.TpaRequest req = TpaCommand.getRequest(target.getUUID());

        if (req == null) {
            target.sendSystemMessage(Component.literal("No TPA request to decline."));
            return 0;
        }

        ServerPlayer sender = Objects.requireNonNull(target.getServer()).getPlayerList().getPlayer(req.sender);
        if (sender != null) {
            sender.sendSystemMessage(Component.literal("Your TPA request was declined."));
        }

        target.sendSystemMessage(Component.literal("You declined the TPA request."));
        TpaCommand.removeRequest(target.getUUID());
        return Command.SINGLE_SUCCESS;
    }
}
