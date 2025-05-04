package org.squiddev.cctweaks.core.packet;

import org.squiddev.cctweaks.CommonProxy;
import org.squiddev.cctweaks.core.registry.Module;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class AbstractPacketHandler<T extends AbstractPacketHandler.IPacket> extends Module
    implements IMessageHandler<T, IMessage> {

    private final int id;
    private final Side side;
    private final Class<T> type;

    public AbstractPacketHandler(int id, Class<T> type) {
        this(id, Side.CLIENT, type);
    }

    public AbstractPacketHandler(int id, Side side, Class<T> type) {
        this.id = id;
        this.side = side;
        this.type = type;
    }

    @Override
    public void preInit() {
        super.preInit();
        CommonProxy.NETWORK.registerMessage(this, type, id, side);
    }

    @Override
    public IMessage onMessage(T message, MessageContext ctx) {
        return message.handle(ctx);
    }

    public interface IPacket extends IMessage {

        IMessage handle(MessageContext ctx);
    }
}
