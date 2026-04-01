package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.PeripheralAPI;

@Mixin(targets = "dan200.computercraft.core.apis.PeripheralAPI$PeripheralWrapper", remap = false)
public interface PeripheralWrapper_Accessor {

    @Accessor("m_peripheral")
    IPeripheral getM_peripheral();

    @Accessor("this$0")
    PeripheralAPI getOuterPeripheralAPI();
}
