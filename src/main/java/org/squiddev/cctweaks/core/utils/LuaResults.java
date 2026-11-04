package org.squiddev.cctweaks.core.utils;

/**
 * Helpers for ComputerCraft's Lua suspension protocol.
 *
 * <p>
 * A peripheral method may now wait for an event without blocking the computer's worker thread: it
 * returns a {@link dan200.computercraft.api.lua.MethodResult}, which the Lua bridge turns into a
 * coroutine suspension. {@code callMethod} still declares an {@code Object[]} return, so a
 * suspending result travels inside a single-element array - the sentinel
 * {@code dan200.computercraft.core.lua.lib.cobalt.CobaltMachine} looks for.
 *
 * @see dan200.computercraft.api.lua.MethodResult
 */
public final class LuaResults {

    private LuaResults() {}

    /**
     * Wrap the result of a delegating call so that a suspension survives the trip back to Lua.
     *
     * <p>
     * Mirrors ComputerCraft's own {@code r instanceof Object[] ? r : new Object[] { r }} for
     * {@code peripheral.call}.
     *
     * @param result An {@code Object[]} of return values, or a {@code MethodResult} to suspend.
     * @return An {@code Object[]} to return from {@code callMethod}.
     */
    public static Object[] wrap(Object result) {
        return result instanceof Object[] ? (Object[]) result : new Object[] { result };
    }
}
