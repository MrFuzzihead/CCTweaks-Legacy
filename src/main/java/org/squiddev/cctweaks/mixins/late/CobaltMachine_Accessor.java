package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.core.lua.lib.cobalt.CobaltMachine;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaState;

@Mixin(value = CobaltMachine.class, remap = false)
public interface CobaltMachine_Accessor {

    @Accessor("state")
    LuaState getState();
}
