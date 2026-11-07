package org.squiddev.cctweaks.turtle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;

public class ToolManipulatorPeripheralTest {

    @Test
    public void methodNamesAreStable() {
        ITurtleAccess access = Mockito.mock(ITurtleAccess.class);
        ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(access, null, TurtleSide.Left);

        String[] names = peripheral.getMethodNames();
        assertEquals(12, names.length);
        assertSame(names, peripheral.getMethodNames());
        assertEquals("use", names[0]);
        assertEquals("canSwingDown", names[11]);
        assertEquals("tool_manipulator", peripheral.getType());
    }

    @Test
    public void parsesDuration() throws LuaException {
        assertEquals(0, ToolManipulatorPeripheral.parseDuration(new Object[0]));
        assertEquals(0, ToolManipulatorPeripheral.parseDuration(new Object[] { null }));
        assertEquals(7, ToolManipulatorPeripheral.parseDuration(new Object[] { 7 }));
        assertEquals(7, ToolManipulatorPeripheral.parseDuration(new Object[] { 7.9d }));

        assertThrows(LuaException.class, () -> ToolManipulatorPeripheral.parseDuration(new Object[] { "7" }));
        assertThrows(LuaException.class, () -> ToolManipulatorPeripheral.parseDuration(new Object[] { -1 }));
    }

    @Test
    public void parsesSneak() throws LuaException {
        assertFalse(ToolManipulatorPeripheral.parseSneak(new Object[0], 0));
        assertFalse(ToolManipulatorPeripheral.parseSneak(new Object[] { null }, 0));
        assertTrue(ToolManipulatorPeripheral.parseSneak(new Object[] { true }, 0));
        assertTrue(ToolManipulatorPeripheral.parseSneak(new Object[] { 1, true }, 1));

        assertThrows(LuaException.class, () -> ToolManipulatorPeripheral.parseSneak(new Object[] { "true" }, 0));
        assertThrows(LuaException.class, () -> ToolManipulatorPeripheral.parseSneak(new Object[] { 1, 2 }, 1));
    }

    @Test
    public void detectsDisabledActions() {
        assertTrue(ToolManipulatorPeripheral.isActionDisabled("dig", new String[] { "dig" }));
        assertTrue(ToolManipulatorPeripheral.isActionDisabled("DIG", new String[] { "dig" }));
        assertFalse(ToolManipulatorPeripheral.isActionDisabled("dig", new String[] { "place" }));
        assertFalse(ToolManipulatorPeripheral.isActionDisabled("dig", new String[] { null }));
        assertFalse(ToolManipulatorPeripheral.isActionDisabled("dig", new String[0]));
    }

    @Test
    public void convertsTurtleResults() {
        assertArrayEquals(
            new Object[] { true },
            ToolManipulatorPeripheral.toObjectArray(TurtleCommandResult.success()));
        assertArrayEquals(
            new Object[] { true, "value" },
            ToolManipulatorPeripheral.toObjectArray(TurtleCommandResult.success(new Object[] { "value" })));
        assertArrayEquals(
            new Object[] { false },
            ToolManipulatorPeripheral.toObjectArray(TurtleCommandResult.failure()));
        assertArrayEquals(
            new Object[] { false, "broken" },
            ToolManipulatorPeripheral.toObjectArray(TurtleCommandResult.failure("broken")));
    }

    @Test
    public void equalsIncludesSide() {
        ITurtleAccess access = Mockito.mock(ITurtleAccess.class);
        ITurtleAccess other = Mockito.mock(ITurtleAccess.class);

        ToolManipulatorPeripheral left = new ToolManipulatorPeripheral(access, null, TurtleSide.Left);
        ToolManipulatorPeripheral left2 = new ToolManipulatorPeripheral(access, null, TurtleSide.Left);
        ToolManipulatorPeripheral right = new ToolManipulatorPeripheral(access, null, TurtleSide.Right);
        ToolManipulatorPeripheral otherLeft = new ToolManipulatorPeripheral(other, null, TurtleSide.Left);

        assertEquals(left, left);
        assertEquals(left, left2);
        assertEquals(left.hashCode(), left2.hashCode());
        assertNotEquals(left, right);
        assertNotEquals(left, otherLeft);
        assertNotEquals(left, null);
    }
}
