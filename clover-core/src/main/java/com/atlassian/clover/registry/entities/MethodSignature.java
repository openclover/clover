package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.Annotation;
import com.atlassian.clover.api.registry.MethodSignatureInfo;
import com.atlassian.clover.instr.java.CloverToken;
import com.atlassian.clover.instr.java.TokenListUtil;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newHashMap;

public class MethodSignature implements TaggedPersistent, MethodSignatureInfo {

    /** Empty arrays - used as a flyweight for empty arrays in @paramTypes and @throwsTypes which seem to regularly occur in the model */
    private static final String[] EMPTY_STRINGS = new String[]{};

    private static final Parameter[] EMPTY_PARAMS = new Parameter[]{};

    private Map<String, List<String>> tags = newHashMap();

    private Modifiers modifiers = new Modifiers();

    private String name = "";

    private String typeParams = "";

    private String returnType = "";

    private Parameter[] parameters = new Parameter[]{};

    private String[] throwsTypes = new String[]{};

    public transient String normSeqPrefix;
    public transient String normSeqSuffix;

    public MethodSignature(String name) {
        this.name = name;
    }

    public MethodSignature(String name, int modifiers, AnnotationImpl[] annotations) {
        this.name = name;
        this.modifiers = Modifiers.createFrom(modifiers, annotations);
    }

    public MethodSignature(String name, String typeParams, String returnType, Parameter[] parameters, String[] throwsTypes, Modifiers modifiers) {
        this(null, null, null, name, typeParams, returnType, parameters, throwsTypes);
        this.modifiers = modifiers;
    }

    public MethodSignature(CloverToken firstToken, CloverToken nameToken, CloverToken lastToken, String name,
                           String typeParams, String returnType, Parameter[] parameters, String[] throwsTypes) {
        this(firstToken, nameToken, lastToken, new HashMap<String, List<String>>(),
                new Modifiers(), name, typeParams, returnType, parameters, throwsTypes);
    }

    public MethodSignature(CloverToken firstToken, CloverToken nameToken, CloverToken lastToken, Map<String, List<String>> tags,
                           Modifiers modifiers, String name, String typeParams, String returnType, Parameter[] parameters, String[] throwsTypes) {
        if ((firstToken != null) || (lastToken != null)) {
            //Make renamed method private so in classes annotated with @org.testng.annoations.Test
            //the renamed method is not counted as a test (thus doubly counting test methods)
            int mods = modifiers.getMask();
            mods &= ~(Modifier.PUBLIC | Modifier.PROTECTED);
            mods |= Modifier.PRIVATE;
            normSeqPrefix = ModifierExt.toString(mods) + (typeParams != null ? " " + typeParams : "") + (returnType != null ? " " + returnType : "") + " ";
            normSeqSuffix = TokenListUtil.getNormalisedSequence(nameToken.getNext(), lastToken);
        }
        this.tags = flyweightIfEmptyFor(tags);
        this.modifiers = modifiers;
        this.name = name;
        this.typeParams = typeParams;
        this.returnType = returnType;
        this.parameters = flyweightIfEmptyFor(parameters);
        this.throwsTypes = flyweightIfEmptyFor(throwsTypes);
    }

    public Map<String, List<String>> getTags() {
        return tags;
    }

    public int getModifiersMask() {
        return modifiers.getMask();
    }

    public void setModifiers(int modifiers) {
        this.modifiers.setMask(modifiers);
    }

    @Override
    public Modifiers getModifiers() {
        return modifiers;
    }

    @Override
    @NotNull
    public Map<String, Collection<Annotation>> getAnnotations() {
        return modifiers.getAnnotations();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    @Override
    public String getTypeParams() {
        return typeParams;
    }

    /**
     * Returns the parameter types for the method signature.
     * This will be null for instance and static initializers. ie {...} and static {...}
     * @return an array of parameter types, or null
     */
    @Override
    public Parameter [] getParameters() {
        return parameters;
    }

    @Override
    public boolean hasParams() {
        return parameters != null && parameters.length > 0;
    }

    @Override
    public int getParamCount() {
        return parameters == null ? 0 : parameters.length;
    }


    public String listParamTypes() {
        StringBuilder types = new StringBuilder();
        String sep = "";
        for (int i = 0; parameters != null && i < parameters.length; i++) {
            types.append(sep);
            types.append(parameters[i].getType());
            sep = ",";
        }
        return types.toString();
    }

    public String listParamIdents() {
        StringBuilder idents = new StringBuilder();
        String sep = "";
        for (int i = 0; parameters != null && i < parameters.length; i++) {
            idents.append(sep);
            idents.append(parameters[i].getName());
            sep = ",";
        }
        return idents.toString();
    }

    public boolean hasThrowsTypes() {
        return throwsTypes != null && throwsTypes.length > 0;
    }

    @Override
    public String [] getThrowsTypes() {
        return throwsTypes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRenamedNormalisedSignature(String newName) {
        if (normSeqPrefix == null) {
            throw new IllegalStateException("This signature is not renameable.");
        }

        StringBuilder renamed = new StringBuilder();
        renamed.append(normSeqPrefix);
        renamed.append(" ");
        renamed.append(newName);
        renamed.append(normSeqSuffix);

        return renamed.toString();

    }

    private <K ,V> Map<K, V> flyweightIfEmptyFor(Map<K, V> map) {
        return (map == null || map.isEmpty()) ? Collections.<K, V>emptyMap() : map;
    }

    private String[] flyweightIfEmptyFor(String[] strings) {
        return (strings != null && strings.length == 0) ? EMPTY_STRINGS : strings;
    }

    private Parameter[] flyweightIfEmptyFor(Parameter[] params) {
        return (params != null && params.length == 0) ? EMPTY_PARAMS : params;
    }

    public static void writeNull(DataOutput out) throws IOException {
        out.writeBoolean(false);
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(typeParams);
        out.writeUTF(returnType);
        out.write(Modifiers.class, modifiers);

        final Set<Map.Entry<String,List<String>>> entries = tags.entrySet();
        out.writeInt(entries.size());
        for (Map.Entry<String, List<String>> entry : entries) {
            out.writeUTF(entry.getKey());
            final List<String> values = entry.getValue();
            out.writeInt(values.size());
            for (String value : values) {
                out.writeUTF(value);
            }
        }

        final int numParameters = parameters == null ? 0 : parameters.length;
        out.writeInt(numParameters);
        for (int i = 0; i < numParameters; i++) {
            out.write(Parameter.class, parameters[i]);
        }

        final int numThrowsTypes = throwsTypes == null ? 0 : throwsTypes.length;
        out.writeInt(numThrowsTypes);
        for (int i = 0; i < numThrowsTypes; i++) {
            out.writeUTF(throwsTypes[i]);
        }
    }

    public static MethodSignature read(TaggedDataInput in) throws IOException {
        final String name = in.readUTF();
        final String typeParam = in.readUTF();
        final String returnType = in.readUTF();
        final Modifiers modifiers = in.read(Modifiers.class);

        final int numTagEntries = in.readInt();
        final Map<String, List<String>> tags = newHashMap();
        for (int i = 0; i < numTagEntries; i++) {
            final String key = in.readUTF();
            final int numValues = in.readInt();
            final List<String> values = newLinkedList();
            for (int j = 0; j < numValues; j++) {
                values.add(in.readUTF());
            }
            tags.put(key, values);
        }

        final int numParameters = in.readInt();
        final Parameter[] parameters = numParameters == 0 ? EMPTY_PARAMS : new Parameter[numParameters];
        for (int i = 0; i < numParameters; i++) {
            parameters[i] = in.read(Parameter.class);
        }

        final int numThrowsTypes = in.readInt();
        final String[] throwsTypes = numThrowsTypes == 0 ? EMPTY_STRINGS : new String[numThrowsTypes];
        for (int i = 0; i < numThrowsTypes; i++) {
            throwsTypes[i] = in.readUTF();
        }

        return new MethodSignature(null, null, null, tags, modifiers, name, typeParam, returnType, parameters, throwsTypes);
    }


    @Override
    public String getNormalizedSignature() {
        // normalize the method sig
        final String modifiers = ModifierExt.toString(getModifiersMask());
        StringBuilder builder = new StringBuilder();
        // TODO: if isGroovy() && getReturnType == 'def' don't add a modifier?
        // TODO: more possible when CLOV-888 is implemented.
        builder.append(modifiers).append(" "); // modifiers 
        if (typeParams != null && !"".equals(typeParams)) {
            builder.append(typeParams).append(" "); // type params
        }
        if (getReturnType() != null && getReturnType().length() > 0) {
            builder.append(getReturnType()).append(" "); // return type
        }
        builder.append(getName()); // name
        appendParameters(builder);
        appendThrowsClause(builder);
        return builder.toString();
    }

    private void appendThrowsClause(StringBuilder builder) {
        if (hasThrowsTypes()) {
            builder.append(" throws "); // exceptions
            for (String e: getThrowsTypes()) {
                builder.append(e).append(", ");
            }
            removeLastTwo(builder);
        }
    }

    private void appendParameters(StringBuilder builder) {
        builder.append("("); // parameters
        if (hasParams()) {
            for (Parameter param : getParameters()) {
                builder.append(param.getType()).append(" ").append(param.getName()).append(", ");
            }
            if (getParameters().length > 0) {
                removeLastTwo(builder);
            }
        }
        builder.append(")");
    }

    private static void removeLastTwo(StringBuilder builder) {
        builder.delete(builder.length() - 2, builder.length());
    }


    ///CLOVER:OFF
    @Override
    public String toString() {
        return "MethodSignature{" +
            "throwsTypes=" + (throwsTypes == null ? null : Arrays.toString(throwsTypes)) +
            ", tags=" + tags +
            ", modifiers=" + modifiers +
            ", name='" + name + '\'' +
            ", typeParams='" + typeParams + '\'' +
            ", returnType='" + returnType + '\'' +
            ", parameters=" + (parameters == null ? null : Arrays.toString(parameters)) +
            '}';
    }
    ///CLOVER:ON
}
