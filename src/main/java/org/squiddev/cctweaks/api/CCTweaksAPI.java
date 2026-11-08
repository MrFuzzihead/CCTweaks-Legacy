package org.squiddev.cctweaks.api;

/**
 * Main entry point for CCTweaks API
 */
public final class CCTweaksAPI {

    /**
     * The class implementing {@link ICCTweaksAPI}. Kept separate so an addon can bundle this API
     * package without the CCTweaks core and get a clear failure when it is missing.
     */
    private static final String IMPLEMENTATION = "org.squiddev.cctweaks.core.API";

    private static final ICCTweaksAPI API = load(ICCTweaksAPI.class, IMPLEMENTATION);

    private CCTweaksAPI() {}

    /**
     * Get the main API entry point
     *
     * @return Main API entry point
     */
    public static ICCTweaksAPI instance() {
        return API;
    }

    /**
     * Instantiate the API implementation.
     *
     * @param type The interface the implementation must implement
     * @param name The fully qualified class name of the implementation
     * @param <T>  The interface type
     * @return The instantiated implementation
     * @throws CoreNotFoundException If the class is missing, cannot be created, does not implement
     *                               {@code type}, or its static initialiser failed
     */
    static <T> T load(Class<T> type, String name) {
        Class<?> clazz;
        try {
            clazz = Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be found", e);
        } catch (LinkageError e) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be linked", e);
        }

        Object instance;
        try {
            instance = clazz.getDeclaredConstructor()
                .newInstance();
        } catch (NoSuchMethodException e) {
            throw new CoreNotFoundException(
                "Cannot load CCTweaks API as " + name + " has no no-argument constructor",
                e);
        } catch (ReflectiveOperationException e) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be created", e);
        } catch (ExceptionInInitializerError e) {
            throw new CoreNotFoundException(
                "Cannot load CCTweaks API as " + name + " threw while initialising",
                e.getCause() == null ? e : e.getCause());
        } catch (LinkageError e) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be linked", e);
        }

        if (!type.isInstance(instance)) {
            throw new CoreNotFoundException(
                "Cannot load CCTweaks API as " + name + " does not implement " + type.getName());
        }

        return type.cast(instance);
    }
}
