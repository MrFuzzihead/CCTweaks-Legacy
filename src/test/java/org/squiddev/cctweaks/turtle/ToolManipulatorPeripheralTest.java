package org.squiddev.cctweaks.turtle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.squiddev.cctweaks.testsupport.StubComputerAccess;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaEnvironment;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.lua.lib.DelayedTasks;

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
    public void resolvesSideFromTheHitVectorWhenItIsMissing() {
        MovingObjectPosition hit = new MovingObjectPosition(0, 0, 0, -1, Vec3.createVectorHelper(0.5, 0.02, 0.5));

        // A ray that started inside the block leaves sideHit at -1; the nearest face is the answer.
        assertEquals(ForgeDirection.DOWN.ordinal(), ToolManipulatorPeripheral.resolveSide(hit));

        hit = new MovingObjectPosition(0, 0, 0, -1, Vec3.createVectorHelper(0.98, 0.5, 0.5));
        assertEquals(ForgeDirection.EAST.ordinal(), ToolManipulatorPeripheral.resolveSide(hit));

        hit = new MovingObjectPosition(0, 0, 0, -1, Vec3.createVectorHelper(0.5, 0.5, 0.03));
        assertEquals(ForgeDirection.NORTH.ordinal(), ToolManipulatorPeripheral.resolveSide(hit));
    }

    @Test
    public void keepsAnExplicitSide() {
        MovingObjectPosition hit = new MovingObjectPosition(
            0,
            0,
            0,
            ForgeDirection.UP.ordinal(),
            Vec3.createVectorHelper(0.1, 0.5, 0.5));
        assertEquals(ForgeDirection.UP.ordinal(), ToolManipulatorPeripheral.resolveSide(hit));
    }

    @Test
    public void itemBlocksNeedASurfaceToClick() {
        assertFalse(ToolManipulatorPeripheral.hasRightClickBehaviour(null));
        assertTrue(ToolManipulatorPeripheral.hasRightClickBehaviour(new ItemStack(new Item())));
        assertFalse(
            ToolManipulatorPeripheral
                .hasRightClickBehaviour(new ItemStack(Mockito.mock(ItemBlock.class, Mockito.CALLS_REAL_METHODS))));
    }

    @Test
    public void noDelayReturnsTheTaskResultAsIs() throws Exception {
        StubComputerAccess computer = new StubComputerAccess(1, "back");
        MethodResult result = MethodResult.of((Object) new Object[] { true, "block" });

        assertSame(result, ToolManipulatorPeripheral.afterDelay(computer, result, 0));
    }

    @Test
    public void durationWaitsForItsOwnTaskEvent() throws Exception {
        DelayedTasks.reset();
        StubComputerAccess computer = new StubComputerAccess(1, "back");

        // An immediate result: the delay task is queued straight away.
        MethodResult result = ToolManipulatorPeripheral.afterDelay(computer, MethodResult.of(true), 5);
        assertFalse(result.isImmediate(), "use(duration) must suspend");

        assertNull(result.resumeWith(new Object[] { ILuaEnvironment.EVENT_NAME, 99L, true }));

        // DelayedTasks ids are handed out from 1 after a reset, so the queued no-op task is #1.
        MethodResult resumed = result.resumeWith(new Object[] { ILuaEnvironment.EVENT_NAME, 1L, true });
        assertTrue(resumed != null && resumed.isImmediate());
        assertArrayEquals(new Object[] { true }, resumed.getResults());
    }

    @Test
    public void durationWaitsForTheActionBeforeTheDelay() throws Exception {
        DelayedTasks.reset();
        StubComputerAccess computer = new StubComputerAccess(1, "back");

        MethodResult action = MethodResult
            .pullEvent(event -> "ready".equals(event[0]) ? MethodResult.of(true, "place") : null);
        MethodResult result = ToolManipulatorPeripheral.afterDelay(computer, action, 3);

        // Still waiting on the action, not on the delay.
        assertNull(result.resumeWith(new Object[] { "other" }));

        // The action finishes: the delay is queued and the call keeps suspending.
        MethodResult afterAction = result.resumeWith(new Object[] { "ready" });
        assertTrue(afterAction != null && !afterAction.isImmediate());

        MethodResult done = afterAction.resumeWith(new Object[] { ILuaEnvironment.EVENT_NAME, 1L, true });
        assertTrue(done != null && done.isImmediate());
        assertArrayEquals(new Object[] { true, "place" }, done.getResults());
    }

    @Test
    public void everyMethodIsGatedByTurtleDisabledActions() throws Exception {
        String[] existing = ComputerCraft.turtleDisabledActions;
        ComputerCraft.turtleDisabledActions = new String[] { "tool" };
        try {
            ITurtleAccess access = Mockito.mock(ITurtleAccess.class);
            ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(access, null, TurtleSide.Left);
            StubComputerAccess computer = new StubComputerAccess(1, "back");

            for (int method = 0; method < peripheral.getMethodNames().length; method++) {
                Object[] result = peripheral.callMethod(computer, null, method, new Object[0]);
                assertTrue(result.length >= 1, peripheral.getMethodNames()[method] + " should return a value");
                assertEquals(
                    Boolean.FALSE,
                    unwrap(result[0]),
                    peripheral.getMethodNames()[method] + " should be disabled");
            }
        } finally {
            ComputerCraft.turtleDisabledActions = existing;
        }
    }

    private static Object unwrap(Object value) {
        if (value instanceof MethodResult) {
            MethodResult result = (MethodResult) value;
            assertTrue(result.isImmediate(), "disabled results must not suspend");
            return result.getResults()[0];
        }
        return value;
    }

    @Test
    public void disabledActionCheckIgnoresCaseAndNulls() {
        assertTrue(
            ToolManipulatorPeripheral.isActionDisabled("tool", new String[] { "TOOL" }),
            "CC config values are lower case, but pack-provided ones may not be");
        assertTrue(ToolManipulatorPeripheral.isActionDisabled("tool", new String[] { null, "tool" }));
        assertFalse(ToolManipulatorPeripheral.isActionDisabled("tool", new String[] { "dig", null }));
        assertFalse(ToolManipulatorPeripheral.isActionDisabled("tool", new String[0]));
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
