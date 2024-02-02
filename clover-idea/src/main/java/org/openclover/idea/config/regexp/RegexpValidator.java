package org.openclover.idea.config.regexp;

import com.atlassian.clover.context.ContextStore;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regexp object validator.<p>
 * Separate class due to overcomplicated dependencies.<p>
 * Override (chain) with context-sensitive logic.
 */
public class RegexpValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[^\\s,]+$");

    /**
     * Verifies regexp without context.<p>
     * Override and extend for context-sensitive validation.
     *
     * @param regexp Regexp to be validated.
     */
    public void validate(Regexp regexp) {
        final String pattern = regexp.getRegex();
        if (pattern != null && pattern.length() > 0) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                regexp.setValidationMessage("Pattern '" + pattern + "' is invalid.");
                return;
            }
        } else {
            regexp.setValidationMessage("Pattern is empty.");
            return;
        }

        final String name = regexp.getName();

        if (name == null || name.length() == 0) {
            regexp.setValidationMessage("Context name must not be empty.");
            return;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            regexp.setValidationMessage("Context name must not contain whitespaces or ',' character.");
            return;
        }

        if (ContextStore.isReservedName(name)) {
            regexp.setValidationMessage("Context name is reserved.");
            return;
        }

        regexp.setValidationMessage(null);
    }

    public static final RegexpValidator CONTEXTLESS_VALIDATOR = new RegexpValidator();
}
