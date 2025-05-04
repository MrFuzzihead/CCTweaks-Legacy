package org.squiddev.cctweaks;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;

public class Config {

    protected static final int MAX_ITEM_ID = 32000;

    public static Set<String> turtleDisabledActions;

    public class Computer {

        public static final String CATEGORY_NAME = "Computer";

        /**
         * Enable upgrading computers.
         */
        public static boolean computerUpgradeEnabled = true;
        /**
         * Enable crafting the computer upgrade.
         * Requires computerUpgradeEnabled.
         */
        public static boolean computerUpgradeCrafting = true;
        /**
         * Enable using the debug wand.
         */
        public static boolean debugWandEnabled = true;
    }

    public class Turtle {

        public static final String CATEGORY_NAME = "Turtle";

        /**
         * Amount of RF required for one refuel point
         * Set to 0 to disable.
         */
        public static int fluxRefuelAmount = 100; // 0-100+
        /**
         * Amount of Eu required for one refuel point.
         * Set to 0 to disable.
         */
        public static int euRefuelAmount = 25; // 0-25+
        /**
         * Fun actions for turtle names
         */
        public static boolean funNames = true;
        /**
         * Disabled turtle actions:
         * (compare, compareTo, craft, detect, dig,
         * drop, equip, inspect, move, place,
         * refuel, select, suck, tool, turn).
         */
        public static String[] disabledActions = new String[] {};

        public static class ToolHost {

            public static final String CATEGORY_NAME = "ToolHost";

            /**
             * Enable the Tool Host
             */
            public static boolean enabled = true;

            /**
             * Enable the Tool Manipulator
             */
            public static boolean advanced = true;

            /**
             * Enable crafting the Tool Host
             */
            public static boolean crafting = true;

            /**
             * Upgrade Id
             */
            public static int upgradeId = 332; // 0-332+

            /**
             * Upgrade Id for Tool Manipulator
             */
            public static int advancedUpgradeId = 333; // 0-333+

            /**
             * The dig speed factor for tool hosts.
             * 20 is about normal player speed.
             */
            public static int digFactor = 10; // 1-10+
        }
    }

    public class Network {

        public static final String CATEGORY_NAME = "Network";

        /**
         * The wireless bridge allows you to connect
         * wired networks across dimensions.
         */
        public static class WirelessBridge {

            public static final String CATEGORY_NAME = "WirelessBridge";

            /**
             * Enable the wireless bridge
             */
            public static boolean enabled = true;

            /**
             * Enable the crafting of Wireless Bridges.
             */
            public static boolean crafting = true;

            /**
             * Enable the Wireless Bridge upgrade for turtles.
             */
            public static boolean turtleEnabled = true;

            /**
             * The turtle upgrade Id
             */
            public static int turtleId = 331; // 1-331

            /**
             * Enable the Wireless Bridge upgrade for pocket computers.
             * Requires Peripherals++
             */
            public static boolean pocketEnabled = true;

            /**
             * The pocket upgrade Id
             * Requires Peripherals++
             */
            public static int pocketId = 331; // 1-331
        }

        /**
         * Enable the crafting of full block modems.
         *
         * If you disable, existing ones will still function,
         * and you can obtain them from creative.
         */
        public static boolean fullBlockModemCrafting = true;
    }

    public class Integration {

        public static final String CATEGORY_NAME = "Integration";

        /**
         * Allows pushing items from one inventory
         * to another inventory on the network.
         */
        public static boolean openPeripheralInventories = true;

        /**
         * Enable ChickenBones Multipart
         * (aka ForgeMultipart) integration.
         */
        public static boolean cbMultipart = true;
    }

    public class Misc {

        public static final String CATEGORY_NAME = "Misc";

        /**
         * The light level given off by normal monitors.
         * Redstone torches are 7, normal torches are 14.
         */
        public static int monitorLight = 7; // 0-15

        /**
         * The light level given off by advanced monitors.
         * Redstone torches are 7, normal torches are 14.
         */
        public static int advancedMonitorLight = 10; // 0-15
    }

    public class Testing {

        public static final String CATEGORY_NAME = "Testing";

        /**
         * Enable debug blocks/items.
         * Only use for testing.
         */
        public static boolean debugItems = false;

        /**
         * Controller validation occurs by default as a
         * way of ensuring that your network has been
         * correctly created.
         *
         * By enabling this it is easier to trace
         * faults, though it may slow things down
         * slightly
         */
        public static boolean extendedControllerValidation = false;
    }

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        sync();

        /**
         * Computer
         */
        Computer.computerUpgradeEnabled = configuration.getBoolean(
            "computerUpgradeEnabled",
            Computer.CATEGORY_NAME,
            Computer.computerUpgradeEnabled,
            "Enable upgrading computers");
        Computer.computerUpgradeCrafting = configuration.getBoolean(
            "computerUpgradeCrafting",
            Computer.CATEGORY_NAME,
            Computer.computerUpgradeCrafting,
            "Enable crafting the computer upgrade *requires computerUpgradeEnabled*");
        Computer.debugWandEnabled = configuration.getBoolean(
            "debugWandEnabled",
            Computer.CATEGORY_NAME,
            Computer.debugWandEnabled,
            "Enable using the debug wand");

        /**
         * Turtle
         */
        Turtle.fluxRefuelAmount = configuration.getInt(
            "fluxRefuelAmount",
            Turtle.CATEGORY_NAME,
            Turtle.fluxRefuelAmount,
            0,
            Integer.MAX_VALUE,
            "Amount of RF required for one refuel point, set to 0 to disable");
        Turtle.euRefuelAmount = configuration.getInt(
            "euRefuelAmount",
            Turtle.CATEGORY_NAME,
            Turtle.euRefuelAmount,
            0,
            Integer.MAX_VALUE,
            "Amount of Eu required for one refuel point, set to 0 to disable");
        Turtle.funNames = configuration
            .getBoolean("funNames", Turtle.CATEGORY_NAME, Turtle.funNames, "Fun actions for turtle names");
        Turtle.disabledActions = configuration.getStringList(
            "disabledActions",
            Turtle.CATEGORY_NAME,
            Turtle.disabledActions,
            "Disables turtle actions from being used");
        /**
         * Turtle > ToolHost
         */
        Turtle.ToolHost.enabled = configuration
            .getBoolean("enabled", Turtle.ToolHost.CATEGORY_NAME, Turtle.ToolHost.enabled, "Enable the Tool Host");
        Turtle.ToolHost.advanced = configuration.getBoolean(
            "advanced",
            Turtle.ToolHost.CATEGORY_NAME,
            Turtle.ToolHost.advanced,
            "Enable the Tool Manipulator");
        Turtle.ToolHost.crafting = configuration.getBoolean(
            "crafting",
            Turtle.ToolHost.CATEGORY_NAME,
            Turtle.ToolHost.crafting,
            "Enable crafting the Tool Host");
        Turtle.ToolHost.upgradeId = configuration.getInt(
            "upgradeId",
            Turtle.ToolHost.CATEGORY_NAME,
            Turtle.ToolHost.upgradeId,
            1,
            MAX_ITEM_ID,
            "Upgrade Id");
        Turtle.ToolHost.advancedUpgradeId = configuration.getInt(
            "advancedUpgradeId",
            Turtle.ToolHost.CATEGORY_NAME,
            Turtle.ToolHost.advancedUpgradeId,
            1,
            MAX_ITEM_ID,
            "Upgrade Id for Tool Manipulator");
        Turtle.ToolHost.digFactor = configuration.getInt(
            "digFactor",
            Turtle.ToolHost.CATEGORY_NAME,
            Turtle.ToolHost.digFactor,
            1,
            Integer.MAX_VALUE,
            "The dig speed factor for tool hosts, 20 is about normal player speed");

        /**
         * Network
         */
        Network.fullBlockModemCrafting = configuration.getBoolean(
            "fullBlockModemCrafting",
            Network.CATEGORY_NAME,
            Network.fullBlockModemCrafting,
            "Enable the crafting of full block modems. If you disable, existing ones will still function, and you can obtain them from creative");
        /**
         * WirelessBridge
         */
        Network.WirelessBridge.enabled = configuration.getBoolean(
            "enabled",
            Network.WirelessBridge.CATEGORY_NAME,
            Network.WirelessBridge.enabled,
            "Enable the wireless bridge");
        Network.WirelessBridge.crafting = configuration.getBoolean(
            "crafting",
            Network.WirelessBridge.CATEGORY_NAME,
            Network.WirelessBridge.crafting,
            "Enable the crafting of Wireless Bridges");
        Network.WirelessBridge.turtleEnabled = configuration.getBoolean(
            "turtleEnabled",
            Network.WirelessBridge.CATEGORY_NAME,
            Network.WirelessBridge.turtleEnabled,
            "Enable the Wireless Bridge upgrade for turtles");
        Network.WirelessBridge.turtleId = configuration.getInt(
            "turtleId",
            Network.WirelessBridge.CATEGORY_NAME,
            Network.WirelessBridge.turtleId,
            1,
            MAX_ITEM_ID,
            "The turtle upgrade Id");
        Network.WirelessBridge.pocketEnabled = configuration.getBoolean(
            "pocketEnabled",
            Network.WirelessBridge.CATEGORY_NAME,
            Network.WirelessBridge.pocketEnabled,
            "Enable the Wireless Bridge upgrade for pocket computers, requires Peripherals++");
        Network.WirelessBridge.pocketId = configuration.getInt(
            "pocketId",
            Network.WirelessBridge.CATEGORY_NAME,
            Network.WirelessBridge.pocketId,
            1,
            MAX_ITEM_ID,
            "The pocket upgrade Id, requires Peripherals++");

        /**
         * Integration
         */
        Integration.openPeripheralInventories = configuration.getBoolean(
            "openPeripheralInventories",
            Integration.CATEGORY_NAME,
            Integration.openPeripheralInventories,
            "Allows pushing items from one inventory to another inventory on the network");
        Integration.cbMultipart = configuration.getBoolean(
            "cbMultipart",
            Integration.CATEGORY_NAME,
            Integration.cbMultipart,
            "Enable ChickenBones Multipart (aka ForgeMultipart) integration");

        /**
         * Misc
         */
        Misc.monitorLight = configuration.getInt(
            "monitorLight",
            Misc.CATEGORY_NAME,
            Misc.monitorLight,
            0,
            15,
            "The light level given off by normal monitors. Redstone torches are 7, normal torches are 14");
        Misc.advancedMonitorLight = configuration.getInt(
            "advancedMonitorLight",
            Misc.CATEGORY_NAME,
            Misc.advancedMonitorLight,
            0,
            15,
            "The light level given off by advanced monitors. Redstone torches are 7, normal torches are 14");

        /**
         * Testing
         */
        Testing.debugItems = configuration.getBoolean(
            "debugItems",
            Testing.CATEGORY_NAME,
            Testing.debugItems,
            "Enable debug blocks/items, only use for testing");
        Testing.extendedControllerValidation = configuration.getBoolean(
            "extendedControllerValidation",
            Testing.CATEGORY_NAME,
            Testing.extendedControllerValidation,
            "Controller validation occurs by default as a way of ensuring that your network has been correctly created\nBy enabling this it is easier to trace faults, though it may slow things down slightly");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    protected static void sync() {
        // Handle generation of HashSets, etc...
        Set<String> disabledActions = turtleDisabledActions = new HashSet<>();
        for (String action : Turtle.disabledActions) {
            disabledActions.add(action.toLowerCase());
        }

        Computer.computerUpgradeCrafting &= Computer.computerUpgradeEnabled;

        Network.WirelessBridge.crafting &= Network.WirelessBridge.enabled;
        Network.WirelessBridge.turtleEnabled &= Network.WirelessBridge.enabled;
    }
}
