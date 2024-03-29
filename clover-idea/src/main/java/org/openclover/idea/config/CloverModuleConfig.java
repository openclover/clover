package org.openclover.idea.config;

public class CloverModuleConfig {
    private boolean excluded;

    /**
     * Construct with default values:
     * <pre>
     *  isExcluded() == false
     * </pre>
     */
    public CloverModuleConfig() {

    }

    /**
     * Construct and set properties as provided.
     * @param isExcluded whether module is excluded from instrumentation
     */
    public CloverModuleConfig(boolean isExcluded) {
        this.excluded = isExcluded;
    }

    /**
     * Construct from another instance
     * @param other
     */
    public CloverModuleConfig(CloverModuleConfig other) {
        this.excluded = other.excluded;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    @SuppressWarnings({"ALL"})
    // autogenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CloverModuleConfig that = (CloverModuleConfig) o;

        if (excluded != that.excluded) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (excluded ? 1 : 0);
    }
}
