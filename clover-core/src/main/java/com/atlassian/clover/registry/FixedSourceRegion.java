package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;

import java.io.IOException;
import java.util.Comparator;

public class FixedSourceRegion implements SourceInfo, TaggedPersistent {
    protected final int startLine;
    protected final int startColumn;
    protected final int endLine;
    protected final int endColumn;

    public FixedSourceRegion(int startLine, int startColumn) {
        this(startLine, startColumn, startLine, startColumn);
    }

    public FixedSourceRegion(SourceInfo other) {
        this(other.getStartLine(), other.getStartColumn(), other.getEndLine(), other.getEndColumn());
    }

    public FixedSourceRegion(int startLine, int startColumn, int endLine, int endColumn) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    @Override
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getStartColumn() {
        return startColumn;
    }

    @Override
    public int getEndLine() {
        return endLine;
    }

    @Override
    public int getEndColumn() {
        return endColumn;
    }

    public FixedSourceRegion setStartLine(int startLine) {
        return new FixedSourceRegion(startLine, startColumn, endLine, endColumn);
    }

    public FixedSourceRegion setStartColumn(int startColumn) {
        return new FixedSourceRegion(startLine, startColumn, endLine, endColumn);
    }

    public FixedSourceRegion setEndLine(int endLine) {
        return new FixedSourceRegion(startLine, startColumn, endLine, endColumn);
    }

    public FixedSourceRegion setEndColumn(int endColumn) {
        return new FixedSourceRegion(startLine, startColumn, endLine, endColumn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FixedSourceRegion that = (FixedSourceRegion)o;

        if (endColumn != that.endColumn) return false;
        if (endLine != that.endLine) return false;
        if (startColumn != that.startColumn) return false;
        if (startLine != that.startLine) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startLine;
        result = 31 * result + startColumn;
        result = 31 * result + endLine;
        result = 31 * result + endColumn;
        return result;
    }

    @Override
    public String toString() {
        return "{" + startLine + ", " + startColumn + ", " + endLine + ", " + endColumn + '}';
    }

    public static final Comparator<SourceInfo> SOURCE_ORDER_COMP = new RegionStartComparator();

    public static FixedSourceRegion of(SourceInfo region) {
        return region instanceof FixedSourceRegion ? (FixedSourceRegion)region : new FixedSourceRegion(region);
    }

    public static class RegionStartComparator implements Comparator<SourceInfo> {

        /**
         * @see  java.util.Comparator
         */
        @Override
        public int compare(SourceInfo reg1, SourceInfo reg2) {
            if (reg1 == null && reg2 == null) {
                return 0;
            } else if (reg1 == null) {
                return -1;
            } else if (reg2 == null) {
                return 1;
            } else {
                int sld = reg1.getStartLine() - reg2.getStartLine();

                if (sld == 0) {
                    int scd = reg1.getStartColumn() - reg2.getStartColumn();
                    if (scd == 0) {
                        int eld = reg1.getEndLine() - reg2.getEndLine();
                        if (eld == 0) {
                            return reg1.getEndColumn() - reg2.getEndColumn();
                        }
                        else {
                            return eld;
                        }
                    }
                    else {
                        return scd;
                    }
                }
                else {
                    return sld;
                }
            }
        }
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        writeRaw(this, out);
    }

    public static void writeRaw(SourceInfo region, TaggedDataOutput out) throws IOException {
        out.writeInt(region.getStartLine());
        out.writeInt(region.getStartColumn());
        out.writeInt(region.getEndLine());
        out.writeInt(region.getEndColumn());
    }

    public static FixedSourceRegion read(TaggedDataInput in) throws IOException {
        return new FixedSourceRegion(in.readInt(), in.readInt(), in.readInt(), in.readInt());
    }
}
