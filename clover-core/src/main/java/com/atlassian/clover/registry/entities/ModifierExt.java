package com.atlassian.clover.registry.entities;

import java.lang.reflect.Modifier;

/**
 * Extension of the java.lang.reflect.Modifier to handle the 'default' keyword in a method signature.
 */
public class ModifierExt {
    /**
     * An artificial modifier to hold the "default" keyword, which is being used to mark the virtual extension
     * method in an interface.
     */
    public static final int DEFAULT      = 0x01000000;

    public static String toString(int modifierMask) {
        String baseModifiers = ((modifierMask & DEFAULT) != 0) ? "default " : "";
        baseModifiers += Modifier.toString(modifierMask);
        return baseModifiers.trim();
    }

}
