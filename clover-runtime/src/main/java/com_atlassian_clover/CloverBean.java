package com_atlassian_clover;

/**
 * This class was created so that maven 1 can easily access the
 * static method Clover.globalFlush.
 *
 * It is also used for the grails plugin, since it doesn't
 *  
 */
public class CloverBean {
    public void flush() {
        Clover.globalFlush();
    }
}
