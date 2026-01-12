package org.squiddev.cctweaks;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.squiddev.cctweaks.core.FmlEvents;
import org.squiddev.cctweaks.core.McEvents;
import org.squiddev.cctweaks.core.network.bridge.NetworkBindings;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.core.visualiser.NetworkPlayerWatcher;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.lua.lib.DelayedTasks;

@Mod(
    modid = CCTweaks.MODID,
    name = CCTweaks.NAME,
    version = CCTweaks.VERSION,
    dependencies = CCTweaks.DEPENDENCIES,
    guiFactory = CCTweaks.GUI_FACTORY)
public class CCTweaks {

    public static final String MODID = "cctweaks";
    public static final String NAME = "CCTweaks";
    public static final String VERSION = Tags.VERSION;
    public static final String DEPENDENCIES = "required-after:ComputerCraft@[1.78.0,);after:CCTurtle;after:ForgeMultipart;after:OpenPeripheralCore;after:OpenPeripheral";
    public static final Logger logger = LogManager.getLogger();

    public static final String ROOT_NAME = "org.squiddev.cctweaks.";
    public static final String GUI_FACTORY = ROOT_NAME + "client.gui.GuiConfigFactory";

    public static SimpleNetworkWrapper NETWORK;

    public static CreativeTabs getCreativeTab() {
        return ComputerCraft.mainCreativeTab;
    }

    // region Config values
    // region computer
    public static boolean computerUpgradeEnabled = true;
    public static boolean computerUpgradeCrafting = true;
    public static boolean debugWandEnabled = true;
    // endregion
    // region integration
    public static boolean cbMultipart = true;
    public static boolean openPeripheralInventories = true;
    // endregion
    // region network
    public static boolean fullBlockModemCrafting = true;
    // region wirelessbridge
    public static boolean wbCrafting = true;
    public static boolean wbEnabled = true;
    public static boolean pocketEnabled = true;
    public static int pocketId = 331;
    public static boolean turtleEnabled = true;
    public static int turtleId = 331;
    // endregion
    // endregion
    // region testing
    public static boolean debug = false;
    public static boolean debugItems = false;
    public static boolean extendedControllerValidation = false;
    // endregion
    // region turtle
    public static int euRefuelAmount = 25;
    public static int fluxRefuelAmount = 100;
    // region toolhost
    public static boolean advanced = true;
    public static int advancedUpgradeId = 333;
    public static boolean thCrafting = true;
    public static int digFactor = 10;
    public static boolean thEnabled = true;
    public static int upgradeId = 332;
    // endregion
    // endregion
    // endregion

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Config additions
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        setConfig(config);

        FMLCommonHandler.instance()
            .bus()
            .register(new FmlEvents());
        MinecraftForge.EVENT_BUS.register(new McEvents());

        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

        Registry.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        Registry.init();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Registry.postInit();
    }

    @EventHandler
    public void onServerStart(FMLServerStartedEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            DelayedTasks.reset();
            NetworkBindings.reset();
            NetworkPlayerWatcher.reset();
        }
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            DelayedTasks.reset();
            NetworkBindings.reset();
            NetworkPlayerWatcher.reset();
        }
    }

    private void setConfig(Configuration config) {
        Property prop;
        // region computer
        prop = config.get("computer", "computerUpgradeEnabled", true);
        prop.comment = "Enable upgrading computers.";
        computerUpgradeEnabled = prop.getBoolean(computerUpgradeEnabled);
        prop = config.get("computer", "computerUpgradeCrafting", true);
        prop.comment = "Enable crafting the computer upgrade. Requires computerUpgradeEnabled.";
        computerUpgradeCrafting = prop.getBoolean(computerUpgradeCrafting);
        prop = config.get("computer", "debugWandEnabled", true);
        prop.comment = "Enable using the debug wand.";
        debugWandEnabled = prop.getBoolean(debugWandEnabled);
        // endregion
        // region integration
        prop = config.get("integration", "cbMultipart", true);
        prop.comment = "Enable compatibility with ChickenBones' Multipart mod.";
        cbMultipart = prop.getBoolean(cbMultipart);
        prop = config.get("integration", "openPeripheralInventories", true);
        prop.comment = "Enable compatibility with OpenPeripheral's inventory modules.";
        openPeripheralInventories = prop.getBoolean(openPeripheralInventories);
        // endregion
        // region network
        prop = config.get("network", "fullBlockModemCrafting", true);
        prop.comment = "Enable crafting full block modems.";
        fullBlockModemCrafting = prop.getBoolean(fullBlockModemCrafting);
        // region wirelessbridge
        prop = config.get("network.wirelessbridge", "wbCrafting", true);
        prop.comment = "Enable crafting the wireless bridge.";
        wbCrafting = prop.getBoolean(wbCrafting);
        prop = config.get("network.wirelessbridge", "wbEnabled", true);
        prop.comment = "Enable the wireless bridge.";
        wbEnabled = prop.getBoolean(wbEnabled);
        prop = config.get("network.wirelessbridge", "pocketEnabled", true);
        prop.comment = "Enable the Wireless Bridge upgrade for pocket computers. Requires Peripherals++";
        pocketEnabled = prop.getBoolean(pocketEnabled);
        prop = config.get("network.wirelessbridge", "pocketId", 331);
        prop.comment = "The pocket upgrade Id. Requires Peripherals++";
        pocketId = prop.getInt(pocketId);
        prop = config.get("network.wirelessbridge", "turtleEnabled", true);
        prop.comment = "Enable the Wireless Bridge upgrade for turtles. Requires Peripherals++";
        turtleEnabled = prop.getBoolean(turtleEnabled);
        prop = config.get("network.wirelessbridge", "turtleId", 331);
        prop.comment = "The turtle upgrade Id. Requires Peripherals++";
        turtleId = prop.getInt(turtleId);
        // endregion
        // endregion
        // region testing
        prop = config.get("testing", "debug", false);
        prop.comment = "Enable debug mode.";
        debug = prop.getBoolean(debug);
        prop = config.get("testing", "debugItems", false);
        prop.comment = "Enable debug items.";
        debugItems = prop.getBoolean(debugItems);
        prop = config.get("testing", "extendedControllerValidation", false);
        prop.comment = "Enable extended controller validation.";
        extendedControllerValidation = prop.getBoolean(extendedControllerValidation);
        // endregion
        // region turtle
        prop = config.get("turtle", "euRefuelAmount", 25);
        prop.comment = "The amount of EU a single refuel action provides.";
        euRefuelAmount = prop.getInt(euRefuelAmount);
        prop = config.get("turtle", "fluxRefuelAmount", 100);
        prop.comment = "The amount of Flux a single refuel action provides.";
        fluxRefuelAmount = prop.getInt(fluxRefuelAmount);
        // region toolhost
        prop = config.get("turtle.toolhost", "advanced", true);
        prop.comment = "Enable the Advanced Tool Host upgrade.";
        advanced = prop.getBoolean(advanced);
        prop = config.get("turtle.toolhost", "advancedUpgradeId", 333);
        prop.comment = "The Advanced Tool Host upgrade Id.";
        advancedUpgradeId = prop.getInt(advancedUpgradeId);
        prop = config.get("turtle.toolhost", "thCrafting", true);
        prop.comment = "Enable crafting the Tool Host upgrade. Requires advanced.";
        thCrafting = prop.getBoolean(thCrafting);
        prop = config.get("turtle.toolhost", "digFactor", 10);
        prop.comment = "The digging speed factor for the Tool Host.";
        digFactor = prop.getInt(digFactor);
        prop = config.get("turtle.toolhost", "thEnabled", true);
        prop.comment = "Enable the Tool Host upgrade.";
        thEnabled = prop.getBoolean(thEnabled);
        prop = config.get("turtle.toolhost", "upgradeId", 332);
        prop.comment = "The Tool Host upgrade Id.";
        upgradeId = prop.getInt(upgradeId);
        // endregion
        // endregion
    }
}
