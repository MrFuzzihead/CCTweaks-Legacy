package org.squiddev.cctweaks.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.api.turtle.AbstractTurtleInteraction;
import org.squiddev.cctweaks.core.turtle.TurtleRegistry;
import org.squiddev.cctweaks.testsupport.StubComputerAccess;
import org.squiddev.cctweaks.testsupport.StubTurtleAccess;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

/**
 * Small end-to-end harness: a stub turtle + stub computer drive the real registry with no
 * Minecraft world or ComputerCraft server. This is the pattern the remaining World-dependent
 * tests follow.
 */
public class HeadlessTurtleHarnessTest {

    private static final class RecordingInteraction extends AbstractTurtleInteraction {

        ITurtleAccess seenTurtle;
        IComputerAccess seenComputer;
        ItemStack seenStack;

        @Override
        public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer,
            net.minecraftforge.common.util.FakePlayer player, ItemStack stack, ForgeDirection direction,
            MovingObjectPosition hit) {
            seenTurtle = turtle;
            seenComputer = computer;
            seenStack = stack;
            return TurtleCommandResult.success(new Object[] { "used" });
        }
    }

    @Test
    public void stubTurtleAndComputerDriveTheRealRegistry() throws Exception {
        StubTurtleAccess turtle = new StubTurtleAccess(null, 16);
        StubComputerAccess computer = new StubComputerAccess(12, "back");
        ItemStack stack = new ItemStack(new Item());

        RecordingInteraction interaction = new RecordingInteraction();
        TurtleRegistry registry = new TurtleRegistry();
        registry.registerInteraction(interaction);

        TurtleCommandResult result = registry.use(turtle, computer, null, stack, ForgeDirection.NORTH, null);

        assertEquals("used", result.getResults()[0]);
        assertSame(turtle, interaction.seenTurtle);
        assertSame(computer, interaction.seenComputer);
        assertSame(stack, interaction.seenStack);
    }

    @Test
    public void stubTurtleTracksSelectionAndFuel() {
        StubTurtleAccess turtle = new StubTurtleAccess(null, 16);
        turtle.setSelectedSlot(5);
        turtle.setFuelLevel(10);

        assertEquals(5, turtle.getSelectedSlot());
        assertEquals(10, turtle.getFuelLevel());
        assertEquals(0, turtle.getSlot(5) == null ? 0 : 1);
    }
}
