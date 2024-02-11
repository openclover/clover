package com.atlassian.clover;


import org.openclover.runtime.Logger;

public class Contract {
    /** use to log messages **/
    private static final Logger LOG = Logger.getInstance();

    private Contract() {
    }

    private static void fail(String msg) {
        ContractFailedException e = new ContractFailedException(msg);
        LOG.error(e.getMessage(), e);
        throw e;
    }


    public static void check(boolean cond, String desc) {
        if (!cond) {
            fail("Assertion failed: " + desc);
        }
    }

    public static void pre(boolean cond) {
        if (!cond) {
            fail("Precondition failed");
        }
    }

    public static void post(boolean cond) {
        if (!cond) {
            fail("Postcondition failed");
        }
    }
}
