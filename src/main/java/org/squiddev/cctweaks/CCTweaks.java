package org.squiddev.cctweaks;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.squiddev.cctweaks.commands.CommandCCTweaks;
import org.squiddev.cctweaks.core.FmlEvents;
import org.squiddev.cctweaks.core.McEvents;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.selfTest.InWorldSelfTest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
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
    public static final String DEPENDENCIES = "required-after:ComputerCraft@[3.3.0,)";
    public static final Logger logger = LogManager.getLogger(MODID);

    /**
     * The config GUI factory, loaded by name so the client-only class is never touched on a server.
     */
    public static final String GUI_FACTORY = "org.squiddev.cctweaks.client.gui.GuiConfigFactory";

    /**
     * The live configuration object. The GUI edits this in place and then calls
     * {@link #saveConfiguration()}, so it has to outlive {@link #preInit}.
     */
    private static Configuration configuration;

    public static CreativeTabs getCreativeTab() {
        return ComputerCraft.mainCreativeTab;
    }

    // region Config values
    // region computer
    public static boolean computerUpgradeEnabled = true;
    public static boolean computerUpgradeCrafting = true;
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
        readConfig(config);
        config.save();
        configuration = config;

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
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCCTweaks());
    }

    @EventHandler
    public void onServerStart(FMLServerStartedEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() != Side.SERVER) return;

        FmlEvents.reset();
        McEvents.reset();
        DelayedTasks.reset();

        // The headless harness (scripts/headless-smoke.sh, CI) sets this so the in-world checks
        // run without a player. It is off by default, so this is inert in normal play.
        if (Boolean.getBoolean("cctweaks.selftest")) runSelfTest();
    }

    private static void runSelfTest() {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null || server.worldServers.length == 0) {
                logger.warn("{} requested but no server world is loaded", InWorldSelfTest.MARKER);
                return;
            }
            InWorldSelfTest.atSpawn(server.worldServers[0])
                .run();
        } catch (Throwable throwable) {
            // A diagnostic must never take down server startup.
            logger.error("{} crashed", InWorldSelfTest.MARKER, throwable);
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

    /**
     * The configuration being edited by the running game, or {@code null} before pre-init.
     *
     * @return The live configuration, or {@code null} if the mod has not been pre-initialised yet
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Persist the live configuration and re-read every static config field from it. Called by the
     * config GUI after the user presses Done; the values it edits are written into the
     * {@link Configuration}'s {@link Property} objects, so saving is all that is needed to persist
     * them, and re-reading applies them without a restart where possible.
     */
    public static void saveConfiguration() {
        Configuration config = configuration;
        if (config == null) return;

        if (config.hasChanged()) config.save();
        readConfig(config);
    }

    static Property bool(Configuration config, String category, String name, boolean value, String comment) {
        Property prop = config.get(category, name, value);
        prop.comment = comment;
        prop.setLanguageKey("gui.config." + MODID + "." + category + "." + name);
        return prop;
    }

    static Property integer(Configuration config, String category, String name, int value, String comment) {
        Property prop = config.get(category, name, value);
        prop.comment = comment;
        prop.setLanguageKey("gui.config." + MODID + "." + category + "." + name);
        return prop;
    }

    /**
     * Read every config value out of {@code config} into the static fields, then validate them.
     *
     * <p>
     * Properties are read via {@link Configuration#get(String, String, boolean)} and the property's
     * own value, so this works both for the initial load and for re-reading after the GUI has
     * edited the properties in memory.
     * </p>
     *
     * @param config The configuration to read from. Not {@code null}.
     */
    static void readConfig(Configuration config) {
        // region computer
        computerUpgradeEnabled = bool(config, "computer", "computerUpgradeEnabled", true, "Enable upgrading computers.")
            .getBoolean(computerUpgradeEnabled);
        computerUpgradeCrafting = bool(
            config,
            "computer",
            "computerUpgradeCrafting",
            true,
            "Enable crafting the computer upgrade. Requires computerUpgradeEnabled.")
                .getBoolean(computerUpgradeCrafting);
        // endregion
        // region testing
        debugItems = bool(config, "testing", "debugItems", false, "Enable debug items.").getBoolean(debugItems);
        // endregion
        // region turtle.toolhost
        advanced = bool(config, "turtle.toolhost", "advanced", true, "Enable the Advanced Tool Host upgrade.")
            .getBoolean(advanced);
        advancedUpgradeId = integer(
            config,
            "turtle.toolhost",
            "advancedUpgradeId",
            333,
            "The Advanced Tool Host upgrade Id.").getInt(advancedUpgradeId);
        thCrafting = bool(
            config,
            "turtle.toolhost",
            "thCrafting",
            true,
            "Enable crafting the Tool Host upgrade. Requires advanced.").getBoolean(thCrafting);
        digFactor = integer(config, "turtle.toolhost", "digFactor", 10, "The digging speed factor for the Tool Host.")
            .getInt(digFactor);
        thEnabled = bool(config, "turtle.toolhost", "thEnabled", true, "Enable the Tool Host upgrade.")
            .getBoolean(thEnabled);
        upgradeId = integer(config, "turtle.toolhost", "upgradeId", 332, "The Tool Host upgrade Id.").getInt(upgradeId);
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
