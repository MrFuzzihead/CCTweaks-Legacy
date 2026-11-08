package org.squiddev.cctweaks.turtle;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Facing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.core.McEvents;
import org.squiddev.cctweaks.core.utils.FakeNetHandler;
import org.squiddev.cctweaks.core.utils.WorldPosition;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;

/**
 * Handles various turtle actions.
 *
 * Emulates large parts of {@link ItemInWorldManager}
 */
public class ToolHostPlayer extends TurtlePlayer implements IToolHostPlayer {

    private ChunkCoordinates digPosition;
    private Block digBlock;
    private ItemStack loadedStack;

    private ChunkCoordinates coordinates;

    /**
     * The drop consumer is created once and reused: it is pure apart from the turtle it writes
     * back to, and an allocation per attack/dig is needless churn on the server tick.
     */
    private ITurtleAccess consumerTurtle;
    private McEvents.IDropConsumer consumer;

    public ToolHostPlayer(ITurtleAccess turtle) {
        super((WorldServer) turtle.getWorld());
        playerNetServerHandler = new FakeNetHandler(this);
    }

    public McEvents.IDropConsumer getConsumer(ITurtleAccess turtle) {
        if (consumer == null || consumerTurtle != turtle) {
            consumerTurtle = turtle;
            consumer = createConsumer(turtle);
        }

        return consumer;
    }

    private McEvents.IDropConsumer createConsumer(final ITurtleAccess turtle) {
        return new McEvents.IDropConsumer() {

            @Override
            public void consumeDrop(ItemStack drop) {
                if (drop == null || drop.stackSize <= 0) return;

                // Store the drop in the fake player's inventory so it is written back to the
                // turtle by unload(). Writing straight to the turtle here can be overwritten
                // by the fake inventory when unload() runs.
                ItemStack remainder = InventoryUtil
                    .storeItems(drop, inventory, 0, inventory.getSizeInventory(), turtle.getSelectedSlot());
                if (remainder != null) {
                    ChunkCoordinates position = turtle.getPosition();
                    WorldUtil.dropItemStack(
                        remainder,
                        worldObj,
                        position.posX,
                        position.posY,
                        position.posZ,
                        Facing.oppositeSide[turtle.getDirection()]);
                }
            }
        };
    }

    @Override
    public FakePlayer asPlayer() {
        return this;
    }

    @Override
    public World getWorld() {
        return worldObj;
    }

    @Override
    public double getX() {
        return posX;
    }

    @Override
    public double getY() {
        return posY;
    }

    @Override
    public double getZ() {
        return posZ;
    }

    @Override
    public void setHeldItem(ItemStack stack) {
        inventory.setInventorySlotContents(inventory.currentItem, stack);
    }

    @Override
    public void moveTowards(ForgeDirection direction, double distance) {
        posX += direction.offsetX * distance;
        posY += direction.offsetY * distance;
        posZ += direction.offsetZ * distance;
    }

    @Override
    public boolean finishUseItem(int duration) {
        boolean using = isUsingItem();
        ItemStack inUse = getItemInUse();

        if (using && inUse != null && !ForgeEventFactory.onUseItemStop(this, inUse, duration)) {
            inUse.onPlayerStoppedUsing(worldObj, this, inUse.getMaxItemUseDuration() - duration);
            clearItemInUse();
        }

        return using;
    }

    public TurtleCommandResult attack(ITurtleAccess turtle, int direction) {
        updateInformation(turtle, direction);

        Vec3 rayDir = getLook(1.0f);
        Vec3 rayStart = Vec3.createVectorHelper(posX, posY, posZ)
            .addVector(rayDir.xCoord * 0.4, rayDir.yCoord * 0.4, rayDir.zCoord * 0.4);

        Entity hitEntity = WorldUtil.rayTraceEntities(turtle.getWorld(), rayStart, rayDir, 1.1);
        return attack(turtle, hitEntity);
    }

    /**
     * Attack an entity which has already been found. The player's position and inventory must
     * already have been loaded with {@link #load(ITurtleAccess, int, boolean)}.
     */
    public TurtleCommandResult attack(ITurtleAccess turtle, Entity hitEntity) {
        if (hitEntity == null) return TurtleCommandResult.failure("Nothing to attack here");

        McEvents.addEntityConsumer(hitEntity, getConsumer(turtle));
        try {
            attackTargetEntityWithCurrentItem(hitEntity);
        } finally {
            McEvents.removeEntityConsumer(hitEntity);
        }

        return TurtleCommandResult.success();
    }

    public TurtleCommandResult dig(ITurtleAccess turtle, int direction) {
        updateInformation(turtle, direction);

        World world = turtle.getWorld();
        ChunkCoordinates turtlePosition = turtle.getPosition();
        int x = turtlePosition.posX + Facing.offsetsXForSide[direction];
        int y = turtlePosition.posY + Facing.offsetsYForSide[direction];
        int z = turtlePosition.posZ + Facing.offsetsZForSide[direction];
        Block block = world.getBlock(x, y, z);

        if (!world.isAirBlock(x, y, z) && !block.getMaterial()
            .isLiquid()) {
            if (ComputerCraft.turtlesObeyBlockProtection && !ComputerCraft.isBlockEditable(world, x, y, z, this)) {
                return TurtleCommandResult.failure("Cannot break protected block");
            }

            if (block == Blocks.bedrock || block.getBlockHardness(world, x, y, z) <= -1) {
                return TurtleCommandResult.failure("Unbreakable block detected");
            }

            ChunkCoordinates pos = new ChunkCoordinates(x, y, z);
            if (block != digBlock || !pos.equals(digPosition)) {
                theItemInWorldManager.cancelDestroyingBlock(x, y, z);
                theItemInWorldManager.durabilityRemainingOnBlock = -1;

                digPosition = pos;
                digBlock = block;
            }

            ItemInWorldManager manager = theItemInWorldManager;

            // The consumer has to cover onBlockClicked too: it can harvest a soft block
            // immediately, and those drops should end up in the turtle as well.
            IWorldPosition position = new WorldPosition(world, x, y, z);
            McEvents.addBlockConsumer(position, getConsumer(turtle));
            try {
                for (int i = 0; i < CCTweaks.digFactor; i++) {
                    if (manager.durabilityRemainingOnBlock == -1) {
                        manager.onBlockClicked(x, y, z, Facing.oppositeSide[direction]);
                    } else {
                        manager.updateBlockRemoving();
                        if (manager.durabilityRemainingOnBlock >= 9) {
                            manager.uncheckedTryHarvestBlock(x, y, z);
                            manager.durabilityRemainingOnBlock = -1;
                            break;
                        }
                    }
                }
            } finally {
                McEvents.removeBlockConsumer(position);
            }

            return TurtleCommandResult.success();
        }

        return TurtleCommandResult.failure("Nothing to dig here");
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return coordinates;
    }

    /**
     * Basically just {@link #getHeldItem()}
     */
    public ItemStack getItem(ITurtleAccess turtle) {
        return turtle.getInventory()
            .getStackInSlot(turtle.getSelectedSlot());
    }

    /**
     * Update the player's position and rotation to match the turtle.
     */
    public void updateInformation(ITurtleAccess turtle, int direction) {
        ChunkCoordinates position = coordinates = turtle.getPosition();

        setPositionAndRotation(
            position.posX + 0.5 + 0.51 * Facing.offsetsXForSide[direction],
            position.posY + 0.5 + 0.51 * Facing.offsetsYForSide[direction],
            position.posZ + 0.5 + 0.51 * Facing.offsetsZForSide[direction],
            direction > 2 ? DirectionUtil.toYawAngle(direction) : DirectionUtil.toYawAngle(turtle.getDirection()),
            direction > 2 ? 0 : DirectionUtil.toPitchAngle(direction));
    }

    /**
     * Prepare the fake player for an operation. This positions the player and copies the
     * turtle's inventory into the fake player's inventory.
     */
    public void load(ITurtleAccess turtle, int direction, boolean sneaking) {
        if (loadedStack != null) {
            getAttributeMap().removeAttributeModifiers(loadedStack.getAttributeModifiers());
            loadedStack = null;
        }

        updateInformation(turtle, direction);
        setSneaking(sneaking);
        loadTurtleInventory(turtle);

        // FakePlayer.onUpdate() is a no-op, so the normal equipment-attribute pass never runs.
        // Apply the held item's modifiers here or a diamond sword uses bare-hand damage.
        loadedStack = inventory.getCurrentItem();
        if (loadedStack != null) {
            getAttributeMap().applyAttributeModifiers(loadedStack.getAttributeModifiers());
        }
    }

    /**
     * Finish an operation, copying the fake player's inventory back to the turtle and
     * clearing out any stale fake inventory slots.
     */
    public void unload(ITurtleAccess turtle) {
        if (loadedStack != null) {
            getAttributeMap().removeAttributeModifiers(loadedStack.getAttributeModifiers());
            loadedStack = null;
        }

        // Releasing the right button is part of finishing the action: every caller used to clear
        // this right before unload(), so the peripheral does not need to know about it.
        clearItemInUse();
        setSneaking(false);
        unloadTurtleInventory(turtle);
    }

    private void loadTurtleInventory(ITurtleAccess turtle) {
        IInventory turtleInventory = turtle.getInventory();
        int largerSize = inventory.getSizeInventory();
        int size = Math.min(turtleInventory.getSizeInventory(), largerSize);

        // Make sure getHeldItem()/getCurrentEquippedItem() uses the turtle's selected slot.
        inventory.currentItem = Math.min(turtle.getSelectedSlot(), largerSize - 1);

        for (int i = 0; i < size; i++) {
            inventory.setInventorySlotContents(i, turtleInventory.getStackInSlot(i));
        }
        for (int i = size; i < largerSize; i++) {
            inventory.setInventorySlotContents(i, null);
        }
    }

    private void unloadTurtleInventory(ITurtleAccess turtle) {
        IInventory turtleInventory = turtle.getInventory();
        int largerSize = inventory.getSizeInventory();
        int size = Math.min(turtleInventory.getSizeInventory(), largerSize);

        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            turtleInventory.setInventorySlotContents(i, stack == null || stack.stackSize <= 0 ? null : stack);
            inventory.setInventorySlotContents(i, null);
        }

        for (int i = size; i < largerSize; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) storeItem(stack, turtle);
            inventory.setInventorySlotContents(i, null);
        }
    }

    private void storeItem(ItemStack stack, ITurtleAccess turtle) {
        IInventory turtleInventory = turtle.getInventory();
        ItemStack remainder = InventoryUtil
            .storeItems(stack, turtleInventory, 0, turtleInventory.getSizeInventory(), turtle.getSelectedSlot());
        if (remainder != null) {
            ChunkCoordinates position = turtle.getPosition();
            WorldUtil.dropItemStack(
                remainder,
                worldObj,
                position.posX,
                position.posY,
                position.posZ,
                Facing.oppositeSide[turtle.getDirection()]);
        }
    }

}
