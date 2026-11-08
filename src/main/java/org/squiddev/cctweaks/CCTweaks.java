package org.squiddev.cctweaks;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.squiddev.cctweaks.core.FmlEvents;
import org.squiddev.cctweaks.core.McEvents;
import org.squiddev.cctweaks.core.registry.Registry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.relauncher.Side;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.lua.lib.DelayedTasks;

@Mod(modid = CCTweaks.MODID, name = CCTweaks.NAME, version = CCTweaks.VERSION, dependencies = CCTweaks.DEPENDENCIES)
public class CCTweaks {

    public static final String MODID = "cctweaks";
    public static final String NAME = "CCTweaks";
    public static final String VERSION = Tags.VERSION;
    public static final String DEPENDENCIES = "required-after:ComputerCraft@[3.3.0,)";
    public static final Logger logger = LogManager.getLogger(MODID);

    public static CreativeTabs getCreativeTab() {
        return ComputerCraft.mainCreativeTab;
    }

    // region Config values
    // region computer
    public static boolean computerUpgradeEnabled = true;
    public static boolean computerUpgradeCrafting = true;
    public static boolean debugWandEnabled = true;
    public static boolean debugWandRequiresOp = false;
    // endregion
    // endregion
    // region testing
    public static boolean debugItems = false;
    // endregion
    // region turtle
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
        config.save();

        FMLCommonHandler.instance()
            .bus()
            .register(new FmlEvents());
        MinecraftForge.EVENT_BUS.register(new McEvents());

        Registry.preInit();
    }

    @EventHandler
    public void onMissingMappings(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.getAll()) {
            if (mapping.name.startsWith("cctweaks:")) {
                logger.warn("Discarding removed CCTweaks mapping '{}' (block/item was removed in 3.x)", mapping.name);
                mapping.warn();
            }
        }
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
            FmlEvents.reset();
            McEvents.reset();
            DelayedTasks.reset();
        }
    }

    @EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            FmlEvents.reset();
            McEvents.reset();
            DelayedTasks.reset();
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
        prop = config.get("computer", "debugWandRequiresOp", false);
        prop.comment = "Require operator permissions to inject the debug library. Recommended on public servers: "
            + "the Cobalt debug library allows scripts to reach outside the computer sandbox.";
        debugWandRequiresOp = prop.getBoolean(debugWandRequiresOp);
        // endregion
        // endregion
        // region testing
        prop = config.get("testing", "debugItems", false);
        prop.comment = "Enable debug items.";
        debugItems = prop.getBoolean(debugItems);
        // endregion
        // region turtle
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

        validateToolHostConfig();
    }

    static void validateToolHostConfig() {
        if (digFactor < 1) {
            logger.warn("turtle.toolhost.digFactor must be at least 1; using 1 (configured {})", digFactor);
            digFactor = 1;
        } else if (digFactor > 100) {
            logger.warn("turtle.toolhost.digFactor is unreasonably high; using 100 (configured {})", digFactor);
            digFactor = 100;
        }

        if (upgradeId < 64 || upgradeId > 32767) {
            logger.error(
                "turtle.toolhost.upgradeId {} is outside CC's custom range 64..32767; disabling Tool Host",
                upgradeId);
            thEnabled = false;
        }

        if (advancedUpgradeId < 64 || advancedUpgradeId > 32767 || advancedUpgradeId == upgradeId) {
            logger.error(
                "turtle.toolhost.advancedUpgradeId {} is invalid or duplicates upgradeId {}; disabling the advanced manipulator",
                advancedUpgradeId,
                upgradeId);
            advanced = false;
        }
    }
}
