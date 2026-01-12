package org.squiddev.cctweaks.mixins.late;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cctweaks.api.network.INetworkAccess;
import org.squiddev.cctweaks.api.network.INetworkedPeripheral;
import org.squiddev.cctweaks.api.network.Packet;
import org.squiddev.cctweaks.api.peripheral.IPeripheralProxy;

import dan200.computercraft.api.peripheral.IPeripheral;
import openperipheral.addons.peripheralproxy.WrappedPeripheral;

@Mixin(WrappedPeripheral.class)
public abstract class PeripheralProxy_Mixin implements INetworkedPeripheral, IPeripheralProxy {

    @Final
    @Shadow(remap = false)
    private IPeripheral peripheral;

    @Override
    public void attachToNetwork(INetworkAccess network, String name) {
        if (peripheral instanceof INetworkedPeripheral) {
            ((INetworkedPeripheral) peripheral).attachToNetwork(network, name);
        }
    }

    @Override
    public void detachFromNetwork(INetworkAccess network, String name) {
        if (peripheral instanceof INetworkedPeripheral) {
            ((INetworkedPeripheral) peripheral).detachFromNetwork(network, name);
        }
    }

    @Override
    public void networkInvalidated(INetworkAccess network, Map<String, IPeripheral> oldPeripherals,
        Map<String, IPeripheral> newPeripherals) {
        if (peripheral instanceof INetworkedPeripheral) {
            ((INetworkedPeripheral) peripheral).networkInvalidated(network, oldPeripherals, newPeripherals);
        }
    }

    @Override
    public void receivePacket(INetworkAccess network, Packet packet, double distanceTravelled) {
        if (peripheral instanceof INetworkedPeripheral) {
            ((INetworkedPeripheral) peripheral).receivePacket(network, packet, distanceTravelled);
        }
    }

    @Override
    public IPeripheral getBasePeripheral() {
        return peripheral;
    }
}
