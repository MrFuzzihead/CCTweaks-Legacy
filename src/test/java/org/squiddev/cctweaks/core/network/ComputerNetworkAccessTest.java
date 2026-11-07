package org.squiddev.cctweaks.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.squiddev.cctweaks.core.peripheral.BasePeripheral;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * Tests for {@link ComputerNetworkAccess}, the replacement for the {@code INetworkAccess} implementation that a mixin
 * used to graft onto ComputerCraft's peripheral wrapper.
 */
public class ComputerNetworkAccessTest {

    @Test
    public void testPeripheralsComeFromTheComputer() {
        final IPeripheral peripheral = new BasePeripheral();
        IComputerAccess computer = new MockComputer("top") {

            @Override
            public Map<String, IPeripheral> getAvailablePeripherals() {
                return Collections.singletonMap("right", peripheral);
            }
        };

        ComputerNetworkAccess access = new ComputerNetworkAccess(computer);
        Map<String, IPeripheral> peripherals = access.getPeripheralsOnNetwork();
        assertEquals(1, peripherals.size());
        assertSame(peripheral, peripherals.get("right"));
    }

    @Test
    public void testNoPeripheralsIsEmptyNotNull() {
        IComputerAccess computer = new MockComputer("top") {

            @Override
            public Map<String, IPeripheral> getAvailablePeripherals() {
                return null;
            }
        };

        assertTrue(
            new ComputerNetworkAccess(computer).getPeripheralsOnNetwork()
                .isEmpty());
    }

    @Test
    public void testPacketTransmissionIsRefused() {
        ComputerNetworkAccess access = new ComputerNetworkAccess(new MockComputer("top"));
        assertFalse(access.transmitPacket(null));
        // Invalidating is a no-op: ComputerCraft rebuilds a computer's sides itself.
        access.invalidateNetwork();
    }

    @Test
    public void testNameIsTheAttachmentName() {
        assertEquals("left", new ComputerNetworkAccess(new MockComputer("left")).getName());
    }

    @Test
    public void testNameIsEmptyWhenDetached() {
        IComputerAccess computer = new MockComputer("left") {

            @Override
            public String getAttachmentName() {
                throw new RuntimeException("You are not attached to this Computer");
            }
        };

        assertEquals("", new ComputerNetworkAccess(computer).getName());
    }

    @Test
    public void testEqualityIsTheComputersIdentity() {
        IComputerAccess first = new MockComputer("top");
        IComputerAccess second = new MockComputer("top");

        assertEquals(new ComputerNetworkAccess(first), new ComputerNetworkAccess(first));
        assertEquals(new ComputerNetworkAccess(first).hashCode(), new ComputerNetworkAccess(first).hashCode());
        assertNotEquals(new ComputerNetworkAccess(first), new ComputerNetworkAccess(second));
    }

    /**
     * Detach callbacks construct a fresh instance, so removal from a delegate must work by the computer's identity.
     */
    @Test
    public void testFreshInstanceRemovesFromDelegate() {
        IComputerAccess first = new MockComputer("top");
        IComputerAccess second = new MockComputer("bottom");

        NetworkAccessDelegate delegate = new NetworkAccessDelegate();
        delegate.add(new ComputerNetworkAccess(first));
        delegate.add(new ComputerNetworkAccess(second));

        delegate.remove(new ComputerNetworkAccess(first));

        Map<String, IPeripheral> remaining = delegate.getPeripheralsOnNetwork();
        assertEquals(0, remaining.size());
    }

    @Test
    public void testDelegateCombinesNetworks() {
        final IPeripheral left = new BasePeripheral();
        final IPeripheral right = new BasePeripheral();
        Map<String, IPeripheral> peripherals = new HashMap<String, IPeripheral>();
        peripherals.put("left", left);
        peripherals.put("right", right);

        IComputerAccess computer = new MockComputer("top") {

            @Override
            public Map<String, IPeripheral> getAvailablePeripherals() {
                return peripherals;
            }
        };

        NetworkAccessDelegate delegate = new NetworkAccessDelegate();
        delegate.add(new ComputerNetworkAccess(computer));

        assertEquals(peripherals, delegate.getPeripheralsOnNetwork());
    }

    private static class MockComputer implements IComputerAccess {

        private final String attachmentName;

        MockComputer(String attachmentName) {
            this.attachmentName = attachmentName;
        }

        @Override
        public String mount(String name, IMount mount) {
            return name;
        }

        @Override
        public String mount(String name, IMount mount, String driveName) {
            return name;
        }

        @Override
        public String mountWritable(String name, IWritableMount mount) {
            return name;
        }

        @Override
        public String mountWritable(String name, IWritableMount mount, String driveName) {
            return name;
        }

        @Override
        public void unmount(String name) {}

        @Override
        public int getID() {
            return 0;
        }

        @Override
        public void queueEvent(String event, Object[] arguments) {}

        @Override
        public String getAttachmentName() {
            return attachmentName;
        }
    }
}
