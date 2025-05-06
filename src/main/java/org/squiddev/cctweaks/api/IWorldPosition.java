package org.squiddev.cctweaks.api;

import net.minecraft.world.IBlockAccess;

public interface IWorldPosition {

    IBlockAccess getWorld();

    int getX();

    int getY();

    int getZ();
}
