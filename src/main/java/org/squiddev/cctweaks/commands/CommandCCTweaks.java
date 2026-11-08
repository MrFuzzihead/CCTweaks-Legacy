package org.squiddev.cctweaks.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.registry.IModule;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.selfTest.InWorldSelfTest;

/**
 * {@code /cctweaks} - diagnostics for operators.
 *
 * <pre>
 * /cctweaks                       list the sub-commands
 * /cctweaks modules               registered modules, in registration order
 * /cctweaks config                the live config values
 * /cctweaks selftest              run the in-world self test (placement, digging, upgrades)
 * </pre>
 *
 * <p>
 * Everything here is read-only apart from {@code selftest}, which writes to a scratch area near
 * the world spawn and restores it afterwards. The command requires permission level 2.
 * </p>
 */
public class CommandCCTweaks extends CommandBase {

    private static final List<String> SUB_COMMANDS = Arrays.asList("modules", "config", "selftest");

    @Override
    public String getCommandName() {
        return "cctweaks";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/cctweaks <modules|config|selftest>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, EnumChatFormatting.AQUA, "CCTweaks " + CCTweaks.VERSION);
            send(sender, EnumChatFormatting.WHITE, "  /cctweaks modules - registered modules");
            send(sender, EnumChatFormatting.WHITE, "  /cctweaks config - live configuration");
            send(sender, EnumChatFormatting.WHITE, "  /cctweaks selftest - run the in-world self test");
            return;
        }

        String sub = args[0].toLowerCase();
        if ("modules".equals(sub)) {
            listModules(sender);
        } else if ("config".equals(sub)) {
            listConfig(sender);
        } else if ("selftest".equals(sub)) {
            selfTest(sender);
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private void listModules(ICommandSender sender) {
        List<IModule> modules = Registry.modules();
        send(sender, EnumChatFormatting.AQUA, modules.size() + " module(s), in registration order:");
        for (int i = 0; i < modules.size(); i++) {
            IModule module = modules.get(i);
            send(
                sender,
                EnumChatFormatting.WHITE,
                "  " + (i + 1)
                    + ". "
                    + module.getClass()
                        .getSimpleName());
        }
    }

    private void listConfig(ICommandSender sender) {
        send(sender, EnumChatFormatting.AQUA, "Configuration (" + CCTweaks.MODID + "):");
        send(sender, EnumChatFormatting.WHITE, "  computerUpgradeEnabled = " + CCTweaks.computerUpgradeEnabled);
        send(sender, EnumChatFormatting.WHITE, "  computerUpgradeCrafting = " + CCTweaks.computerUpgradeCrafting);
        send(sender, EnumChatFormatting.WHITE, "  debugWandEnabled = " + CCTweaks.debugWandEnabled);
        send(sender, EnumChatFormatting.WHITE, "  debugWandRequiresOp = " + CCTweaks.debugWandRequiresOp);
        send(sender, EnumChatFormatting.WHITE, "  debugItems = " + CCTweaks.debugItems);
        send(sender, EnumChatFormatting.WHITE, "  thEnabled = " + CCTweaks.thEnabled);
        send(sender, EnumChatFormatting.WHITE, "  upgradeId = " + CCTweaks.upgradeId);
        send(sender, EnumChatFormatting.WHITE, "  advanced = " + CCTweaks.advanced);
        send(sender, EnumChatFormatting.WHITE, "  advancedUpgradeId = " + CCTweaks.advancedUpgradeId);
        send(sender, EnumChatFormatting.WHITE, "  thCrafting = " + CCTweaks.thCrafting);
        send(sender, EnumChatFormatting.WHITE, "  digFactor = " + CCTweaks.digFactor);
    }

    private void selfTest(ICommandSender sender) {
        WorldServer world;
        if (sender instanceof EntityPlayerMP) {
            world = ((EntityPlayerMP) sender).getServerForPlayer();
        } else {
            world = sender.getEntityWorld() instanceof WorldServer ? (WorldServer) sender.getEntityWorld() : null;
        }

        if (world == null) {
            send(sender, EnumChatFormatting.RED, "The self test needs a world: run it from a player or the console.");
            return;
        }

        send(sender, EnumChatFormatting.AQUA, "Running the CCTweaks self test near the world spawn...");
        int failed = InWorldSelfTest.atSpawn(world)
            .run();

        if (failed == 0) {
            send(sender, EnumChatFormatting.GREEN, "Self test passed. Details are in the server log.");
        } else {
            send(
                sender,
                EnumChatFormatting.RED,
                "Self test failed (" + failed + " check(s)). Details are in the server log.");
        }
    }

    private static void send(ICommandSender sender, EnumChatFormatting colour, String message) {
        sender.addChatMessage(
            new ChatComponentText(message).setChatStyle(new net.minecraft.util.ChatStyle().setColor(colour)));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<String>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) matches.add(sub);
            }
            return matches;
        }
        return null;
    }
}
