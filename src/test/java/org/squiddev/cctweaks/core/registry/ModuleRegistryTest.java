package org.squiddev.cctweaks.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The module lifecycle used to be static state on {@link Registry}, so it could only be exercised by
 * booting a game. These tests drive a fresh {@link ModuleRegistry} with recording modules.
 */
public class ModuleRegistryTest {

    /**
     * Records every lifecycle call so ordering can be asserted.
     */
    static class RecordingModule implements IModule {

        final String name;
        final List<String> log;
        boolean canLoad = true;

        RecordingModule(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public boolean canLoad() {
            return canLoad;
        }

        @Override
        public void preInit() {
            log.add(name + ":pre");
        }

        @Override
        public void init() {
            log.add(name + ":init");
        }

        @Override
        public void postInit() {
            log.add(name + ":post");
        }
    }

    private final List<String> log = new ArrayList<String>();

    @Test
    public void runsEveryPhaseInRegistrationOrder() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.add(new RecordingModule("a", log));
        registry.add(new RecordingModule("b", log));

        registry.preInit();
        registry.init();
        registry.postInit();

        assertEquals(Arrays.asList("a:pre", "b:pre", "a:init", "b:init", "a:post", "b:post"), log);
    }

    @Test
    public void skipsModulesThatCannotLoad() {
        ModuleRegistry registry = new ModuleRegistry();
        RecordingModule enabled = new RecordingModule("enabled", log);
        RecordingModule disabled = new RecordingModule("disabled", log);
        disabled.canLoad = false;

        registry.add(disabled);
        registry.add(enabled);

        registry.preInit();
        registry.init();

        assertEquals(Arrays.asList("enabled:pre", "enabled:init"), log);
    }

    @Test
    public void addingTheSameModuleTwiceRunsItOnce() {
        ModuleRegistry registry = new ModuleRegistry();
        RecordingModule module = new RecordingModule("a", log);
        registry.add(module);
        registry.add(module);

        registry.preInit();

        assertEquals(1, log.size());
        assertEquals(
            1,
            registry.modules()
                .size());
    }

    @Test
    public void modulesAddedLateCatchUp() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.add(new RecordingModule("early", log));

        registry.preInit();
        registry.init();

        registry.add(new RecordingModule("late", log));

        assertEquals(Arrays.asList("early:pre", "early:init", "late:pre", "late:init"), log);

        registry.postInit();
        assertTrue(log.contains("late:post"), "a module added before postInit still gets postInit");
    }

    @Test
    public void lateModuleOnlyRunsThePhasesThatAlreadyRan() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.preInit();
        registry.add(new RecordingModule("late", log));

        assertEquals(Arrays.asList("late:pre"), log);
    }

    @Test
    public void phasesCannotRunOutOfOrderOrTwice() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.add(new RecordingModule("a", log));

        assertThrows(IllegalStateException.class, registry::init);
        assertThrows(IllegalStateException.class, registry::postInit);

        registry.preInit();
        assertThrows(IllegalStateException.class, registry::preInit);
        assertThrows(IllegalStateException.class, registry::postInit);

        registry.init();
        assertThrows(IllegalStateException.class, registry::init);

        registry.postInit();
        assertThrows(IllegalStateException.class, registry::postInit);
    }

    @Test
    public void moduleListIsAnImmutableSnapshot() {
        ModuleRegistry registry = new ModuleRegistry();
        RecordingModule first = new RecordingModule("a", log);
        registry.add(first);

        List<IModule> snapshot = registry.modules();
        registry.add(new RecordingModule("b", log));

        assertEquals(1, snapshot.size(), "the snapshot must not change under the caller");
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertFalse(
            registry.modules()
                .isEmpty());
    }
}
