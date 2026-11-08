package org.squiddev.cctweaks.core.registry;

import java.util.List;

import org.squiddev.cctweaks.blocks.debug.BlockDebug;
import org.squiddev.cctweaks.core.peripheral.PeripheralHostProvider;
import org.squiddev.cctweaks.core.turtle.DefaultTurtleProviders;
import org.squiddev.cctweaks.integration.PortalGunIntegration;
import org.squiddev.cctweaks.items.ItemComputerUpgrade;
import org.squiddev.cctweaks.items.ItemToolHost;

/**
 * The proxy class
 */
public final class Registry {

    public static final ItemComputerUpgrade itemComputerUpgrade;
    public static final ItemToolHost itemToolHost;

    private static final ModuleRegistry registry = new ModuleRegistry();

    static {
        addModule(itemComputerUpgrade = new ItemComputerUpgrade());
        addModule(itemToolHost = new ItemToolHost());

        addModule(new BlockDebug());

        addModule(new PeripheralHostProvider());
        /*
         * addModule(new Module() {
         * @Override
         * public void init() {
         * ApiRegister.init();
         * }
         * });
         */

        addModule(new DefaultTurtleProviders());
        addModule(new PortalGunIntegration());

    }

    public static void addModule(IModule module) {
        registry.add(module);
    }

    public static void preInit() {
        registry.preInit();
    }

    public static void init() {
        registry.init();
    }

    public static void postInit() {
        registry.postInit();
    }

    /**
     * @return A snapshot of the registered modules, in registration order
     */
    public static List<IModule> modules() {
        return registry.modules();
    }
}
