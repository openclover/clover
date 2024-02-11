package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;

public class PackageMetrics extends FileMetrics {

    private int numFiles;

    public PackageMetrics(HasMetrics owner) {
        super(owner);
    }


    @Override
    public String getType() {
       return "package";
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    @Override
    public int getNumChildren() {
        return getNumFiles();
    }

    @Override
    public String getChildType() {
        return super.getType();
    }

    public float getAvgClassesPerFile() {
        return getFraction(getNumClasses(), numFiles);
    }

    public PackageMetrics add(PackageMetrics metrics) {
        super.add(metrics);
        numFiles += metrics.numFiles;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PackageMetrics that = (PackageMetrics) o;
        return numFiles == that.numFiles;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + numFiles;
        return result;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "PackageMetrics{" +
            "numFiles=" + numFiles +
            "} " + super.toString();
    }
    ///CLOVER:ON
}
