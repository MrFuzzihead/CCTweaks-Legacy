package org.squiddev.cctweaks.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.turtle.AbstractTurtleInteraction;
import org.squiddev.cctweaks.api.turtle.ITurtleRegistry;

import cpw.mods.fml.common.Optional;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import portalgun.common.core.EntityHelper;
import portalgun.common.item.ItemPortalGun;

/**
 * Adds turtle interaction for the portal gun mod
 */
public class PortalGunIntegration extends ModIntegration {

    public static final String MOD_NAME = "PortalGun";

    public PortalGunIntegration() {
        super(MOD_NAME);
    }

    @Override
    @Optional.Method(modid = MOD_NAME)
    public void init() {
        ITurtleRegistry registry = CCTweaksAPI.instance()
            .turtleRegistry();

        registry.registerInteraction(new AbstractTurtleInteraction() {

            @Override
            public boolean canSwing(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
                MovingObjectPosition hit) {
                return stack != null && stack.getItem() instanceof ItemPortalGun;
            }

            @Override
            public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
                MovingObjectPosition hit) {
                return stack != null && stack.getItem() instanceof ItemPortalGun;
            }

            @Override
            public TurtleCommandResult swing(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player,
                ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
                if (!(stack.getItem() instanceof ItemPortalGun)) return null;

                // PortalGun's mouse semantics are left click = blue portal (1), right click =
                // orange portal (2), regardless of which gun item is held. swing is the left
                // click, use is the right click, so mirror that here. Keying off the held item
                // (blue gun -> 1) made both actions fire blue with a blue gun.
                EntityHelper.shootPortal(player, stack, 1);
                return TurtleCommandResult.success(new Object[] { "portal" });
            }

            @Override
            public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player,
                ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
                if (!(stack.getItem() instanceof ItemPortalGun)) return null;

                EntityHelper.shootPortal(player, stack, 2);
                return TurtleCommandResult.success(new Object[] { "portal" });
            }
        });
    }
}
