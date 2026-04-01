package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.PeripheralAPI;

/**
 * Exposes {@code PeripheralAPI.m_environment} so that {@link PeripheralAPI_Mixin} can call
 * {@link IAPIEnvironment#getPeripheral(int)} for each side, avoiding any need to access the
 * private {@code PeripheralWrapper[]} array directly.
 */
@Mixin(value = PeripheralAPI.class, remap = false)
public interface PeripheralAPI_Accessor {

    @Accessor("m_environment")
    IAPIEnvironment getEnvironment();
}
