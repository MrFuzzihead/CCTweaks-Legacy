package org.squiddev.cctweaks.api.network;

import java.util.Map;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface INetworkAccess {

    Map<String, IPeripheral> getPeripheralsOnNetwork();

    void invalidateNetwork();

    boolean transmitPacket(Packet var1);
}
