package org.squiddev.cctweaks.integration;

import org.squiddev.cctweaks.core.registry.Module;

import cpw.mods.fml.common.ModAPIManager;

/**
 * A module that is loaded when an API is on the class path
 */
public abstract class APIIntegration extends Module {

    public final String apiName;

    public APIIntegration(String modName) {
        this.apiName = modName;
    }

    @Override
    public boolean canLoad() {
        return ModAPIManager.INSTANCE.hasAPI(apiName);
    }
}
