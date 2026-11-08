package org.squiddev.cctweaks.selfTest;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleSide;

/**
 * An {@link ITurtleAccess} backed by a real world and a plain {@link InventoryBasic}, for the
 * in-world self test.
 *
 * <p>
 * The unit tests use {@code testsupport.StubTurtleAccess}, which lives in the test source set and
 * therefore is not available at runtime. This is the runtime equivalent: it has no computer, no
 * peripheral and no fuel behaviour, because the self test only needs the parts of the turtle that
 * the tool host reads (position, direction, selected slot and inventory).
 * </p>
 */
public class SelfTestTurtle implements ITurtleAccess {

    private final World world;
    private final IInventory inventory;
    private ChunkCoordinates position;
    private int direction;
    private int selectedSlot;
    private int fuelLevel = 1000;
    private final int fuelLimit = 100000;
    private final ITurtleUpgrade[] upgrades = new ITurtleUpgrade[2];
    private final IPeripheral[] peripherals = new IPeripheral[2];

    public SelfTestTurtle(World world, int slots) {
        this.world = world;
        this.inventory = new InventoryBasic("cctweaks.selftest", false, slots);
        this.position = new ChunkCoordinates(0, 0, 0);
    }

    public SelfTestTurtle setPosition(int x, int y, int z) {
        position = new ChunkCoordinates(x, y, z);
        return this;
    }

    public SelfTestTurtle setSlot(int slot, ItemStack stack) {
        inventory.setInventorySlotContents(slot, stack);
        return this;
    }

    public ItemStack getSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    /**
     * @param item The item to look for, in any slot
     * @return The total stack size of that item in the turtle's inventory
     */
    public int count(net.minecraft.item.Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item) total += stack.stackSize;
        }
        return total;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public ChunkCoordinates getPosition() {
        return new ChunkCoordinates(position);
    }

    @Override
    public boolean teleportTo(World world, int x, int y, int z) {
        position = new ChunkCoordinates(x, y, z);
        return true;
    }

    @Override
    public Vec3 getVisualPosition(float partialTicks) {
        return Vec3.createVectorHelper(position.posX + 0.5, position.posY + 0.5, position.posZ + 0.5);
    }

    @Override
    public float getVisualYaw(float partialTicks) {
        return 0;
    }

    @Override
    public int getDirection() {
        return direction;
    }

    @Override
    public void setDirection(int direction) {
        this.direction = direction;
    }

    @Override
    public int getSelectedSlot() {
        return selectedSlot;
    }

    @Override
    public void setSelectedSlot(int slot) {
        selectedSlot = slot;
    }

    @Override
    public void setDyeColour(int colour) {}

    @Override
    public int getDyeColour() {
        return -1;
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean isFuelNeeded() {
        return false;
    }

    @Override
    public int getFuelLevel() {
        return fuelLevel;
    }

    @Override
    public void setFuelLevel(int level) {
        fuelLevel = level;
    }

    @Override
    public int getFuelLimit() {
        return fuelLimit;
    }

    @Override
    public boolean consumeFuel(int amount) {
        return true;
    }

    @Override
    public void addFuel(int amount) {
        fuelLevel = Math.min(fuelLimit, fuelLevel + Math.max(0, amount));
    }

    @Override
    public Object[] executeCommand(ILuaContext context, ITurtleCommand command) throws LuaException {
        return new Object[] { command.execute(this) };
    }

    @Override
    public void playAnimation(TurtleAnimation animation) {}

    @Override
    public ITurtleUpgrade getUpgrade(TurtleSide side) {
        return upgrades[side == TurtleSide.Left ? 0 : 1];
    }

    @Override
    public void setUpgrade(TurtleSide side, ITurtleUpgrade upgrade) {
        upgrades[side == TurtleSide.Left ? 0 : 1] = upgrade;
    }

    @Override
    public IPeripheral getPeripheral(TurtleSide side) {
        return peripherals[side == TurtleSide.Left ? 0 : 1];
    }

    @Override
    public NBTTagCompound getUpgradeNBTData(TurtleSide side) {
        return new NBTTagCompound();
    }

    @Override
    public void updateUpgradeNBTData(TurtleSide side) {}
}
