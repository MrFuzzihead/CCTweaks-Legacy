package org.squiddev.cctweaks.core.peripheral;

import java.util.HashSet;
import java.util.Set;

import org.squiddev.cctweaks.api.CCTweaksAPI;
import org.squiddev.cctweaks.api.peripheral.IPeripheralHelpers;
import org.squiddev.cctweaks.api.peripheral.IPeripheralProxy;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * An simple peripheral proxy instance.
 */
public abstract class PeripheralProxy implements IPeripheral, IPeripheralProxy {

    protected volatile IPeripheral instance;
    protected final Set<IComputerAccess> mounts = new HashSet<IComputerAccess>();

    protected String defaultType = null;

    public PeripheralProxy() {}

    public PeripheralProxy(String defaultType) {
        this.defaultType = defaultType;
    }

    protected abstract IPeripheral createPeripheral();

    @Override
    public synchronized IPeripheral getBasePeripheral() {
        IPeripheral instance = this.instance;
        if (instance == null) {
            instance = createPeripheral();
            if (instance == null) {
                throw new IllegalStateException(getClass().getName() + " returned a null base peripheral");
            }

            this.instance = instance;
            for (IComputerAccess mount : mounts) {
                instance.attach(mount);
            }
        }

        return instance;
    }

    @Override
    public String getType() {
        return instance == null && defaultType != null ? defaultType : getBasePeripheral().getType();
    }

    @Override
    public String[] getMethodNames() {
        return getBasePeripheral().getMethodNames();
    }

    @Override
    public Object[] callMethod(IComputerAccess access, ILuaContext context, int i, Object[] objects)
        throws LuaException, InterruptedException {
        return getBasePeripheral().callMethod(access, context, i, objects);
    }

    @Override
    public synchronized void attach(IComputerAccess access) {
        if (instance == null) {
            // We want to be as lazy as possible with loading
            mounts.add(access);
        } else {
            instance.attach(access);
        }
    }

    @Override
    public synchronized void detach(IComputerAccess access) {
        mounts.remove(access);
        if (instance != null) {
            instance.detach(access);
        }
    }

    @Override
    public boolean equals(IPeripheral other) {
        IPeripheralHelpers helpers = CCTweaksAPI.instance()
            .peripheralHelpers();
        return helpers.getBasePeripheral(this)
            .equals(helpers.getBasePeripheral(other));
    }
}
