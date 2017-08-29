package org.openclover.ci;

import clover.com.google.common.base.Predicate;

/**
 * Search for properties in a form:
 * <pre>
 *     -Dname=value
 *     --define name=value
 * </pre>
 */
public class HasPropertyPredicate implements Predicate<String> {
    private boolean isDefineBefore;
    private String name;

    public HasPropertyPredicate(final String name) {
        this.name = name;
    }

    @Override
    public boolean apply(final String property) {
        boolean isFound = property.startsWith("-D" + name + "=")   // -Dname=
                || property.equals("-D" + name)                    // -Dname
                || (isDefineBefore && property.startsWith(name + "="))   // --define/-D name=
                || (isDefineBefore && property.equals(name));            // --define/-D name
        isDefineBefore = property.equals("--define") || property.equals("-D");
        return isFound;
    }
}