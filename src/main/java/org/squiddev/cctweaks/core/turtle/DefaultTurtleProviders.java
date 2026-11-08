package org.squiddev.cctweaks.core.turtle;

import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.core.registry.Module;

/**
 * Registers turtle related things
 */
public class DefaultTurtleProviders extends Module {

    @Override
    public void init() {
        // A bucket counts as a tool for a tool-host turtle: it fills from a fluid source and empties
        // against a block. See BucketInteraction - it used to answer canUse and do nothing.
        CCTweaksAPI.instance()
            .turtleRegistry()
            .registerInteraction(new BucketInteraction());
    }
}
