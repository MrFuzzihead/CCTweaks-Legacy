package org.squiddev.cctweaks.api;

public final class CCTweaksAPI {

    private static final ICCTweaksAPI API;

    public static ICCTweaksAPI instance() {
        return API;
    }

    static {
        String name = "org.squiddev.cctweaks.core.API";

        ICCTweaksAPI api;
        try {
            Class<?> registryClass = Class.forName(name);
            api = (ICCTweaksAPI) registryClass.newInstance();
        } catch (ClassNotFoundException var3) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be found", var3);
        } catch (InstantiationException var4) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be created", var4);
        } catch (IllegalAccessException var5) {
            throw new CoreNotFoundException("Cannot load CCTweaks API as " + name + " cannot be accessed", var5);
        }

        API = api;
    }
}
