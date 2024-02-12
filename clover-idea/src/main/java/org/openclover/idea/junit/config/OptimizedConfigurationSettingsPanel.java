package org.openclover.idea.junit.config;

import com.intellij.ui.components.panels.HorizontalBox;
import com.intellij.ui.components.panels.VerticalBox;
import org.openclover.core.api.optimization.OptimizationOptions;
import org.openclover.idea.config.GBC;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.GridBagLayout;

public class OptimizedConfigurationSettingsPanel extends JPanel {
    private final JCheckBox discardSnapshot = new JCheckBox("Discard snapshot every ");
    private final JFormattedTextField discardInterval = new JFormattedTextField();
    private final JCheckBox minimize = new JCheckBox("Minimize tests");

    private final JRadioButton noReorder = new JRadioButton("Do not reorder");
    private final JRadioButton failfast = new JRadioButton("Failing tests first");
    private final JRadioButton random = new JRadioButton("Random order");

    public OptimizedConfigurationSettingsPanel() {
        setLayout(new GridBagLayout());

        JPanel discardPanel = new HorizontalBox();
        discardPanel.add(discardSnapshot);
        discardPanel.add(discardInterval);
        discardPanel.add(new JLabel("compiles"));
        add(discardPanel, new GBC(0, 0).setWeight(1, 0).setFill(GBC.BOTH));

        add(minimize,  new GBC(0, GBC.RELATIVE).setWeight(1, 0).setFill(GBC.BOTH));

        VerticalBox reorderPanel = new VerticalBox();
        reorderPanel.setBorder(BorderFactory.createTitledBorder("Test reordering"));
        reorderPanel.add(noReorder);
        reorderPanel.add(failfast);
        reorderPanel.add(random);

        ButtonGroup bg = new ButtonGroup();
        bg.add(noReorder);
        bg.add(failfast);
        bg.add(random);

        add(reorderPanel,  new GBC(0, GBC.RELATIVE).setWeight(1, 0).setFill(GBC.BOTH));

        add(new JPanel(),  new GBC(0, GBC.RELATIVE).setWeight(1, 1));

        discardSnapshot.addActionListener(actionEvent -> discardInterval.setEnabled(discardSnapshot.isSelected()));
        discardInterval.setValue(0);
    }

    public void resetEditorFrom(OptimizedConfigurationSettings settings) {
        discardSnapshot.setSelected(settings.isDiscardSnapshots());
        discardInterval.setValue(settings.getCompilesBeforeStaleSnapshot());
        discardInterval.setEnabled(settings.isDiscardSnapshots());

        minimize.setSelected(settings.isMinimize());

        final OptimizationOptions.TestSortOrder reordering = settings.getReorder();
        noReorder.setSelected(OptimizationOptions.TestSortOrder.NONE == reordering);
        failfast.setSelected(OptimizationOptions.TestSortOrder.FAILFAST == reordering);
        random.setSelected(OptimizationOptions.TestSortOrder.RANDOM == reordering);
    }

    public void applyEditorTo(OptimizedConfigurationSettings settings) {
        settings.setDiscardSnapshots(discardSnapshot.isSelected());
        settings.setMinimize(minimize.isSelected());
        settings.setCompilesBeforeStaleSnapshot((Integer)discardInterval.getValue());
        OptimizationOptions.TestSortOrder reordering = noReorder.isSelected() ? OptimizationOptions.TestSortOrder.NONE :
                random.isSelected() ? OptimizationOptions.TestSortOrder.RANDOM : OptimizationOptions.TestSortOrder.FAILFAST;
        settings.setReorder(reordering);
    }
}
