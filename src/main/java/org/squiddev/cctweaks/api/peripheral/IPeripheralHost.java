package org.squiddev.cctweaks.api.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * An object that hosts a {@link IPeripheral}.
 *
 * Instead of implementing {@link IPeripheral} and delegating methods to it,
 * you can implement this interface instead. This is supported for TileEntities.
 *
 * <h2>Limitation: tiles that are also inventories</h2>
 *
 * <p>
 * Peripherals are resolved by asking ComputerCraft's providers in registration order, and CC's
 * own provider is registered during CC's pre-init, before any addon can register one. Its
 * provider wraps every {@code IInventory} tile in an {@code InventoryPeripheral}, so a tile that
 * implements both {@code IPeripheralHost} and {@code IInventory} will be seen as an inventory
 * peripheral only: this interface is never consulted. The provider list itself is private in
 * CC, so CCTweaks cannot re-order it.
 * </p>
 *
 * <p>
 * Workarounds, in order of preference:
 * </p>
 *
 * <ul>
 * <li>Implement CC's {@code dan200.computercraft.shared.peripheral.common.IPeripheralTile}
 * instead: CC's provider checks that first and skips the generic wrappers.</li>
 * <li>Do not implement {@code IInventory} on the tile: expose the inventory through the
 * peripheral's own methods.</li>
 * <li>Move the inventory to a separate, non-peripheral tile.</li>
 * </ul>
 */
public interface IPeripheralHost {

    /**
     * Get this host's peripheral
     *
     * @param side The side to get the peripheral from
     * @return The peripheral this object holds
     */
    IPeripheral getPeripheral(int side);
}
