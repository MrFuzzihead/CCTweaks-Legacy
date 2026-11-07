package org.squiddev.cctweaks.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.squiddev.cctweaks.api.IWorldPosition;

/**
 * Base tile for all TileEntities
 */
public abstract class TileBase extends TileEntity implements IWorldPosition {

    private boolean destroyed;

    @Override
    public int getX() {
        return xCoord;
    }

    @Override
    public int getY() {
        return yCoord;
    }

    @Override
    public int getZ() {
        return zCoord;
    }

    @Override
    public IBlockAccess getWorld() {
        return worldObj;
    }

    /**
     * Called when the TileEntity is validated
     */
    public void create() {}

    /**
     * Called when the TileEntity is destroyed
     */
    public void destroy() {}

    @Override
    public void validate() {
        super.validate();
        destroyed = false;
        if (worldObj == null || !worldObj.isRemote) {
            create();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (worldObj == null || !worldObj.isRemote) {
            destroyOnce();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj == null || !worldObj.isRemote) {
            destroyOnce();
        }
    }

    /**
     * Destroy this tile exactly once. Chunk unload, block removal and {@link #invalidate()} can all
     * fire for the same tile; callers that need to destroy explicitly should use this instead of
     * {@link #destroy()}.
     */
    public final void destroyOnce() {
        if (!destroyed) {
            destroyed = true;
            destroy();
        }
    }

    /**
     * Called to save data for the client
     *
     * @param tag The data to send
     * @return If data needs to be sent
     */
    protected boolean writeDescription(NBTTagCompound tag) {
        return false;
    }

    /**
     * Read data from the server
     *
     * @param tag The data to read
     */
    protected void readDescription(NBTTagCompound tag) {}

    @Override
    public final Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        return writeDescription(tag) ? new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag) : null;
    }

    @Override
    public final void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        switch (packet.func_148853_f()) {
            case 0:
                readDescription(packet.func_148857_g());
                break;
        }
    }

    /**
     * Improvement over {@link #markDirty()}
     */
    public void markForUpdate() {
        if (worldObj == null) return;
        markDirty();
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /**
     * Called when the block is activated
     *
     * @param player The player who triggered this
     * @param side   The side the block is activated on
     * @return If the event succeeded
     */
    public boolean onActivated(EntityPlayer player, int side) {
        return false;
    }

    /**
     * Called when a neighbor tile/block changed
     */
    public void onNeighborChanged() {}
}
