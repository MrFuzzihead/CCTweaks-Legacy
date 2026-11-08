package org.squiddev.cctweaks.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CCTweaksAPILoaderTest {

    @Test
    public void instanceIsStableAndLooksLikeTheApi() {
        ICCTweaksAPI api = CCTweaksAPI.instance();

        assertNotNull(api);
        assertSame(api, CCTweaksAPI.instance());
        assertNotNull(api.turtleRegistry());
        assertNotNull(api.peripheralHelpers());
    }

    @Test
    public void missingClassIsAClearFailure() {
        CoreNotFoundException error = assertThrows(
            CoreNotFoundException.class,
            () -> CCTweaksAPI.load(ICCTweaksAPI.class, "org.squiddev.cctweaks.DoesNotExist"));

        assertTrue(
            error.getMessage()
                .contains("cannot be found"));
    }

    @Test
    public void wrongTypeIsAClearFailure() {
        CoreNotFoundException error = assertThrows(
            CoreNotFoundException.class,
            () -> CCTweaksAPI.load(ICCTweaksAPI.class, "java.lang.String"));

        assertTrue(
            error.getMessage()
                .contains("does not implement"));
    }

    @Test
    public void missingConstructorIsAClearFailure() {
        CoreNotFoundException error = assertThrows(
            CoreNotFoundException.class,
            () -> CCTweaksAPI.load(ICCTweaksAPI.class, "org.squiddev.cctweaks.testsupport.NoDefaultConstructor"));

        assertTrue(
            error.getMessage()
                .contains("no no-argument constructor"));
    }

    @Test
    public void throwingConstructorIsAClearFailure() {
        CoreNotFoundException error = assertThrows(
            CoreNotFoundException.class,
            () -> CCTweaksAPI.load(ICCTweaksAPI.class, "org.squiddev.cctweaks.testsupport.ThrowingConstructor"));

        assertTrue(
            error.getMessage()
                .contains("cannot be created"));
    }
}
