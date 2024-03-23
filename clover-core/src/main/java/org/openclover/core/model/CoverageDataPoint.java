package org.openclover.core.model;

import org.openclover.core.api.registry.ProjectInfo;

import java.io.File;
import java.util.Comparator;


/**
 * a simple encapsulation of a history point 
 */
public class CoverageDataPoint {

    private String version;
    private long generated;
    private ProjectInfo project;
    private File dataFile;

    public static final Comparator<CoverageDataPoint> CHRONOLOGICAL_CMP = (dataPoint1, dataPoint2) -> {
        if (dataPoint1 == null && dataPoint2 == null) {
            return 0;
        } else if (dataPoint1 == null) {
            return -1;
        } else if (dataPoint2 == null) {
            return 1;
        } else {
            try {
                Long ts1 = dataPoint1.getProject().getVersion();
                Long ts2 = dataPoint2.getProject().getVersion();
                return ts1.compareTo(ts2);
            } catch (ClassCastException e) {
                return 0;
            }
        }
    };

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getGenerated() {
        return generated;
    }

    public void setGenerated(long generated) {
        this.generated = generated;
    }

    public ProjectInfo getProject() {
        return project;
    }

    public void setProject(ProjectInfo project) {
        this.project = project;
    }

    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    public File getDataFile() {
        return dataFile;
    }
}
