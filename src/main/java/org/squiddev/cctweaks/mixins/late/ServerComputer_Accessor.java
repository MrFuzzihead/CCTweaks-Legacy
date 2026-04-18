package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.computer.core.ServerComputer;

@Mixin(value = ServerComputer.class, remap = false)
public interface ServerComputer_Accessor {

    @Accessor("m_computer")
    Computer getComputer();
}
