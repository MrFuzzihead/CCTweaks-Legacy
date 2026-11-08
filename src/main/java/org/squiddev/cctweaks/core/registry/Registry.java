package org.squiddev.cctweaks.core.registry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.squiddev.cctweaks.blocks.debug.BlockDebug;
import org.squiddev.cctweaks.core.peripheral.PeripheralHostProvider;
import org.squiddev.cctweaks.core.turtle.DefaultTurtleProviders;
import org.squiddev.cctweaks.integration.PortalGunIntegration;
import org.squiddev.cctweaks.items.ItemComputerUpgrade;
import org.squiddev.cctweaks.items.ItemDebugger;
import org.squiddev.cctweaks.items.ItemToolHost;

/**
 * The proxy class
 */
public final class Registry {

    public static final ItemComputerUpgrade itemComputerUpgrade;
    public static final ItemDebugger itemDebugger;
    public static final ItemToolHost itemToolHost;

    private static final Set<IModule> modules = new LinkedHashSet<IModule>();

    private static boolean preInit = false;
    private static boolean init = false;
    private static boolean postInit = false;

    static {
        addModule(itemComputerUpgrade = new ItemComputerUpgrade());
        addModule(itemDebugger = new ItemDebugger());
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
        modules.add(module);

        if (preInit && module.canLoad()) {
            module.preInit();
            if (init) {
                module.init();
                if (postInit) module.postInit();
            }
        }
    }

    public static void preInit() {
        if (preInit) throw new IllegalStateException("Attempting to preInit twice");
        preInit = true;
        for (IModule module : new ArrayList<IModule>(modules)) {
            if (module.canLoad()) module.preInit();
        }
    }

    public static void init() {
        if (!preInit) throw new IllegalStateException("Cannot init before preInit");
        if (init) throw new IllegalStateException("Attempting to init twice");

        init = true;
        for (IModule module : new ArrayList<IModule>(modules)) {
            if (module.canLoad()) module.init();
        }
    }

    public static void postInit() {
        if (!preInit) throw new IllegalStateException("Cannot init before preInit");
        if (!init) throw new IllegalStateException("Cannot postInit before init");
        if (postInit) throw new IllegalStateException("Attempting to postInit twice");

        postInit = true;
        for (IModule module : new ArrayList<IModule>(modules)) {
            if (module.canLoad()) module.postInit();
        }
    }

}
