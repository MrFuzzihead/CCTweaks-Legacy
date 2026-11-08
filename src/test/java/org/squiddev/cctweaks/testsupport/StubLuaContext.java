package org.squiddev.cctweaks.testsupport;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;

/**
 * An {@link ILuaContext} that runs main-thread tasks immediately, which makes the peripheral code
 * testable without a ComputerThread.
 */
public class StubLuaContext implements ILuaContext {

    private long nextId = 1;

    @Override
    public MethodResult executeMainThreadTask(ILuaTask task) throws LuaException {
        return MethodResult.of(task.execute());
    }

    @Override
    public long issueMainThreadTask(ILuaTask task) throws LuaException {
        task.execute();
        return nextId++;
    }
}
