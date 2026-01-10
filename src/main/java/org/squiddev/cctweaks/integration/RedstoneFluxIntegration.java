package org.squiddev.cctweaks.integration;

import net.minecraft.item.ItemStack;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.turtle.ITurtleFuelProvider;

import cofh.api.energy.IEnergyContainerItem;
import dan200.computercraft.api.turtle.ITurtleAccess;

/**
 * Registers Redstone Flux as a fuel
 */
public class RedstoneFluxIntegration extends APIIntegration {

    public RedstoneFluxIntegration() {
        super("CoFHAPI|energy");
    }

    @Override
    public void init() {
        CCTweaksAPI.instance()
            .fuelRegistry()
            .addFuelProvider(new ITurtleFuelProvider() {

                @Override
                public boolean canRefuel(ITurtleAccess turtle, ItemStack stack) {
                    return CCTweaks.fluxRefuelAmount > 0 && stack.getItem() instanceof IEnergyContainerItem;
                }

                @Override
                public int refuel(ITurtleAccess turtle, ItemStack stack, int limit) {
                    int fluxAmount = CCTweaks.fluxRefuelAmount;

                    // Avoid over refueling
                    int maxRefuel = turtle.getFuelLimit() - turtle.getFuelLevel();
                    int fluxLimit = (limit >= 64 ? maxRefuel : limit) * fluxAmount;

                    IEnergyContainerItem container = (IEnergyContainerItem) stack.getItem();

                    // Don't want to pull to much
                    fluxLimit = Math.min(container.getMaxEnergyStored(stack), fluxLimit);

                    int change = 1;
                    int progress = 0;
                    while (progress < fluxLimit && change > 0) {
                        change = container.extractEnergy(stack, fluxLimit - progress, false);
                        progress += change;
                    }
                    return progress / fluxAmount;
                }
            });
    }
}
