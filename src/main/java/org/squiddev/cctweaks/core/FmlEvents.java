package org.squiddev.cctweaks.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.squiddev.cctweaks.CCTweaks;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dan200.computercraft.core.lua.lib.DelayedTasks;

/**
 * This handles various events.
 *
 * <p>
 * {@link #onServerTick} is the only caller of {@link DelayedTasks#update()} in ComputerCraft
 * 3.3.0: CC's own proxy ticks {@code MainThread} but not the delayed-task list. If CC starts
 * ticking it too, every delayed task will run twice per tick and every delay halves. Keep the
 * single call here until CC owns it.
 * </p>
 */
public final class FmlEvents {

    private static final Queue<Runnable> serverQueue = new ConcurrentLinkedQueue<Runnable>();

    public FmlEvents() {}

    /**
     * Queue a task for the next server tick.
     */
    public static void schedule(Runnable runnable) {
        if (runnable != null) serverQueue.add(runnable);
    }

    /**
     * Drop queued tasks. Called when a server stops so runnables from an old world cannot run in
     * the next one.
     */
    public static void reset() {
        serverQueue.clear();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        DelayedTasks.update();

        Runnable scheduled;
        while ((scheduled = serverQueue.poll()) != null) {
            try {
                scheduled.run();
            } catch (Throwable throwable) {
                // A broken task must not take the whole server tick down with it.
                CCTweaks.logger.error("Error in scheduled CCTweaks task", throwable);
            }
        }
    }

}
