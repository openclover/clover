package org.openclover.core.registry.entities;

import java.lang.reflect.Modifier;

/**
 * Extension of the java.lang.reflect.Modifier to handle the 'default' keyword in a method signature.
 */
public class ModifierExt {
    /**
     * An artificial modifier to hold the "default" keyword, which is being used to mark the virtual extension
     * method in an interface.
     */
    public static final long DEFAULT      = 0x0001_0000_0000L;

    /**
     * An artificial modifier to hold the "sealed" pseudo-keyword, used to mark classes or interfaces as sealed.
     */
    public static final long SEALED       = 0x0002_0000_0000L;

    /**
     * An artificial modifier to hold the "non-sealed" pseudo-keyword, used to marked subclasses or
     * sub-interfaces of a sealed class/interface as open for extension.
     */
    public static final long NON_SEALED   = 0x0004_0000_0000L;

    /**
     * An artificial modifier to hold the "record" pseudo-keyword, used to delare record classes.
     * Keep in mind this modifier does not appear with the "class" keyword (e.g. "record class A")
     * but on its own (e.g. "record A"). That's why it's NOT included in the toString().
     */
    public static final long RECORD       = 0x0008_0000_0000L;

    public static String toString(long modifierMask) {
        String optDefault = ((modifierMask & DEFAULT) != 0) ? "default " : "";
        String optSealed =  ((modifierMask & SEALED) != 0) ? "sealed " : "";
        String optNonSealed =  ((modifierMask & NON_SEALED) != 0) ? "non-sealed " : "";
        String baseModifiers = optDefault + optSealed + optNonSealed + Modifier.toString((int)modifierMask);
        return baseModifiers.trim();
    }

}
