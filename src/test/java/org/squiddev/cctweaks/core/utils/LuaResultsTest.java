package org.squiddev.cctweaks.core.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import dan200.computercraft.api.lua.MethodResult;

public class LuaResultsTest {

    @Test
    public void preservesObjectArray() {
        Object[] values = new Object[] { true, "ok" };
        assertSame(values, LuaResults.wrap(values));
    }

    @Test
    public void wrapsMethodResultInSentinel() {
        MethodResult result = MethodResult.of(true);
        Object[] wrapped = LuaResults.wrap(result);

        assertArrayEquals(new Object[] { result }, wrapped);
    }

    @Test
    public void wrapsNullAsSingleNull() {
        assertArrayEquals(new Object[] { null }, LuaResults.wrap(null));
    }
}
