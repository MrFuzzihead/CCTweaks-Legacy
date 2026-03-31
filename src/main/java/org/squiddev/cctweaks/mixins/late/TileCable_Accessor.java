package org.squiddev.cctweaks.mixins.late;

import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dan200.computercraft.shared.peripheral.modem.TileCable;

@Mixin(value = TileCable.class, remap = false)
public interface TileCable_Accessor {

    @Accessor("m_attachedPeripheralID")
    int getAttachedPeripheralID();

    @Accessor("s_cableIcons")
    static IIcon[] getS_cableIcons() {
        throw new AssertionError();
    }

    @Accessor("s_modemIcons")
    static IIcon[] getS_modemIcons() {
        throw new AssertionError();
    }
}
