package com.atlassian.clover.idea.config;

public interface ConfigChangeListener {

    /**
     * @param evt ConfigChangeEvent
     */
    public void configChange(ConfigChangeEvent evt);

}
