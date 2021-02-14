package org.openclover.eclipse.core.views.testcontributions;

import com.atlassian.clover.api.registry.ClassInfo;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.views.nodes.Nodes;

import java.text.DateFormat;
import java.util.Date;

public class TestCaseInfoLabelProvider extends LabelProvider implements ITableLabelProvider {
    @Override
    public Image getColumnImage(Object value, int column) {
        if (column == 0) {
            if (value instanceof ClassInfo) {
                return CloverPlugin.getImage(CloverPluginIcons.TEST_CLASS_ICON);
            } else if (value instanceof TestCaseInfo) {
                return Nodes.iconFor((TestCaseInfo)value);
            }
        }
        return getImage(value);
    }

    @Override
    public String getColumnText(Object value, int column) {
        if (column == 0) {
            if (value instanceof TestCaseInfo) {
                TestCaseInfo testCase = (TestCaseInfo) value;
                return
                    testCase.getTestName()
                    + "() - "
                    + DateFormat.getDateTimeInstance(
                        DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(testCase.getStartTime()));
            } else if (value instanceof ClassInfo) {
                ClassInfo classInfo = (ClassInfo)value;
                return classInfo.getName() + " - " + classInfo.getPackage().getName();
            } else {
                return "";
            }
        } else {
            return getText(value);
        }
    }
}
