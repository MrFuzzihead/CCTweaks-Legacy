package org.squiddev.cctweaks.turtle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.api.turtle.ITurtleInteraction;
import org.squiddev.cctweaks.testsupport.StubComputerAccess;
import org.squiddev.cctweaks.testsupport.StubLuaContext;
import org.squiddev.cctweaks.testsupport.StubToolHostPlayer;
import org.squiddev.cctweaks.testsupport.StubTurtleAccess;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.core.InteractDirection;

/**
 * Exercises the use/swing decision tree through the {@link IToolHostPlayer} seam.
 *
 * <p>
 * These paths used to be untestable because they needed a live {@code WorldServer} for the fake
 * player. The stub player plus a fixed hit resolver make the whole ordering observable: registered
 * interaction, then entity/block interaction, then item use, with vanilla protections in between.
 * </p>
 */
public class ToolManipulatorActionsTest {

    private String[] previousDisabled;
    private boolean previousProtection;

    private World world;
    private StubTurtleAccess turtle;
    private StubComputerAccess computer;
    private StubLuaContext context;
    private StubToolHostPlayer player;

    @BeforeEach
    public void setUp() {
        previousDisabled = ComputerCraft.turtleDisabledActions;
        previousProtection = ComputerCraft.turtlesObeyBlockProtection;
        ComputerCraft.turtleDisabledActions = new String[0];
        // isBlockEditable() needs a live MinecraftServer to consult the claim providers, so the
        // protection branches are left to in-world testing; these tests cover the decision tree.
        ComputerCraft.turtlesObeyBlockProtection = false;

        world = mock(World.class);
        turtle = new StubTurtleAccess(world, 16);
        turtle.direction = ForgeDirection.SOUTH.ordinal();
        computer = new StubComputerAccess(1, "back");
        context = new StubLuaContext();
        player = StubToolHostPlayer.create(world);
    }

    @AfterEach
    public void tearDown() {
        ComputerCraft.turtleDisabledActions = previousDisabled;
        ComputerCraft.turtlesObeyBlockProtection = previousProtection;
    }

    private ToolManipulatorPeripheral peripheral(ToolHitResolver hits) {
        return new ToolManipulatorPeripheral(turtle, player, TurtleSide.Left, new StubInteractions(), hits);
    }

    private static ToolHitResolver hitAt(final MovingObjectPosition hit) {
        return (world, x, y, z, facing, range) -> hit;
    }

    private static MovingObjectPosition blockHit(int x, int y, int z) {
        return new MovingObjectPosition(
            x,
            y,
            z,
            ForgeDirection.UP.ordinal(),
            Vec3.createVectorHelper(x + 0.5, y + 1, z + 0.5));
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item, 1);
    }

    private void plainBlockAt(int x, int y, int z) {
        Block block = mock(Block.class);
        when(world.getBlock(x, y, z)).thenReturn(block);
        when(block.isAir(any(), anyInt(), anyInt(), anyInt())).thenReturn(false);
    }

    // region use

    @Test
    public void registeredInteractionWins() throws Exception {
        ItemStack held = stack(new Item());
        player.held = held;
        MovingObjectPosition hit = blockHit(10, 64, 11);

        StubInteractions interactions = new StubInteractions();
        interactions.useResult = TurtleCommandResult.success(new Object[] { "custom" });
        ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(
            turtle,
            player,
            TurtleSide.Left,
            interactions,
            hitAt(hit));

        assertArrayEquals(new Object[] { true, "custom" }, peripheral.doUse(computer, turtle.direction, false, 0));
        assertSame(hit, interactions.lastHit);
        assertSame(held, interactions.lastStack);
        assertTrue(player.calls.contains("load:" + turtle.direction + ":false"), "the player is loaded");
        assertEquals("unload", player.calls.get(player.calls.size() - 1), "the player is always unloaded");
    }

    @Test
    public void disabledToolSkipsEverything() throws Exception {
        ComputerCraft.turtleDisabledActions = new String[] { "tool" };
        player.held = stack(new Item());

        Object[] result = peripheral(hitAt(null)).use(computer, context, InteractDirection.Forward, new Object[0]);

        assertArrayEquals(new Object[] { false, "Turtle action \"tool\" is disabled" }, result);
        assertTrue(player.calls.isEmpty(), "a disabled action must not touch the player");
    }

    @Test
    public void entityInteractionIsReported() throws Exception {
        Entity entity = mock(Entity.class);
        player.held = stack(new Item());
        player.interactResult = true;

        Object[] result = peripheral(hitAt(new MovingObjectPosition(entity)))
            .doUse(computer, turtle.direction, false, 0);

        assertArrayEquals(new Object[] { true, "entity", "interact" }, result);
        assertTrue(player.calls.contains("interactWith"));
    }

    @Test
    public void blockActivationIsReported() throws Exception {
        Block block = mock(Block.class);
        when(world.getBlock(10, 64, 11)).thenReturn(block);
        when(block.isAir(any(), anyInt(), anyInt(), anyInt())).thenReturn(false);
        when(
            block.onBlockActivated(
                any(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(),
                anyInt(),
                anyFloat(),
                anyFloat(),
                anyFloat())).thenReturn(true);
        player.held = stack(new Item());

        Object[] result = peripheral(hitAt(blockHit(10, 64, 11))).doUse(computer, turtle.direction, false, 0);

        assertArrayEquals(new Object[] { true, "block", "interact" }, result);
    }

    @Test
    public void disabledPlacementIsRefused() throws Exception {
        ComputerCraft.turtleDisabledActions = new String[] { "place" };
        plainBlockAt(10, 64, 11);
        player.held = stack(new ItemBlock(mock(Block.class)));

        Object[] result = peripheral(hitAt(blockHit(10, 64, 11))).doUse(computer, turtle.direction, false, 0);

        assertArrayEquals(new Object[] { false, "Turtle action \"place\" is disabled" }, result);
        assertFalse(player.calls.contains("setHeldItem"), "nothing may be removed from the inventory");
    }

    @Test
    public void itemUseInAirReplacesTheHeldStack() throws Exception {
        // A mock item returns null from useItemRightClick, so the stack changes and the result is
        // reported as a successful item use.
        ItemStack held = stack(new Item());
        player.held = held;
        player.wasUsingItem = true;

        Object[] result = peripheral(hitAt(null)).doUse(computer, turtle.direction, false, 0);

        assertArrayEquals(new Object[] { true, "item", "use" }, result);
        assertEquals(ForgeDirection.SOUTH, player.movedDirection, "the player steps forward before using in air");
        assertEquals(0.6, player.movedDistance, 0.0001);
        assertTrue(player.calls.contains("setHeldItem"));
        assertTrue(player.calls.contains("finishUseItem:0"), "the use is always finished");
    }

    @Test
    public void itemThatDoesNothingInAirIsReportedAsFailure() throws Exception {
        // A plain Item returns itself from useItemRightClick, so nothing changed.
        player.held = stack(new Item());

        assertArrayEquals(new Object[] { false }, peripheral(hitAt(null)).doUse(computer, turtle.direction, false, 0));
        assertFalse(player.calls.contains("setHeldItem"));
    }

    @Test
    public void nothingInFrontAndNoItemDoesNothing() throws Exception {
        player.held = null;

        assertArrayEquals(new Object[] { false }, peripheral(hitAt(null)).doUse(computer, turtle.direction, false, 0));
        assertTrue(player.calls.contains("unload"));
    }

    // endregion

    // region swing

    @Test
    public void swingUsesRegisteredInteractionAndAnimates() throws Exception {
        player.held = stack(new Item());
        CountingTurtleAccess counting = new CountingTurtleAccess(turtle);
        StubInteractions interactions = new StubInteractions();
        TurtleCommandResult expected = TurtleCommandResult.success();
        interactions.swingResult = expected;

        ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(
            counting,
            player,
            TurtleSide.Left,
            interactions,
            hitAt(blockHit(10, 64, 11)));

        assertSame(expected, peripheral.doSwing(computer, turtle.direction, false));
        assertEquals(1, counting.animations);
        assertEquals("unload", player.calls.get(player.calls.size() - 1));
    }

    @Test
    public void swingAttacksAnEntityThatIsInRange() throws Exception {
        Entity entity = mock(Entity.class);
        player.held = null;

        assertSame(
            player.attackResult,
            peripheral(hitAt(new MovingObjectPosition(entity))).doSwing(computer, turtle.direction, false));
        assertSame(entity, player.attacked);
        assertEquals(-1, player.dugDirection);
    }

    @Test
    public void swingDigsTheBlockInFront() throws Exception {
        plainBlockAt(10, 64, 11);
        player.held = null;

        assertSame(
            player.digResult,
            peripheral(hitAt(blockHit(10, 64, 11))).doSwing(computer, turtle.direction, false));
        assertEquals(turtle.direction, player.dugDirection);
        assertEquals("unload", player.calls.get(player.calls.size() - 1));
    }

    @Test
    public void swingRefusesDisabledAttack() throws Exception {
        ComputerCraft.turtleDisabledActions = new String[] { "attack" };
        Entity entity = mock(Entity.class);

        TurtleCommandResult result = peripheral(hitAt(new MovingObjectPosition(entity)))
            .doSwing(computer, turtle.direction, false);

        assertFalse(result.isSuccess());
        assertEquals("Turtle action \"attack\" is disabled", result.getErrorMessage());
        assertNull(player.attacked);
    }

    @Test
    public void swingRefusesDisabledDig() throws Exception {
        ComputerCraft.turtleDisabledActions = new String[] { "dig" };
        plainBlockAt(10, 64, 11);

        TurtleCommandResult result = peripheral(hitAt(blockHit(10, 64, 11))).doSwing(computer, turtle.direction, false);

        assertFalse(result.isSuccess());
        assertEquals("Turtle action \"dig\" is disabled", result.getErrorMessage());
        assertEquals(-1, player.dugDirection);
    }

    @Test
    public void swingWithNothingInFrontFails() throws Exception {
        player.held = null;

        TurtleCommandResult result = peripheral(hitAt(null)).doSwing(computer, turtle.direction, false);

        assertFalse(result.isSuccess());
        assertEquals("Nothing to do here", result.getErrorMessage());
    }

    @Test
    public void swingOnlyAnimatesOnSuccess() throws Exception {
        player.held = stack(new Item());
        CountingTurtleAccess counting = new CountingTurtleAccess(turtle);
        StubInteractions interactions = new StubInteractions();
        interactions.swingResult = TurtleCommandResult.failure("nope");

        ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(
            counting,
            player,
            TurtleSide.Left,
            interactions,
            hitAt(blockHit(10, 64, 11)));

        peripheral.doSwing(computer, 0, false);
        assertEquals(0, counting.animations, "a failed swing must not animate");
    }

    // endregion

    // region canUse / canSwing

    @Test
    public void canUseAcceptsRegisteredInteraction() throws Exception {
        StubInteractions interactions = new StubInteractions();
        interactions.canUseResult = true;
        player.held = stack(new Item());

        ToolManipulatorPeripheral peripheral = new ToolManipulatorPeripheral(
            turtle,
            player,
            TurtleSide.Left,
            interactions,
            hitAt(null));

        assertTrue(unwrapBoolean(peripheral.canUse(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canUseAcceptsATileEntityBlock() throws Exception {
        Block block = mock(Block.class);
        when(world.getBlock(10, 64, 11)).thenReturn(block);
        when(block.isAir(any(), anyInt(), anyInt(), anyInt())).thenReturn(false);
        when(block.hasTileEntity(anyInt())).thenReturn(true);

        assertTrue(
            unwrapBoolean(
                peripheral(hitAt(blockHit(10, 64, 11)))
                    .canUse(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canUseRejectsAPlainBlock() throws Exception {
        plainBlockAt(10, 64, 11);

        assertFalse(
            unwrapBoolean(
                peripheral(hitAt(blockHit(10, 64, 11)))
                    .canUse(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canUseSneakingIgnoresTheBlock() throws Exception {
        Block block = mock(Block.class);
        when(world.getBlock(10, 64, 11)).thenReturn(block);
        when(block.isAir(any(), anyInt(), anyInt(), anyInt())).thenReturn(false);
        when(block.hasTileEntity(anyInt())).thenReturn(true);
        player.held = stack(new Item());

        // The peripheral loads the player with sneak=false for canUse, so the check itself is
        // driven through the stub's isSneaking() rather than load().
        player.sneaking = true;

        assertFalse(
            unwrapBoolean(
                peripheral(hitAt(blockHit(10, 64, 11)))
                    .canUse(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canUseInAirDependsOnTheItem() throws Exception {
        // A plain item may do something on right click; a block needs a surface.
        player.held = stack(new Item());
        assertTrue(
            unwrapBoolean(peripheral(hitAt(null)).canUse(computer, context, InteractDirection.Forward, new Object[0])));

        player.held = stack(new ItemBlock(mock(Block.class)));
        assertFalse(
            unwrapBoolean(peripheral(hitAt(null)).canUse(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canSwingAcceptsAnEntity() throws Exception {
        Entity entity = mock(Entity.class);

        assertTrue(
            unwrapBoolean(
                peripheral(hitAt(new MovingObjectPosition(entity)))
                    .canSwing(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canSwingChecksHarvestability() throws Exception {
        Block harvestable = mock(Block.class);
        when(world.getBlock(10, 64, 11)).thenReturn(harvestable);
        when(harvestable.canHarvestBlock(any(), anyInt())).thenReturn(true);
        assertTrue(
            unwrapBoolean(
                peripheral(hitAt(blockHit(10, 64, 11)))
                    .canSwing(computer, context, InteractDirection.Forward, new Object[0])));

        Block unharvestable = mock(Block.class);
        when(world.getBlock(10, 64, 11)).thenReturn(unharvestable);
        assertFalse(
            unwrapBoolean(
                peripheral(hitAt(blockHit(10, 64, 11)))
                    .canSwing(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canSwingRespectsDisabledActions() throws Exception {
        ComputerCraft.turtleDisabledActions = new String[] { "dig" };
        plainBlockAt(10, 64, 11);

        assertFalse(
            unwrapBoolean(
                peripheral(hitAt(blockHit(10, 64, 11)))
                    .canSwing(computer, context, InteractDirection.Forward, new Object[0])));
    }

    @Test
    public void canSwingWithNothingInFrontIsFalse() throws Exception {
        player.held = null;

        assertFalse(
            unwrapBoolean(
                peripheral(hitAt(null)).canSwing(computer, context, InteractDirection.Forward, new Object[0])));
    }

    // endregion

    @Test
    public void callMethodWrapsTheSuspensionSentinel() throws Exception {
        // Guards the Lua bridge contract: callMethod hands back a single-element array holding the
        // MethodResult, not raw values.
        player.held = null;
        Object[] result = peripheral(hitAt(null)).callMethod(computer, context, 3, new Object[0]);

        assertEquals(1, result.length);
        assertTrue(result[0] instanceof MethodResult);
        assertArrayEquals(new Object[] { false }, ((MethodResult) result[0]).getResults());
    }

    /**
     * An item whose right click replaces the stack with nothing (a bucket-emptying style item).
     */
    static class NullRightClickItem extends Item {

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world,
            net.minecraft.entity.player.EntityPlayer player) {
            return null;
        }
    }

    private static Object unwrap(MethodResult result) {
        assertTrue(result.isImmediate(), "these checks must not suspend");
        return result.getResults()[0];
    }

    private static boolean unwrapBoolean(MethodResult result) {
        return (Boolean) unwrap(result);
    }

    /**
     * A registry whose answers are configured per test.
     */
    static class StubInteractions implements ITurtleInteraction {

        TurtleCommandResult useResult;
        TurtleCommandResult swingResult;
        boolean canUseResult;
        boolean canSwingResult;

        ItemStack lastStack;
        MovingObjectPosition lastHit;

        @Override
        public TurtleCommandResult swing(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player,
            ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) {
            lastStack = stack;
            lastHit = hit;
            return swingResult;
        }

        @Override
        public boolean canSwing(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
            MovingObjectPosition hit) {
            return canSwingResult;
        }

        @Override
        public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player,
            ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) {
            lastStack = stack;
            lastHit = hit;
            return useResult;
        }

        @Override
        public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
            MovingObjectPosition hit) {
            return canUseResult;
        }
    }

    /**
     * Counts animations so "only animate on success" can be asserted.
     */
    static class CountingTurtleAccess extends StubTurtleAccess {

        int animations;

        CountingTurtleAccess(StubTurtleAccess delegate) {
            super(delegate.getWorld(), 16);
            direction = delegate.getDirection();
        }

        @Override
        public void playAnimation(TurtleAnimation animation) {
            animations++;
        }
    }
}
