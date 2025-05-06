package org.squiddev.cctweaks.api.turtle;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;

public interface IExtendedTurtleUpgrade extends ITurtleUpgrade {

    void upgradeChanged(ITurtleAccess var1, TurtleSide var2, ITurtleUpgrade var3, ITurtleUpgrade var4);

    boolean alsoPeripheral();
}
