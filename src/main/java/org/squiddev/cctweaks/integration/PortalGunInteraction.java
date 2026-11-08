package org.squiddev.cctweaks.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.api.turtle.AbstractTurtleInteraction;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import portalgun.common.core.EntityHelper;
import portalgun.common.item.ItemPortalGun;

/**
 * Turtle interaction for the Portal Gun mod: swinging or using a portal gun fires a portal.
 *
 * <p>
 * Extracted from the anonymous class in {@link PortalGunIntegration} so the click semantics can be
 * tested without a live PortalGun world: the {@link Shooter} seam replaces
 * {@link EntityHelper#shootPortal}.
 * </p>
 */
public class PortalGunInteraction extends AbstractTurtleInteraction {

    /**
     * PortalGun's own semantics: left click fires the blue portal, right click the orange one.
     */
    public static final int BLUE = 1;
    public static final int ORANGE = 2;

    /**
     * Fires a portal. {@code portal} is {@link #BLUE} or {@link #ORANGE}.
     */
    public interface Shooter {

        void shoot(FakePlayer player, ItemStack stack, int portal);
    }

    private final Shooter shooter;

    public PortalGunInteraction() {
        this(new Shooter() {

            @Override
            public void shoot(FakePlayer player, ItemStack stack, int portal) {
                EntityHelper.shootPortal(player, stack, portal);
            }
        });
    }

    PortalGunInteraction(Shooter shooter) {
        this.shooter = shooter;
    }

    @Override
    public boolean canSwing(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        return isPortalGun(stack);
    }

    @Override
    public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        return isPortalGun(stack);
    }

    @Override
    public TurtleCommandResult swing(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
        if (!isPortalGun(stack)) return null;

        // swing is the left click, so it mirrors PortalGun's left-click keybind.
        shooter.shoot(player, stack, BLUE);
        return TurtleCommandResult.success(new Object[] { "portal" });
    }

    @Override
    public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
        if (!isPortalGun(stack)) return null;

        shooter.shoot(player, stack, ORANGE);
        return TurtleCommandResult.success(new Object[] { "portal" });
    }

    private static boolean isPortalGun(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemPortalGun;
    }
}
