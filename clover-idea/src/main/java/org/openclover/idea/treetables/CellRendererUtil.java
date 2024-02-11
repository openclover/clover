package org.openclover.idea.treetables;

import org.openclover.core.api.registry.MethodInfo;
import org.openclover.idea.util.ui.CloverIcons;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.entities.TestCaseInfo;

import javax.swing.Icon;

/**
 *
 */
public class CellRendererUtil {
    /**
     * Returns an icon which best represents the object: test class, interface, annotation, enum or plain class.
     * @param classInfo
     * @return Icon icon representation
     */
    public static Icon getIconForClassInfo(BaseClassInfo classInfo) {
        if (classInfo.isTestClass()) {
            return CloverIcons.JUNIT_TEST_CLASS_ICON;
        } else if (classInfo.isInterface()) {
            return CloverIcons.INTERFACE_ICON;
        } else if (classInfo.isAnnotationType()) {
            return CloverIcons.ANNOTATION_TYPE_ICON;
        } else if (classInfo.isEnum()) {
            return CloverIcons.ENUM_ICON;
        } else {
            return CloverIcons.CLASS_ICON;
        }
    }

    /**
     * Returns an icon which best represents given method: test method or standard method.
     * @param methodInfo
     * @return Icon icon representation
     */
    public static Icon getIconForMethodInfo(MethodInfo methodInfo) {
        return methodInfo.isTest() ? CloverIcons.TEST_METHOD : CloverIcons.METHOD_ICON;
    }

    /**
     * Returns an icon representing test case result: error, failed, passed or configuration error.
     * @param testCaseInfo
     * @return Icon test result representation
     */
    public static Icon getIconForTestCaseInfo(TestCaseInfo testCaseInfo) {
        if (testCaseInfo.isError()) {
            return CloverIcons.TEST_ERROR;
        } else if (testCaseInfo.isFailure()) {
            return CloverIcons.TEST_FAILED;
        } else if (testCaseInfo.isSuccess()) {
            return CloverIcons.TEST_PASSED;
        } else {
            return CloverIcons.CONFIGURATION_ERROR;
        }
    }
}
