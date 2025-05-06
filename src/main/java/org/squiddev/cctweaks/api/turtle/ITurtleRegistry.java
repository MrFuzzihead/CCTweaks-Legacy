package org.squiddev.cctweaks.api.turtle;

import net.minecraft.item.Item;

public interface ITurtleRegistry {

    void registerInteraction(ITurtleInteraction var1);

    void registerInteraction(Item var1, ITurtleInteraction var2);
}
