package org.squiddev.cctweaks.integration;

import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.turtle.ITurtleRegistry;

import cpw.mods.fml.common.Optional;

/**
 * Adds turtle interaction for the portal gun mod
 */
public class PortalGunIntegration extends ModIntegration {

    public static final String MOD_NAME = "PortalGun";

    public PortalGunIntegration() {
        super(MOD_NAME);
    }

    @Override
    @Optional.Method(modid = MOD_NAME)
    public void init() {
        ITurtleRegistry registry = CCTweaksAPI.instance()
            .turtleRegistry();

        registry.registerInteraction(new PortalGunInteraction());
    }
}
