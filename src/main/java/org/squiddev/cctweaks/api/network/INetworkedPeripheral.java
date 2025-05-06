package org.squiddev.cctweaks.api.network;

import java.util.Map;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface INetworkedPeripheral extends INetworkCompatiblePeripheral {

    void attachToNetwork(INetworkAccess var1, String var2);

    void detachFromNetwork(INetworkAccess var1, String var2);

    void networkInvalidated(INetworkAccess var1, Map<String, IPeripheral> var2, Map<String, IPeripheral> var3);

    void receivePacket(INetworkAccess var1, Packet var2, double var3);
}
