package org.openclover.eclipse.core.upgrade.hooks;

public class ConfigUpdateAbortedException extends ConfigUpdateException {
    public ConfigUpdateAbortedException(String message, Exception e) {
        super(message, e);
    }

    public ConfigUpdateAbortedException(String message) {
        super(message);
    }
}
