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
        // The Cobalt debug library exposes getlocal/setlocal/upvalue access, which reaches outside
        // the computer sandbox. Server owners can gate it behind operator permissions.
        if (CCTweaks.debugWandRequiresOp && !player.canCommandSenderUseCommand(2, "cctweaks.debug")) {
            return fail(player, "You must be an operator to inject the debug library");
        }

        if (!computerTile.isUsable(player, false)) {
            return fail(player, "That computer is out of reach");
        }

        ServerComputer serverComputer = computerTile.getServerComputer();
        if (serverComputer == null) {
            return fail(player, "This computer has not been started yet");
        }

        Computer computer = serverComputer.getComputer();
        if (computer == null) {
            return fail(player, "This computer has no runtime");
        }

        // The machine is null while the computer is off or between reboots, so say so rather
        // than reporting a generic failure -- a wand that silently does nothing is untestable.
        if (!computer.isOn()) {
            return fail(player, "Turn the computer on before injecting the debug library");
        }

        dan200.computercraft.core.lua.ILuaMachine luaMachine = computer.getLuaMachine();
        if (!(luaMachine instanceof CobaltMachine)) {
            return fail(
                player,
                "Unsupported Lua machine: " + (luaMachine == null ? "none"
                    : luaMachine.getClass()
                        .getName()));
        }

        try {
            DebugLib.add(((CobaltMachine) luaMachine).getLuaState());
        } catch (Exception e) {
            // Normally a Lua error from the library installation itself.
            CCTweaks.logger.warn("Could not inject the Cobalt debug library", e);
            return fail(player, "Injection failed: " + e.getMessage());
        }

        // Note: CobaltMachine.state is not volatile and the computer runs on a worker thread, so
        // this write races with a concurrently executing script. It is a debug tool; see
        // AGENTS.md "Known Fragile Points".
        player.addChatMessage(
            new ChatComponentText("Injected the debug library into computer #" + serverComputer.getID())
                .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)));
        return true;
    }

    private static boolean fail(EntityPlayer player, String message) {
        player.addChatMessage(
            new ChatComponentText(message).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
        return false;
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
