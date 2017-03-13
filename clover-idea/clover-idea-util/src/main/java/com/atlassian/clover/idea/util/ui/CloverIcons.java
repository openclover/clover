package com.atlassian.clover.idea.util.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class CloverIcons {
    private CloverIcons() {
    }

    public static final Icon TOOL_WINDOW = new ImageIcon(CloverIcons.class.getResource("/icons/clover_tny.png"));
    public static final Icon CLOVER_BIG = new ImageIcon(CloverIcons.class.getResource("/icons/clover_lge.png"));
    public static final Icon CLOVER_LOGO = new ImageIcon(CloverIcons.class.getResource("/icons/clover_logo.png"));
    public static final Icon CLOVER_MDL = new ImageIcon(CloverIcons.class.getResource("/icons/clover_mdl.gif"));

    public static final Icon XML_FILETYPE = new ImageIcon(Configurable.class.getResource("/fileTypes/xml.png"));
    public static final Icon HTML_FILETYPE = new ImageIcon(Configurable.class.getResource("/fileTypes/html.png"));
    public static final Icon PDF_FILETYPE = new ImageIcon(CloverIcons.class.getResource("/icons/pdf.png"));

    public static final Icon GENERAL_ADD = IconLoader.getIcon("/icons/general_add.png");

    public static final Icon COMPILE_ICON = IconLoader.getIcon("/icons/compile_with_clover.png");
    public static final Icon VIEW_ICON = IconLoader.getIcon("/icons/view_with_clover2.png");
    public static final Icon FILTER_ICON = IconLoader.getIcon("/icons/ant_filter.png");

    public static final Icon TEST_MARK = IconLoader.getIcon("/nodes/junitTestMark.png");
    public static final Icon TEST_METHOD = new LayeredIcon(2) {{
        setIcon(IconLoader.getIcon("/nodes/method.png"), 0);
        setIcon(TEST_MARK, 1);
    }};
    public static final Icon TEST_PACKAGE = new LayeredIcon(2) {{
        setIcon(IconLoader.getIcon("/icons/nodes_packageClosed.png"), 0);
        setIcon(TEST_MARK, 1);
    }};

    public static final Icon PACKAGE_CLOSED = IconLoader.getIcon("/icons/nodes_packageClosed.png");
    public static final Icon PACKAGE_OPEN = IconLoader.getIcon("/icons/nodes_packageOpen.png");

    public static final Icon TEST_ERROR = IconLoader.getIcon("/runConfigurations/testError.png");
    public static final Icon TEST_FAILED = IconLoader.getIcon("/runConfigurations/testFailed.png");
    public static final Icon TEST_PASSED = IconLoader.getIcon("/runConfigurations/testPassed.png");

    public static final Icon CONFIGURATION_ERROR = IconLoader.getIcon("/runConfigurations/configurationWarning.png");
    public static final Icon CONFIGURATION_INFO = IconLoader.getIcon("/actions/intentionBulb.png");
    public static final Icon CONFIGURATION_INVALID = IconLoader.getIcon("/runConfigurations/invalidConfigurationLayer.png");

    public static final Icon TEST_ROOT_FOLDER = IconLoader.getIcon("/icons/modules_testRootClosed.png");
    public static final Icon TEST_ROOT_FOLDER_OPEN = IconLoader.getIcon("/icons/modules_testRootOpened.png");

    public static final Icon SOURCE_ROOT_FOLDER = IconLoader.getIcon("/icons/modules_sourceRootClosed.png");
    public static final Icon SOURCE_ROOT_FOLDER_OPEN = IconLoader.getIcon("/icons/modules_sourceRootOpened.png");

    public static final Icon FOLDER = IconLoader.getIcon("/icons/nodes_TreeClosed.png");

    public static final Icon IDEA_PROJECT = IconLoader.getIcon("/icon_small.png");
    public static final Icon INTERFACE_ICON = IconLoader.getIcon("/nodes/interface.png");
    public static final Icon CLASS_ICON = IconLoader.getIcon("/nodes/class.png");
    public static final Icon ENUM_ICON = IconLoader.getIcon("/nodes/enum.png");
    public static final Icon ANNOTATION_TYPE_ICON = IconLoader.getIcon("/nodes/annotationtype.png");
    public static final Icon JUNIT_TEST_CLASS_ICON = new LayeredIcon(2) {{
        setIcon(IconLoader.getIcon("/nodes/class.png"), 0);
        setIcon(TEST_MARK, 1);
    }};
    public static final Icon METHOD_ICON = IconLoader.getIcon("/nodes/method.png");
    public static final Icon STATEMENT_ICON = IconLoader.getIcon("/icons/statement.png");

    public static final Icon PROJECT = IconLoader.getIcon("/nodes/project.png");
    public static final Icon HELP = IconLoader.getIcon("/actions/help.png");
    public static final Icon ABOUT = IconLoader.getIcon("/icons/about_clover.png");
    public static final Icon ABOUT_EXPIRED = IconLoader.getIcon("/icons/about_clover_expired.png");
    public static final Icon ABOUT_MAINT_EXPIRED = IconLoader.getIcon("/icons/about_clover_maint_expired.png");

    public static final Icon CLOVERIZED_RUN = IconLoader.getIcon("/icons/run_optimized.png");

    public static final Icon CLOVER_ENABLED_OVL = IconLoader.getIcon("/icons/clover_ovl_green.gif");
    public static final Icon CLOVER_DISABLED_OVL = IconLoader.getIcon("/icons/clover_ovl_gray.gif");

    public static final Icon ALWAYS_LOAD_PER_TEST_COVERAGE = new LayeredIcon(2) {{
        setIcon(IconLoader.getIcon("/runConfigurations/junit.png"), 0);
        setIcon(IconLoader.getIcon("/runConfigurations/withCoverageLayer.png"), 1, 2, 5);
    }};

    public static final Icon LOAD_COVERAGE_DATA = new LayeredIcon(2) {{
        setIcon(IconLoader.getIcon("/actions/sync.png"), 0);
        setIcon(IconLoader.getIcon("/runConfigurations/withCoverageLayer.png"), 1, 2, 5);
    }};

    public static final Icon MODEL_SCOPE_ALL = IconLoader.getIcon("/nodes/project.png");
    public static final Icon MODEL_SCOPE_APP = IconLoader.getIcon("/nodes/sourceFolder.png");
    public static final Icon MODEL_SCOPE_TEST = IconLoader.getIcon("/nodes/testSourceFolder.png");
}
