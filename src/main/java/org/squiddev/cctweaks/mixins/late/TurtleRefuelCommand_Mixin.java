package org.squiddev.cctweaks.mixins.late;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.turtle.ITurtleFuelProvider;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.core.TurtleRefuelCommand;

@Mixin(TurtleRefuelCommand.class)
public abstract class TurtleRefuelCommand_Mixin implements ITurtleCommand {

    @Shadow(remap = false)
    @Final
    private int m_limit;

    /**
     * @author MrFuzzihead
     * @reason Keeping the original patch but moving it to Mixins. Will eventually merge into ComputerCraft source.
     */
    @Overwrite(remap = false)
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        ItemStack stack = turtle.getInventory()
            .getStackInSlot(turtle.getSelectedSlot());
        if (stack == null) {
            return TurtleCommandResult.failure("No items to combust");
        }

        ITurtleFuelProvider source = CCTweaksAPI.instance()
            .fuelRegistry()
            .getProvider(turtle, stack);
        if (source != null) {
            if (m_limit != 0) {
                turtle.addFuel(source.refuel(turtle, stack, m_limit));
                turtle.playAnimation(TurtleAnimation.Wait);
            }
            return TurtleCommandResult.success();
        }

        return TurtleCommandResult.failure("Items not combustible");
    }
}
