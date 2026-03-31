package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.lua.ILuaMachine;

@Mixin(value = Computer.class, remap = false)
public interface Computer_Accessor {

    @Accessor("m_machine")
    ILuaMachine getMachine();
}
