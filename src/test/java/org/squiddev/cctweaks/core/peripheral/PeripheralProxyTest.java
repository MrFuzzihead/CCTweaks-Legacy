package org.squiddev.cctweaks.core.peripheral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public class PeripheralProxyTest {

    private static final class RecordingPeripheral extends BasePeripheral {

        final List<String> attached = new ArrayList<String>();
        final List<String> detached = new ArrayList<String>();
        final String type;

        private RecordingPeripheral(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String[] getMethodNames() {
            return new String[0];
        }

        @Override
        public void attach(IComputerAccess computer) {
            attached.add(computer.getAttachmentName());
        }

        @Override
        public void detach(IComputerAccess computer) {
            detached.add(computer.getAttachmentName());
        }
    }

    private static final class TestProxy extends PeripheralProxy {

        final RecordingPeripheral base;
        int created;

        private TestProxy(String defaultType, RecordingPeripheral base) {
            super(defaultType);
            this.base = base;
        }

        @Override
        protected IPeripheral createPeripheral() {
            created++;
            return base;
        }
    }

    private static IComputerAccess computer(String name) {
        IComputerAccess access = Mockito.mock(IComputerAccess.class);
        Mockito.when(access.getAttachmentName())
            .thenReturn(name);
        return access;
    }

    @Test
    public void createsLazilyAndReplaysAttach() {
        RecordingPeripheral base = new RecordingPeripheral("base");
        TestProxy proxy = new TestProxy("default", base);

        assertEquals(0, proxy.created);

        IComputerAccess before = computer("back");
        proxy.attach(before);
        assertEquals(0, proxy.created);

        assertSame(base, proxy.getBasePeripheral());
        assertEquals(1, proxy.created);
        assertEquals(java.util.Collections.singletonList("back"), base.attached);

        IComputerAccess after = computer("top");
        proxy.attach(after);
        assertEquals(java.util.Arrays.asList("back", "top"), base.attached);
    }

    @Test
    public void detachBeforeAndAfterCreation() {
        RecordingPeripheral base = new RecordingPeripheral("base");
        TestProxy proxy = new TestProxy(null, base);

        IComputerAccess before = computer("back");
        proxy.attach(before);
        proxy.detach(before);
        assertSame(base, proxy.getBasePeripheral());
        assertTrue(base.attached.isEmpty());

        IComputerAccess after = computer("top");
        proxy.attach(after);
        proxy.detach(after);
        assertEquals(java.util.Collections.singletonList("top"), base.detached);
    }

    @Test
    public void defaultTypeAvoidsCreatingBase() {
        RecordingPeripheral base = new RecordingPeripheral("base");
        TestProxy proxy = new TestProxy("default", base);

        assertEquals("default", proxy.getType());
        assertEquals(0, proxy.created);

        RecordingPeripheral noDefault = new RecordingPeripheral("real");
        TestProxy proxy2 = new TestProxy(null, noDefault);
        assertEquals("real", proxy2.getType());
        assertEquals(1, proxy2.created);
    }

    @Test
    public void delegatesMethodsToBase() throws Exception {
        RecordingPeripheral base = new RecordingPeripheral("base");
        TestProxy proxy = new TestProxy("default", base);
        IComputerAccess access = computer("back");
        ILuaContext context = Mockito.mock(ILuaContext.class);

        assertNotNull(proxy.getMethodNames());
        proxy.callMethod(access, context, 0, new Object[0]);
        proxy.attach(access);
        proxy.detach(access);
    }

    @Test
    public void equalsUnwrapsProxies() {
        RecordingPeripheral base = new RecordingPeripheral("base");
        TestProxy proxy = new TestProxy(null, base);

        assertTrue(proxy.equals((IPeripheral) base));
        assertFalse(proxy.equals((IPeripheral) new BasePeripheral()));
    }
}
