package org.squiddev.cctweaks.mixins.late;

import dan200.computercraft.core.apis.PeripheralAPI;

public class PeripheralAPI_Accessor {

    public static Object[] getM_peripherals(PeripheralAPI instance) {
        try {
            java.lang.reflect.Field field = PeripheralAPI.class.getDeclaredField("m_peripherals");
            field.setAccessible(true);
            return (Object[]) field.get(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
