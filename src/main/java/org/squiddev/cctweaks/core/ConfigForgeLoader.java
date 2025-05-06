package org.squiddev.cctweaks.core;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class ConfigForgeLoader {

    private static Configuration configuration;

    public static void doSync() {
        Configuration config = configuration;
        Config.Computer.computerUpgradeEnabled = config
            .get("computer", "computerUpgradeEnabled", true, "Enable upgrading computers.")
            .setLanguageKey("gui.config.cctweaks.computer.computerUpgradeEnabled")
            .getBoolean();
        Config.Computer.computerUpgradeCrafting = config
            .get(
                "computer",
                "computerUpgradeCrafting",
                true,
                "Enable crafting the computer upgrade.\n Requires computerUpgradeEnabled.")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setLanguageKey("gui.config.cctweaks.computer.computerUpgradeCrafting")
            .getBoolean();
        Config.Computer.debugWandEnabled = config
            .get("computer", "debugWandEnabled", true, "Enable using the debug wand.")
            .setLanguageKey("gui.config.cctweaks.computer.debugWandEnabled")
            .getBoolean();
        config.getCategory("computer")
            .setLanguageKey("gui.config.cctweaks.computer")
            .setComment("Computer tweaks and items.");
        Config.Turtle.ToolHost.enabled = config.get("turtle.toolhost", "enabled", true, "Enable the Tool Host")
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost.enabled")
            .getBoolean();
        Config.Turtle.ToolHost.advanced = config.get("turtle.toolhost", "advanced", true, "Enable the Tool Manipulator")
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost.advanced")
            .getBoolean();
        Config.Turtle.ToolHost.crafting = config
            .get("turtle.toolhost", "crafting", true, "Enable crafting the Tool Host")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost.crafting")
            .getBoolean();
        Config.Turtle.ToolHost.upgradeId = config.get("turtle.toolhost", "upgradeId", 332, "Upgrade Id")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setMinValue(0)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost.upgradeId")
            .getInt();
        Config.Turtle.ToolHost.advancedUpgradeId = config
            .get("turtle.toolhost", "advancedUpgradeId", 333, "Upgrade Id for Tool Manipulator")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setMinValue(0)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost.advancedUpgradeId")
            .getInt();
        Config.Turtle.ToolHost.digFactor = config
            .get(
                "turtle.toolhost",
                "digFactor",
                10,
                "The dig speed factor for tool hosts.\n 20 is about normal player speed.")
            .setMinValue(1)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost.digFactor")
            .getInt();
        config.getCategory("turtle.toolhost")
            .setLanguageKey("gui.config.cctweaks.turtle.toolhost")
            .setComment("Various tool host options");
        Config.Turtle.fluxRefuelAmount = config
            .get("turtle", "fluxRefuelAmount", 100, "Amount of RF required for one refuel point\n Set to 0 to disable.")
            .setMinValue(0)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.turtle.fluxRefuelAmount")
            .getInt();
        Config.Turtle.euRefuelAmount = config
            .get("turtle", "euRefuelAmount", 25, "Amount of Eu required for one refuel point.\n Set to 0 to disable.")
            .setMinValue(0)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.turtle.euRefuelAmount")
            .getInt();
        Config.Turtle.funNames = config.get("turtle", "funNames", true, "Fun actions for turtle names")
            .setLanguageKey("gui.config.cctweaks.turtle.funNames")
            .getBoolean();
        Config.Turtle.disabledActions = config.get(
            "turtle",
            "disabledActions",
            new String[0],
            "Disabled turtle actions:\n (compare, compareTo, craft, detect, dig,\n drop, equip, inspect, move, place,\n refuel, select, suck, tool, turn).")
            .setLanguageKey("gui.config.cctweaks.turtle.disabledActions")
            .getStringList();
        config.getCategory("turtle")
            .setLanguageKey("gui.config.cctweaks.turtle")
            .setComment("Turtle tweaks and items.");
        Config.Network.WirelessBridge.enabled = config
            .get("network.wirelessbridge", "enabled", true, "Enable the wireless bridge")
            .setRequiresWorldRestart(true)
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge.enabled")
            .getBoolean();
        Config.Network.WirelessBridge.crafting = config
            .get("network.wirelessbridge", "crafting", true, "Enable the crafting of Wireless Bridges.")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge.crafting")
            .getBoolean();
        Config.Network.WirelessBridge.turtleEnabled = config
            .get("network.wirelessbridge", "turtleEnabled", true, "Enable the Wireless Bridge upgrade for turtles.")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge.turtleEnabled")
            .getBoolean();
        Config.Network.WirelessBridge.turtleId = config
            .get("network.wirelessbridge", "turtleId", 331, "The turtle upgrade Id")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setMinValue(1)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge.turtleId")
            .getInt();
        Config.Network.WirelessBridge.pocketEnabled = config
            .get(
                "network.wirelessbridge",
                "pocketEnabled",
                true,
                "Enable the Wireless Bridge upgrade for pocket computers.\n Requires Peripherals++")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge.pocketEnabled")
            .getBoolean();
        Config.Network.WirelessBridge.pocketId = config
            .get("network.wirelessbridge", "pocketId", 331, "The pocket upgrade Id\n Requires Peripherals++")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setMinValue(1)
            .setMaxValue(Integer.MAX_VALUE)
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge.pocketId")
            .getInt();
        config.getCategory("network.wirelessbridge")
            .setLanguageKey("gui.config.cctweaks.network.wirelessbridge")
            .setComment("The wireless bridge allows you to connect\n wired networks across dimensions.");
        Config.Network.fullBlockModemCrafting = config.get(
            "network",
            "fullBlockModemCrafting",
            true,
            "Enable the crafting of full block modems.\n\n If you disable, existing ones will still function,\n and you can obtain them from creative.")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setLanguageKey("gui.config.cctweaks.network.fullBlockModemCrafting")
            .getBoolean();
        config.getCategory("network")
            .setLanguageKey("gui.config.cctweaks.network")
            .setComment("Additional network functionality.");
        Config.Integration.openPeripheralInventories = config
            .get(
                "integration",
                "openPeripheralInventories",
                true,
                "Allows pushing items from one inventory\n to another inventory on the network.")
            .setLanguageKey("gui.config.cctweaks.integration.openPeripheralInventories")
            .getBoolean();
        Config.Integration.cbMultipart = config
            .get(
                "integration",
                "cbMultipart",
                true,
                "Enable ChickenBones Multipart\n (aka ForgeMultipart) integration.")
            .setLanguageKey("gui.config.cctweaks.integration.cbMultipart")
            .getBoolean();
        config.getCategory("integration")
            .setLanguageKey("gui.config.cctweaks.integration")
            .setRequiresWorldRestart(true)
            .setRequiresMcRestart(true)
            .setComment("Integration with other mods.");
        Config.Misc.monitorLight = config
            .get(
                "misc",
                "monitorLight",
                7,
                "The light level given off by normal monitors.\n Redstone torches are 7, normal torches are 14.")
            .setMinValue(0)
            .setMaxValue(15)
            .setLanguageKey("gui.config.cctweaks.misc.monitorLight")
            .getInt();
        Config.Misc.advancedMonitorLight = config
            .get(
                "misc",
                "advancedMonitorLight",
                10,
                "The light level given off by advanced monitors.\n Redstone torches are 7, normal torches are 14.")
            .setMinValue(0)
            .setMaxValue(15)
            .setLanguageKey("gui.config.cctweaks.misc.advancedMonitorLight")
            .getInt();
        config.getCategory("misc")
            .setLanguageKey("gui.config.cctweaks.misc")
            .setComment("Various tweaks that don't belong to anything");
        Config.Testing.debugItems = config
            .get("testing", "debugItems", false, "Enable debug blocks/items.\n Only use for testing.")
            .setLanguageKey("gui.config.cctweaks.testing.debugItems")
            .getBoolean();
        Config.Testing.extendedControllerValidation = config.get(
            "testing",
            "extendedControllerValidation",
            false,
            "Controller validation occurs by default as a\n way of ensuring that your network has been\n correctly created.\n\n By enabling this it is easier to trace\n faults, though it may slow things down\n slightly")
            .setLanguageKey("gui.config.cctweaks.testing.extendedControllerValidation")
            .getBoolean();
        config.getCategory("testing")
            .setLanguageKey("gui.config.cctweaks.testing")
            .setComment("Only used when testing and developing the mod.\n Nothing to see here, move along...");
        Config.onSync();
    }

    public static void init(File file) {
        configuration = new Configuration(file);
        configuration.load();
        sync();
    }

    public static void init(Configuration config) {
        configuration = config;
        sync();
    }

    public static void sync() {
        doSync();
        configuration.save();
    }

    public static Configuration getConfiguration() {
        return configuration;
    }
}
