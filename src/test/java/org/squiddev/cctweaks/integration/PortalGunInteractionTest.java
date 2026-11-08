package org.squiddev.cctweaks.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.testsupport.StubComputerAccess;
import org.squiddev.cctweaks.testsupport.StubTurtleAccess;

import dan200.computercraft.api.turtle.TurtleCommandResult;
import portalgun.common.item.ItemPortalGunBlue;
import portalgun.common.item.ItemPortalGunOrange;

/**
 * PortalGun is an optional dependency, so this is the only automated coverage of the integration.
 *
 * <p>
 * The regression it locks in: {@code use} used to key off the held item (blue gun → blue portal),
 * which made both {@code swing} and {@code use} fire a blue portal with a blue gun. PortalGun's
 * own bindings are left click = blue (1), right click = orange (2), so a turtle's {@code swing}
 * must fire blue and {@code use} must fire orange regardless of which gun is held.
 * </p>
 */
public class PortalGunInteractionTest {

    private final StubTurtleAccess turtle = new StubTurtleAccess(null, 16);
    private final StubComputerAccess computer = new StubComputerAccess(1, "back");

    private static final class RecordingShooter implements PortalGunInteraction.Shooter {

        final List<Integer> portals = new ArrayList<Integer>();
        final List<ItemStack> stacks = new ArrayList<ItemStack>();

        @Override
        public void shoot(FakePlayer player, ItemStack stack, int portal) {
            portals.add(portal);
            stacks.add(stack);
        }
    }

    @Test
    public void swingFiresBlueAndUseFiresOrange() throws Exception {
        RecordingShooter shooter = new RecordingShooter();
        PortalGunInteraction interaction = new PortalGunInteraction(shooter);
        ItemStack stack = new ItemStack(new ItemPortalGunBlue());

        TurtleCommandResult swing = interaction.swing(turtle, computer, null, stack, ForgeDirection.NORTH, null);
        TurtleCommandResult use = interaction.use(turtle, computer, null, stack, ForgeDirection.NORTH, null);

        assertNotNull(swing);
        assertNotNull(use);
        assertTrue(swing.isSuccess(), "swing should report success");
        assertTrue(use.isSuccess(), "use should report success");
        assertEquals("portal", swing.getResults()[0]);

        assertEquals(2, shooter.portals.size());
        assertEquals(
            PortalGunInteraction.BLUE,
            shooter.portals.get(0)
                .intValue(),
            "left click is blue");
        assertEquals(
            PortalGunInteraction.ORANGE,
            shooter.portals.get(1)
                .intValue(),
            "right click is orange");
        assertEquals(stack, shooter.stacks.get(0));
    }

    @Test
    public void orangeGunUsesTheSameMouseSemantics() throws Exception {
        RecordingShooter shooter = new RecordingShooter();
        PortalGunInteraction interaction = new PortalGunInteraction(shooter);
        ItemStack stack = new ItemStack(new ItemPortalGunOrange());

        interaction.swing(turtle, computer, null, stack, ForgeDirection.NORTH, null);
        interaction.use(turtle, computer, null, stack, ForgeDirection.NORTH, null);

        assertEquals(
            PortalGunInteraction.BLUE,
            shooter.portals.get(0)
                .intValue());
        assertEquals(
            PortalGunInteraction.ORANGE,
            shooter.portals.get(1)
                .intValue());
    }

    @Test
    public void nonPortalGunIsIgnored() throws Exception {
        RecordingShooter shooter = new RecordingShooter();
        PortalGunInteraction interaction = new PortalGunInteraction(shooter);
        ItemStack stack = new ItemStack(Items.stick);

        assertFalse(interaction.canSwing(turtle, null, stack, ForgeDirection.NORTH, null));
        assertFalse(interaction.canUse(turtle, null, stack, ForgeDirection.NORTH, null));
        assertNull(interaction.swing(turtle, computer, null, stack, ForgeDirection.NORTH, null));
        assertNull(interaction.use(turtle, computer, null, stack, ForgeDirection.NORTH, null));

        assertEquals(0, shooter.portals.size(), "nothing should have been fired");
    }

    @Test
    public void nullStackIsIgnored() throws Exception {
        RecordingShooter shooter = new RecordingShooter();
        PortalGunInteraction interaction = new PortalGunInteraction(shooter);

        assertFalse(interaction.canSwing(turtle, null, null, ForgeDirection.NORTH, null));
        assertFalse(interaction.canUse(turtle, null, null, ForgeDirection.NORTH, null));
        assertNull(interaction.swing(turtle, computer, null, null, ForgeDirection.NORTH, null));
        assertNull(interaction.use(turtle, computer, null, null, ForgeDirection.NORTH, null));
        assertEquals(0, shooter.portals.size());
    }

    @Test
    public void canSwingAndCanUseMatchASubclass() {
        RecordingShooter shooter = new RecordingShooter();
        PortalGunInteraction interaction = new PortalGunInteraction(shooter);
        ItemStack stack = new ItemStack(new ItemPortalGunOrange());

        assertTrue(interaction.canSwing(turtle, null, stack, ForgeDirection.NORTH, null));
        assertTrue(interaction.canUse(turtle, null, stack, ForgeDirection.NORTH, null));
    }
}
