package org.squiddev.cctweaks.turtle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;

/**
 * Regression test for the entity ray direction: WorldUtil.rayTraceEntities multiplies its
 * direction argument by the distance, so passing the endpoint as a direction sent the ray off
 * into the distance.
 */
public class ToolManipulatorFindHitTest {

    @Test
    public void entityTraceEndsAtTheTargetNotBeyondIt() {
        World world = mock(World.class);
        when(
            world.rayTraceBlocks(
                org.mockito.ArgumentMatchers.any(Vec3.class),
                org.mockito.ArgumentMatchers.any(Vec3.class))).thenReturn(null);
        when(
            world.getEntitiesWithinAABBExcludingEntity(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(AxisAlignedBB.class))).thenReturn(Collections.<Entity>emptyList());

        ToolHostPlayer player = mock(ToolHostPlayer.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getX()).thenReturn(10.5);
        when(player.getY()).thenReturn(64.0);
        when(player.getZ()).thenReturn(10.5);

        ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(
            mock(ITurtleAccess.class),
            player,
            TurtleSide.Left);

        assertNull(peripheral.findHit(ForgeDirection.EAST, 0.65));

        ArgumentCaptor<Vec3> starts = ArgumentCaptor.forClass(Vec3.class);
        ArgumentCaptor<Vec3> ends = ArgumentCaptor.forClass(Vec3.class);
        verify(world, atLeast(2)).rayTraceBlocks(starts.capture(), ends.capture());

        Vec3 lastEnd = ends.getAllValues()
            .get(
                ends.getAllValues()
                    .size() - 1);
        // origin 10.5 + 0.51 (block centre) + 0.65 (range) = 11.66
        assertEquals(11.66, lastEnd.xCoord, 0.001);
        assertEquals(10.5, lastEnd.zCoord, 0.001);
    }
}
