package org.squiddev.cctweaks.api.network;

import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.squiddev.cctweaks.api.IWorldPosition;

public interface INetworkHelpers {

    boolean canConnect(IBlockAccess var1, int var2, int var3, int var4, ForgeDirection var5);

    boolean canConnect(IWorldPosition var1, ForgeDirection var2);

    Set<INetworkNode> getAdjacentNodes(IWorldNetworkNode var1);

    Set<INetworkNode> getAdjacentNodes(IWorldNetworkNode var1, boolean var2);

    void joinOrCreateNetwork(IWorldNetworkNode var1);

    void joinOrCreateNetwork(INetworkNode var1, Set<? extends INetworkNode> var2);

    void joinNewNetwork(INetworkNode var1);

    void scheduleJoin(IWorldNetworkNode var1);

    void scheduleJoin(IWorldNetworkNode var1, TileEntity var2);
}
