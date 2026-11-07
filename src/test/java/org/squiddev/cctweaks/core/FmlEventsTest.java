package org.squiddev.cctweaks.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.testsupport.StubComputerAccess;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dan200.computercraft.api.lua.ILuaEnvironment;
import dan200.computercraft.core.lua.lib.DelayedTasks;
import dan200.computercraft.core.lua.lib.LuaEnvironment;

public class FmlEventsTest {

    private final FmlEvents events = new FmlEvents();

    @BeforeEach
    public void resetBefore() {
        FmlEvents.reset();
        DelayedTasks.reset();
    }

    @AfterEach
    public void resetAfter() {
        FmlEvents.reset();
        DelayedTasks.reset();
    }

    @Test
    public void runsScheduledTasksOnServerTickStart() {
        AtomicInteger count = new AtomicInteger();
        FmlEvents.schedule(count::incrementAndGet);

        events.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertEquals(1, count.get());

        events.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertEquals(1, count.get());
    }

    @Test
    public void continuesAfterAThrowingTask() {
        AtomicInteger count = new AtomicInteger();
        FmlEvents.schedule(() -> { throw new IllegalStateException("expected test failure"); });
        FmlEvents.schedule(count::incrementAndGet);

        events.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertEquals(1, count.get());
    }

    @Test
    public void resetDropsQueuedTasks() {
        AtomicInteger count = new AtomicInteger();
        FmlEvents.schedule(count::incrementAndGet);
        FmlEvents.reset();

        events.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertEquals(0, count.get());
    }

    @Test
    public void resolvesRemovedCctweaksMappingsWithoutPrompting() {
        ListMultimap<String, FMLMissingMappingsEvent.MissingMapping> missing = ArrayListMultimap.create();
        FMLMissingMappingsEvent.MissingMapping mapping = new FMLMissingMappingsEvent.MissingMapping(
            "\u0001cctweaks:networkedBlock",
            1);
        missing.put("cctweaks", mapping);

        new org.squiddev.cctweaks.CCTweaks().onMissingMappings(new FMLMissingMappingsEvent(missing));

        assertEquals(FMLMissingMappingsEvent.Action.WARN, mapping.getAction());
    }

    @Test
    public void ignoresMappingsFromOtherMods() {
        ListMultimap<String, FMLMissingMappingsEvent.MissingMapping> missing = ArrayListMultimap.create();
        FMLMissingMappingsEvent.MissingMapping mapping = new FMLMissingMappingsEvent.MissingMapping(
            "\u0001othermod:thing",
            2);
        missing.put("othermod", mapping);

        new org.squiddev.cctweaks.CCTweaks().onMissingMappings(new FMLMissingMappingsEvent(missing));

        assertEquals(FMLMissingMappingsEvent.Action.DEFAULT, mapping.getAction());
    }

    @Test
    public void ticksComputerCraftDelayedTasks() throws Exception {
        StubComputerAccess access = new StubComputerAccess(7, "back");
        AtomicInteger executed = new AtomicInteger();

        LuaEnvironment.instance.issueTask(access, () -> {
            executed.incrementAndGet();
            return new Object[] { "done" };
        }, 0);

        assertTrue(access.events.isEmpty());

        events.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));

        assertEquals(1, executed.get());
        assertFalse(access.events.isEmpty());
        assertEquals(ILuaEnvironment.EVENT_NAME, access.events.get(0).name);
    }
}
