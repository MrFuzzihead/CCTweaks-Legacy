package org.squiddev.cctweaks.core.network.mock;

import java.util.UUID;

import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.network.bridge.NetworkBinding;

public class BoundNetworkNode extends KeyedNetworkNode {

    public static final UUID id = UUID.randomUUID();

    public final NetworkBinding binding = new NetworkBinding(position);

    public BoundNetworkNode(IWorldPosition position, String character) {
        super(position, character);
        binding.setUuid(id);
    }

    @Override
    public void connect() {
        super.connect();
        binding.connect();
        getAttachedNetwork().formConnection(this, binding);
    }

    static {
        Config.Network.WirelessBridge.enabled = true;
    }
}
