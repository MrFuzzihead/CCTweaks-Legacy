package org.squiddev.cctweaks.api.network;

import net.minecraft.tileentity.TileEntity;

public interface INetworkNodeProvider {

    IWorldNetworkNode getNode(TileEntity var1);

    boolean isNode(TileEntity var1);
}
