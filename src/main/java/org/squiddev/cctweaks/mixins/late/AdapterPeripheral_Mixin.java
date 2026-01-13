package org.squiddev.cctweaks.mixins.late;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.squiddev.cctweaks.api.network.INetworkAccess;
import org.squiddev.cctweaks.api.network.INetworkedPeripheral;
import org.squiddev.cctweaks.api.network.Packet;
import org.squiddev.cctweaks.api.peripheral.IPeripheralEnvironments;
import org.squiddev.cctweaks.core.network.NetworkAccessDelegate;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTargeted;
import openperipheral.adapter.IMethodCall;
import openperipheral.adapter.IMethodExecutor;
import openperipheral.interfaces.cc.ModuleComputerCraft;
import openperipheral.interfaces.cc.wrappers.AdapterPeripheral;

@Mixin(AdapterPeripheral.class)
public abstract class AdapterPeripheral_Mixin implements IPeripheralTargeted, INetworkedPeripheral {

    @Final
    @Shadow(remap = false)
    protected Object target;

    @Unique
    private NetworkAccessDelegate cCTweaks_Legacy$network;

    @Override
    public Object getTarget() {
        return target;
    }

    @Unique
    public NetworkAccessDelegate cCTweaks_Legacy$getNetworkAccess() {
        if (cCTweaks_Legacy$network == null) cCTweaks_Legacy$network = new NetworkAccessDelegate();
        return cCTweaks_Legacy$network;
    }

    /**
     * @author MrFuzzihead
     * @reason Injecting network environment
     */
    @Overwrite(remap = false)
    private IMethodCall prepareCall(IMethodExecutor executor, IComputerAccess computer, ILuaContext context) {
        IMethodCall call = executor.startCall(this.target);
        call = ModuleComputerCraft.ENV.addPeripheralArgs(call, computer, context);
        call.setEnv(IPeripheralEnvironments.ARG_NETWORK, cCTweaks_Legacy$getNetworkAccess());
        return call;
    }

    @Override
    public void attachToNetwork(INetworkAccess network, String name) {
        cCTweaks_Legacy$getNetworkAccess().add(network);
    }

    @Override
    public void detachFromNetwork(INetworkAccess network, String name) {
        cCTweaks_Legacy$getNetworkAccess().remove(network);
    }

    @Override
    public void networkInvalidated(INetworkAccess network, Map<String, IPeripheral> oldPeripherals,
        Map<String, IPeripheral> newPeripherals) {}

    @Override
    public void receivePacket(INetworkAccess network, Packet packet, double distanceTravelled) {}
}
