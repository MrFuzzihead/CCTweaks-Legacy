package org.squiddev.cctweaks.mixins.late;

import java.util.HashMap;
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

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.computer.Computer;

@Mixin(targets = "dan200.computercraft.core.apis.PeripheralAPI$PeripheralWrapper")
public abstract class PeripheralAPI_Mixin implements INetworkAccess, IComputerAccess {

    @Final
    @Shadow(remap = false)
    private String m_side;
    @Final
    @Shadow(remap = false)
    private IPeripheral m_peripheral;

    @Inject(at = @At("TAIL"), method = "attach", remap = false)
    public void cCTweaks_legacy$attach(CallbackInfo ci) {
        if (m_peripheral instanceof INetworkedPeripheral) {
            ((INetworkedPeripheral) m_peripheral).attachToNetwork(this, m_side);
        }
    }

    @Inject(at = @At("HEAD"), method = "detach", remap = false)
    public void cCTweaks_legacy$detach(CallbackInfo ci) {
        if (m_peripheral instanceof INetworkedPeripheral) {
            ((INetworkedPeripheral) m_peripheral).detachFromNetwork(this, m_side);
        }
    }

    @Override
    public Map<String, IPeripheral> getPeripheralsOnNetwork() {
        Map<String, IPeripheral> peripheralMap = new HashMap<>();
        PeripheralAPI outer = ((PeripheralWrapper_Accessor) this).getOuterPeripheralAPI();
        IAPIEnvironment env = ((PeripheralAPI_Accessor) outer).getEnvironment();
        for (int i = 0; i < 6; ++i) {
            IPeripheral peripheral = env.getPeripheral(i);
            if (peripheral != null) {
                peripheralMap.put(Computer.s_sideNames[i], peripheral);
            }
        }
        return peripheralMap;
    }

    @Override
    public void invalidateNetwork() {}

    @Override
    public boolean transmitPacket(Packet packet) {
        return false;
    }
}
