package com.atlassian.clover.registry.metrics;

import com.atlassian.clover.api.registry.HasMetrics;

public class FileMetrics extends ClassMetrics {

    private int numClasses;
    private int lineCount;
    private int ncLineCount;

    public FileMetrics(HasMetrics owner) {
        super(owner);
    }

    @Override
    public String getType() {
       return "file";
    }

    public int getNumClasses() {
        return numClasses;
    }

    public void addNumClasses(int numClasses) {
        this.numClasses += numClasses;
    }

    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    @Override
    public String getChildType() {
        return super.getType();
    }

    @Override
    public int getNumChildren() {
        return getNumClasses();
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public int getNcLineCount() {
        return ncLineCount;
    }

    public void setNcLineCount(int ncLineCount) {
        this.ncLineCount = ncLineCount;
    }

    public FileMetrics add(FileMetrics metrics) {
        super.add(metrics);
        numClasses += metrics.numClasses;
        lineCount += metrics.lineCount;
        ncLineCount += metrics.ncLineCount;
        return this;
    }

    public float getAvgMethodsPerClass() {
        return  getFraction(getNumMethods(), numClasses);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FileMetrics that = (FileMetrics) o;

        if (lineCount != that.lineCount) return false;
        if (ncLineCount != that.ncLineCount) return false;
        if (numClasses != that.numClasses) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + numClasses;
        result = 31 * result + lineCount;
        result = 31 * result + ncLineCount;
        return result;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "FileMetrics{" +
            "numClasses=" + numClasses +
            ", lineCount=" + lineCount +
            ", ncLineCount=" + ncLineCount +
            "} " + super.toString();
    }
    ///CLOVER:ON
}
