package org.squiddev.cctweaks.testsupport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.turtle.IToolHostPlayer;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

/**
 * A {@link IToolHostPlayer} that records what the tool manipulator asked of it.
 *
 * <p>
 * This is the whole point of the {@code IToolHostPlayer} seam: the real player needs a
 * {@code WorldServer} to construct, so without a stub the use/swing decision tree could not be
 * exercised headlessly.
 * </p>
 */
public class StubToolHostPlayer implements IToolHostPlayer {

    /** Actions in the order the peripheral performed them. */
    public final List<String> calls = new ArrayList<String>();

    public final EntityPlayer entityPlayer;
    public final World world;

    public ItemStack held;
    public boolean sneaking;
    public boolean interactResult;
    public boolean wasUsingItem;
    public TurtleCommandResult attackResult = TurtleCommandResult.success();
    public TurtleCommandResult digResult = TurtleCommandResult.success();

    public Entity attacked;
    public int dugDirection = -1;
    public ForgeDirection movedDirection;
    public double movedDistance;

    public StubToolHostPlayer(EntityPlayer entityPlayer, World world) {
        this.entityPlayer = entityPlayer;
        this.world = world;
    }

    public static StubToolHostPlayer create(World world) {
        return new StubToolHostPlayer(StubToolHostPlayer.mockPlayer(), world);
    }

    static EntityPlayer mockPlayer() {
        return org.mockito.Mockito.mock(FakePlayer.class);
    }

    @Override
    public FakePlayer asPlayer() {
        return (FakePlayer) entityPlayer;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public double getX() {
        return 0;
    }

    @Override
    public double getY() {
        return 0;
    }

    @Override
    public double getZ() {
        return 0;
    }

    @Override
    public ItemStack getHeldItem() {
        return held;
    }

    @Override
    public boolean isSneaking() {
        return sneaking;
    }

    @Override
    public ItemStack getItem(ITurtleAccess turtle) {
        return held;
    }

    @Override
    public void setHeldItem(ItemStack stack) {
        calls.add("setHeldItem");
        held = stack;
    }

    @Override
    public void load(ITurtleAccess turtle, int direction, boolean sneaking) {
        // Deliberately does not touch `sneaking`: tests set that field to describe the state they
        // want and assert on the recorded load() call for what the peripheral asked for.
        calls.add("load:" + direction + ":" + sneaking);
    }

    @Override
    public void unload(ITurtleAccess turtle) {
        calls.add("unload");
    }

    @Override
    public void moveTowards(ForgeDirection direction, double distance) {
        calls.add("move");
        movedDirection = direction;
        movedDistance = distance;
    }

    @Override
    public boolean interactWith(Entity entity) {
        calls.add("interactWith");
        return interactResult;
    }

    @Override
    public boolean finishUseItem(int duration) {
        calls.add("finishUseItem:" + duration);
        return wasUsingItem;
    }

    @Override
    public TurtleCommandResult attack(ITurtleAccess turtle, Entity hitEntity) {
        calls.add("attack");
        attacked = hitEntity;
        return attackResult;
    }

    @Override
    public TurtleCommandResult dig(ITurtleAccess turtle, int direction) {
        calls.add("dig:" + direction);
        dugDirection = direction;
        return digResult;
    }
}
