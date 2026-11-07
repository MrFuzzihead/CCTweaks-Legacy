package org.squiddev.cctweaks.core.network;

import java.util.Collections;
import java.util.Map;

import org.squiddev.cctweaks.api.network.INetworkAccess;
import org.squiddev.cctweaks.api.network.Packet;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * The artificial network represented by a single computer's own side connections.
 *
 * <p>
 * This replaces the {@code INetworkAccess} implementation that used to be bolted onto ComputerCraft's
 * {@code PeripheralAPI$PeripheralWrapper} by a mixin. A peripheral is handed one of these from
 * {@link IPeripheral#attach(IComputerAccess)} (and has it taken away again in
 * {@link IPeripheral#detach(IComputerAccess)}), which is ComputerCraft's own lifecycle for a peripheral being
 * connected to a computer.
 * </p>
 *
 * <p>
 * Equality is the identity of the wrapped {@link IComputerAccess}: callbacks may construct a fresh instance for
 * {@code detach} and still remove the instance handed to {@code attach} from a
 * {@link NetworkAccessDelegate}.
 * </p>
 */
public class ComputerNetworkAccess implements INetworkAccess {

    private final IComputerAccess computer;

    public ComputerNetworkAccess(IComputerAccess computer) {
        if (computer == null) throw new IllegalArgumentException("computer cannot be null");
        this.computer = computer;
    }

    public IComputerAccess getComputer() {
        return computer;
    }

    /**
     * The name this network is known by: the side the peripheral is attached on.
     *
     * @return The attachment name, as reported by ComputerCraft, or the empty string if the computer is no longer
     *         attached (ComputerCraft throws rather than reporting a stale name).
     */
    public String getName() {
        try {
            return computer.getAttachmentName();
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public Map<String, IPeripheral> getPeripheralsOnNetwork() {
        Map<String, IPeripheral> peripherals = computer.getAvailablePeripherals();
        return peripherals == null ? Collections.<String, IPeripheral>emptyMap() : peripherals;
    }

    @Override
    public void invalidateNetwork() {
        // The computer's side connections are rebuilt by ComputerCraft itself.
    }

    @Override
    public boolean transmitPacket(Packet packet) {
        // A computer's sides cannot broadcast: there is no cable between them.
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other == this
            || other instanceof ComputerNetworkAccess && ((ComputerNetworkAccess) other).computer == computer;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(computer);
    }

    @Override
    public String toString() {
        return "ComputerNetworkAccess(" + computer + ")";
    }
}
