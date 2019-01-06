package com.atlassian.clover.model;

import com.atlassian.clover.registry.entities.BaseProjectInfo;

import java.io.File;
import java.util.Comparator;


/**
 * a simple encapsulation of a history point 
 */
public class CoverageDataPoint {

    private String version;
    private long generated;
    private BaseProjectInfo project;
    private File dataFile;

    public static final Comparator<CoverageDataPoint> CHRONOLOGICAL_CMP = new Comparator<CoverageDataPoint>() {
        @Override
        public int compare(CoverageDataPoint obj1, CoverageDataPoint obj2) {
            if (obj1 == null && obj2 == null) {
                return 0;
            } else if (obj1 == null) {
                return -1;
            } else if (obj2 == null) {
                return 1;
            } else {
                try {
                    Long ts1 = obj1.getProject().getVersion();
                    Long ts2 = obj2.getProject().getVersion();
                    return ts1.compareTo(ts2);
                } catch (ClassCastException e) {
                    return 0;
                }
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

    public BaseProjectInfo getProject() {
        return project;
    }

    public void setProject(BaseProjectInfo project) {
        this.project = project;
    }

    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }

    public File getDataFile() {
        return dataFile;
    }
}
