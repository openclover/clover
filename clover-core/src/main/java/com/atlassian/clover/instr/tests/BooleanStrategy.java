package com.atlassian.clover.instr.tests;

public interface BooleanStrategy {

    /**
     * Apply the strategy to the particular detector
     * @param values an array of booleans to process
     * @return true if all elements in the array were true 
     */
    boolean process(boolean[] values);
}

