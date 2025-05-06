package org.squiddev.cctweaks.api.network;

import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.api.IWorldPosition;

public interface IWorldNetworkNode extends INetworkNode {

    IWorldPosition getPosition();

    boolean canConnect(ForgeDirection var1);
}
