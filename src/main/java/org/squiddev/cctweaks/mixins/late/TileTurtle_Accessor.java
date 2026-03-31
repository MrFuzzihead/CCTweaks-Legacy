package org.squiddev.cctweaks.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.shared.turtle.blocks.TileTurtle;

@Mixin(value = TileTurtle.class, remap = false)
public interface TileTurtle_Accessor {

    @Accessor("m_moved")
    boolean getMoved();

    @Accessor("m_moved")
    void setMoved(boolean moved);
}

