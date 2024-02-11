package org.openclover.ant;

import java.util.List;

import static org.openclover.core.util.Lists.newLinkedList;

/**
 * Handles the following declaration
 * <pre>
 *     &lt;profiles&gt;
 *         &lt;profile ...&gt;
 *         &lt;profile ...&gt;
 *         &lt;profile ...&gt;
 *         ...
 *     &lt;/profiles&gt;
 * </pre>
 * in Ant build script.
 * @see AntCloverProfile
 */
public class AntCloverProfiles {
    private List<AntCloverProfile> profiles = newLinkedList();

    /**
     * Setter for Ant's
     * <pre>
     * &lt;profiles&gt;
     *      &lt;profile ...&gt;  &lt;!-- one or more --&gt;
     * &lt;/profiles&gt;
     * </pre>
     */
    public void addConfiguredProfile(AntCloverProfile profile) {
        validate(profile);
        profiles.add(profile);
    }

    /**
     * Data validation:
     *  - profile names must be unique
     */
    private void validate(AntCloverProfile profile) throws IllegalArgumentException {
        for (AntCloverProfile p : profiles) {
            if (p.getName().equals(profile.getName())) {
                throw new IllegalArgumentException(
                        "Duplicated value in the <profile name=\"" + profile.getName() + "\">"
                        + " - all names of profiles must be unique");
            }
        }
    }

    public List<AntCloverProfile> getProfiles() {
        return profiles;
    }
}
