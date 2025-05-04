package org.squiddev.cctweaks;

import static org.squiddev.cctweaks.CCTweaks.MODID;

import net.minecraftforge.common.MinecraftForge;

import org.squiddev.cctweaks.core.FmlEvents;
import org.squiddev.cctweaks.core.McEvents;
import org.squiddev.cctweaks.core.network.bridge.NetworkBindings;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.core.visualiser.NetworkPlayerWatcher;
import org.squiddev.cctweaks.lua.lib.DelayedTasks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class CommonProxy {

    public static SimpleNetworkWrapper NETWORK;

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        FMLCommonHandler.instance()
            .bus()
            .register(new FmlEvents());
        MinecraftForge.EVENT_BUS.register(new McEvents());

        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

        Registry.preInit();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        Registry.init();
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        Registry.postInit();
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            DelayedTasks.reset();
            NetworkBindings.reset();
            NetworkPlayerWatcher.reset();
        }
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStopping(FMLServerStoppingEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            DelayedTasks.reset();
            NetworkBindings.reset();
            NetworkPlayerWatcher.reset();
        }
    }
}
