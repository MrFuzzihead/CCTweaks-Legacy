package org.squiddev.cctweaks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.api.ICCTweaksAPI;
import org.squiddev.cctweaks.api.peripheral.IPeripheralHelpers;
import org.squiddev.cctweaks.api.turtle.ITurtleInteraction;
import org.squiddev.cctweaks.api.turtle.ITurtleRegistry;

import dan200.computercraft.api.lua.ILuaEnvironment;
import dan200.computercraft.api.turtle.ITurtleAccess;

/**
 * Pins the public addon-facing surface. A signature change here is a breaking API change and needs
 * a deliberate decision, not a silent edit.
 */
public class ApiSurfaceTest {

    @Test
    public void modIdentityIsStable() {
        assertEquals("cctweaks", CCTweaks.MODID);
        assertEquals("CCTweaks", CCTweaks.NAME);
        assertEquals("required-after:ComputerCraft@[3.3.0,)", CCTweaks.DEPENDENCIES);
    }

    @Test
    public void apiEntryPointsHaveTheExpectedSignatures() throws Exception {
        assertMethod(ICCTweaksAPI.class, "turtleRegistry", ITurtleRegistry.class);
        assertMethod(ICCTweaksAPI.class, "peripheralHelpers", IPeripheralHelpers.class);
        assertMethod(ICCTweaksAPI.class, "luaEnvironment", ILuaEnvironment.class);

        assertMethod(ITurtleRegistry.class, "registerInteraction", void.class, ITurtleInteraction.class);
        assertMethod(
            ITurtleRegistry.class,
            "registerInteraction",
            void.class,
            net.minecraft.item.Item.class,
            ITurtleInteraction.class);

        assertMethod(
            IPeripheralHelpers.class,
            "getBasePeripheral",
            dan200.computercraft.api.peripheral.IPeripheral.class,
            dan200.computercraft.api.peripheral.IPeripheral.class);
        assertMethod(
            IPeripheralHelpers.class,
            "getTarget",
            Object.class,
            dan200.computercraft.api.peripheral.IPeripheral.class);
    }

    @Test
    public void turtleInteractionSurfaceIsPinned() {
        List<String> methods = Arrays.stream(ITurtleInteraction.class.getDeclaredMethods())
            .map(Method::getName)
            .sorted()
            .collect(Collectors.toList());

        assertEquals(Arrays.asList("canSwing", "canUse", "swing", "use"), methods);
        assertNotNull(ITurtleAccess.class);
    }

    private static void assertMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... parameters)
        throws Exception {
        Method method = owner.getMethod(name, parameters);
        assertEquals(returnType, method.getReturnType());
    }
}
