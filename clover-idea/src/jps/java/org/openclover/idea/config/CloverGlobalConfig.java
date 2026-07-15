package org.openclover.idea.config;

/**
 * Clover configuration stored globally (in the IDEA installation directory). IDEA's bean serialization mechanism is
 * used (see {@link com.intellij.openapi.components.PersistentStateComponent} and {@link com.intellij.util.xmlb.BeanBinding}).
 *
 * @see org.openclover.idea.CloverPlugin
 */
public class CloverGlobalConfig {

    public CloverGlobalConfig() {

    }

}
