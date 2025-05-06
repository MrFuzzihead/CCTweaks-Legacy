package org.squiddev.cctweaks.api.network;

import org.squiddev.cctweaks.api.CCTweaksAPI;

public final class NetworkAPI {

    private static final INetworkRegistry REGISTRY = CCTweaksAPI.instance()
        .networkRegistry();
    private static final INetworkHelpers HELPERS = CCTweaksAPI.instance()
        .networkHelpers();

    public static INetworkRegistry registry() {
        return REGISTRY;
    }

    public static INetworkHelpers helpers() {
        return HELPERS;
    }
}
