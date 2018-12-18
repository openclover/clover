package com.atlassian.clover.api.command;

/**
 * For parsing command line arguments.
 */
public interface ArgProcessor<Config> {
    boolean matches(String[] args, int i);

    int process(String[] args, int i, Config cfg);

    String help();
}
