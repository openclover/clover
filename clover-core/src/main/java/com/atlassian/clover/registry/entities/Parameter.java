package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.ParameterInfo;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;

import java.io.IOException;

// IMPLEMENTATIAON NOTE
// see CLOV-55
// These objects have a long life in the JVM during instrumentation,
// so be careful what they keep a reference to, esp CloverToken and its
// linked list of the whole file
public class Parameter implements TaggedPersistent, ParameterInfo {
    public static final String INFERRED = "<inferred>";

    private final String type;
    private final String name;

    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeUTF(type);
        out.writeUTF(name);
    }

    public static Parameter read(TaggedDataInput in) throws IOException {
        return new Parameter(in.readUTF(), in.readUTF());
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "Parameter{" +
            "type='" + type + '\'' +
            ", name='" + name + '\'' +
            '}';
    }
    ///CLOVER:ON
}
