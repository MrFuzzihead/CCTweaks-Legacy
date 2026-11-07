package org.squiddev.cctweaks.blocks.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.testsupport.StubComputerAccess;

import dan200.computercraft.api.peripheral.IPeripheral;

public class TileDebugPeripheralTest {

    @Test
    public void cachesPerSideAndRejectsOutOfRangeSides() throws Exception {
        TileDebugPeripheral tile = new TileDebugPeripheral();

        assertNull(tile.getPeripheral(-1));
        assertNull(tile.getPeripheral(6));

        IPeripheral side0 = tile.getPeripheral(0);
        IPeripheral side1 = tile.getPeripheral(1);
        assertNotNull(side0);
        assertNotNull(side1);
        assertSame(side0, tile.getPeripheral(0));
        assertSame(side1, tile.getPeripheral(1));

        assertEquals("sided", side0.getType());
        assertEquals("DOWN", side0.callMethod(null, null, 0, new Object[0])[0]);
        assertEquals("UP", side1.callMethod(null, null, 0, new Object[0])[0]);
    }

    @Test
    public void tracksAttachedComputers() throws Exception {
        TileDebugPeripheral tile = new TileDebugPeripheral();
        TileDebugPeripheral.SidedPeripheral peripheral = (TileDebugPeripheral.SidedPeripheral) tile.getPeripheral(0);

        StubComputerAccess access = new StubComputerAccess(4, "back");
        peripheral.attach(access);
        peripheral.attach(access);
        assertEquals(
            1,
            peripheral.computers()
                .size());
        assertTrue(
            peripheral.computers()
                .contains(access));

        peripheral.detach(access);
        assertFalse(
            peripheral.computers()
                .contains(access));
    }

    @Test
    public void supportsOnlyTheGetSideMethod() throws Exception {
        TileDebugPeripheral.SidedPeripheral peripheral = new TileDebugPeripheral.SidedPeripheral(2);

        assertEquals(1, peripheral.getMethodNames().length);
        assertEquals("getSide", peripheral.getMethodNames()[0]);
        assertEquals("NORTH", peripheral.callMethod(null, null, 0, new Object[0])[0]);
    }
}
