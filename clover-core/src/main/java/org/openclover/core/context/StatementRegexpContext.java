package org.openclover.core.context;

import org.openclover.core.instr.java.FileStructureInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;

import java.io.IOException;
import java.util.regex.Pattern;

public class StatementRegexpContext extends RegexpContext implements TaggedPersistent {
    public StatementRegexpContext(StatementRegexpContext ctx) {
        this(ctx.getIndex(), ctx.getName(), ctx.getPattern());
    }

    public StatementRegexpContext(String name, Pattern pattern) {
        super(ContextStore.NO_INDEX, name, pattern);
    }

    public StatementRegexpContext(int index, String name, Pattern pattern) {
        super(index, name, pattern);
    }

    public boolean matches(FileStructureInfo.Marker marker) {
        return super.matches(marker.getNormalisedString());
    }

    @Override
    public boolean isEquivalent(RegexpContext other) {
        return (other instanceof StatementRegexpContext) && super.isEquivalent(other);
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeUTF(getName());
        out.writeInt(getIndex());
        out.writeUTF(getPattern().pattern());
    }

    public static StatementRegexpContext read(TaggedDataInput in) throws IOException {
        final String name = in.readUTF();
        final int index = in.readInt();
        final Pattern pattern = Pattern.compile(in.readUTF());
        return new StatementRegexpContext(index, name, pattern);
    }
}
