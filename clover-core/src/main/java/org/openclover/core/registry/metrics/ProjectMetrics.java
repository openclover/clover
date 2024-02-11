package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;

public class ProjectMetrics extends PackageMetrics {

    private int numPackages;

    public ProjectMetrics(HasMetrics owner) {
        super(owner);
    }

    @Override
    public String getType() {
       return "project";
    }

    public int getNumPackages() {
        return numPackages;
    }

    public void setNumPackages(int numPackages) {
        this.numPackages = numPackages;
    }

    @Override
    public int getNumChildren() {
        return getNumPackages();
    }

    @Override
    public String getChildType() {
        return super.getType();
    }

    public float getAvgClassesPerPackage() {
        return getFraction(getNumClasses(), getNumPackages());
    }

    public float getAvgFilesPerPackage() {
        return getFraction(getNumFiles(), getNumPackages());
    }

    public ProjectMetrics add(ProjectMetrics metrics) {
        super.add(metrics);
        numPackages += metrics.numPackages;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProjectMetrics that = (ProjectMetrics) o;
        return numPackages == that.numPackages;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + numPackages;
        return result;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "ProjectMetrics{" +
            "numPackages=" + numPackages +
            "} " + super.toString();
    }
    ///CLOVER:ON
}
