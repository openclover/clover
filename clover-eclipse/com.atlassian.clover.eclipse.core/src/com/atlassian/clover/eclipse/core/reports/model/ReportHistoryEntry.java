package com.atlassian.clover.eclipse.core.reports.model;

import org.osgi.service.prefs.Preferences;

import java.io.File;
import java.math.BigDecimal;
import java.util.Objects;

import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.Type;

public class ReportHistoryEntry {
    private String path = "";
    private Type type = Type.HTML;
    private String name = "unknown";
    private long when = -1l;

    public ReportHistoryEntry(Current config, long when) {
        path = config.getMainOutFile().getAbsolutePath();
        type = config.getFormat().getType();
        name = config.getTitle();
        this.when = when;
    }

    public ReportHistoryEntry(Preferences preferences) {
        path = preferences.get("path", path);
        type = Type.valueOf(preferences.get("type", type.name()));
        name = preferences.get("name", name);
        when = preferences.getLong("when", when);
    }

    public void saveTo(Preferences preferences) {
        preferences.put("path", path);
        preferences.put("type", type.name());
        preferences.put("name", name);
        preferences.putLong("when", when);
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getWhen() {
        return when;
    }

    public Type getType() {
        return type;
    }

    public boolean pathExists() {
        return new File(path).exists();
    }

    public boolean isValid() {
        return pathExists() && when != -1;
    }

    public String toString() {
        return
            "\"" + name + "\"\t" + type + " - generated "
            + new Interval(BigDecimal.valueOf(
                (System.currentTimeMillis() - when) / 1000),
                Interval.UNIT_SECOND).toSensibleString()
            + " ago";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportHistoryEntry that = (ReportHistoryEntry)o;

        if (when != that.when) return false;
        if (!Objects.equals(name, that.name))
            return false;
        if (!Objects.equals(path, that.path))
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (path != null ? path.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (int)(when ^ (when >>> 32));
        return result;
    }
}
