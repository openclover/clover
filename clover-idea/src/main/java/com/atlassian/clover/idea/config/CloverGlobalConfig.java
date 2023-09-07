package com.atlassian.clover.idea.config;

/**
 * Clover configuration stored globally (in the IDEA installation directory). IDEA's bean serialization mechanism is
 * used (see {@link com.intellij.openapi.components.PersistentStateComponent} and {@link com.intellij.util.xmlb.BeanBinding}).
 *
 * @see com.atlassian.clover.idea.CloverPlugin
 */
public class CloverGlobalConfig {

    public CloverGlobalConfig() {

    }

}
