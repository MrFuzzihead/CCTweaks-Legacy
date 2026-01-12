package org.squiddev.cctweaks.mixins.late;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockCable;
import dan200.computercraft.shared.peripheral.modem.IReceiver;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import dan200.computercraft.shared.peripheral.modem.TileModemBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.api.network.INetworkHelpers;
import org.squiddev.cctweaks.api.network.IWorldNetworkNode;
import org.squiddev.cctweaks.api.network.IWorldNetworkNodeHost;
import org.squiddev.cctweaks.api.network.NetworkAPI;
import org.squiddev.cctweaks.api.network.Packet;
import org.squiddev.cctweaks.core.FmlEvents;
import org.squiddev.cctweaks.core.network.cable.SingleModemCable;
import org.squiddev.cctweaks.core.network.modem.DirectionalPeripheralModem;
import org.squiddev.cctweaks.core.utils.Helpers;

@Mixin(TileCable.class)
public abstract class TileCable_Mixin extends TileModemBase implements IWorldNetworkNodeHost, IWorldPosition {
    @Unique
    private static final double cCTweaks_Legacy$MIN = 0.375;
    @Unique
    private static final double cCTweaks_Legacy$MAX = 1 - cCTweaks_Legacy$MIN;

    @Shadow(remap = false)
    private boolean m_destroyed;
    @Final
    @Shadow(remap = false)
    private static IIcon[] s_cableIcons;
    @Shadow(remap = false)
    private boolean m_peripheralAccessAllowed;

    @Unique
    protected DirectionalPeripheralModem cCTweaks_Legacy$modem;
    @Unique
    protected SingleModemCable cCTweaks_Legacy$cable;
    @Unique
    protected NBTTagCompound cCTweaks_Legacy$lazyTag;
    @Unique
    private static final ThreadLocal<PeripheralType> cCTweaks_Legacy$type = new ThreadLocal<>();

    protected TileCable_Mixin(IIcon[] icons) {
        super(icons);
    }

    /**
     * The patcher doesn't enable constructors (yet) so we lazy load the modem
     *
     * @return The resulting modem
     */
    @Unique
    public DirectionalPeripheralModem cCTweaks_Legacy$getModem() {
        if (cCTweaks_Legacy$modem == null) {
            return cCTweaks_Legacy$modem = new DirectionalPeripheralModem() {
                @Override
                public int getDirection() {
                    return TileCable_Mixin.this.getDirection();
                }

                @Override
                public IWorldPosition getPosition() {
                    return TileCable_Mixin.this;
                }

                @Override
                public void setPeripheralEnabled(boolean peripheralEnabled) {
                    super.setPeripheralEnabled(peripheralEnabled);

                    // Required for OpenPeripheral's PeripheralProxy
                    // https://github.com/OpenMods/OpenPeripheral-Addons/blob/master/src/main/java/openperipheral/addons/peripheralproxy/TileEntityPeripheralProxy.java#L23-L32
                    m_peripheralAccessAllowed = peripheralEnabled;
                }

                @Override
                protected boolean isPeripheralEnabled() {
                    return super.isPeripheralEnabled() && !m_destroyed
                        && cCTweaks_Legacy$getPeripheralTypeSafe() == PeripheralType.WiredModemWithCable;
                }
            };
        }
        return cCTweaks_Legacy$modem;
    }

    @Unique
    public SingleModemCable cCTweaks_Legacy$getCable() {
        if (cCTweaks_Legacy$cable == null) {
            return cCTweaks_Legacy$cable = new SingleModemCable() {
                @Override
                public DirectionalPeripheralModem getModem() {
                    return TileCable_Mixin.this.cCTweaks_Legacy$getModem();
                }

                @Override
                public IWorldPosition getPosition() {
                    return TileCable_Mixin.this;
                }

                @Override
                public boolean canConnect(ForgeDirection direction) {
                    // Can't be visited by other nodes if it is destroyed
                    if (m_destroyed || worldObj == null) return false;

                    // Or has no cable or is the side it is facing
                    PeripheralType type = cCTweaks_Legacy$getPeripheralTypeSafe();
                    return type == PeripheralType.Cable
                        || (type == PeripheralType.WiredModemWithCable && direction.ordinal() != getDirection());
                }
            };
        }
        return cCTweaks_Legacy$cable;
    }

    /**
     * Shouldn't throw NPEs. You never know though.
     *
     * @return The type of this peripheral
     */
    @Unique
    public PeripheralType cCTweaks_Legacy$getPeripheralTypeSafe() {
        int metadata = getBlockMetadata();
        if (metadata < 6) {
            return PeripheralType.WiredModem;
        } else if (metadata < 12) {
            return PeripheralType.WiredModemWithCable;
        } else if (metadata == 13) {
            return PeripheralType.Cable;
        } else {
            return PeripheralType.WiredModem;
        }
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting destroy
     */
    @Overwrite(remap = false)
    public void destroy() {
        if (!m_destroyed) {
            m_destroyed = true;
            cCTweaks_Legacy$getModem().destroy();
            cCTweaks_Legacy$getCable().destroy();
        }
        super.destroy();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        destroy();
    }

    @Override
    public void validate() {
        super.validate();
        if (!worldObj.isRemote) {
            FmlEvents.schedule(new Runnable() {
                @Override
                public void run() {
                    // In case the tile is removed within this tick
                    if (worldObj != null && worldObj.getBlock(xCoord, yCoord, zCoord) == ComputerCraft.Blocks.cable) {
                        cCTweaks_Legacy$getCable().connect();
                        if (cCTweaks_Legacy$lazyTag != null) {
                            cCTweaks_Legacy$readLazyNBT(cCTweaks_Legacy$lazyTag);
                            cCTweaks_Legacy$lazyTag = null;
                        }
                    }
                }
            });
        }
    }

    @Inject(at = @At("HEAD"), method = "onNeighbourChange", remap = false)
    private void StorePeripheralType(CallbackInfo ci) {
        // Update the neighbor first as this might break the type
        cCTweaks_Legacy$type.set(cCTweaks_Legacy$getPeripheralTypeSafe());
    }

    @Inject(at = @At("TAIL"), method = "onNeighbourChange", remap = false)
    private void UsePeripheralType(CallbackInfo ci) {
        if (cCTweaks_Legacy$type.get() == PeripheralType.WiredModemWithCable) {
            if (cCTweaks_Legacy$getModem().updateEnabled()) {
                cCTweaks_Legacy$modem.getAttachedNetwork().invalidateNode(cCTweaks_Legacy$modem);
                updateAnim();
            }
        }
        cCTweaks_Legacy$type.remove();
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting onActivate
     */
    @Overwrite(remap = false)
    public boolean onActivate(EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (cCTweaks_Legacy$getPeripheralTypeSafe() == PeripheralType.WiredModemWithCable && !player.isSneaking()) {
            if (!worldObj.isRemote) {

                String oldPeriphName = cCTweaks_Legacy$getModem().getPeripheralName();
                cCTweaks_Legacy$getModem().toggleEnabled();
                String periphName = cCTweaks_Legacy$getModem().getPeripheralName();

                if (!Helpers.equals(periphName, oldPeriphName)) {
                    if (oldPeriphName != null) {
                        player.addChatMessage(
                            new ChatComponentTranslation(
                                "gui.computercraft:wired_modem.peripheral_disconnected",
                                oldPeriphName));
                    }
                    if (periphName != null) {
                        player.addChatMessage(
                            new ChatComponentTranslation(
                                "gui.computercraft:wired_modem.peripheral_connected",
                                periphName));
                    }

                    cCTweaks_Legacy$getModem().getAttachedNetwork()
                        .invalidateNode(cCTweaks_Legacy$modem);
                    updateAnim();
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting readFromNBT
     */
    @Overwrite(remap = false)
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        cCTweaks_Legacy$getModem().id = tag.getInteger("peripheralID");
        if (worldObj == null) {
            cCTweaks_Legacy$lazyTag = tag;
        } else {
            cCTweaks_Legacy$readLazyNBT(tag);
        }
    }

    @Unique
    protected void cCTweaks_Legacy$readLazyNBT(NBTTagCompound tag) {
        cCTweaks_Legacy$getModem().setPeripheralEnabled(tag.getBoolean("peripheralAccess"));
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting writeToNBT
     */
    @Overwrite(remap = false)
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (cCTweaks_Legacy$lazyTag != null) {
            tag.setBoolean("peripheralAccess", tag.getBoolean("peripheralAccess"));
            tag.setInteger("peripheralID", tag.getInteger("peripheralID"));
        } else {
            tag.setBoolean("peripheralAccess", cCTweaks_Legacy$modem.isEnabled());
            tag.setInteger("peripheralID", cCTweaks_Legacy$modem.id);
        }
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting createPeripheral
     */
    @Overwrite(remap = false)
    protected ModemPeripheral createPeripheral() {
        return cCTweaks_Legacy$getModem().modem;
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting updateAnim
     */
    @Overwrite(remap = false)
    protected void updateAnim() {
        cCTweaks_Legacy$getModem().refreshState();
        setAnim(cCTweaks_Legacy$modem.state);
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting getPeripheral
     */
    @Overwrite(remap = false)
    public IPeripheral getPeripheral(int side) {
        return side == getDirection() && cCTweaks_Legacy$getPeripheralTypeSafe() != PeripheralType.Cable ? cCTweaks_Legacy$getModem().modem : null;
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting updateEntity
     */
    @Overwrite(remap = false)
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.isRemote) return;

        if (cCTweaks_Legacy$getModem().modem.pollChanged()) updateAnim();
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting addReceiver
     */
    @Overwrite(remap = false)
    public void addReceiver(IReceiver receiver) {
        cCTweaks_Legacy$getModem().addReceiver(receiver);
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting removeReceiver
     */
    @Overwrite(remap = false)
    public void removeReceiver(IReceiver receiver) {
        cCTweaks_Legacy$getModem().removeReceiver(receiver);
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting transmit
     */
    @Overwrite(remap = false)
    public void transmit(int channel, int replyChannel, Object payload, double range, double xPos, double yPos,
                         double zPos, Object senderObject) {
        cCTweaks_Legacy$getModem().transmit(channel, replyChannel, payload, range, xPos, yPos, zPos, senderObject);
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating attachPeripheral
     */
    @Overwrite(remap = false)
    @Deprecated
    private void attachPeripheral(String name, IPeripheral peripheral) {
        cCTweaks_Legacy$getModem().attachPeripheral(name, peripheral);
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating detachPeripheral
     */
    @Overwrite(remap = false)
    @Deprecated
    private void detachPeripheral(String name) {
        cCTweaks_Legacy$getModem().detachPeripheral(name);
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating getTypeRemote
     */
    @Overwrite(remap = false)
    @Deprecated
    private String getTypeRemote(String remoteName) {
        return null;
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating getMethodNamesRemote
     */
    @Overwrite(remap = false)
    @Deprecated
    private String[] getMethodNamesRemote(String remoteName) {
        return null;
    }

    /**
     * @author MrFuzzihead
     * @reason rewriting callMethodRemote
     */
    @Overwrite(remap = false)
    @Deprecated
    private Object[] callMethodRemote(String remoteName, ILuaContext context, String method, Object[] arguments) {
        return null;
    }

    /**
     * @author MrFuzzihead
     * @reason Rewriting networkChanged
     */
    @Overwrite(remap = false)
    public void networkChanged() {
        cCTweaks_Legacy$getCable().updateConnections();
        if (!worldObj.isRemote) {
            cCTweaks_Legacy$getModem().getAttachedNetwork()
                .invalidateNode(cCTweaks_Legacy$modem);
        }
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating dispatchPacket
     */
    @Overwrite(remap = false)
    @Deprecated
    private void dispatchPacket(TileCable.Packet packet) {
        Packet cCTweaks_Legacy$Packet = new Packet(packet.channel, packet.replyChannel, packet.payload, packet.senderObject);
        cCTweaks_Legacy$getModem().getAttachedNetwork()
            .transmitPacket(cCTweaks_Legacy$modem, cCTweaks_Legacy$Packet);
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating receivePacket
     */
    @Overwrite(remap = false)
    @Deprecated
    private void receivePacket(TileCable.Packet packet, int distanceTravelled) {
        Packet cCTweaks_Legacy$Packet = new Packet(packet.channel, packet.replyChannel, packet.payload, packet.senderObject);
        cCTweaks_Legacy$getModem().receivePacket(cCTweaks_Legacy$Packet, distanceTravelled);
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating findPeripherals
     */
    @Overwrite(remap = false)
    @Deprecated
    private void findPeripherals() {
        // Deprecated method, do nothing
    }

    /**
     * @author MrFuzzihead
     * @reason This is needed for OpenPeripherals' peripheral proxy, see <a href="https://github.com/OpenMods/OpenPeripheral-Addons/blob/master/src/main/java/openperipheral/addons/peripheralproxy/TileEntityPeripheralProxy.java#L23-L32">...</a>
     */
    @Overwrite(remap = false)
    public void togglePeripheralAccess() {
        cCTweaks_Legacy$getModem().toggleEnabled();
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating getConnectedPeripheralName
     */
    @Overwrite(remap = false)
    @Deprecated
    public String getConnectedPeripheralName() {
        return cCTweaks_Legacy$getModem().getPeripheralName();
    }

    /**
     * @author MrFuzzihead
     * @reason deprecating getConnectedPeripheral
     */
    @Overwrite(remap = false)
    @Deprecated
    private IPeripheral getConnectedPeripheral() {
        return cCTweaks_Legacy$getModem().isEnabled() ? cCTweaks_Legacy$modem.getPeripheral() : null;
    }

    @Override
    protected boolean isAttached() {
        return cCTweaks_Legacy$getModem().modem.getComputer() != null;
    }

    /**
     * @author MrFuzzihead
     * @reason Rewriting the getCableBounds method for some reason
     */
    @Overwrite(remap = false)
    public AxisAlignedBB getCableBounds() {
        int x = xCoord, y = yCoord, z = zCoord;
        IBlockAccess world = worldObj;

        INetworkHelpers helpers = NetworkAPI.helpers();
        return AxisAlignedBB.getBoundingBox(
            helpers.canConnect(world, x, y, z, ForgeDirection.WEST) ? 0 : cCTweaks_Legacy$MIN,
            helpers.canConnect(world, x, y, z, ForgeDirection.DOWN) ? 0 : cCTweaks_Legacy$MIN,
            helpers.canConnect(world, x, y, z, ForgeDirection.NORTH) ? 0 : cCTweaks_Legacy$MIN,
            helpers.canConnect(world, x, y, z, ForgeDirection.EAST) ? 1 : cCTweaks_Legacy$MAX,
            helpers.canConnect(world, x, y, z, ForgeDirection.UP) ? 1 : cCTweaks_Legacy$MAX,
            helpers.canConnect(world, x, y, z, ForgeDirection.SOUTH) ? 1 : cCTweaks_Legacy$MAX);
    }

    /**
     * @author MrFuzzihead
     * @reason Rewriting the getTexture method for some reason
     */
    @Overwrite(remap = false)
    public IIcon getTexture(int side) {
        PeripheralType type = BlockCable.renderAsModem ? PeripheralType.WiredModem : cCTweaks_Legacy$getPeripheralTypeSafe();

        if (type == PeripheralType.Cable || type == PeripheralType.WiredModemWithCable) {
            int dir = -1;
            if (type == PeripheralType.WiredModemWithCable) {
                dir = getDirection();
                dir -= dir % 2;
            }

            int x = xCoord, y = yCoord, z = zCoord;
            IBlockAccess world = worldObj;

            INetworkHelpers helpers = NetworkAPI.helpers();
            if (helpers.canConnect(world, x, y, z, ForgeDirection.EAST)
                || helpers.canConnect(world, x, y, z, ForgeDirection.WEST)) {
                dir = dir == -1 || dir == 4 ? 4 : -2;
            }
            if (helpers.canConnect(world, x, y, z, ForgeDirection.UP)
                || helpers.canConnect(world, x, y, z, ForgeDirection.DOWN)) {
                dir = dir == -1 || dir == 0 ? 0 : -2;
            }
            if (helpers.canConnect(world, x, y, z, ForgeDirection.NORTH)
                || helpers.canConnect(world, x, y, z, ForgeDirection.SOUTH)) {
                dir = dir == -1 || dir == 2 ? 2 : -2;
            }

            if (dir == -1) dir = 2;
            return dir >= 0 && (side == dir || side == Facing.oppositeSide[dir]) ? s_cableIcons[1] : s_cableIcons[0];
        }

        return super.getTexture(side);
    }

    @Override
    public IWorldNetworkNode getNode() {
        return cCTweaks_Legacy$getCable();
    }

    @Override
    public IBlockAccess getWorld() {
        return worldObj;
    }

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
}
