package org.openclover.eclipse.core.projects.settings;

import clover.com.google.common.collect.Sets;
import com.atlassian.clover.cfg.instr.InstrumentationConfig;
import com.atlassian.clover.cfg.instr.InstrumentationLevel;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.Logger;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.StatementRegexpContext;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.source.SourceFolderPattern;
import org.openclover.eclipse.core.settings.Settings;
import com.atlassian.clover.util.FilterUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ProjectSettings extends Settings {
    public static final String DEFAULT_INCLUDE_PATTERN = "**/*.java";
    public static final String DEFAULT_TEST_INCLUDE_PATTERN = "**/*Test.java,**/Test*.java";
    public static final String DEFAULT_EXCLUDE_PATTERN = "";

    public static class Keys {
        public static final String INSTRUMENTATION_ENABLED = "instrumentation_enabled";
        public static final String OUTPUT_ROOT_SAME_AS_PROJECT = "output_root_same_as_project";
        public static final String OUTPUT_ROOT = "output_root";
        public static final String INIT_STRING_DEFAULT = "init_string_default";
        public static final String INIT_STRING = "init_string";
        public static final String INIT_STRING_PROJECT_RELATIVE = "init_string_project_relative";
        public static final String FLUSH_POLICY = "flush_policy";
        public static final String FLUSH_INTERVAL = "flush_interval";
        public static final String INCLUDE_FILTER = "include_filter";
        public static final String EXCLUDE_FILTER = "exclude_filter";
        public static final String TEST_INCLUDE_FILTER = "test_include_filter";
        public static final String TEST_EXCLUDE_FILTER = "test_exclude_filter";
        public static final String BLOCK_FILTERS = "block_filters";
        public static final String REGEXP_FILTERS = "regexp_filters";
        public static final String SHOULD_QUALIFY_JAVA_LANG = "should_qualify_java_lang";
        public static final String USING_DEFAULT_TEST_DETECTION = "using_default_test_detection";
        public static final String TEST_SOURCE_FOLDERS = "test_source_folders";
        public static final String SELECTED_TEST_SOURCE_FOLDERS = "selected_test_source_folders";
        public static final String INSTRUMENT_SELECTED_SOURCE_FOLDERS = "instrument_selected_source_folders";
        public static final String INSTRUMENTED_FOLDER_PATTERNS = "instrumented_source_folder_patterns";
        public static final String INSTRUMENTATION_LEVEL = "instrumentation_level";
        public static final String INSTRUMENT_LAMBDA = "instrument_lambda";
        public static final String RECREATE_OUTPUT_DIRS = "recreate_output_dirs";
    }

    public static class Values {
        public static final String DIRECTED_FLUSH_POLICY = "directed";
        public static final String INTERVAL_FLUSH_POLICY = "interval";
        public static final String THREADED_FLUSH_POLICY = "threaded";
        public static final String AUTOMATIC_INIT_STRING_TYPE = "automatic";
        public static final String USE_SPECIFIED_INIT_STRING_TYPE = "userspecified";
        public static final int ALL_FOLDERS = 0;
        public static final int SELECTED_FOLDERS = 1;
        public static final int NO_TEST_FOLDERS = 2;
    }

    public static class Defaults {
        public static final int FLUSH_POLICY = InstrumentationConfig.DEFAULT_FLUSHING;
        public static final int WEBAPP_FLUSH_POLICY = InstrumentationConfig.THREADED_FLUSHING;
        public static final int FLUSH_INTERVAL = 1000;
        public static final String USER_OUTPUT_DIR = ".clover/bin";
        public static final boolean RECREATE_OUTPUT_DIRS = true;
        public static final String USER_INITSTRING = ".clover/coverage.db";
        public static final boolean OPEN_IN_BROWSER = true;
        public static final boolean SHOULD_QUALIFY_JAVA_LANG = true;
        public static final boolean USING_DEFAULT_TEST_DETECTION = true;
        public static final boolean OUTPUT_ROOT_SAME_AS_PROJECT = true;
        public static final int TEST_SOURCE_FOLDERS = 0;
        public static final InstrumentationLevel INSTRUMENTATION_LEVEL = InstrumentationLevel.STATEMENT;
        public static final LambdaInstrumentation INSTRUMENT_LAMBDA = LambdaInstrumentation.NONE;
    }

    private static final Set<String> CONTAINER_NATURES = Sets.newHashSet(
            "org.eclipse.wst.common.project.facet.core.nature"
    );
    
    private static final ListMarshaller<RegexpEntry> REGEXP_CONTEXT_MARSHALLER = new RegexpFilterMarshaller();
    private static final ListMarshaller<String> STRING_LIST_MARSHALLER = new StringListMarshaller();
    private static final ListMarshaller<SourceFolderPattern> FOLDER_LIST_MARSHALLER = new FolderListMarshaller();

    protected IProject project;
    private IScopeContext[] scopeContext;

    public ProjectSettings(IProject project) {
        this.project = project;
        this.isolatedPreferences = (IEclipsePreferences)Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE).node(project.getName()).node(CloverPlugin.ID);
    }

    @Override
    protected IScopeContext[] getScopeDelegation() {
        if (scopeContext == null) {
            scopeContext = new IScopeContext[] {new ProjectScope(project.getProject())};
        }
        return scopeContext;
    }

    public IProject getProject() {
        return project.getProject();
    }

    public boolean isInstrumentationEnabled() {
        return getBoolean(Keys.INSTRUMENTATION_ENABLED);
    }

    public void setInstrumentationEnabled(boolean enabled) {
        setValue(Keys.INSTRUMENTATION_ENABLED, enabled);
    }

    public void updateOutputRoot(boolean sameAsProject, String path, boolean recreateOutputDirs) {
        setValue(Keys.OUTPUT_ROOT_SAME_AS_PROJECT, sameAsProject);
        setValue(Keys.OUTPUT_ROOT, path);
        setValue(Keys.RECREATE_OUTPUT_DIRS, recreateOutputDirs);
    }

    public boolean isOutputRootSameAsProject() {
        return getBoolean(Keys.OUTPUT_ROOT_SAME_AS_PROJECT);
    }

    public String getOutputRoot() {
        return getString(Keys.OUTPUT_ROOT);
    }

    public boolean isRecreateOutputDirs() {
        return getBoolean(Keys.RECREATE_OUTPUT_DIRS);
    }

    public boolean isInitStringDefault() {
        return getBoolean(Keys.INIT_STRING_DEFAULT);
    }

    public String getInitString() {
        return getString(Keys.INIT_STRING);
    }

    public boolean isInitStringProjectRelative() {
        return getBoolean(Keys.INIT_STRING_PROJECT_RELATIVE);
    }

    public void updateInitString(boolean useDefault, String initString, boolean projectRelative) {
        setValue(Keys.INIT_STRING_DEFAULT, useDefault);
        setValue(Keys.INIT_STRING, initString);
        setValue(Keys.INIT_STRING_PROJECT_RELATIVE, projectRelative);
    }

    public int getFlushPolicy() {
        int policy = getInt(Keys.FLUSH_POLICY, -1);
        if (-1 == policy) {
            if (mightBeContainedProject(project)) {
                policy = Defaults.WEBAPP_FLUSH_POLICY;
            } else {
                policy = Defaults.FLUSH_POLICY;
            }
        }
        return policy;
    }

    public void updateFlushPolicy(int flushPolicy, int flushInterval) {
        setValue(Keys.FLUSH_POLICY, flushPolicy);
        setValue(Keys.FLUSH_INTERVAL, flushInterval);
    }

    public int getFlushInterval() {
        return getInt(Keys.FLUSH_INTERVAL);
    }

    private boolean mightBeContainedProject(IProject project) {
        if (isContainerProject(project)) {
            return true;
        } else {
            //If a contained project references this one, this may
            //be a library used by a web app, EJB, etc
            IProject[] referencingProjects = project.getReferencingProjects();

            //Only do a single level of reference checking - unexplained StackOverflowError if recursively checked (CEP-329)
            for (IProject referencingProject : referencingProjects) {
                if (isContainerProject(referencingProject)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isContainerProject(IProject project) {
        for(String nature : CONTAINER_NATURES) {
            try {
                if (project.hasNature(nature)) {
                    return true;
                }
            } catch (CoreException e) {
                Logger.getInstance().error(
                    String.format("Failed to query project \"%s\" for nature \"%s\"", project, nature), e);
            }
        }
        return false;
    }

    public void setFlushInterval(int interval) {
        setValue(Keys.FLUSH_INTERVAL, interval);
    }

    public Interval calcEffectiveSpanInterval(CloverDatabase database) {
        return new Interval(
            BigDecimal.valueOf(calcEffectiveSpanMS(database) / 1000),
            Interval.UNIT_SECOND);
    }

    public long calcEffectiveSpanMS(CloverDatabase database) {
        final long userSpan = CloverPlugin.getInstance().getInstallationSettings().getCoverageSpan().getValueInMillis();

        if (CloverPlugin.getInstance().getInstallationSettings().isAggregatingCoverage()) {
            final long now = System.currentTimeMillis();

            if (database != null) {
                return (database.getRegistry().getVersion() - database.getRegistry().getFirstVersion()) + userSpan;
            } else {
                final long lastClean = CloverProject.getLastCleanBuildStamp(project);
                return
                    (lastClean != 0l
                        ? System.currentTimeMillis() - lastClean
                        : 0l) + userSpan;
            }
        } else {
            return userSpan;
        }
    }

    public String getIncludeFilter() {
        return getString(Keys.INCLUDE_FILTER);
    }

    public String getExcludeFilter() {
        return getString(Keys.EXCLUDE_FILTER);
    }

    /**
     * May return empty Strings in the filter
     * @return tokenized include filter
     */
    public String[] calculateIncludeFilter() {
        final String filter = getIncludeFilter();
        return FilterUtils.tokenizePattern(filter != null ? filter : DEFAULT_INCLUDE_PATTERN);
    }

    /**
     * May return empty Strings in the filter
     * @return tokenized exclude filter
     */
    public String[] calculateExcludeFilter() {
        final String filter = getExcludeFilter();
        return FilterUtils.tokenizePattern(filter != null ? filter : DEFAULT_EXCLUDE_PATTERN);
    }

    public ContextSet getContextFilter() {
        String blockFilterString = getString(Keys.BLOCK_FILTERS);
        return
            blockFilterString == null
                ? new ContextSet()
                : getContextRegistry().createContextSetFilter(blockFilterString, false);
    }

    public void setContextFilter(ContextSet blockFilter) {
        setValue(
            Keys.BLOCK_FILTERS,
            getContextRegistry().getContextsAsString(blockFilter));
    }

    public ContextStore getContextRegistry() {
        final ContextStore reg = new ContextStore();
        final List<RegexpEntry> filters = getRegexpFilters();
        for (RegexpEntry entry : filters) {
            try {
                if (entry.getType() == RegexpEntry.METHOD_TYPE) {
                    reg.addMethodContext(new MethodRegexpContext(entry.getName(), Pattern.compile(entry.getRegexp())));
                } else {
                    reg.addStatementContext(new StatementRegexpContext(entry.getName(), Pattern.compile(entry.getRegexp())));
                }
            } catch (Exception e) {
                CloverPlugin.logError("Unable to load regex entry " + entry.getName(), e);
            }
        }
        return reg;
    }

    public void setIncludeFilter(String filter) {
        setValue(Keys.INCLUDE_FILTER, filter);
    }

    public void setExcludeFilter(String filter) {
        setValue(Keys.EXCLUDE_FILTER, filter);
    }

    public boolean isInstrumentSelectedSourceFolders() {
        return getBoolean(Keys.INSTRUMENT_SELECTED_SOURCE_FOLDERS);
    }

    public void setInstrumentSelectedSourceFolders(boolean instrumentSelectedOnly) {
        setValue(Keys.INSTRUMENT_SELECTED_SOURCE_FOLDERS, instrumentSelectedOnly);
    }

    public List<SourceFolderPattern> getInstrumentedFolderPatterns() {
        return getListProperty(Keys.INSTRUMENTED_FOLDER_PATTERNS, FOLDER_LIST_MARSHALLER);
    }

    public void setInstrumentedFolderPatterns(List<SourceFolderPattern> folderPatterns) {
        setListProperty(Keys.INSTRUMENTED_FOLDER_PATTERNS, folderPatterns, FOLDER_LIST_MARSHALLER);
    }

    public List<RegexpEntry> getRegexpFilters() {
        return getListProperty(Keys.REGEXP_FILTERS, REGEXP_CONTEXT_MARSHALLER);
    }

    public void setRegexpFilters(List<RegexpEntry> filters) {
        setListProperty(Keys.REGEXP_FILTERS, filters, REGEXP_CONTEXT_MARSHALLER);
    }

    public boolean shouldQualifyJavaLang() {
        return getBoolean(Keys.SHOULD_QUALIFY_JAVA_LANG);
    }

    public void shouldQualifyJavaLang(boolean shouldQualify) {
        setValue(Keys.SHOULD_QUALIFY_JAVA_LANG, shouldQualify);
    }
    
    public boolean isUsingDefaultTestDetection() {
        return getBoolean(Keys.USING_DEFAULT_TEST_DETECTION);
    }

    public void setIsUsingDefaultTestDetection(boolean using) {
        setValue(Keys.USING_DEFAULT_TEST_DETECTION, using);
    }

    public int getTestSourceFolders() {
        return getInt(Keys.TEST_SOURCE_FOLDERS, ProjectSettings.Defaults.TEST_SOURCE_FOLDERS);
    }

    public void setTestSourceFolders(int testFolders) {
        if (testFolders < 0 || testFolders > 2) {
            throw new IllegalArgumentException("Invalid Test Source Folder option: " + testFolders);
        } else {
            setValue(Keys.TEST_SOURCE_FOLDERS, testFolders);
        }
    }

    public void setTestIncludeFilter(String filter) {
        setValue(Keys.TEST_INCLUDE_FILTER, filter);
    }

    public void setTestExcludeFilter(String filter) {
        setValue(Keys.TEST_EXCLUDE_FILTER, filter);
    }

    public String getTestIncludeFilter() {
        return getString(Keys.TEST_INCLUDE_FILTER);
    }

    public String getTestExcludeFilter() {
        return getString(Keys.TEST_EXCLUDE_FILTER);
    }

    /**
     * May return empty Strings in the filter
     * @return tokenized test include filter
     */
    public String[] calculateTestIncludeFilter() {
        final String filter = getTestIncludeFilter();
        return FilterUtils.tokenizePattern(filter != null ? filter : DEFAULT_TEST_INCLUDE_PATTERN);
    }

    /**
     * May return empty Strings in the filter
     * @return tokenized test exclude filter
     */
    public String[] calculateTestExcludeFilter() {
        final String filter = getTestExcludeFilter();
        return FilterUtils.tokenizePattern(filter != null ? filter : DEFAULT_EXCLUDE_PATTERN);
    }

    public List<String> getSelectedTestFolders() {
        return getListProperty(Keys.SELECTED_TEST_SOURCE_FOLDERS, STRING_LIST_MARSHALLER);
    }

    public void setSelectedTestFolders(List<String> folders) {
        setListProperty(Keys.SELECTED_TEST_SOURCE_FOLDERS, folders, STRING_LIST_MARSHALLER);
    }

    public void setInstrumentationLevel(InstrumentationLevel level) {
        setValue(Keys.INSTRUMENTATION_LEVEL, level.name());
    }

    public InstrumentationLevel getInstrumentationLevel() {
        final String levelAsString = getString(Keys.INSTRUMENTATION_LEVEL);
        return levelAsString == null ? Defaults.INSTRUMENTATION_LEVEL : InstrumentationLevel.valueOf(levelAsString);
    }

    public LambdaInstrumentation getInstrumentLambda() {
        final String lambdaAsString = getString(Keys.INSTRUMENT_LAMBDA);
        return lambdaAsString == null ? Defaults.INSTRUMENT_LAMBDA : LambdaInstrumentation.valueOf(lambdaAsString);
    }

    public void setInstrumentLambda(LambdaInstrumentation level) {
        setValue(Keys.INSTRUMENT_LAMBDA, level.name());
    }

    public void initialise() {
    }

    public void upgrade() {
        String[] keys = new String[] {
            Keys.INSTRUMENTATION_ENABLED,
            Keys.OUTPUT_ROOT_SAME_AS_PROJECT,
            Keys.OUTPUT_ROOT,
            Keys.INIT_STRING_DEFAULT,
            Keys.INIT_STRING,
            Keys.INIT_STRING_PROJECT_RELATIVE,
            Keys.FLUSH_POLICY,
            Keys.FLUSH_INTERVAL,
            Keys.INCLUDE_FILTER,
            Keys.EXCLUDE_FILTER,
            Keys.BLOCK_FILTERS,
            Keys.SHOULD_QUALIFY_JAVA_LANG,
            Keys.USING_DEFAULT_TEST_DETECTION,
            Keys.TEST_SOURCE_FOLDERS,
            Keys.SELECTED_TEST_SOURCE_FOLDERS
        };
        for (String key : keys) {
            try {
                QualifiedName qualifiedName = new QualifiedName(CloverPlugin.ID, key);
                String value = project.getProject().getPersistentProperty(qualifiedName);
                if (value != null) {
                    setValue(key, value);
                    project.getProject().setPersistentProperty(qualifiedName, null);
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to migrate project setting " + key, e);
            }
        }

        int i = 0;
        String value = null;
        do {
            String indexedKey = Keys.REGEXP_FILTERS + "[" + i + "]";
            QualifiedName qualifiedName = new QualifiedName(CloverPlugin.ID, indexedKey);
            try {
                value = project.getPersistentProperty(qualifiedName);
                if (value != null) {
                    setValue(indexedKey, value);
                    project.getProject().setPersistentProperty(qualifiedName, null);
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to migrate project setting " + indexedKey, e);
            }
        } while (value != null);

        save();
    }

    protected boolean isPropertySet(String key) {
        return getString(key) != null;
    }

    private <T> void setListProperty(String key, List<T> list, ListMarshaller<T> marshaller) {
        // need to convert each element of the list into a string, and
        // store them in indexed names. Note, there is a limit of 2K of data
        // for each qualified name. Therefore use separate name for each
        // data element.

        for (int i = 0; ; i++) {
            String indexedKey = key + "[" + i + "]";

            String data = getString(indexedKey);
            if (data == null) { // no data means end of list.
                break;
            }
            setValue(indexedKey, null);
        }
        for (int i = 0; ; i++) {
            String indexedKey = key + "[" + i + "]";
            if (i == list.size()) {
                break;
            }

            String str = marshaller.marshall(list.get(i));
            CloverPlugin.logVerbose("Storing project property data:'" + str + "'", null);
            setValue(indexedKey, str);
        }
    }


    private <T> List<T> getListProperty(String key, ListMarshaller<T> marshaller) {
        // read all of the properties.
        List<T> property = new ArrayList<>();
        for (int i = 0; ; i++) {
            String indexedKey = key + "[" + i + "]";

            String data = getString(indexedKey);
            if (data == null || data.equals("")) { // no data means end of list.
                break;
            }

            final T ctx = marshaller.unmarshall(data);

            CloverPlugin.logDebug("converted '" + data + "' to " + ctx, null);
            if (ctx != null) {
                property.add(ctx);
            }
        }

        return property;
    }

    public interface ListMarshaller<T> {
        T unmarshall(String data);
        String marshall(T object);
    }

    private static class RegexpFilterMarshaller implements ListMarshaller<RegexpEntry> {
        @Override
        public RegexpEntry unmarshall(String data) {
            return new RegexpEntry(data);
        }

        @Override
        public String marshall(RegexpEntry object) {
            return object.toString();
        }
    }

    private static class StringListMarshaller implements ListMarshaller<String> {
        @Override
        public String unmarshall(String data) {
            return data;
        }

        @Override
        public String marshall(String object) {
            return object;
        }
    }

    private static class FolderListMarshaller implements ListMarshaller<SourceFolderPattern> {
        @Override
        public SourceFolderPattern unmarshall(String data) {
            final String[] strings = data.split("\n");
            return new SourceFolderPattern(
                    strings.length > 0 ? strings[0] : "",
                    strings.length > 1 ? strings[1] : DEFAULT_INCLUDE_PATTERN,
                    strings.length > 2 ? strings[2] : DEFAULT_EXCLUDE_PATTERN,
                    strings.length > 3 && "enabled".equals(strings[3]));
        }

        @Override
        public String marshall(SourceFolderPattern sfp) {
            return (sfp == null) ? "" : sfp.getSrcPath() + '\n'
                    + sfp.getIncludePattern() + '\n'
                    + sfp.getExcludePattern() + '\n'
                    + (sfp.isEnabled() ? "enabled" : "disabled");
        }
    }
}