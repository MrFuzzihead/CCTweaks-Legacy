package org.squiddev.cctweaks.selfTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

/**
 * The self test turtle is the runtime stand-in for {@code testsupport.StubTurtleAccess}; if its
 * inventory bookkeeping is wrong, every in-world check that goes through it is meaningless.
 */
public class SelfTestTurtleTest {

    /**
     * Items are constructed directly, not taken from {@code net.minecraft.init.Items}: that class
     * is populated during game bootstrap, so in a plain JUnit JVM every entry is still null and
     * all stacks would compare equal.
     */
    private static Item item() {
        return new Item();
    }

    @Test
    public void slotsAndSelectedSlotAreIndependent() {
        SelfTestTurtle turtle = new SelfTestTurtle(null, 16);

        turtle.setSlot(3, new ItemStack(item(), 5));
        turtle.setSelectedSlot(3);

        assertEquals(5, turtle.getSlot(3).stackSize);
        assertEquals(3, turtle.getSelectedSlot());
        assertEquals(
            16,
            turtle.getInventory()
                .getSizeInventory());
    }

    @Test
    public void countSumsEverySlot() {
        SelfTestTurtle turtle = new SelfTestTurtle(null, 16);
        Item gold = item();
        Item apple = item();
        Item diamond = item();

        turtle.setSlot(0, new ItemStack(gold, 2));
        turtle.setSlot(9, new ItemStack(gold, 3));
        turtle.setSlot(10, new ItemStack(apple, 4));

        assertEquals(5, turtle.count(gold));
        assertEquals(4, turtle.count(apple));
        assertEquals(0, turtle.count(diamond));
    }

    @Test
    public void positionIsDefensive() {
        SelfTestTurtle turtle = new SelfTestTurtle(null, 16).setPosition(1, 2, 3);
        turtle.getPosition().posX = 99;

        assertEquals(1, turtle.getPosition().posX, "callers must not be able to move the turtle");
    }

    @Test
    public void directionAndTeleport() {
        SelfTestTurtle turtle = new SelfTestTurtle(null, 16);
        turtle.setDirection(5);
        assertEquals(5, turtle.getDirection());

        assertTrue(turtle.teleportTo(null, 7, 8, 9));
        assertEquals(7, turtle.getPosition().posX);
        assertEquals(8, turtle.getPosition().posY);
        assertEquals(9, turtle.getPosition().posZ);
    }

    @Test
    public void upgradesAndFuelAreInert() {
        SelfTestTurtle turtle = new SelfTestTurtle(null, 16);

        assertNull(turtle.getUpgrade(dan200.computercraft.api.turtle.TurtleSide.Left));
        assertTrue(turtle.consumeFuel(10), "the self test turtle never runs out of fuel");
        assertEquals(1000, turtle.getFuelLevel());
    }
}
