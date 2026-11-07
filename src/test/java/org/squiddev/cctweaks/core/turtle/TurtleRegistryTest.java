package org.squiddev.cctweaks.core.turtle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.squiddev.cctweaks.api.turtle.AbstractTurtleInteraction;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

public class TurtleRegistryTest {

    private final ITurtleAccess turtle = Mockito.mock(ITurtleAccess.class);
    private final IComputerAccess computer = Mockito.mock(IComputerAccess.class);
    private final FakePlayer player = null;
    private final ForgeDirection direction = ForgeDirection.NORTH;
    private final MovingObjectPosition hit = null;

    private static final class RecordingInteraction extends AbstractTurtleInteraction {

        final TurtleCommandResult useResult;
        final boolean canUse;
        int uses;

        private RecordingInteraction(TurtleCommandResult useResult, boolean canUse) {
            this.useResult = useResult;
            this.canUse = canUse;
        }

        @Override
        public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player,
            ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) {
            uses++;
            return useResult;
        }

        @Override
        public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
            MovingObjectPosition hit) {
            return canUse;
        }
    }

    @Test
    public void itemInteractionRunsBeforeGeneric() throws Exception {
        TurtleRegistry registry = new TurtleRegistry();
        Item item = new Item();
        ItemStack stack = new ItemStack(item);

        RecordingInteraction generic = new RecordingInteraction(
            TurtleCommandResult.success(new Object[] { "generic" }),
            true);
        RecordingInteraction specific = new RecordingInteraction(
            TurtleCommandResult.success(new Object[] { "specific" }),
            true);

        registry.registerInteraction(generic);
        registry.registerInteraction(item, specific);

        TurtleCommandResult result = registry.use(turtle, computer, player, stack, direction, hit);

        assertEquals("specific", result.getResults()[0]);
        assertEquals(1, specific.uses);
        assertEquals(0, generic.uses);
    }

    @Test
    public void firstNonNullResultWins() throws Exception {
        TurtleRegistry registry = new TurtleRegistry();
        ItemStack stack = new ItemStack(new Item());

        RecordingInteraction first = new RecordingInteraction(null, false);
        RecordingInteraction second = new RecordingInteraction(
            TurtleCommandResult.success(new Object[] { "second" }),
            true);

        registry.registerInteraction(first);
        registry.registerInteraction(second);

        TurtleCommandResult result = registry.use(turtle, computer, player, stack, direction, hit);
        assertEquals("second", result.getResults()[0]);
        assertEquals(1, first.uses);
        assertEquals(1, second.uses);
    }

    @Test
    public void canUseIsOrOfInteractions() {
        TurtleRegistry registry = new TurtleRegistry();
        ItemStack stack = new ItemStack(new Item());

        registry.registerInteraction(new RecordingInteraction(null, false));
        registry.registerInteraction(new RecordingInteraction(null, true));

        assertTrue(registry.canUse(turtle, player, stack, direction, hit));
    }

    @Test
    public void nullStackIsIgnored() throws Exception {
        TurtleRegistry registry = new TurtleRegistry();

        assertNull(registry.use(turtle, computer, player, null, direction, hit));
        assertNull(registry.swing(turtle, computer, player, null, direction, hit));
        assertFalse(registry.canUse(turtle, player, null, direction, hit));
        assertFalse(registry.canSwing(turtle, player, null, direction, hit));
    }

    @Test
    public void rejectsNullRegistrations() {
        TurtleRegistry registry = new TurtleRegistry();

        assertThrows(
            IllegalArgumentException.class,
            () -> registry.registerInteraction((org.squiddev.cctweaks.api.turtle.ITurtleInteraction) null));
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.registerInteraction(null, new RecordingInteraction(null, false)));
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.registerInteraction(new Item(), (org.squiddev.cctweaks.api.turtle.ITurtleInteraction) null));
    }
}
