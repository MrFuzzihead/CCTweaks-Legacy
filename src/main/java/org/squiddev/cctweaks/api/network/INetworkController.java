package org.squiddev.cctweaks.api.network;

import java.util.Map;
import java.util.Set;

import org.squiddev.cctweaks.api.UnorderedPair;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface INetworkController {

    void formConnection(INetworkNode var1, INetworkNode var2);

    void breakConnection(UnorderedPair<INetworkNode> var1);

    void removeNode(INetworkNode var1);

    Map<String, IPeripheral> getPeripheralsOnNetwork();

    void invalidateNetwork();

    void invalidateNode(INetworkNode var1);

    Set<INetworkNode> getNodesOnNetwork();

    Set<UnorderedPair<INetworkNode>> getNodeConnections();

    void transmitPacket(INetworkNode var1, Packet var2);
}
