package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import dan200.computercraft.shared.computer.blocks.TileComputerBase;

@Mixin(value = TileComputerBase.class, remap = false)
public interface TileComputerBase_Invoker {

    @Invoker("transferStateFrom")
    void invokeTransferStateFrom(TileComputerBase source);
}
