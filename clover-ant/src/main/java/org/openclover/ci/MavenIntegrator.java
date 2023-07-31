package org.openclover.ci;

import clover.com.google.common.collect.Iterables;
import com.atlassian.clover.api.ci.CIOptions;
import com.atlassian.clover.api.ci.Integrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Sets.newHashSet;

/**
 * A utility class that contains methods to assist in integrating clover into a CI server for Maven2 builds.
 */
public class MavenIntegrator implements Integrator {

    static final String GROUP_ID = "org.openclover";
    static final String ARTIFACT_ID = "clover-maven-plugin";
    static final String PREFIX = String.format("%s::%s:", GROUP_ID, ARTIFACT_ID);

    /**
     * List of goals or phases before which the 'clover:setup' shall be NOT be inserted. Some plug-ins may re-create
     * source roots (as a result of source generation, for instance) and we must ensure that Clover's source folders
     * will be preserved.
     */
    public static final Set<String> CLOVER_SETUP_NOT_BEFORE = newHashSet("clean", "jaxb2:generate", "wsdl2java");

    /**
     * List of goals or phases before which the 'clover:optimize' shall be inserted. Optimization goal must be called
     * before a test phase.
     */
    public static final Set<String> CLOVER_OPTIMIZE_BEFORE = newHashSet(
            "test", "prepare-package", "package",
            "pre-integration-test", "integration-test", "post-integration-test",
            "verify", "install", "deploy"
    );

    /**
     * List of goals or phases which lack of should trigger adding the "verify" phase. We expect that if Clover
     * integration is enabled, then something must be compiled at least (not necessarily tested as a build may be
     * configured in such way that files are instrumented and compiled by Clover and next used in another build).
     * Thus, lack of the "compile" Maven phase (or any later one from Maven's build life cycle) will trigger adding
     * the "verify" phase.
     */
    public static final Set<String> ADD_VERIFY_IF_NONE_PRESENT = newHashSet(
            "compile",
            "test", "prepare-package", "package",
            "pre-integration-test", "integration-test", "post-integration-test",
            "verify", "install", "deploy"
    );

    private static final String CLOVER_LICENSE = "maven.clover.license";
    private static final String CLOVER_LICENSE_LOCATION = "maven.clover.licenseLocation";
    private static final String CLOVER_GENERATE_XML = "maven.clover.generateXml";
    private static final String CLOVER_GENERATE_PDF = "maven.clover.generatePdf";
    private static final String CLOVER_GENERATE_JSON = "maven.clover.generateJson";
    private static final String CLOVER_GENERATE_HTML = "maven.clover.generateHtml";
    private static final String CLOVER_GENERATE_HISTORICAL = "maven.clover.generateHistorical";

    private final CIOptions options;

    public MavenIntegrator(CIOptions options) {
        this.options = options;
    }

    /**
     *
     * @param args a modifiable List of arguments to decorate. This list must be arguments to the build tool only, and may
     * not include the actual command that invokes the build too. e.g. {"clean", "test"} and NOT {"mvn", "clean", "test"}
     */
    @Override
    public void decorateArguments(final List<String> args) {
        final ArrayList<String> newArgs = newArrayList(args);

        // add "clean" at the very beginning
        if (options.isFullClean() && !newArgs.contains("clean")) {
            newArgs.add(0, "clean");
        }

        // insert "clover:setup" after all of CLOVER_SETUP_NOT_BEFORE
        insertAfterAllOf(newArgs, CLOVER_SETUP_NOT_BEFORE, PREFIX + "setup", true);

        // insert "clover:optimize" before test phase, don't add optimize goal if no test phase is found
        if (options.isOptimize()) {
            insertBeforeAnyOf(newArgs, CLOVER_OPTIMIZE_BEFORE, PREFIX + "optimize", false);
        }

        if (needsVerify(newArgs)) {
            newArgs.add("verify");
        }

        // insert "clover:snapshot" after other goals
        if (options.isOptimize()) {
            newArgs.add(PREFIX + "snapshot");
        }

        // insert "clover:aggregate" and "clover:clover" at the end
        newArgs.add(PREFIX + "aggregate");
        newArgs.add(PREFIX + "clover");

        //additional build properties
        addHistoricalReportProperties(newArgs);
        addReportFormatsProperties(newArgs);
        addLicenseProperties(newArgs);

        // copy to the output argument
        args.clear();
        args.addAll(newArgs);
    }

    private boolean needsVerify(ArrayList<String> newArgs) {
        // insert "verify" phase if there's no compilation
        boolean needsVerify = true;
        for (String phase : ADD_VERIFY_IF_NONE_PRESENT) {
            if (newArgs.contains(phase)) {
                needsVerify = false;
                break;
            }
        }
        return needsVerify;
    }

    private void addHistoricalReportProperties(ArrayList<String> newArgs) {
        // generateHistorical is false by default
        if (options.isHistorical() && !containsArg(newArgs, CLOVER_GENERATE_HISTORICAL)) {
            final String histDir = options.getHistoryDir() != null ?
                    options.getHistoryDir().getAbsolutePath() : ".cloverhistory";
            addProperty(newArgs, CLOVER_GENERATE_HISTORICAL, true);
            addProperty(newArgs, "maven.clover.historyDir", histDir);
            newArgs.add(PREFIX + "save-history");
        }
    }

    private void addReportFormatsProperties(List<String> args) {
        // json, pdf, html, xml report formats
        if (!containsArg(args, CLOVER_GENERATE_HTML)) {
            addProperty(args, CLOVER_GENERATE_HTML, options.isHtml());
        }
        if (!containsArg(args, CLOVER_GENERATE_JSON)) {
            addProperty(args, CLOVER_GENERATE_JSON, options.isJson());
        }
        if (!containsArg(args, CLOVER_GENERATE_PDF)) {
            addProperty(args, CLOVER_GENERATE_PDF, options.isPdf());
        }
        if (!containsArg(args, CLOVER_GENERATE_XML)) {
            addProperty(args, CLOVER_GENERATE_XML, options.isXml());
        }
    }

    private void addLicenseProperties(List<String> args) {
        // license key / file if defined
        if (!containsArg(args, CLOVER_LICENSE) && options.getLicenseCert() != null && !options.getLicenseCert().trim().equals("")) {
            addProperty(args, CLOVER_LICENSE, options.getLicenseCert());
        }
        if (!containsArg(args, CLOVER_LICENSE_LOCATION) && options.getLicense() != null) {
            addProperty(args, CLOVER_LICENSE_LOCATION, options.getLicense().getAbsolutePath());
        }
    }

    private void addProperty(List<String> args, String name, String value) {
        args.add(String.format("-D%s=%s", name, value));
    }

    private void addProperty(List<String> args, String name, boolean value) {
        args.add(String.format("-D%s=%b", name, value));
    }

    private boolean containsArg(List<String> args, String property) {
        return Iterables.tryFind(args, new HasPropertyPredicate(property)).isPresent();
    }

    /**
     * Puts <code>element</code> element into the <code>list</code> at the first position after all occurrences of
     * <code>afterAllOf</code>. If none of them was found, inserts as a first list element.
     */
    static <T> void insertAfterAllOf(ArrayList<T> list, Set<T> afterAllOf, T element, boolean putIfNotFound) {
        // search list backwards and stop on a first occurrence of any element from afterAllOf
        for (int i = list.size() - 1; i >= 0; i--) {
            T goal = list.get(i);
            if (afterAllOf.contains(goal)) {
                // add on the right of afterAllOf
                list.add(i + 1, element);
                break;
            } else if (i == 0 && putIfNotFound) {
                // or add as the first one
                list.add(0, element);
            }
        }
    }

    static <T> void insertBeforeAnyOf(ArrayList<T> list, Set<T> beforeAnyOf, T element, boolean putIfNotFound) {
        // search list forwards and stop on a first occurrence of any element from beforeAnyOf
        for (int i = 0; i < list.size(); i++) {
            T goal = list.get(i);
            if (beforeAnyOf.contains(goal)) {
                // add on the left of beforeAnyOf
                list.add(i, element);
                break;
            } else if (i == list.size() - 1 && putIfNotFound) {
                // or add as the last one
                list.add(i + 1, element);
                break;
            }
        }
    }

}
