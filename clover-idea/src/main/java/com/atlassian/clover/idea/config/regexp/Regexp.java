package com.atlassian.clover.idea.config.regexp;

import com.atlassian.clover.idea.util.ComparatorUtil;
import com.atlassian.clover.idea.config.ContextFilterRegexpType;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.context.RegexpContext;
import com.atlassian.clover.context.StatementRegexpContext;

import java.text.MessageFormat;
import java.util.List;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regexp extends Observable {

    private String name;
    private int type;
    private String regex;
    private boolean changed;
    private boolean enabled;

    private String validationMessage;
    private RegexpValidator validator;

    public Regexp() {
        this(RegexpValidator.CONTEXTLESS_VALIDATOR);
    }

    public Regexp(RegexpValidator validator) {
        this.validator = validator;
    }

    public Regexp(Regexp other) {
        this(other, other.validator);
    }

    @SuppressWarnings({"SetterBypass"})
    public Regexp(Regexp other, RegexpValidator validator) {
        this.name = other.name;
        this.type = other.type;
        this.regex = other.regex;
        this.changed = other.changed;
        this.enabled = other.enabled;
        this.validator = validator;
        validate();
    }

    @SuppressWarnings({"SetterBypass"})
    public Regexp(RegexpContext other, RegexpValidator validator) {
        this.name = other.getName();
        this.type = other instanceof MethodRegexpContext
                ? ContextFilterRegexpType.Method.ordinal()
                : ContextFilterRegexpType.Statement.ordinal();
        this.regex = other.getPattern().pattern();
//        this.changed = other.isChanged();
//        this.enabled = other.isEnabled();
        this.validator = validator;
        validate();
    }

    public void copyFrom(Regexp other) {
        setName(other.getName());
        setType(other.getType());
        setRegex(other.getRegex());
        setChanged(other.isChanged());
        setEnabled(other.isEnabled());
        validator = other.validator;
        validate();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (ComparatorUtil.areDifferent(this.name, name)) {
            this.name = name;
            validate();
            setChanged();
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        if (this.type != type) {
            this.type = type;
            validate();
            setChanged();
        }
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        if (ComparatorUtil.areDifferent(this.regex, regex)) {
            this.regex = regex;
            validate();
            setChanged();
        }
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        if (this.changed != changed) {
            this.changed = changed;
            setChanged();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            setChanged();
        }
    }

    public RegexpContext toContextSetting() {
        Pattern regexpPattern = Pattern.compile(getRegex());

        return type == ContextFilterRegexpType.Method.ordinal()
                ? new MethodRegexpContext(getName(), regexpPattern)
                : new StatementRegexpContext(getName(), regexpPattern);
    }

    /**
     * Suggest a name to be used as the default for a new regexp context.
     *
     * @param existing list of existing regexps
     * @return suggested new pattern name
     */
    public static String suggestNewName(List<Regexp> existing) {
        return suggestName("Unnamed_{0}", existing);
    }

    /**
     * Suggest a name to be used as the default for a copy of the specified
     * regexp context.
     *
     * @param ctx      Regexp context to retrieve name from
     * @param existing list of existing regexps
     * @return suggested copied pattern name
     */
    public static String suggestCopyName(Regexp ctx, List<Regexp> existing) {
        return suggestName("Copy_{0}_of_" + ctx.getName(), existing);
    }

    public static String suggestName(String template, List<Regexp> existing) {
        String regexp = MessageFormat.format(template, "(\\d+)");
        Pattern pattern = Pattern.compile(regexp);

        long maxIndex = 0;

        for (Regexp regex : existing) {
            Matcher m = pattern.matcher(regex.getName());
            if (m.matches()) {
                long index = Long.parseLong(m.group(1));
                if (index > maxIndex) {
                    maxIndex = index;
                }
            }
        }
        return MessageFormat.format(template, maxIndex + 1);
    }

    public boolean isValid() {
        return validationMessage == null;
    }

    /**
     * Package local so RegexpValidator can use it.
     *
     * @param validationMessage message when invalid or null if all OK.
     */
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void validate() {
        validator.validate(this);
    }

    /**
     * Call to determine whether this configuration-scope Regex is changed in comparison to the equivalent one in
     * the ContextRegistry.<p>
     * Not all fields are compared, just the ones that make the regexp logically different.
     *
     * @param context context to compare to
     * @return true when there is a significant difference, false if Regexp matches the context
     */
    public boolean isDifferent(NamedContext context) {
        RegexpContext regexpContext;
        if (type == ContextFilterRegexpType.Method.ordinal()) {
            if (context instanceof MethodRegexpContext) {
                regexpContext = (RegexpContext) context;
            } else {
                return true;
            }
        } else {
            if (context instanceof StatementRegexpContext) {
                regexpContext = (RegexpContext) context;
            } else {
                return true;
            }
        }
        if (!name.equals(context.getName())) {
            return true;
        }

        return !(regex == null ? regexpContext.getPattern() == null : regex.equals(regexpContext.getPattern().pattern()));

    }

    @SuppressWarnings({"ControlFlowStatementWithoutBraces"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Regexp regexp = (Regexp) o;

        if (enabled != regexp.enabled) return false;
        if (type != regexp.type) return false;
        if (name != null ? !name.equals(regexp.name) : regexp.name != null) return false;
        return !(regex != null ? !regex.equals(regexp.regex) : regexp.regex != null);

    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + type;
        result = 31 * result + (regex != null ? regex.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }
}
