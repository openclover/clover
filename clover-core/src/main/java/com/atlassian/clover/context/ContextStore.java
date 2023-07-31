package com.atlassian.clover.context;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.cfg.instr.MethodContextDef;
import com.atlassian.clover.cfg.instr.StatementContextDef;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.Logger;
import com.atlassian.clover.registry.entities.FullBranchInfo;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.FileElementVisitor;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.FullStatementInfo;
import org.openclover.util.Lists;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Sets.newHashSet;


public class ContextStore implements TaggedPersistent {
    public static final int NO_INDEX = -1;

    public static final int CONTEXT_CLOVER_OFF = 0;
    public static final int CONTEXT_STATIC = 1;
    public static final int CONTEXT_INSTANCE = 2;
    public static final int CONTEXT_CTOR = 3;
    public static final int CONTEXT_METHOD = 4;
    public static final int CONTEXT_SWITCH = 5;
    public static final int CONTEXT_WHILE = 6;
    public static final int CONTEXT_DO = 7;
    public static final int CONTEXT_FOR = 8;
    public static final int CONTEXT_IF = 9;
    public static final int CONTEXT_ELSE = 10;
    public static final int CONTEXT_TRY = 11;
    public static final int CONTEXT_CATCH = 12;
    public static final int CONTEXT_FINALLY = 13;
    public static final int CONTEXT_SYNC = 14;
    public static final int CONTEXT_ASSERT = 15;
    public static final int CONTEXT_DEPRECATED = 16;

    public static final int CONTEXT_PRIVATE_METHOD = 17;
    public static final int CONTEXT_PROPERTY_ACCESSOR = 18;

    public static final int NEXT_INDEX = 19;

    private static Map<String, SimpleContext> reservedContexts = new LinkedHashMap<>();
    private static Map<String, MethodRegexpContext> reservedMethodContexts = new LinkedHashMap<>();
    private static Set<String> reservedNames = newHashSet();
    static {
        addContext(reservedContexts, new SimpleContext(CONTEXT_CLOVER_OFF, "SourceDirective"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_STATIC, "static"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_CTOR, "constructor"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_METHOD, "method"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_SWITCH, "switch"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_WHILE, "while"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_DO, "do"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_FOR, "for"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_IF, "if"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_ELSE, "else"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_TRY, "try"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_CATCH, "catch"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_FINALLY, "finally"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_SYNC, "sync"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_ASSERT, "assert"));
        addContext(reservedContexts, new SimpleContext(CONTEXT_DEPRECATED, "@deprecated"));

        addContext(reservedMethodContexts, new MethodRegexpContext(CONTEXT_PRIVATE_METHOD, "private", Pattern.compile("(.* )?private .*")));
        addContext(reservedMethodContexts, new PropertyMethodRegexpContext(CONTEXT_PROPERTY_ACCESSOR, "property"));

        reservedNames.addAll(reservedContexts.keySet());
        reservedNames.addAll(reservedMethodContexts.keySet());
    }

    private int nextIndex = NEXT_INDEX;
    private Map<String, MethodRegexpContext> methodContexts = new LinkedHashMap<>();
    private Map<String, StatementRegexpContext> statementContexts = new LinkedHashMap<>();
    /**
     * A map to cache Strings keyed on ContextSets
     */
    private transient ConcurrentHashMap<com.atlassian.clover.api.registry.ContextSet, String> namedContextCache;

    public ContextStore() {
        methodContexts.putAll(reservedMethodContexts);
        initCache();
    }

    private ContextStore(int nextIndex, Map<String, MethodRegexpContext> methodContexts, Map<String, StatementRegexpContext> statementContexts) {
        this.nextIndex = nextIndex;
        this.methodContexts = methodContexts;
        this.statementContexts = statementContexts;
        initCache();
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initCache();
    }

    private void initCache() {
        namedContextCache = new ConcurrentHashMap<>();
    }

    public int addMethodContext(MethodRegexpContext ctx) throws CloverException {

        MethodRegexpContext context = new MethodRegexpContext(ctx);

        checkForReservedName(context);

        int index = NO_INDEX;

        // if there is an existing statement context by this name, remove it and reuse id
        index = removeExistingContext(context, statementContexts, index);
        index = removeExistingContext(context, methodContexts, index);

        if (index == NO_INDEX) {
            index = nextIndex++;
        }
        context.setIndex(index);
        logContext("adding", "method", context, index);
        addContext(methodContexts, context);
        return index;
    }

    private int removeExistingContext(NamedContext context, Map contexts, int index) {
        NamedContext existingContext = (NamedContext)contexts.get(context.getName());
        if (existingContext != null) {
            index = existingContext.getIndex();
            contexts.remove(context.getName());
            namedContextCache.clear();
        }
        return index;
    }

    public int addStatementContext(StatementRegexpContext ctx) throws CloverException {
        StatementRegexpContext context = new StatementRegexpContext(ctx);

        checkForReservedName(context);

        int index = NO_INDEX;

        // if there is an existing method context by this name, remove it and reuse id
        index = removeExistingContext(context, methodContexts, index);
        index = removeExistingContext(context, statementContexts, index);

        if (index == NO_INDEX) {
            index = nextIndex++;
        }
        context.setIndex(index);
        logContext("adding", "statement", context, index);
        addContext(statementContexts, context);
        return index;
    }

    private static void logContext(String verb, String type, RegexpContext context, int index) {
        if (Logger.isDebug()) {
            Logger.getInstance().debug(verb + " " + type + " context id=" + index + ", name=" + context.getName() + ", pattern=" + context.getPattern());
        }
    }


    public List<MethodRegexpContext> getMethodContexts() {
        return newArrayList(methodContexts.values());
    }

    public List<StatementRegexpContext> getStatementContexts() {
        return newArrayList(statementContexts.values());
    }

    public List<NamedContext> getReservedContexts() {
        return Lists.<NamedContext>newArrayList(reservedContexts.values());
    }

    public List<MethodRegexpContext> getReservedMethodContexts() {
        return newArrayList(reservedMethodContexts.values());
    }

    public List<NamedContext> getAllUserContexts() {
        List<NamedContext> contexts = Lists.<NamedContext>newArrayList(methodContexts.values());
        contexts.addAll(statementContexts.values());
        contexts.removeAll(reservedMethodContexts.values());
        return contexts;
    }

    public ContextSet createContextSetFilter(String spec) {
        return createContextSetFilter(spec, false);
    }
    /**
     * @return filter for the supplied spec (1 for filtered contexts, 0 for non-filtered), possibly inverted.
     * @param spec a comma or space separated list of context names. unrecognised names are ignored
     * @param invert whether to invert the mask
     */
    public ContextSet createContextSetFilter(String spec, boolean invert) {
        ContextSet result = new ContextSet(nextIndex);

        StringTokenizer toks = new StringTokenizer(spec, ", ");

        while (toks.hasMoreTokens()) {
            String filter  = toks.nextToken();
            NamedContext context = getContext(filter);
            if (context != null) {
                result = result.set(context.getIndex());
            }
            else {
                Logger.getInstance().warn("Ignoring unknown context filter \"" + filter + "\"");
            }
        }

        if (invert) {
            result = result.flip(0, nextIndex);
        }

        // always filter CLOVER_OFF
        return result.set(CONTEXT_CLOVER_OFF);
    }

    /**
     * @return context for name or null if not found
     * @param name to look for
     */
    public NamedContext getContext(String name) {
        NamedContext result = reservedContexts.get(name);
        if (result == null) {
            result = reservedMethodContexts.get(name);
        }
        if (result == null) {
            result = methodContexts.get(name);
        }
        if (result == null) {
            result = statementContexts.get(name);
        }

        return result;
    }

    public String getContextsAsString(com.atlassian.clover.api.registry.ContextSet set) {

        // cache previous named contexts, since this method is called for every filtered line of code.
        String ctxAsString = namedContextCache.get(set);
        if (ctxAsString != null) {
            return ctxAsString;
        }

        NamedContext[] contexts = getContexts(set);
        StringBuilder contextString = new StringBuilder();
        String sep = "";

        for (NamedContext context : contexts) {
            contextString.append(sep);
            contextString.append(context.getName());
            sep = ", ";
        }

        namedContextCache.putIfAbsent(set, contextString.toString());
        return contextString.toString();
    }



    /**
     * @return all named contexts that are set in ctxSet
     * @param ctxSet a bit set holding the index of the contexts to get
     */
    public NamedContext[] getContexts(com.atlassian.clover.api.registry.ContextSet ctxSet) {
        List<NamedContext> allContexts =  Lists.<NamedContext>newArrayList(reservedContexts.values());
        allContexts.addAll(reservedMethodContexts.values());
        allContexts.addAll(methodContexts.values());
        allContexts.addAll(statementContexts.values());

        List<NamedContext> contexts = newArrayList();

        for (int i = ctxSet.nextSetBit(0); i >= 0; i = ctxSet.nextSetBit(i + 1)) {
             collectContextAt(allContexts, i, contexts);
        }

        return contexts.toArray(new NamedContext[]{});
    }

    public static void saveCustomContexts(InstrumentationConfig config)
            throws CloverException {

        if (config.hasCustomContexts()) {
            ContextStore contexts = new ContextStore();

            if (config.getMethodContexts() != null) {
                for (MethodContextDef contextDef : config.getMethodContexts()) {
                    contextDef.validate();
                    try {
                        MethodRegexpContext context = new MethodRegexpContext(contextDef.getName(),
                                Pattern.compile(contextDef.getRegexp()),
                                contextDef.getMaxComplexity(),
                                contextDef.getMaxStatements(),
                                contextDef.getMaxAggregatedComplexity(),
                                contextDef.getMaxAggregatedStatements());
                        contexts.addMethodContext(context);
                    } catch (PatternSyntaxException e) {
                        throw new CloverException("Invalid context definition: " + e.getMessage(), e);
                    }
                }
            }
            if (config.getStatementContexts() != null) {
                for (StatementContextDef contextDef : config.getStatementContexts()) {
                    contextDef.validate();
                    try {
                        StatementRegexpContext context = new StatementRegexpContext(contextDef.getName(), Pattern.compile(contextDef.getRegexp()));
                        contexts.addStatementContext(context);
                    } catch (PatternSyntaxException e) {
                        throw new CloverException("Invalid context definition: " + e.getMessage(), e);
                    }
                }
            }

            Clover2Registry registry;
            try {
                registry = Clover2Registry.createOrLoad(config.getRegistryFile(), config.getProjectName());
            }
            catch (IOException e) {
                throw new CloverException(e.getClass().getName() + " accessing Clover database: " + e.getMessage(), e);
            }

            // todo - check for registry equivalence here. if not equiv, contexts will need to be deleted from elements in the registry
            registry.setContextStore(contexts);

            try {
                registry.saveAndOverwriteFile();
            }
            catch (IOException e) {
                throw new CloverException(e.getClass().getName() + " writing Clover database: " + e.getMessage());
            }
        }
    }

    public static class ContextMapper {
        private ContextStore contextStore;
        private Map<CloverDatabase, Map<Integer, Integer>> mappings;

        ContextMapper(ContextStore cs, Map<CloverDatabase, Map<Integer, Integer>> mappings) {
            this.contextStore = cs;
            this.mappings = mappings;
        }

        public void applyContextMapping(CloverDatabase db, FullFileInfo finfo) {
            final Map<Integer, Integer> mapping = mappings.get(db);
            if (mapping == null) {
                return;
            }
            finfo.visitElements(new FileElementVisitor() {
                @Override
                public void visitClass(ClassInfo info) {

                }

                @Override
                public void visitMethod(MethodInfo info) {
                    ((FullMethodInfo)info).setContext(ContextSet.remap((ContextSet)info.getContext(), mapping));
                }

                @Override
                public void visitStatement(StatementInfo info) {
                    ((FullStatementInfo)info).setContext(ContextSet.remap((ContextSet)info.getContext(), mapping));
                }

                @Override
                public void visitBranch(BranchInfo info) {
                    ((FullBranchInfo)info).setContext(ContextSet.remap((ContextSet)info.getContext(), mapping));
                }
            });

        }

        public ContextStore getContextStore() {
            return contextStore;
        }
    }

    public static ContextMapper mergeContextStores(Clover2Registry newReg, Collection<CloverDatabase> mergingDbs) {
        ContextStore merged = new ContextStore();

        // for a context to survive the merge, it must be present in all context stores.
        Map<CloverDatabase, Map<Integer, Integer>> oldMappings = newHashMap();
        Map<CloverDatabase, Map<Integer, Integer>> newMappings = newHashMap();

        // find the smallest context store and use this as the basis for the merge
        // the merged store will be a subset of the smallest store
        ContextStore smallest = null;
        for (CloverDatabase mergingDb : mergingDbs) {
            ContextStore store = mergingDb.getContextStore();
            if (smallest == null || store.size() < smallest.size()) {
                smallest = store;
            }
        }

        // for each context in the smallest registry, determine if the context is universal
        for (NamedContext namedContext : smallest.getAllUserContexts()) {
            RegexpContext context = (RegexpContext) namedContext;
            Integer contextIdx = context.getIndex();
            boolean universal = true;
            for (CloverDatabase db : mergingDbs) {
                ContextStore store = db.getContextStore();

                int equiv = store.getEquivalentContextIndex(context);
                if (equiv >= 0) {
                    // found an equivalent context in this store, record the mapping from
                    // smallest -> this reg
                    Map<Integer, Integer> oldMapping = oldMappings.get(db);
                    if (oldMapping == null) {
                        oldMapping = newHashMap();
                        oldMappings.put(db, oldMapping);
                    }
                    oldMapping.put(contextIdx, equiv);

                } else {
                    // this context is missing from atleast one store, so it isn't universal.
                    // skip to the next context
                    universal = false;
                    break;
                }
            }
            if (universal) {
                logContext("merging", context.getClass().getName(), context, context.getIndex());
                // this context (or its equiv) has been found in all mergingDbs) so construct local mappings
                int mergedIndex = merged.addContextFromTemplate(context);

                if (mergedIndex == ContextStore.NO_INDEX) {
                    Logger.getInstance().error("skipping problem user context " + context + ", " + context.getClass().getName());
                    continue;
                }

                for (CloverDatabase db : mergingDbs) {
                    Map<Integer, Integer> oldMapping = oldMappings.get(db);
                    Integer equiv = oldMapping.get(contextIdx);
                    Map<Integer, Integer> newMapping = newMappings.get(db);
                    if (newMapping == null) {
                        newMapping = newHashMap();
                        newMappings.put(db, newMapping);
                    }
                    newMapping.put(equiv, mergedIndex);
                }
            }
        }

         // add mappings for built-in contexts
        final List<NamedContext> allReservedContexts = merged.getReservedContexts();
        allReservedContexts.addAll(merged.getReservedMethodContexts());

        for (Map<Integer, Integer> mapping : newMappings.values()) {
            for (NamedContext context : allReservedContexts) {
                Integer contextIdx = context.getIndex();
                mapping.put(contextIdx, contextIdx);
            }
        }

        newReg.setContextStore(merged);
        return new ContextMapper(merged, newMappings);
    }

    private int addContextFromTemplate(RegexpContext context) {
        int newIndex = NO_INDEX;
        try {
            if (context instanceof MethodRegexpContext) {
                newIndex = addMethodContext((MethodRegexpContext)context);
            }
            else if (context instanceof StatementRegexpContext) {
                newIndex = addStatementContext((StatementRegexpContext)context);
            }
            else {
                Logger.getInstance().warn("Expecting a user defined context in merge, but got " + context.getClass().getName() + ", " + context);
            }
        }
        catch (CloverException e) {
            Logger.getInstance().error("when merging, encountered context with illegal name, skipping", e);
        }
        return newIndex;
    }

    private int getEquivalentContextIndex(RegexpContext context) {
        Collection<? extends RegexpContext> search = null;
        if (context instanceof MethodRegexpContext) {
             search = methodContexts.values();
        }
        else if (context instanceof StatementRegexpContext) {
             search = statementContexts.values();
        }

        if (search != null) {
            for (RegexpContext regexpContext : search) {
                if (regexpContext.isEquivalent(context)) {
                    return regexpContext.getIndex();
                }
            }
        }

        return -1;
    }

    /**
     * @return the size of this registry
     */
    public int size() {
        return reservedContexts.size() + methodContexts.size() + statementContexts.size();
    }

    private void collectContextAt(List<NamedContext> reservedList, int i, List<NamedContext>contexts) {
        for (NamedContext namedContext : reservedList) {
            if (namedContext.getIndex() == i) {
                contexts.add(namedContext);
                break;
            }
        }
    }

    private static <T extends NamedContext> void addContext(Map<String, T> map, T context) {
        map.put(context.getName(), context);
    }

    public static boolean isReservedName(String name) {
        return reservedNames.contains(name);
    }

    private void checkForReservedName(NamedContext context) throws CloverException {
        if (isReservedName(context.getName())) {
            throw new CloverException("The name \"" + context.getName() + "\" is already in use by one of the builtin contexts");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContextStore that = (ContextStore)o;

        if (nextIndex != that.nextIndex) return false;
        if (!Objects.equals(methodContexts, that.methodContexts))
            return false;
        return Objects.equals(statementContexts, that.statementContexts);
    }

    @Override
    public int hashCode() {
        int result = nextIndex;
        result = 31 * result + (methodContexts != null ? methodContexts.hashCode() : 0);
        result = 31 * result + (statementContexts != null ? statementContexts.hashCode() : 0);
        return result;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeInt(nextIndex);
        final Set<String> customNames = newHashSet(methodContexts.keySet());
        customNames.removeAll(reservedNames);

        out.writeInt(customNames.size());
        for (Map.Entry<String, MethodRegexpContext> entry : methodContexts.entrySet()) {
            if (!reservedNames.contains(entry.getKey())) {
                out.write(MethodRegexpContext.class, entry.getValue());
            }
        }
        
        out.writeInt(statementContexts.size());
        for (Map.Entry<String, StatementRegexpContext> entry : statementContexts.entrySet()) {
            out.write(StatementRegexpContext.class, entry.getValue());
        }
    }

    public static ContextStore read(TaggedDataInput in) throws IOException {
        final int nextIndex = in.readInt();

        final int numMethodContexts = in.readInt();
        final Map<String, MethodRegexpContext> methodContexts = newHashMap();
        for(int i = 0; i < numMethodContexts; i++) {
            final MethodRegexpContext ctx = in.read(MethodRegexpContext.class);
            methodContexts.put(ctx.getName(), ctx);
        }
        for (Map.Entry<String, MethodRegexpContext> entry : reservedMethodContexts.entrySet()) {
            methodContexts.put(entry.getKey(), entry.getValue());
        }

        final int numStmtContexts = in.readInt();
        final Map<String, StatementRegexpContext> stmtContexts = newHashMap();
        for(int i = 0; i < numStmtContexts; i++) {
            final StatementRegexpContext ctx = in.read(StatementRegexpContext.class);
            stmtContexts.put(ctx.getName(), ctx);
        }

        return new ContextStore(nextIndex, methodContexts, stmtContexts);
    }
}
