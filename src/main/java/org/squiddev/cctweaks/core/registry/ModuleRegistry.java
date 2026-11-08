package org.squiddev.cctweaks.core.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sequences the mod's {@link IModule}s through their lifecycle.
 *
 * <p>
 * This used to be static state on {@link Registry}, which made it impossible to test: once
 * {@code preInit} had run in one test, every later test inherited its phase flags. Making it an
 * instance lets a test drive a fresh registry with stub modules and assert the ordering and the
 * illegal phase transitions.
 * </p>
 */
public final class ModuleRegistry {

    private final Set<IModule> modules = new LinkedHashSet<IModule>();

    private boolean preInitialised = false;
    private boolean initialised = false;
    private boolean postInitialised = false;

    /**
     * Register a module. If the lifecycle has already started, the module catches up: a module
     * added during {@code init} still receives {@code preInit} and {@code init} immediately.
     *
     * @param module The module to add
     */
    public void add(IModule module) {
        modules.add(module);

        if (preInitialised && module.canLoad()) {
            module.preInit();
            if (initialised) {
                module.init();
                if (postInitialised) module.postInit();
            }
        }
    }

    public void preInit() {
        if (preInitialised) throw new IllegalStateException("Attempting to preInit twice");
        preInitialised = true;
        for (IModule module : snapshot()) {
            if (module.canLoad()) module.preInit();
        }
    }

    public void init() {
        if (!preInitialised) throw new IllegalStateException("Cannot init before preInit");
        if (initialised) throw new IllegalStateException("Attempting to init twice");

        initialised = true;
        for (IModule module : snapshot()) {
            if (module.canLoad()) module.init();
        }
    }

    public void postInit() {
        if (!preInitialised) throw new IllegalStateException("Cannot init before preInit");
        if (!initialised) throw new IllegalStateException("Cannot postInit before init");
        if (postInitialised) throw new IllegalStateException("Attempting to postInit twice");

        postInitialised = true;
        for (IModule module : snapshot()) {
            if (module.canLoad()) module.postInit();
        }
    }

    /**
     * @return An immutable snapshot of the registered modules, in registration order
     */
    public List<IModule> modules() {
        return Collections.unmodifiableList(new ArrayList<IModule>(modules));
    }

    /**
     * @return A copy, so a module registering another module cannot break the current pass
     */
    private List<IModule> snapshot() {
        return new ArrayList<IModule>(modules);
    }
}
