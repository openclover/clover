package org.openclover.core.context;

import org.openclover.core.instr.java.FileStructureInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;

import java.io.IOException;
import java.util.regex.Pattern;

public class MethodRegexpContext extends RegexpContext implements TaggedPersistent {
    private int maxComplexity;
    private int maxStatements;
    private int maxAggregatedComplexity;
    private int maxAggregatedStatements;

    public MethodRegexpContext(MethodRegexpContext ctx) {
        this(ctx.getIndex(), ctx.getName(), ctx.getPattern(), ctx.getMaxComplexity(), ctx.getMaxStatements(),
                ctx.getMaxAggregatedComplexity(), ctx.getMaxAggregatedStatements());
    }

    public MethodRegexpContext(String name, Pattern pattern) {
        this(name, pattern, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public MethodRegexpContext(String name, Pattern pattern, int maxComplexity, int maxStatements) {
        this(name, pattern, maxComplexity, maxStatements, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public MethodRegexpContext(String name, Pattern pattern, int maxComplexity, int maxStatements,
                               int maxAggregatedComplexity, int maxAggregatedStatements) {
        this(ContextStore.NO_INDEX, name, pattern, maxComplexity, maxStatements, maxAggregatedComplexity, maxAggregatedStatements);
    }

    public MethodRegexpContext(int index, String name, Pattern pattern) {
        this(index, name, pattern, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public MethodRegexpContext(int index, String name, Pattern pattern, int maxComplexity, int maxStatements,
                               int maxAggregatedComplexity, int maxAggregatedStatements) {
        super(index, name, pattern);
        this.maxComplexity = maxComplexity;
        this.maxStatements = maxStatements;
        this.maxAggregatedComplexity = maxAggregatedComplexity;
        this.maxAggregatedStatements = maxAggregatedStatements;
    }

    public int getMaxComplexity() {
        return maxComplexity;
    }

    public int getMaxStatements() {
        return maxStatements;
    }

    public int getMaxAggregatedComplexity() {
        return maxAggregatedComplexity;
    }

    public int getMaxAggregatedStatements() {
        return maxAggregatedStatements;
    }

    public boolean matches(FileStructureInfo.MethodMarker methodMarker) {
        return super.matches(methodMarker.getNormalisedSignature())
                && methodMarker.getMethod().getMetrics().getComplexity() <= maxComplexity
                && methodMarker.getMethod().getStatements().size() <= maxStatements
                && methodMarker.getMethod().getAggregatedComplexity() <= maxAggregatedComplexity
                && methodMarker.getMethod().getAggregatedStatementCount() <= maxAggregatedStatements;
    }

    @Override
    public boolean isEquivalent(RegexpContext other) {
        return (other instanceof MethodRegexpContext)
                && ((MethodRegexpContext)other).getMaxComplexity() == maxComplexity
                && ((MethodRegexpContext)other).getMaxStatements() == maxStatements
                && ((MethodRegexpContext)other).getMaxAggregatedComplexity() == maxAggregatedComplexity
                && ((MethodRegexpContext)other).getMaxAggregatedStatements() == maxAggregatedStatements
                && super.isEquivalent(other);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MethodRegexpContext that = (MethodRegexpContext) o;
        return (maxComplexity == that.maxComplexity)
                && (maxStatements == that.maxStatements)
                && (maxAggregatedComplexity == that.maxAggregatedComplexity)
                && (maxAggregatedStatements == that.maxAggregatedStatements);
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + maxComplexity;
        result = 31 * result + maxStatements;
        result = 31 * result + maxAggregatedComplexity;
        result = 31 * result + maxAggregatedStatements;
        return result;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeUTF(getName());
        out.writeInt(getIndex());
        out.writeUTF(getPattern().pattern());
        out.writeInt(maxComplexity);
        out.writeInt(maxStatements);
        out.writeInt(maxAggregatedComplexity);
        out.writeInt(maxAggregatedStatements);
    }

    public static MethodRegexpContext read(TaggedDataInput in) throws IOException {
        final String name = in.readUTF();
        final int index = in.readInt();
        final Pattern pattern = Pattern.compile(in.readUTF());
        final int maxComplexity = in.readInt();
        final int maxStatements = in.readInt();
        final int maxAggregatedComplexity = in.readInt();
        final int maxAggregatedStatements = in.readInt();
        return new MethodRegexpContext(index, name, pattern, maxComplexity, maxStatements, maxAggregatedComplexity, maxAggregatedStatements);
    }
}
