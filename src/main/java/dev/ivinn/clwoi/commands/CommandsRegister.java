package dev.ivinn.clwoi.commands;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CommandsRegister {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        HomeCommand.register(event.getDispatcher());
    }
}
