package org.squiddev.cctweaks.testsupport;

/**
 * Test fixture: a class the {@code CCTweaksAPI} loader must reject because its constructor throws.
 */
public class ThrowingConstructor {

    public ThrowingConstructor() {
        throw new IllegalStateException("boom");
    }
}
