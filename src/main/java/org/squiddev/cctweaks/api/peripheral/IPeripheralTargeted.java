package org.squiddev.cctweaks.api.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface IPeripheralTargeted extends IPeripheral {

    Object getTarget();
}
