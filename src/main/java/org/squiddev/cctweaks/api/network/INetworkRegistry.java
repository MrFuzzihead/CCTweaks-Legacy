package org.squiddev.cctweaks.api.network;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.squiddev.cctweaks.api.IWorldPosition;

public interface INetworkRegistry {

    void addNodeProvider(INetworkNodeProvider var1);

    boolean isNode(IBlockAccess var1, int var2, int var3, int var4);

    boolean isNode(TileEntity var1);

    boolean isNode(IWorldPosition var1);

    IWorldNetworkNode getNode(IBlockAccess var1, int var2, int var3, int var4);

    IWorldNetworkNode getNode(TileEntity var1);

    IWorldNetworkNode getNode(IWorldPosition var1);
}
