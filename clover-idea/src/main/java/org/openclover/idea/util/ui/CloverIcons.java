package org.openclover.idea.util.ui;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;

import javax.swing.Icon;

public class CloverIcons {
    private static final ClassLoader CL = CloverIcons.class.getClassLoader();

    private CloverIcons() {
    }

    private static Icon load(String path) {
        try {
            return IconLoader.getIcon(path, CL);
        } catch (Exception e) {
            return null;
        }
    }

    public static final Icon TOOL_WINDOW = load("/icons/clover_tny.png");
    public static final Icon CLOVER_BIG = load("/icons/clover_lge.png");
    public static final Icon CLOVER_LOGO = load("/icons/clover_logo.png");
    public static final Icon CLOVER_MDL = load("/icons/clover_mdl.gif");

    public static final Icon XML_FILETYPE = load("/icons/xml.png");
    public static final Icon HTML_FILETYPE = load("/icons/html.png");
    public static final Icon PDF_FILETYPE = load("/icons/pdf.png");

    public static final Icon GENERAL_ADD = load("/icons/general_add.png");

    public static final Icon COMPILE_ICON = load("/icons/compile_with_clover.png");
    public static final Icon VIEW_ICON = load("/icons/view_with_clover2.png");
    public static final Icon FILTER_ICON = load("/icons/ant_filter.png");

    public static final Icon TEST_MARK = load("/nodes/junitTestMark.png");
    public static final Icon TEST_METHOD = new LayeredIcon(2) {{
        setIcon(load("/nodes/method.png"), 0);
        setIcon(TEST_MARK, 1);
    }};
    public static final Icon TEST_PACKAGE = new LayeredIcon(2) {{
        setIcon(load("/icons/nodes_packageClosed.png"), 0);
        setIcon(TEST_MARK, 1);
    }};

    public static final Icon PACKAGE_CLOSED = load("/icons/nodes_packageClosed.png");
    public static final Icon PACKAGE_OPEN = load("/icons/nodes_packageOpen.png");

    public static final Icon TEST_ERROR = load("/runConfigurations/testError.png");
    public static final Icon TEST_FAILED = load("/runConfigurations/testFailed.png");
    public static final Icon TEST_PASSED = load("/runConfigurations/testPassed.png");

    public static final Icon CONFIGURATION_ERROR = load("/runConfigurations/configurationWarning.png");
    public static final Icon CONFIGURATION_INFO = load("/actions/intentionBulb.png");
    public static final Icon CONFIGURATION_INVALID = load("/runConfigurations/invalidConfigurationLayer.png");

    public static final Icon TEST_ROOT_FOLDER = load("/icons/modules_testRootClosed.png");
    public static final Icon TEST_ROOT_FOLDER_OPEN = load("/icons/modules_testRootOpened.png");

    public static final Icon SOURCE_ROOT_FOLDER = load("/icons/modules_sourceRootClosed.png");
    public static final Icon SOURCE_ROOT_FOLDER_OPEN = load("/icons/modules_sourceRootOpened.png");

    public static final Icon FOLDER = load("/icons/nodes_TreeClosed.png");

    public static final Icon IDEA_PROJECT = load("/icon_small.png");
    public static final Icon INTERFACE_ICON = load("/nodes/interface.png");
    public static final Icon CLASS_ICON = load("/nodes/class.png");
    public static final Icon ENUM_ICON = load("/nodes/enum.png");
    public static final Icon ANNOTATION_TYPE_ICON = load("/nodes/annotationtype.png");
    public static final Icon JUNIT_TEST_CLASS_ICON = new LayeredIcon(2) {{
        setIcon(load("/nodes/class.png"), 0);
        setIcon(TEST_MARK, 1);
    }};
    public static final Icon METHOD_ICON = load("/nodes/method.png");
    public static final Icon STATEMENT_ICON = load("/icons/statement.png");

    public static final Icon PROJECT = load("/nodes/project.png");
    public static final Icon HELP = load("/actions/help.png");
    public static final Icon ABOUT = load("/icons/about_clover.png");

    public static final Icon CLOVERIZED_RUN = load("/icons/run_optimized.png");

    public static final Icon CLOVER_ENABLED_OVL = load("/icons/clover_ovl_green.gif");
    public static final Icon CLOVER_DISABLED_OVL = load("/icons/clover_ovl_gray.gif");

    public static final Icon ALWAYS_LOAD_PER_TEST_COVERAGE = new LayeredIcon(2) {{
        setIcon(load("/runConfigurations/junit.png"), 0);
        setIcon(load("/runConfigurations/withCoverageLayer.png"), 1, 2, 5);
    }};

    public static final Icon LOAD_COVERAGE_DATA = new LayeredIcon(2) {{
        setIcon(load("/actions/sync.png"), 0);
        setIcon(load("/runConfigurations/withCoverageLayer.png"), 1, 2, 5);
    }};

    public static final Icon MODEL_SCOPE_ALL = load("/nodes/project.png");
    public static final Icon MODEL_SCOPE_APP = load("/nodes/sourceFolder.png");
    public static final Icon MODEL_SCOPE_TEST = load("/nodes/testSourceFolder.png");
}
