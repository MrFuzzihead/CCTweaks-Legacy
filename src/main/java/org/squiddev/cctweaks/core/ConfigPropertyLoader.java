package org.squiddev.cctweaks.core;

public final class ConfigPropertyLoader {

    public static void init() {
        Config.Computer.computerUpgradeEnabled = getBoolean("Config.Computer.computerUpgradeEnabled", true);
        Config.Computer.computerUpgradeCrafting = getBoolean("Config.Computer.computerUpgradeCrafting", true);
        Config.Computer.debugWandEnabled = getBoolean("Config.Computer.debugWandEnabled", true);
        Config.Turtle.ToolHost.enabled = getBoolean("Config.Turtle.ToolHost.enabled", true);
        Config.Turtle.ToolHost.advanced = getBoolean("Config.Turtle.ToolHost.advanced", true);
        Config.Turtle.ToolHost.crafting = getBoolean("Config.Turtle.ToolHost.crafting", true);
        Config.Turtle.ToolHost.upgradeId = getInt("Config.Turtle.ToolHost.upgradeId", 332);
        Config.Turtle.ToolHost.advancedUpgradeId = getInt("Config.Turtle.ToolHost.advancedUpgradeId", 333);
        Config.Turtle.ToolHost.digFactor = getInt("Config.Turtle.ToolHost.digFactor", 10);
        Config.Turtle.fluxRefuelAmount = getInt("Config.Turtle.fluxRefuelAmount", 100);
        Config.Turtle.euRefuelAmount = getInt("Config.Turtle.euRefuelAmount", 25);
        Config.Turtle.funNames = getBoolean("Config.Turtle.funNames", true);
        Config.Turtle.disabledActions = getStringList("Config.Turtle.disabledActions", new String[0]);
        Config.Network.WirelessBridge.enabled = getBoolean("Config.Network.WirelessBridge.enabled", true);
        Config.Network.WirelessBridge.crafting = getBoolean("Config.Network.WirelessBridge.crafting", true);
        Config.Network.WirelessBridge.turtleEnabled = getBoolean("Config.Network.WirelessBridge.turtleEnabled", true);
        Config.Network.WirelessBridge.turtleId = getInt("Config.Network.WirelessBridge.turtleId", 331);
        Config.Network.WirelessBridge.pocketEnabled = getBoolean("Config.Network.WirelessBridge.pocketEnabled", true);
        Config.Network.WirelessBridge.pocketId = getInt("Config.Network.WirelessBridge.pocketId", 331);
        Config.Network.fullBlockModemCrafting = getBoolean("Config.Network.fullBlockModemCrafting", true);
        Config.Integration.openPeripheralInventories = getBoolean("Config.Integration.openPeripheralInventories", true);
        Config.Integration.cbMultipart = getBoolean("Config.Integration.cbMultipart", true);
        Config.Misc.monitorLight = getInt("Config.Misc.monitorLight", 7);
        Config.Misc.advancedMonitorLight = getInt("Config.Misc.advancedMonitorLight", 10);
        Config.Testing.debugItems = getBoolean("Config.Testing.debugItems", false);
        Config.Testing.extendedControllerValidation = getBoolean("Config.Testing.extendedControllerValidation", false);
    }

    private static String getString(String name, String def) {
        String value = System.getProperty(name);
        return value == null ? def : value;
    }

    private static String[] getStringList(String name, String[] def) {
        String value = System.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return value.isEmpty() ? new String[0] : value.split(",");
        }
    }

    private static int getInt(String name, int def) {
        String value = System.getProperty(name);
        return value == null ? def : Integer.parseInt(value);
    }

    private static int[] getIntList(String name, int[] def) {
        String value = System.getProperty(name);
        if (value == null) {
            return def;
        } else if (value.isEmpty()) {
            return new int[0];
        } else {
            String[] values = value.split(",");
            int[] outs = new int[values.length];

            for (int i = 0; i < values.length; i++) {
                outs[i] = Integer.parseInt(values[i]);
            }

            return outs;
        }
    }

    private static double getDouble(String name, double def) {
        String value = System.getProperty(name);
        return value == null ? def : Double.parseDouble(value);
    }

    private static double[] getDoubleList(String name, double[] def) {
        String value = System.getProperty(name);
        if (value == null) {
            return def;
        } else if (value.isEmpty()) {
            return new double[0];
        } else {
            String[] values = value.split(",");
            double[] outs = new double[values.length];

            for (int i = 0; i < values.length; i++) {
                outs[i] = Double.parseDouble(values[i]);
            }

            return outs;
        }
    }

    private static boolean getBoolean(String name, boolean def) {
        String value = System.getProperty(name);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    private static boolean[] getBooleanList(String name, boolean[] def) {
        String value = System.getProperty(name);
        if (value == null) {
            return def;
        } else if (value.isEmpty()) {
            return new boolean[0];
        } else {
            String[] values = value.split(",");
            boolean[] outs = new boolean[values.length];

            for (int i = 0; i < values.length; i++) {
                outs[i] = Boolean.parseBoolean(values[i]);
            }

            return outs;
        }
    }
}
