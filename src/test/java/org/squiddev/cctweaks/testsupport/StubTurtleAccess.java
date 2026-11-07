package org.squiddev.cctweaks.testsupport;

import net.minecraft.inventory.IInventory;
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
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;

/**
 * A minimal {@link ITurtleAccess} for headless tests. World-dependent methods are intentionally
 * simple: tests that need a real world mock it separately.
 */
public class StubTurtleAccess implements ITurtleAccess {

    public World world;
    public final IInventory inventory;
    public ChunkCoordinates position = new ChunkCoordinates(10, 64, 10);
    public int direction = 3;
    public int selectedSlot;
    public int fuelLevel = 1000;
    public int fuelLimit = 100000;
    public final ITurtleUpgrade[] upgrades = new ITurtleUpgrade[2];
    public final IPeripheral[] peripherals = new IPeripheral[2];

    public StubTurtleAccess(World world, int inventorySize) {
        this.world = world;
        this.inventory = new StubInventory(inventorySize);
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
        this.world = world;
        this.position = new ChunkCoordinates(x, y, z);
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
        return true;
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
        if (amount <= 0) return true;
        if (fuelLevel < amount) return false;
        fuelLevel -= amount;
        return true;
    }

    @Override
    public void addFuel(int amount) {
        fuelLevel = Math.min(fuelLimit, fuelLevel + Math.max(0, amount));
    }

    @Override
    public Object[] executeCommand(ILuaContext context, ITurtleCommand command) throws LuaException {
        TurtleCommandResult result = command.execute(this);
        return new Object[] { result };
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

    public ItemStack getSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    public void setSlot(int slot, ItemStack stack) {
        inventory.setInventorySlotContents(slot, stack);
    }
}
