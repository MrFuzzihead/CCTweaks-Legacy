package org.squiddev.cctweaks.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code /cctweaks} is the operator-facing entry point for the diagnostics and the in-world self
 * test, so its argument handling needs to stay predictable.
 */
public class CommandCCTweaksTest {

    private CommandCCTweaks command;
    private ICommandSender sender;

    @BeforeEach
    public void setUp() {
        command = new CommandCCTweaks();
        sender = mock(ICommandSender.class);
        when(sender.getCommandSenderName()).thenReturn("tester");
    }

    @Test
    public void metadata() {
        assertEquals("cctweaks", command.getCommandName());
        assertEquals(2, command.getRequiredPermissionLevel(), "diagnostics are operator-only");
        assertTrue(
            command.getCommandUsage(sender)
                .contains("selftest"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void tabCompletionFiltersSubCommands() {
        List<String> all = command.addTabCompletionOptions(sender, new String[] { "" });
        assertEquals(3, all.size());

        List<String> filtered = command.addTabCompletionOptions(sender, new String[] { "se" });
        assertEquals(1, filtered.size());
        assertEquals("selftest", filtered.get(0));

        assertEquals(
            0,
            command.addTabCompletionOptions(sender, new String[] { "nope" })
                .size());

        // Extra arguments are not completed.
        assertEquals(null, command.addTabCompletionOptions(sender, new String[] { "modules", "x" }));
    }

    @Test
    public void noArgumentsPrintsHelp() {
        command.processCommand(sender, new String[0]);

        verify(sender, org.mockito.Mockito.atLeastOnce()).addChatMessage(any(IChatComponent.class));
    }

    @Test
    public void modulesListsTheRegistry() {
        command.processCommand(sender, new String[] { "modules" });

        verify(sender, org.mockito.Mockito.atLeastOnce()).addChatMessage(any(IChatComponent.class));
    }

    @Test
    public void configPrintsEveryFlag() {
        command.processCommand(sender, new String[] { "config" });

        verify(sender, org.mockito.Mockito.atLeastOnce()).addChatMessage(any(IChatComponent.class));
    }

    @Test
    public void unknownSubCommandIsAUsageError() {
        assertThrows(WrongUsageException.class, () -> command.processCommand(sender, new String[] { "bogus" }));
        verify(sender, never()).addChatMessage(any(IChatComponent.class));
    }

    @Test
    public void caseInsensitiveSubCommands() {
        command.processCommand(sender, new String[] { "MODULES" });
        verify(sender, org.mockito.Mockito.atLeastOnce()).addChatMessage(any(IChatComponent.class));
    }

    @Test
    public void nonWorldEntitySenderIsRejected() {
        // A console sender (and any non-server world) cannot host the scratch area; the command
        // must explain that instead of throwing.
        when(sender.getEntityWorld()).thenReturn(null);
        command.processCommand(sender, new String[] { "selftest" });

        org.mockito.ArgumentCaptor<IChatComponent> captor = org.mockito.ArgumentCaptor.forClass(IChatComponent.class);
        verify(sender, org.mockito.Mockito.times(1)).addChatMessage(captor.capture());
        assertTrue(
            captor.getValue()
                .getUnformattedText()
                .contains("needs a world"));
    }
}
