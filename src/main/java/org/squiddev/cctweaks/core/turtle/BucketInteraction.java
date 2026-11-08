package org.squiddev.cctweaks.core.turtle;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Facing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import org.squiddev.cctweaks.api.turtle.AbstractTurtleInteraction;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

/**
 * Lets a tool-host turtle use a bucket as its tool: fill it from a fluid source, or empty it into
 * the block in front of the turtle.
 *
 * <p>
 * The interaction is deliberately faithful to vanilla {@link ItemBucket}: filling only takes a
 * <em>source</em> block, and emptying targets the block on the clicked side of the hit (so the
 * turtle can pour a bucket against a wall, exactly like a player right-clicking it).
 * </p>
 *
 * <p>
 * One extension: with no block hit at all, a filled bucket can still be emptied into the cell in
 * front of the turtle <em>if that cell has solid support underneath it</em>. A turtle cannot aim
 * diagonally down like a player can, so without this it could never pour onto a floor one block
 * below and in front of it - the exact spot a player would reach by clicking the top face of that
 * floor. Unsupported (mid-air) placement stays refused, as it would for a player right-clicking
 * the air.
 * </p>
 *
 * <p>
 * Fluid handling goes through {@link Actions} so the decision tree - which position is targeted,
 * protection, inventory bookkeeping - can be tested without Forge's fluid registry; the
 * {@link ForgeActions} implementation is what runs in game and is exercised by the in-world self
 * test.
 * </p>
 */
public class BucketInteraction extends AbstractTurtleInteraction {

    /**
     * The fluid/block operations the interaction needs.
     */
    public interface Actions {

        /**
         * @return Whether the stack is any kind of bucket/fluid container (empty or full)
         */
        boolean isBucket(ItemStack stack);

        /**
         * @return Whether the container currently holds a fluid
         */
        boolean isFilled(ItemStack stack);

        /**
         * @return Whether this block can be drained into the container, without changing the world
         */
        boolean canFill(World world, int x, int y, int z, ItemStack empty);

        /**
         * @return Whether the fluid in the container could be placed at this position, without
         *         changing the world
         */
        boolean canPlace(World world, int x, int y, int z, ItemStack filled);

        /**
         * @return Whether the position has solid support underneath it, so pouring there is the
         *         same action as clicking the top face of the block below
         */
        boolean isSupported(World world, int x, int y, int z);

        /**
         * Drain the source block into the container.
         *
         * @return The filled container, or {@code null} if nothing was drained
         */
        ItemStack fill(World world, int x, int y, int z, ItemStack empty);

        /**
         * Place the container's fluid and empty it.
         *
         * @return The emptied container, or {@code null} if nothing was placed
         */
        ItemStack place(World world, int x, int y, int z, ItemStack filled);
    }

    private final Actions actions;

    public BucketInteraction() {
        this(new ForgeActions());
    }

    BucketInteraction(Actions actions) {
        this.actions = actions;
    }

    @Override
    public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        if (!actions.isBucket(stack)) return false;

        World world = turtle.getWorld();
        if (actions.isFilled(stack)) {
            int[] target = placeTarget(turtle, direction, hit);
            if (target == null) return false;

            return isEditable(world, target[0], target[1], target[2], player)
                && actions.canPlace(world, target[0], target[1], target[2], stack);
        }

        if (!isBlockHit(hit)) return false;
        return isEditable(world, hit.blockX, hit.blockY, hit.blockZ, player)
            && actions.canFill(world, hit.blockX, hit.blockY, hit.blockZ, stack);
    }

    @Override
    public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) {
        if (!actions.isBucket(stack)) return null;

        World world = turtle.getWorld();

        if (actions.isFilled(stack)) {
            // Empty into the block on the clicked side, like vanilla ItemBucket: right-clicking a
            // wall pours the fluid into the space next to it, not into the wall.
            int[] target = placeTarget(turtle, direction, hit);
            if (target == null) return null;

            int x = target[0], y = target[1], z = target[2];

            if (!isEditable(world, x, y, z, player)) return TurtleCommandResult.failure("Cannot use protected block");

            ItemStack emptied = actions.place(world, x, y, z, stack);
            if (emptied == null) return null;

            replaceHeld(player, stack, emptied);
            return TurtleCommandResult.success(new Object[] { "bucket", "place" });
        }

        if (!isBlockHit(hit)) return null;

        // Fill from the block itself; a source block is what has to be drained.
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (!isEditable(world, x, y, z, player)) return TurtleCommandResult.failure("Cannot use protected block");

        ItemStack filled = actions.fill(world, x, y, z, stack);
        if (filled == null) return null;

        replaceHeld(player, stack, filled);
        return TurtleCommandResult.success(new Object[] { "bucket", "fill" });
    }

    /**
     * @return The cell a filled bucket would be emptied into, or {@code null} if there is nowhere
     *         valid to pour: either the clicked side is not a valid facing, or there is no block
     *         hit and the cell in front of the turtle is unsupported
     */
    private int[] placeTarget(ITurtleAccess turtle, ForgeDirection direction, MovingObjectPosition hit) {
        if (isBlockHit(hit)) {
            int side = hit.sideHit;
            if (side < 0 || side >= Facing.offsetsXForSide.length) return null;

            return new int[] { hit.blockX + Facing.offsetsXForSide[side], hit.blockY + Facing.offsetsYForSide[side],
                hit.blockZ + Facing.offsetsZForSide[side] };
        }

        // No block hit at all: fall back to the cell in front of the turtle, but only if it has
        // solid support underneath - the one case the turtle can reach that vanilla allows (a
        // player clicking the top face of the floor in front of them). An entity hit is not a
        // no-hit: the manipulator's entity interaction still gets to run for those.
        if (hit != null) return null;

        ChunkCoordinates position = turtle.getPosition();
        int x = position.posX + direction.offsetX;
        int y = position.posY + direction.offsetY;
        int z = position.posZ + direction.offsetZ;
        if (!actions.isSupported(turtle.getWorld(), x, y, z)) return null;

        return new int[] { x, y, z };
    }

    private static boolean isBlockHit(MovingObjectPosition hit) {
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private static boolean isEditable(World world, int x, int y, int z, FakePlayer player) {
        return !ComputerCraft.turtlesObeyBlockProtection || ComputerCraft.isBlockEditable(world, x, y, z, player);
    }

    /**
     * Swap the used item for its container in the fake player's inventory.
     *
     * <p>
     * The tool host copies this inventory back to the turtle after the action, so a whole stack of
     * buckets keeps the remainder and gets the container in a spare slot rather than losing it. A
     * single bucket is simply replaced in the held slot.
     * </p>
     */
    private static void replaceHeld(FakePlayer player, ItemStack held, ItemStack container) {
        container.stackSize = 1;

        if (held.stackSize > 1) {
            held.stackSize--;
            if (!player.inventory.addItemStackToInventory(container)) {
                player.dropPlayerItemWithRandomChoice(container, false);
            }
        } else {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, container);
        }
    }

    /**
     * The in-game implementation: vanilla water/lava first (so it behaves exactly like an
     * {@link ItemBucket}), then {@link FluidContainerRegistry} for modded fluids and containers.
     */
    public static class ForgeActions implements Actions {

        @Override
        public boolean isBucket(ItemStack stack) {
            if (stack == null || stack.getItem() == null) return false;

            return stack.getItem() == Items.bucket || stack.getItem() == Items.water_bucket
                || stack.getItem() == Items.lava_bucket
                || FluidContainerRegistry.isEmptyContainer(stack)
                || FluidContainerRegistry.isFilledContainer(stack);
        }

        @Override
        public boolean isFilled(ItemStack stack) {
            if (stack == null || stack.getItem() == null) return false;

            return stack.getItem() == Items.water_bucket || stack.getItem() == Items.lava_bucket
                || FluidContainerRegistry.isFilledContainer(stack);
        }

        @Override
        public boolean canFill(World world, int x, int y, int z, ItemStack empty) {
            return sourceFluid(world, x, y, z) != null;
        }

        @Override
        public boolean canPlace(World world, int x, int y, int z, ItemStack filled) {
            Material material = world.getBlock(x, y, z)
                .getMaterial();
            // Vanilla ItemBucket#tryPlaceContainedLiquid: only into air or a non-solid block.
            return world.isAirBlock(x, y, z) || !material.isSolid();
        }

        @Override
        public boolean isSupported(World world, int x, int y, int z) {
            Block below = world.getBlock(x, y - 1, z);
            return below != null && below.getMaterial()
                .isSolid();
        }

        @Override
        public ItemStack fill(World world, int x, int y, int z, ItemStack empty) {
            Block block = world.getBlock(x, y, z);
            int metadata = world.getBlockMetadata(x, y, z);

            // Vanilla first: this is the behaviour a player expects from a bucket, and it does not
            // depend on Forge having registered the water/lava containers.
            if (block.getMaterial() == Material.water && metadata == 0) {
                world.setBlockToAir(x, y, z);
                return new ItemStack(Items.water_bucket);
            }

            if (block.getMaterial() == Material.lava && metadata == 0) {
                world.setBlockToAir(x, y, z);
                return new ItemStack(Items.lava_bucket);
            }

            // Modded fluid blocks: check that the container accepts the fluid *before* draining, so
            // a rejected fluid leaves the world untouched.
            FluidStack fluid = sourceFluid(world, x, y, z);
            if (fluid == null) return null;

            ItemStack filled = FluidContainerRegistry.fillFluidContainer(fluid, empty);
            if (filled == null) return null;

            FluidStack contents = FluidContainerRegistry.getFluidForFilledItem(filled);
            if (contents == null || contents.amount <= 0) return null;

            if (!takeFluid(world, x, y, z)) return null;
            return filled;
        }

        @Override
        public ItemStack place(World world, int x, int y, int z, ItemStack filled) {
            if (!(filled.getItem() instanceof ItemBucket)) return null;

            if (!((ItemBucket) filled.getItem()).tryPlaceContainedLiquid(world, x, y, z)) return null;

            ItemStack empty = emptyContainer(filled);
            return empty == null ? new ItemStack(Items.bucket) : empty;
        }

        /**
         * @return The fluid a <em>source</em> block holds, or {@code null} for anything else. Does
         *         not change the world.
         */
        private static FluidStack sourceFluid(World world, int x, int y, int z) {
            Block block = world.getBlock(x, y, z);
            if (block.getMaterial() == Material.water) {
                return world.getBlockMetadata(x, y, z) == 0
                    ? new FluidStack(FluidRegistry.WATER, FluidContainerRegistry.BUCKET_VOLUME)
                    : null;
            }

            if (block.getMaterial() == Material.lava) {
                return world.getBlockMetadata(x, y, z) == 0
                    ? new FluidStack(FluidRegistry.LAVA, FluidContainerRegistry.BUCKET_VOLUME)
                    : null;
            }

            return block instanceof IFluidBlock ? ((IFluidBlock) block).drain(world, x, y, z, false) : null;
        }

        /**
         * Remove one source block of fluid.
         *
         * @return Whether a block was removed
         */
        private static boolean takeFluid(World world, int x, int y, int z) {
            Block block = world.getBlock(x, y, z);

            if (block instanceof IFluidBlock) {
                FluidStack drained = ((IFluidBlock) block).drain(world, x, y, z, true);
                return drained != null && drained.amount > 0;
            }

            return block.getMaterial()
                .isLiquid() && world.setBlockToAir(x, y, z);
        }

        /**
         * @return The registered empty container for a filled one, or {@code null}
         */
        private static ItemStack emptyContainer(ItemStack filled) {
            for (FluidContainerData data : FluidContainerRegistry.getRegisteredFluidContainerData()) {
                if (data.filledContainer.getItem() == filled.getItem()
                    && data.filledContainer.getItemDamage() == filled.getItemDamage()) {
                    ItemStack empty = data.emptyContainer.copy();
                    empty.stackSize = 1;
                    return empty;
                }
            }

            return null;
        }
    }
}
