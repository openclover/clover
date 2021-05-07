package org.openclover.eclipse.core.upgrade.hooks;

public class ConfigUpdateException extends Exception {
    public ConfigUpdateException(String message, Exception e) {
        super(message, e);
    }

    public ConfigUpdateException(String message) {
        super(message);
    }
}
