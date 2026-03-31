package org.squiddev.cctweaks.core.utils;

import java.lang.reflect.Field;

import dan200.computercraft.shared.peripheral.modem.TileCable;

/**
 * Reflection for stuff Dan200 doesn't let us do.
 *
 * @deprecated Fields in this class are being migrated to Mixin {@code @Accessor}/{@code @Invoker}
 *             interfaces. See {@code docs/remove-reflection.md}. Only entries pending Step 2
 *             remain here.
 */
@Deprecated
public final class ComputerAccessor {

    /**
     * The peripheral ID of a wired modem
     *
     * @see TileCable
     * @deprecated To be replaced by {@code TileCable_Accessor} in Step 2.
     */
    @Deprecated
    public static Field cablePeripheralId;

    static {
        try {
            cablePeripheralId = TileCable.class.getDeclaredField("m_attachedPeripheralID");
            cablePeripheralId.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
