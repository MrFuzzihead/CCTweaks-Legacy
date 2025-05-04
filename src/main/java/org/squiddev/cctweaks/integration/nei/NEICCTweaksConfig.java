package org.squiddev.cctweaks.integration.nei;

import org.squiddev.cctweaks.CCTweaks;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

public class NEICCTweaksConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.registerUsageHandler(new DescriptionHandler());
        API.registerRecipeHandler(new DescriptionHandler());
    }

    @Override
    public String getName() {
        return CCTweaks.NAME + ": NEI Integration";
    }

    @Override
    public String getVersion() {
        return CCTweaks.VERSION;
    }
}
