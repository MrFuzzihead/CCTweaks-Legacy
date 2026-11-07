package org.squiddev.cctweaks.core.peripheral;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.squiddev.cctweaks.api.peripheral.IPeripheralHelpers;
import org.squiddev.cctweaks.api.peripheral.IPeripheralProxy;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTargeted;

public class PeripheralHelpers implements IPeripheralHelpers {

    @Override
    public IPeripheral getBasePeripheral(IPeripheral peripheral) {
        IPeripheral previous = null;
        Set<IPeripheral> seen = Collections.newSetFromMap(new IdentityHashMap<IPeripheral, Boolean>());

        while (peripheral != null && seen.add(peripheral)) {
            previous = peripheral;
            peripheral = peripheral instanceof IPeripheralProxy ? ((IPeripheralProxy) peripheral).getBasePeripheral()
                : null;
        }

        return previous;
    }

    @Override
    public Object getTarget(IPeripheral peripheral) {
        IPeripheral previous = null;
        Set<IPeripheral> seen = Collections.newSetFromMap(new IdentityHashMap<IPeripheral, Boolean>());

        while (peripheral != null && seen.add(peripheral)) {
            previous = peripheral;

            if (peripheral instanceof IPeripheralTargeted) {
                Object result = ((IPeripheralTargeted) peripheral).getTarget();
                if (result != null) return result;
            }

            peripheral = peripheral instanceof IPeripheralProxy ? ((IPeripheralProxy) peripheral).getBasePeripheral()
                : null;
        }

        return previous;
    }
}
