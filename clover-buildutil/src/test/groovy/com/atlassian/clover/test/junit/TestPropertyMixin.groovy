package com.atlassian.clover.test.junit

/** Mixin for grabbing mandatory system properties required for testing  */
class TestPropertyMixin {
    File getFileProp(String name, boolean mandatory) {
        String prop = System.getProperty(name)
        if (prop == null) {
            if (mandatory) {
                throw new RuntimeException("Mandatory property ${name} must be specified for this test to run")
            } else {
                return null
            }
        }
        new File(prop)
    }

    File getFileProp(String name) {
        getFileProp(name, true)
    }
}