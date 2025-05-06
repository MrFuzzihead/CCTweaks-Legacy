package org.squiddev.cctweaks.api.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;

public interface IPeripheralProxy extends IPeripheral {

    IPeripheral getBasePeripheral();
}
