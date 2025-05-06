package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public final class ArgumentDelegator {

    private ArgumentDelegator() {
        throw new RuntimeException("Cannot create ArgumentDelegator");
    }

    public static Object[] delegateLuaObject(ILuaObject object, ILuaContext context, int method, IArguments arguments)
        throws LuaException, InterruptedException {
        if (object instanceof ILuaObjectWithArguments) {
            return ((ILuaObjectWithArguments) object).callMethod(context, method, arguments);
        } else {
            return object instanceof IBinaryHandler ? object.callMethod(context, method, arguments.asBinary())
                : object.callMethod(context, method, arguments.asArguments());
        }
    }

    public static Object[] delegatePeripheral(IPeripheral peripheral, IComputerAccess computer, ILuaContext context,
        int method, IArguments arguments) throws LuaException, InterruptedException {
        if (peripheral instanceof IPeripheralWithArguments) {
            return ((IPeripheralWithArguments) peripheral).callMethod(computer, context, method, arguments);
        } else {
            return peripheral instanceof IBinaryHandler
                ? peripheral.callMethod(computer, context, method, arguments.asBinary())
                : peripheral.callMethod(computer, context, method, arguments.asArguments());
        }
    }
}
