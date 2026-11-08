package org.squiddev.cctweaks.turtle;

import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import dan200.computercraft.shared.util.WorldUtil;

/**
 * Finds what a tool manipulator action would hit.
 *
 * <p>
 * Separated from {@link ToolManipulatorPeripheral} so headless tests can feed a fixed hit (or
 * {@code null}) instead of needing a real world to ray trace.
 * </p>
 */
public interface ToolHitResolver {

    /**
     * The real resolver: traces the block and entity in front of the given position.
     */
    ToolHitResolver DEFAULT = new ToolHitResolver() {

        @Override
        public MovingObjectPosition find(World world, double x, double y, double z, ForgeDirection facing,
            double range) {
            Vec3 origin = Vec3.createVectorHelper(x, y, z);
            Vec3 blockCenter = origin.addVector(facing.offsetX * 0.51, facing.offsetY * 0.51, facing.offsetZ * 0.51);
            Vec3 target = blockCenter.addVector(facing.offsetX * range, facing.offsetY * range, facing.offsetZ * range);

            MovingObjectPosition hit = world.rayTraceBlocks(origin, target);

            // WorldUtil treats its third argument as a direction and multiplies it by the distance,
            // so passing the endpoint here traces a ray into the distance instead of towards the
            // target block. Pass the relative vector and a distance of 1 instead.
            Vec3 direction = Vec3.createVectorHelper(
                target.xCoord - origin.xCoord,
                target.yCoord - origin.yCoord,
                target.zCoord - origin.zCoord);
            Entity entity = WorldUtil.rayTraceEntities(world, origin, direction, 1.0);

            if (entity != null) {
                return new MovingObjectPosition(entity);
            } else {
                return hit;
            }
        }
    };

    /**
     * @param world  The world to trace in
     * @param x      The player's X position
     * @param y      The player's Y position
     * @param z      The player's Z position
     * @param facing The direction the turtle is facing
     * @param range  How far past the adjacent block to look
     * @return The entity or block hit, or {@code null}
     */
    MovingObjectPosition find(World world, double x, double y, double z, ForgeDirection facing, double range);
}
