package dev.ivinn.clwoi;

//import com.mojang.logging.LogUtils;
import dev.ivinn.clwoi.commands.BackCommand;
import dev.ivinn.clwoi.commands.HomeCommands;
import dev.ivinn.clwoi.commands.TpaCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//import org.slf4j.Logger;

import static dev.ivinn.clwoi.commands.BackCommand.lastDeath;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Clwoi.MOD_ID)
public class Clwoi {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "clwoi";
    // Directly reference a slf4j logger
//    public static final Logger LOGGER = LogUtils.getLogger();

    public Clwoi(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for mod loading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CompoundTag oldData = event.getOriginal().getPersistentData();
            CompoundTag newData = event.getEntity().getPersistentData();

            if (oldData.contains("homes")) {
                newData.put("homes", oldData.getCompound("homes"));
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }

    @Mod.EventBusSubscriber
    public static class Events {

        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            HomeCommands.register(event.getDispatcher());
            TpaCommands.register(event.getDispatcher());
            BackCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                lastDeath.put(player.getUUID(), new BackCommand.BackData(
                        player.level().dimension(),
                        player.blockPosition(),
                        player.getYRot(),
                        player.getXRot(),
                        System.currentTimeMillis()
                ));
            }
        }
    }
}
