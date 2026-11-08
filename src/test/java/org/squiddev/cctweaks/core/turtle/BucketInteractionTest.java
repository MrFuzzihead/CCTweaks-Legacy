package org.squiddev.cctweaks.core.turtle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.testsupport.StubTurtleAccess;

import dan200.computercraft.api.turtle.TurtleCommandResult;

/**
 * The decision tree of the bucket interaction: which position is targeted, when it is refused, and
 * how the held stack is replaced. The Forge fluid/placement calls are stubbed through
 * {@link BucketInteraction.Actions}; the real ones run in the in-world self test.
 */
public class BucketInteractionTest {

    private final StubTurtleAccess turtle = new StubTurtleAccess(null, 16);

    /**
     * Container items; {@code Items} is not bootstrapped in a test JVM, so plain Items stand in.
     * The empty and filled containers are <em>different</em> items, exactly like a bucket and a
     * water bucket: {@code InventoryPlayer} merges stacks whose item matches and which report no
     * subtypes, so reusing one item with different damage would merge the fresh container back into
     * the held stack.
     */
    private final Item emptyItem = new Item();
    private final Item filledItem = new Item();
    private final ItemStack empty = new ItemStack(emptyItem, 2);
    private final ItemStack filled = new ItemStack(filledItem, 1);

    private final class StubActions implements BucketInteraction.Actions {

        boolean filledMode;
        boolean bucket = true;
        boolean placeResult = true;
        boolean fillResult = true;
        boolean canPlace = true;
        boolean canFill = true;

        final List<String> calls = new ArrayList<String>();
        int placeX = Integer.MIN_VALUE, placeY, placeZ;
        int fillX = Integer.MIN_VALUE, fillY, fillZ;

        @Override
        public boolean isBucket(ItemStack stack) {
            return bucket;
        }

        @Override
        public boolean isFilled(ItemStack stack) {
            return filledMode;
        }

        @Override
        public boolean canFill(World world, int x, int y, int z, ItemStack empty) {
            return canFill;
        }

        @Override
        public boolean canPlace(World world, int x, int y, int z, ItemStack filled) {
            return canPlace;
        }

        @Override
        public ItemStack fill(World world, int x, int y, int z, ItemStack empty) {
            calls.add("fill");
            fillX = x;
            fillY = y;
            fillZ = z;
            return fillResult ? new ItemStack(filledItem, 1) : null;
        }

        @Override
        public ItemStack place(World world, int x, int y, int z, ItemStack filled) {
            calls.add("place");
            placeX = x;
            placeY = y;
            placeZ = z;
            return placeResult ? new ItemStack(emptyItem, 1) : null;
        }
    }

    private static MovingObjectPosition hit(int x, int y, int z, int side) {
        return new MovingObjectPosition(x, y, z, side, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
    }

    private FakePlayer player() {
        FakePlayer player = org.mockito.Mockito.mock(FakePlayer.class);
        player.inventory = new net.minecraft.entity.player.InventoryPlayer(player);
        return player;
    }

    @Test
    public void nonBucketIsIgnored() {
        StubActions actions = new StubActions();
        actions.bucket = false;
        BucketInteraction interaction = new BucketInteraction(actions);

        assertNull(interaction.use(turtle, null, player(), empty, ForgeDirection.NORTH, hit(1, 2, 3, 1)));
        assertEquals(0, actions.calls.size());
        assertFalse(interaction.canUse(turtle, player(), empty, ForgeDirection.NORTH, hit(1, 2, 3, 1)));
    }

    @Test
    public void nonBlockHitIsIgnored() {
        StubActions actions = new StubActions();
        BucketInteraction interaction = new BucketInteraction(actions);

        assertNull(interaction.use(turtle, null, player(), empty, ForgeDirection.NORTH, null));
        assertFalse(interaction.canUse(turtle, player(), empty, ForgeDirection.NORTH, null));

        MovingObjectPosition entity = new MovingObjectPosition(
            org.mockito.Mockito.mock(net.minecraft.entity.Entity.class));
        assertNull(interaction.use(turtle, null, player(), empty, ForgeDirection.NORTH, entity));
        assertEquals(0, actions.calls.size());
    }

    @Test
    public void emptyBucketFillsFromTheHitBlock() throws Exception {
        StubActions actions = new StubActions();
        BucketInteraction interaction = new BucketInteraction(actions);
        FakePlayer player = player();

        TurtleCommandResult result = interaction
            .use(turtle, null, player, empty, ForgeDirection.NORTH, hit(10, 64, 10, 1));

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("bucket", result.getResults()[0]);
        assertEquals("fill", result.getResults()[1]);
        assertEquals(1, actions.calls.size());
        assertEquals(10, actions.fillX, "the fluid is taken from the hit block itself");
        assertEquals(64, actions.fillY);
        assertEquals(10, actions.fillZ);
    }

    @Test
    public void filledBucketEmptiesAgainstTheClickedSide() throws Exception {
        StubActions actions = new StubActions();
        actions.filledMode = true;
        BucketInteraction interaction = new BucketInteraction(actions);
        FakePlayer player = player();

        TurtleCommandResult result = interaction
            .use(turtle, null, player, filled, ForgeDirection.NORTH, hit(10, 64, 10, 1));

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("place", result.getResults()[1]);
        assertEquals(10, actions.placeX, "side 1 (+Y) places above the hit block");
        assertEquals(65, actions.placeY);
        assertEquals(10, actions.placeZ);
    }

    @Test
    public void eachSidePlacesAgainstTheRightNeighbour() throws Exception {
        int[][] offsets = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };

        for (int side = 0; side < 6; side++) {
            StubActions actions = new StubActions();
            actions.filledMode = true;
            BucketInteraction interaction = new BucketInteraction(actions);

            interaction.use(turtle, null, player(), filled, ForgeDirection.NORTH, hit(10, 64, 10, side));

            assertEquals(10 + offsets[side][0], actions.placeX, "side " + side + " x");
            assertEquals(64 + offsets[side][1], actions.placeY, "side " + side + " y");
            assertEquals(10 + offsets[side][2], actions.placeZ, "side " + side + " z");
        }
    }

    @Test
    public void invalidSideIsIgnored() {
        StubActions actions = new StubActions();
        actions.filledMode = true;
        BucketInteraction interaction = new BucketInteraction(actions);

        assertNull(interaction.use(turtle, null, player(), filled, ForgeDirection.NORTH, hit(10, 64, 10, 6)));
        assertEquals(0, actions.calls.size());
        assertFalse(interaction.canUse(turtle, player(), filled, ForgeDirection.NORTH, hit(10, 64, 10, 6)));
    }

    @Test
    public void failedActionsLeaveTheStackAlone() throws Exception {
        StubActions actions = new StubActions();
        actions.fillResult = false;
        BucketInteraction interaction = new BucketInteraction(actions);
        FakePlayer player = player();
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, empty);

        assertNull(interaction.use(turtle, null, player, empty, ForgeDirection.NORTH, hit(10, 64, 10, 1)));
        assertEquals(2, player.inventory.getStackInSlot(0).stackSize, "nothing was filled, so the stack is untouched");
    }

    @Test
    public void aSingleBucketIsReplacedInPlace() throws Exception {
        StubActions actions = new StubActions();
        BucketInteraction interaction = new BucketInteraction(actions);
        FakePlayer player = player();
        player.inventory.currentItem = 3;
        ItemStack single = new ItemStack(emptyItem, 1);
        player.inventory.setInventorySlotContents(3, single);

        interaction.use(turtle, null, player, single, ForgeDirection.NORTH, hit(10, 64, 10, 1));

        ItemStack held = player.inventory.getStackInSlot(3);
        assertNotNull(held);
        assertEquals(1, held.stackSize);
        assertEquals(filledItem, held.getItem(), "the filled container replaces the held bucket");
    }

    @Test
    public void aStackKeepsTheRemainderAndAddsTheContainer() throws Exception {
        StubActions actions = new StubActions();
        BucketInteraction interaction = new BucketInteraction(actions);
        FakePlayer player = player();
        player.inventory.currentItem = 0;
        ItemStack stack = new ItemStack(emptyItem, 3);
        player.inventory.setInventorySlotContents(0, stack);

        TurtleCommandResult result = interaction
            .use(turtle, null, player, stack, ForgeDirection.NORTH, hit(10, 64, 10, 1));
        System.out.println(
            "DEBUG calls=" + actions.calls
                + " success="
                + result
                + " size="
                + stack.stackSize
                + " slot0="
                + player.inventory.getStackInSlot(0)
                + " slot1="
                + player.inventory.getStackInSlot(1));

        assertEquals(2, player.inventory.getStackInSlot(0).stackSize, "one bucket was used");
        ItemStack container = player.inventory.getStackInSlot(1);
        assertNotNull(container, "the filled bucket should land in a spare slot");
        assertEquals(1, container.stackSize);
        assertEquals(filledItem, container.getItem());
    }

    @Test
    public void canUseMirrorsTheTargetPosition() {
        StubActions actions = new StubActions();
        actions.filledMode = true;
        actions.canPlace = true;
        BucketInteraction interaction = new BucketInteraction(actions);

        assertTrue(interaction.canUse(turtle, player(), filled, ForgeDirection.NORTH, hit(10, 64, 10, 1)));

        actions.canPlace = false;
        assertFalse(interaction.canUse(turtle, player(), filled, ForgeDirection.NORTH, hit(10, 64, 10, 1)));

        actions.filledMode = false;
        actions.canFill = false;
        assertFalse(interaction.canUse(turtle, player(), empty, ForgeDirection.NORTH, hit(10, 64, 10, 1)));

        actions.canFill = true;
        assertTrue(interaction.canUse(turtle, player(), empty, ForgeDirection.NORTH, hit(10, 64, 10, 1)));
    }

    @Test
    public void bucketsAreNeverSwung() throws Exception {
        StubActions actions = new StubActions();
        BucketInteraction interaction = new BucketInteraction(actions);

        assertFalse(interaction.canSwing(turtle, player(), empty, ForgeDirection.NORTH, hit(10, 64, 10, 1)));
        assertNull(interaction.swing(turtle, null, player(), empty, ForgeDirection.NORTH, hit(10, 64, 10, 1)));
    }
}
