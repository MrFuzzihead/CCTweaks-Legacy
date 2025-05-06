package org.squiddev.cctweaks.api.network;

import java.util.Map;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface INetworkNode {

    Map<String, IPeripheral> getConnectedPeripherals();

    void receivePacket(Packet var1, double var2);

    void networkInvalidated(Map<String, IPeripheral> var1, Map<String, IPeripheral> var2);

    void detachFromNetwork();

    void attachToNetwork(INetworkController var1);

    INetworkController getAttachedNetwork();
}
