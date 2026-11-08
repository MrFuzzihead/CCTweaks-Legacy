package org.squiddev.cctweaks.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.world.IBlockAccess;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class WorldPositionTest {

    @Test
    public void equalForSameWorldAndCoordinates() {
        IBlockAccess world = Mockito.mock(IBlockAccess.class);
        WorldPosition a = new WorldPosition(world, 1, 2, 3);
        WorldPosition b = new WorldPosition(world, 1, 2, 3);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a);
    }

    @Test
    public void differentCoordinatesAreNotEqual() {
        IBlockAccess world = Mockito.mock(IBlockAccess.class);

        assertNotEquals(new WorldPosition(world, 1, 2, 3), new WorldPosition(world, 1, 2, 4));
        assertNotEquals(new WorldPosition(world, 1, 2, 3), new WorldPosition(world, 1, 3, 3));
        assertNotEquals(new WorldPosition(world, 1, 2, 3), new WorldPosition(world, 0, 2, 3));
    }

    @Test
    public void differentWorldIsNotEqual() {
        IBlockAccess first = Mockito.mock(IBlockAccess.class);
        IBlockAccess second = Mockito.mock(IBlockAccess.class);

        assertNotEquals(new WorldPosition(first, 1, 2, 3), new WorldPosition(second, 1, 2, 3));
    }

    @Test
    public void rejectsOtherTypesAndNull() {
        IBlockAccess world = Mockito.mock(IBlockAccess.class);
        WorldPosition position = new WorldPosition(world, 1, 2, 3);

        assertFalse(position.equals(null));
        assertFalse(position.equals("not a position"));
    }
}
