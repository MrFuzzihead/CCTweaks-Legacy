package org.squiddev.cctweaks.integration;

import org.squiddev.cctweaks.core.registry.Module;

import cpw.mods.fml.common.Loader;

/**
 * A module that is loaded when a mod is installed
 */
public abstract class ModIntegration extends Module {

    public final String modName;

    public ModIntegration(String modName) {
        this.modName = modName;
    }

    @Override
    public boolean canLoad() {
        return Loader.isModLoaded(modName);
    }
}
