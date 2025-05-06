package org.squiddev.cctweaks.api;

public class CoreNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -3257530593207305353L;

    public CoreNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
