package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import dan200.computercraft.core.lua.lib.cobalt.CobaltMachine;

@Mixin(value = CobaltMachine.class, remap = false)
public interface CobaltMachine_Accessor {

    @Accessor("state")
    LuaState getState();

    @Accessor("globals")
    LuaTable getGlobals();
}
