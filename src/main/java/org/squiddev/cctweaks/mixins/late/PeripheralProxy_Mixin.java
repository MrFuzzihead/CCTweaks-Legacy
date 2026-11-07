package org.squiddev.cctweaks.mixins.late;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cctweaks.api.network.INetworkAccess;
import org.squiddev.cctweaks.api.network.INetworkedPeripheral;
import org.squiddev.cctweaks.api.network.Packet;
import org.squiddev.cctweaks.api.peripheral.IPeripheralProxy;
import org.squiddev.cctweaks.core.network.ComputerNetworkAccess;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import openperipheral.addons.peripheralproxy.WrappedPeripheral;

@Mixin(WrappedPeripheral.class)
public abstract class PeripheralProxy_Mixin implements INetworkedPeripheral, IPeripheralProxy {

    @Final
    @Shadow(remap = false)
    private IPeripheral peripheral;

    @Inject(at = @At("TAIL"), method = "attach", remap = false)
    public void cCTweaks_Legacy$attach(IComputerAccess computer, CallbackInfo ci) {
        if (peripheral instanceof INetworkedPeripheral) {
            ComputerNetworkAccess network = new ComputerNetworkAccess(computer);
            ((INetworkedPeripheral) peripheral).attachToNetwork(network, network.getName());
        }
    }

    @Inject(at = @At("HEAD"), method = "detach", remap = false)
    public void cCTweaks_Legacy$detach(IComputerAccess computer, CallbackInfo ci) {
        if (peripheral instanceof INetworkedPeripheral) {
            ComputerNetworkAccess network = new ComputerNetworkAccess(computer);
            ((INetworkedPeripheral) peripheral).detachFromNetwork(network, network.getName());
        }
    }

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
