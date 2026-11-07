package org.squiddev.cctweaks.items;

import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.blocks.debug.TileDebugPeripheral;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.lua.lib.cobalt.CobaltMachine;
import dan200.computercraft.shadow.org.squiddev.cobalt.LuaState;
import dan200.computercraft.shadow.org.squiddev.cobalt.lib.DebugLib;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.util.PeripheralUtil;

public class ItemDebugger extends ItemComputerAction {

    public ItemDebugger() {
        super("debugger");
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return CCTweaks.debugWandEnabled && super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    protected boolean useComputer(ItemStack stack, EntityPlayer player, TileComputerBase computerTile, int side) {
        if (!computerTile.isUsable(player, false)) return false;

        ServerComputer serverComputer = computerTile.getServerComputer();
        if (serverComputer == null) return false;

        try {
            Computer computer = serverComputer.getComputer();
            dan200.computercraft.core.lua.ILuaMachine luaMachine = computer.getLuaMachine();

            if (!(luaMachine instanceof CobaltMachine machine)) {
                return false;
            }

            LuaState state = machine.getLuaState();
            DebugLib.add(state);
        } catch (Exception e) {
            // The computer may be off (no Lua machine) or mid-restart (state being replaced).
            // This is a debug tool: log at debug and leave the wand usable.
            CCTweaks.logger.debug("Could not inject the Cobalt debug library", e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean shouldConsume(ItemStack stack, EntityPlayer player) {
        // The debug wand is a reusable inspection tool, not a one-shot upgrade.
        return false;
    }

    @Override
    protected boolean useGeneric(ItemStack stack, EntityPlayer player, TileEntity tile, int side) {
        String blockName = tile.getBlockType() == null ? "<unknown>"
            : tile.getBlockType()
                .getLocalizedName();
        player.addChatMessage(
            withColor("Tile: ", EnumChatFormatting.DARK_PURPLE).appendSibling(
                info(
                    tile.getClass()
                        .getSimpleName() + ": "
                        + blockName)));

        {
            IPeripheral peripheral = PeripheralUtil
                .getPeripheral(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, side);
            if (peripheral != null) {
                player.addChatMessage(
                    withColor("Peripheral: ", EnumChatFormatting.AQUA).appendSibling(info(peripheral.getType())));

                if (peripheral instanceof TileDebugPeripheral.SidedPeripheral) {
                    Set<IComputerAccess> computers = ((TileDebugPeripheral.SidedPeripheral) peripheral).computers();
                    IChatComponent boundList = withColor("Bound as: ", EnumChatFormatting.AQUA);
                    boolean first = true;
                    for (IComputerAccess access : computers) {
                        if (first) {
                            first = false;
                        } else {
                            boundList.appendSibling(info(", "));
                        }

                        String text;
                        ChatStyle style = new ChatStyle().setItalic(true);
                        try {
                            text = access.getAttachmentName() + " (#" + access.getID() + ")";
                            style.setColor(EnumChatFormatting.GRAY);
                        } catch (RuntimeException e) {
                            text = e.getMessage();
                            style.setColor(EnumChatFormatting.RED);
                        }

                        boundList.appendSibling(new ChatComponentText(text).setChatStyle(style));
                    }

                    player.addChatMessage(boundList);
                }
            }
        }

        return false;
    }

    private static IChatComponent withColor(String message, EnumChatFormatting color) {
        return new ChatComponentText(message).setChatStyle(new ChatStyle().setColor(color));
    }

    private static IChatComponent info(String message) {
        return new ChatComponentText(message).setChatStyle(
            new ChatStyle().setColor(EnumChatFormatting.GRAY)
                .setItalic(true));
    }
}
