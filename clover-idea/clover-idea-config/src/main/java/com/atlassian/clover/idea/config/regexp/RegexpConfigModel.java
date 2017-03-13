package com.atlassian.clover.idea.config.regexp;

import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.idea.config.regexp.Regexp;
import com.atlassian.clover.idea.config.regexp.RegexpValidator;
import com.atlassian.clover.idea.util.ComparatorUtil;
import com.atlassian.clover.idea.config.CloverPluginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newHashMap;

public class RegexpConfigModel extends Observable implements Observer {


    public static final String ADD = "add";
    public static final String REMOVE = "remove";
    public static final String EDIT = "edit";
    public static final String SELECTED = "selected";

    /**
     * List of Regexp objects.
     */
    private List<Regexp> regexps = newArrayList();

    /**
     * The currently selected regexp.
     */
    private Regexp currentRegexp;

    /**
     * Error message for the current regexp if applicable.
     */
    private String errorMessage;

    private final ContextStore context;

    private final ModelContextRegexpValidator modelContextRegexpValidator = new ModelContextRegexpValidator();

    public RegexpValidator getModelContextRegexpValidator() {
        return modelContextRegexpValidator;
    }

    public RegexpConfigModel(ContextStore context) {
        this.context = context;
    }

    public List<Regexp> getRegexps() {
        return regexps;
    }

    public Regexp getSelected() {
        return currentRegexp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setSelected(Regexp regexp) {
        if (regexp != null && !regexps.contains(regexp)) {
            throw new IllegalArgumentException("Can not select unknown regexp.");
        }
        if (ComparatorUtil.areDifferent(regexp, currentRegexp)) {
            currentRegexp = regexp;
            setChanged();
        }
        notifyObservers(SELECTED);
    }

    public void loadFrom(CloverPluginConfig config) {
        regexps.clear();
        currentRegexp = null;
        errorMessage = null;

        for (Regexp c : config.getRegexpContexts()) {
            Regexp r = new Regexp(c, modelContextRegexpValidator);
            r.addObserver(this);
            regexps.add(r);
        }

        setChanged();
        notifyObservers();

        modelContextRegexpValidator.verifyDuplicateNames();
    }

    /**
     * Updates ContextSet Spec with custom regexps state.
     *
     * @param config config to commit data to
     */
    public void commitTo(CloverPluginConfig config) {
        final List<Regexp> l = new ArrayList<Regexp>(regexps.size());
        for (Regexp r : regexps) {
            l.add(new Regexp(r));
        }
        config.setRegexpContexts(l);
    }

    public void add(Regexp regexp) {
        regexps.add(regexp);
        regexp.addObserver(this);
        setChanged();
        notifyObservers(ADD);
    }

    public void remove(Regexp regexp) {
        int indexOf = regexps.indexOf(regexp);
        if (indexOf == -1) {
            throw new IllegalArgumentException("Can not remove unknown regexp: " + regexp);
        }

        regexp.deleteObserver(this);
        regexps.remove(regexp);
        setChanged();
        notifyObservers(REMOVE);

        if (regexps.size() > indexOf) {
            currentRegexp = regexps.get(indexOf);
        } else if (regexps.size() == 0) {
            currentRegexp = null;
        } else {
            currentRegexp = regexps.get(regexps.size() - 1);
        }
        setChanged();
        notifyObservers(SELECTED);

        modelContextRegexpValidator.verifyDuplicateNames(); // in case a conflicting name disappeared
    }

    @Override
    public void update(Observable o, Object arg) {
        setChanged();
        notifyObservers(EDIT);
    }

    private class ModelContextRegexpValidator extends RegexpValidator {
        @Override
        public void validate(Regexp validatedRegexp) {
            final String name = validatedRegexp.getName();
            final NamedContext existingContext = context.getContext(name);
            if (existingContext != null) {
                validatedRegexp.setChanged(validatedRegexp.isDifferent(existingContext));
            } else {
                validatedRegexp.setChanged(true);
            }

            // Current regexp might not be on the list yet - after add new or copy. Name uniqueness is guaranteed,
            // but there may by other problems - check for them.
            // Verification may be repeated in the for() below, but that's OK.
            super.validate(validatedRegexp);
            verifyDuplicateNames();

            if (ContextStore.isReservedName(name)) {
                validatedRegexp.setValidationMessage("Name '" + name + "' is reserved.");
            }

        }

        private void verifyDuplicateNames() {
            // resolve (non)duplicate names
            Map<String, Regexp> map = newHashMap();
            for (Regexp regexp : regexps) {
                String name = regexp.getName();
                Regexp previous = map.get(name);
                if (previous != null) {
                    previous.setValidationMessage("Duplicate name"); // will be set multiple times, but that's ok
                    regexp.setValidationMessage("Duplicate name");
                } else {
                    // not a duplicate (for now), check for other problems
                    super.validate(regexp); // in case it has some problem other than duplicate name
                    map.put(name, regexp);
                }
            }
        }
    }
}
