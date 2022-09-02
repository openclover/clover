package com.atlassian.clover;

import com.atlassian.clover.api.CloverException;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** Used to deal with runtime environment. Heavily borrowed from Ant. */
public class Environment {
    private static final char PROP_START;
    
    static {
        String propStart = null;
        try {
            propStart = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(CloverNames.PROP_ENV_PROPREF_STARTCHAR, "$");
                }
            });
        } catch (Exception e) {
            //Ignore
        }
        PROP_START = propStart == null ? '$' : (propStart.length() >= 1 ? propStart.charAt(0) : '$');
    }
    
    /**
     * Replaces ${foo} style properties in a string where foo is a system property that can be read.
     * @return the string with references replaced where they can be. properties that do not exist or can't be
     * replaced because of security exceptions will not be replaced.
     */
    public static String substituteSysPropRefs(String value) {
        try {
            if(value != null && value.indexOf(PROP_START) != -1) {
                Collection<String> fragments = new ArrayList<>();
                Collection<String> propertyRefs = new ArrayList<>();
                parsePropertyString(value, fragments, propertyRefs);
                StringBuilder sb = new StringBuilder();
                Iterator<String> i = fragments.iterator();
                Iterator<String> j = propertyRefs.iterator();
                String fragment;
                for(; i.hasNext(); sb.append(fragment)) {
                    fragment = i.next();
                    if(fragment != null) {
                        continue;
                    }
                    final String propertyName = j.next();
                    String replacement = null;
                    try {
                        replacement = AccessController.doPrivileged(new PrivilegedAction<String>() {
                            @Override
                            public String run() {
                                return System.getProperty(propertyName);
                            }
                        });
                        if(replacement == null) {
                            Logger.getInstance().verbose("System property \"" + propertyName + "\" was null and so was not set");
                        }
                    } catch (SecurityException e) {
                        Logger.getInstance().verbose("Security exception getting system property \"" + propertyName + "\" (will not be substituted)");
                    }
                    fragment = replacement == null ? (PROP_START + "{" + propertyName + "}") : replacement.toString();
                }

                return sb.toString();
            }
        } catch (IllegalPropertySyntax e) {
            Logger.getInstance().verbose("Value \"" + value + "\" is incorrectly specified and no property substitution has taken place", e);
        }
        return value;
    }

    private static void parsePropertyString(String value, Collection<String> fragments, Collection<String> propertyRefs) throws IllegalPropertySyntax {
        int prev = 0;
        int pos;
        while((pos = value.indexOf(PROP_START, prev)) >= 0)  {
            if(pos > 0) {
                fragments.add(value.substring(prev, pos));
            }
            if(pos == value.length() - 1) {
                fragments.add(String.valueOf(PROP_START));
                prev = pos + 1;
            } else
            if(value.charAt(pos + 1) != '{') {
                if(value.charAt(pos + 1) == PROP_START) {
                    fragments.add(String.valueOf(PROP_START));
                    prev = pos + 2;
                } else {
                    fragments.add(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }
            } else {
                int endName = value.indexOf('}', pos);
                if(endName < 0) {
                    throw new IllegalPropertySyntax("Syntax error in property: " + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.add(null);
                propertyRefs.add(propertyName);
                prev = endName + 1;
            }
        }
        if(prev < value.length()) {
            fragments.add(value.substring(prev));
        }
    }

    private static class IllegalPropertySyntax extends CloverException {
        public IllegalPropertySyntax(String reason) {
            super(reason);
        }
    }
}
