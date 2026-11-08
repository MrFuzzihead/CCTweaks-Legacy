package org.squiddev.cctweaks.turtle;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

/**
 * The player a tool manipulator acts through.
 *
 * <p>
 * This is the seam that makes {@link ToolManipulatorPeripheral} testable: the peripheral only talks
 * to this interface, so a headless test can replace the fake player (which needs a live
 * {@code WorldServer} for its superclass) with a stub and still exercise the use/swing decision
 * tree.
 * </p>
 *
 * @see ToolHostPlayer The in-game implementation
 */
public interface IToolHostPlayer {

    /**
     * The underlying Forge player, needed wherever a Minecraft API asks for an {@code EntityPlayer}.
     *
     * @return The player, never {@code null}
     */
    FakePlayer asPlayer();

    /**
     * @return The world the player (and turtle) is in
     */
    World getWorld();

    /**
     * @return The player's X position
     */
    double getX();

    /**
     * @return The player's Y position
     */
    double getY();

    /**
     * @return The player's Z position
     */
    double getZ();

    /**
     * @return The item currently held, or {@code null}
     */
    ItemStack getHeldItem();

    /**
     * @return Whether the player is currently sneaking
     */
    boolean isSneaking();

    /**
     * The item in the turtle's selected slot.
     *
     * @param turtle The turtle to read from
     * @return The held stack, or {@code null}
     */
    ItemStack getItem(ITurtleAccess turtle);

    /**
     * Replace the currently held stack in the player's inventory.
     *
     * @param stack The new stack, may be {@code null}
     */
    void setHeldItem(ItemStack stack);

    /**
     * Position the player at the turtle and copy its inventory in, ready for an action.
     *
     * @param turtle    The turtle to load from
     * @param direction The direction to face
     * @param sneaking  Whether the player should sneak
     */
    void load(ITurtleAccess turtle, int direction, boolean sneaking);

    /**
     * Copy the player's inventory back to the turtle and reset transient state.
     *
     * @param turtle The turtle to write back to
     */
    void unload(ITurtleAccess turtle);

    /**
     * Move the player the given distance along a direction, used before a right-click-air.
     *
     * @param direction The direction to move in
     * @param distance  The distance to move
     */
    void moveTowards(ForgeDirection direction, double distance);

    /**
     * Interact with an entity, as a right click would.
     *
     * @param entity The entity to interact with
     * @return Whether the interaction was handled
     */
    boolean interactWith(Entity entity);

    /**
     * Finish an in-progress item use, as the player releasing the right button would.
     *
     * @param duration How many ticks the item was used for
     * @return Whether the player was using an item at all
     */
    boolean finishUseItem(int duration);

    /**
     * Attack an entity which has already been found.
     *
     * @param turtle    The turtle acting
     * @param hitEntity The entity to attack
     * @return The command result
     */
    TurtleCommandResult attack(ITurtleAccess turtle, Entity hitEntity);

    /**
     * Dig the block in front of the turtle.
     *
     * @param turtle    The turtle acting
     * @param direction The direction to dig in
     * @return The command result
     */
    TurtleCommandResult dig(ITurtleAccess turtle, int direction);
}
