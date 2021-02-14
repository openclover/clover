package org.openclover.eclipse.core.upgrade.hooks;

public class ConfigUpdateFailedException extends ConfigUpdateException {
    public ConfigUpdateFailedException(String message, Exception e) {
        super(message, e);
    }
}
