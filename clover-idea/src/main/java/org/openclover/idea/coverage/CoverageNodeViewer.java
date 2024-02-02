package org.openclover.idea.coverage;

import com.atlassian.clover.api.registry.HasMetrics;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;

import static org.openclover.util.Maps.newHashMap;

/**
 */
public class CoverageNodeViewer extends BaseCoverageNodeViewer {
    private final JPanel mPane = new JPanel();
    private JLabel mMethodsCovered = makeLabel();
    private JLabel mStmtsCovered = makeLabel();
    private JLabel mCondsCovered = makeLabel();

    private PercentBarPanel mMethsPc = makePcBar();

    private static final String METRICS_FORM_COLUMNS_DEFINITION = "fill:d:g, fill:max(min;30dlu), 3dlu, fill:d:g, fill:max(min;30dlu), 3dlu";
    private static final int METRICS_FORM_COL_1 = 1; // left 'meaningful' column
    private static final int METRICS_FORM_COL_2 = 4; // right 'meaningful' column

    private static final int[][] METRICS_FORM_GROUPED_COLUMNS = {{METRICS_FORM_COL_1, METRICS_FORM_COL_2}};
    private static final RowSpec METRICS_FORM_ROW_SPEC = new RowSpec("pref");

    private static final String COVERAGE_FORM_COLUMNS_DEFINITION = "3dlu, pref, pref, fill:60dlu:g, 3dlu";
    private static final int COVERAGE_FORM_COL_1 = 2; // left 'meaningful' column
    private static final int COVERAGE_FORM_COL_2 = 4; // right 'meaningful' column

    private static final RowSpec COVERAGE_FORM_ROW_SPEC = new RowSpec("fill:pref");
    private JPanel coveragePane;
    private JPanel metricsPane;

    private PercentBarPanel makePcBar() {
        return makePcBar(PercentBarColors.GREEN_ON_RED);
    }

    private PercentBarPanel makePcBar(PercentBarColors colors) {
        PercentBarPanel pcbar = new PercentBarPanel(colors);
        pcbar.setOpaque(false);
        pcbar.setPercent(-1);
        return pcbar;
    }

    private final PercentBarPanel mStmtsPc = makePcBar();
    private final PercentBarPanel mCondsPc = makePcBar();
    private final PercentBarPanel mTotalPc = makePcBar();

    private final JLabel mLOC = makeLabel();
    private final JLabel mNLOC = makeLabel();
    private final JLabel mTComp = makeLabel();
    private final JLabel mCompDensity = makeLabel();
    private final JLabel mAvgComp = makeLabel();

    private final JLabel mMethods = makeLabel();
    private final JLabel mStatements = makeLabel();
    private final JLabel mBranches = makeLabel();

    private final JLabel mFiles = makeLabel();
    private final JLabel mClasses = makeLabel();
    private final JLabel mPackages = makeLabel();

    private final JLabel mTestMethods = makeLabel();
    private final JLabel mTests = makeLabel();
    private final JLabel mTestsRun = makeLabel();
    private final JLabel mTestsPassed = makeLabel();
    private final PercentBarPanel mPCTestsPassed = makePcBar();
    private final JLabel mTestsSummary = makeLabel();

    private String mTestsRunStr = "-";
    private String mTestsPassedStr = "-";

    private static final Font THE_FONT = new JLabel().getFont().deriveFont(Font.PLAIN);

    public CoverageNodeViewer() {
        setupComponents();
    }

    public void clearNode() {
        setNode(null, null);
    }

    @Override
    public void setNode(HasMetrics hasMetrics, TestPassInfo testPassInfo) {
        super.setNode(hasMetrics, testPassInfo);
        mPane.invalidate();
    }

    @Override
    protected void setExtraStatsVisible(boolean enabled) {
        mLOC.setVisible(enabled);
        mNLOC.setVisible(enabled);
        mTComp.setVisible(enabled);
        mAvgComp.setVisible(enabled);
        mCompDensity.setVisible(enabled);
        mFiles.setVisible(enabled);
        mMethods.setVisible(enabled);
        mClasses.setVisible(enabled);
        mPackages.setVisible(enabled);
        mStatements.setVisible(enabled);
        mBranches.setVisible(enabled);

        mTestMethods.setVisible(enabled);
        mTests.setVisible(enabled);
        mTestsRun.setVisible(enabled);
        mTestsPassed.setVisible(enabled);
    }


    @Override
    protected void setNumPackagesText(String s) {
        mPackages.setText(s);
    }


    @Override
    protected void setNumClassesText(String s) {
        mClasses.setText(s);
    }


    @Override
    protected void setNumFilesText(String s) {
        mFiles.setText(s);
    }


    @Override
    protected void setNumMethodsText(String i) {
        mMethods.setText(i);
    }

    @Override
    protected void setNumBranchesText(String s) {
        mBranches.setText(s);
    }

    @Override
    protected void setNumStatementsText(String s) {
        mStatements.setText(s);
    }

    @Override
    protected void setNCLOCText(String i) {
        mNLOC.setText(i);
    }


    @Override
    protected void setLOCText(String i) {
        mLOC.setText(i);
    }


    @Override
    protected void setTotalPercent(float i) {
        mTotalPc.setPercent(i);
    }


    @Override
    protected void setCondsPercent(float setCondsPercent) {
        mCondsPc.setPercent(setCondsPercent);
    }


    @Override
    protected void setStmtsPercent(float i) {
        mStmtsPc.setPercent(i);
    }


    @Override
    protected void setMethodsPercent(float i) {
        mMethsPc.setPercent(i);
    }


    @Override
    protected void setCondsCoveredText(String s) {
        mCondsCovered.setText(s);
    }


    @Override
    protected void setStmtsCoveredText(String s) {
        mStmtsCovered.setText(s);
    }


    @Override
    protected void setMethodsCoveredText(String s) {
        mMethodsCovered.setText(s);
    }

    @Override
    protected void setAvgComplexityText(String s) {
        mAvgComp.setText(s);
    }

    @Override
    protected void setComplexityDensityText(String s) {
        mCompDensity.setText(s);
    }

    @Override
    protected void setTotalComplexityText(String s) {
        mTComp.setText(s);
    }

    @Override
    protected void setTestMethodsText(String s) {
        mTestMethods.setText(s);
    }

    @Override
    protected void setTestsText(String s) {
        mTests.setText(s);
    }

    @Override
    protected void setTestsRunText(String s) {
        mTestsRunStr = s;
        mTestsRun.setText(s);
        updateTestsSummary();
    }

    @Override
    protected void setTestsPassedText(String s) {
        mTestsPassedStr = s;
        mTestsPassed.setText(s);
        updateTestsSummary();
    }

    @Override
    protected void setTestsFailedText(String s) {
    }

    @Override
    protected void setTestsErrorsText(String s) {
    }

    @Override
    protected void setPCTestsPassed(float p) {
        mPCTestsPassed.setPercent(p);
    }

    @Override
    protected void setPCTestsFailed(float p) {
    }

    @Override
    protected void setPCTestsError(float p) {
    }

    private void updateTestsSummary() {
        mTestsSummary.setText(mTestsPassedStr + " / " + mTestsRunStr);
    }

    public JComponent getPane() {
        return mPane;
    }

    private static JLabel makeLabel() {
        return makeLabel(null);
    }

    private static JLabel makeLabel(String aLabel) {
        JLabel label = aLabel == null ? new JLabel() : new JLabel(aLabel);
        label.setHorizontalAlignment(JLabel.TRAILING);
        return label;
    }

    private static JLabel makeLabel(String aLabel, Font font) {
        JLabel label = makeLabel(aLabel);
        label.setFont(font);
        return label;
    }

    private JPanel createCoveragePane() {
        JPanel coveragePane = new JPanel();
        coveragePane.setOpaque(false);

        TitledBorder title = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Coverage and Tests");
        title.setTitleJustification(TitledBorder.LEFT);
        coveragePane.setBorder(title);

        FormLayout formLayout = new FormLayout(COVERAGE_FORM_COLUMNS_DEFINITION);
        coveragePane.setLayout(formLayout);

        CellConstraints pcBarCC = new CellConstraints(COVERAGE_FORM_COL_2, 1);

        formLayout.appendRow(COVERAGE_FORM_ROW_SPEC);
        addField(coveragePane, "Methods: ", mMethodsCovered, COVERAGE_FORM_COL_1, pcBarCC.gridY);
        coveragePane.add(mMethsPc, pcBarCC);

        ++pcBarCC.gridY;
        formLayout.appendRow(COVERAGE_FORM_ROW_SPEC);
        addField(coveragePane, "Statements: ", mStmtsCovered, COVERAGE_FORM_COL_1, pcBarCC.gridY);
        coveragePane.add(mStmtsPc, pcBarCC);

        ++pcBarCC.gridY;
        formLayout.appendRow(COVERAGE_FORM_ROW_SPEC);
        addField(coveragePane, "Conditionals: ", mCondsCovered, COVERAGE_FORM_COL_1, pcBarCC.gridY);
        coveragePane.add(mCondsPc, pcBarCC);

        ++pcBarCC.gridY;
        formLayout.appendRow(new RowSpec("3dlu"));

        ++pcBarCC.gridY;
        formLayout.appendRow(COVERAGE_FORM_ROW_SPEC);
        JLabel totalTitle = makeLabel("<html><b>TOTAL: ", THE_FONT);
        coveragePane.add(totalTitle, new CellConstraints(COVERAGE_FORM_COL_1, pcBarCC.gridY));
        coveragePane.add(mTotalPc, new CellConstraints(COVERAGE_FORM_COL_1 + 1, pcBarCC.gridY, 2, 1));

        ++pcBarCC.gridY;
        formLayout.appendRow(new RowSpec("6dlu"));

        ++pcBarCC.gridY;
        formLayout.appendRow(COVERAGE_FORM_ROW_SPEC);
        addField(coveragePane, "Tests passed: ", mTestsSummary, COVERAGE_FORM_COL_1, pcBarCC.gridY);
        coveragePane.add(mPCTestsPassed, pcBarCC);

        return coveragePane;
    }


    private JPanel createMetricsPane() {
        JPanel metricsPane = new JPanel();
        metricsPane.setOpaque(false);

        TitledBorder title = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Metrics");
        title.setTitleJustification(TitledBorder.LEFT);
        metricsPane.setBorder(title);

        FormLayout formLayout = new FormLayout(METRICS_FORM_COLUMNS_DEFINITION);
        formLayout.setColumnGroups(METRICS_FORM_GROUPED_COLUMNS);
        metricsPane.setLayout(formLayout);

        int row = 1;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "Lines of Code: ", mLOC, METRICS_FORM_COL_1, row);
        addField(metricsPane, "Conditionals: ", mBranches, METRICS_FORM_COL_2, row);

        row++;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "NC Lines of Code: ", mNLOC, METRICS_FORM_COL_1, row);
        addField(metricsPane, "Statements: ", mStatements, METRICS_FORM_COL_2, row);

        row++;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "Methods: ", mMethods, METRICS_FORM_COL_2, row);

        row++;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "Total Complexity: ", mTComp, METRICS_FORM_COL_1, row);
        addField(metricsPane, "Classes: ", mClasses, METRICS_FORM_COL_2, row);

        row++;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "Avg Complexity: ", mAvgComp, METRICS_FORM_COL_1, row);
        addField(metricsPane, "Files: ", mFiles, METRICS_FORM_COL_2, row);

        row++;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "Complexity Density: ", mCompDensity, METRICS_FORM_COL_1, row);
        addField(metricsPane, "Packages: ", mPackages, METRICS_FORM_COL_2, row);

        row++;
        final RowSpec spacerSpec = new RowSpec("6dlu");
        formLayout.appendRow(spacerSpec);
        testOnlyRows.put(row, spacerSpec);

        row++;
        formLayout.appendRow(METRICS_FORM_ROW_SPEC);
        addField(metricsPane, "Recorded Test Cases: ", mTests, METRICS_FORM_COL_1, row);
        addField(metricsPane, "Test Methods: ", mTestMethods, METRICS_FORM_COL_2, row);
        testOnlyRows.put(row, METRICS_FORM_ROW_SPEC);

        metricsPaneLayout = formLayout;
        return metricsPane;

    }


    private static void addField(JPanel metricsPane, final String aLabel, JComponent field, int labelColumn, int row) {
        CellConstraints cc = new CellConstraints(labelColumn, row);

        JLabel testsMethodsTitle = makeLabel(aLabel, THE_FONT);
        Dimension minSize = testsMethodsTitle.getMinimumSize();
        minSize.width = 0;
        testsMethodsTitle.setMinimumSize(minSize);
        metricsPane.add(testsMethodsTitle, cc);

        field.setFont(THE_FONT);
        metricsPane.add(field, cc.xy(labelColumn + 1, row));

        testsMethodsTitle.setToolTipText(aLabel);
        field.setToolTipText(aLabel);
    }

    private boolean isHorizontal;

    private void setupComponents() {
        mPane.setOpaque(false);
        mPane.setBorder(new EmptyBorder(3, 3, 3, 3));
        mPane.setLayout(new BoxLayout(mPane, BoxLayout.Y_AXIS));

        coveragePane = createCoveragePane();
        metricsPane = createMetricsPane();

        mPane.add(coveragePane);
        mPane.add(metricsPane);
    }

    private final Map<Integer, RowSpec> testOnlyRows = newHashMap();
    private FormLayout metricsPaneLayout;
    private static final RowSpec EMPTY_ROW_SPEC = new RowSpec("0px");

    /**
     * Recorded Test Cases and Test Methods are not applicable in App Only view.
     * Provide capability to turn them off.
     *
     * @param visible should row be visible
     */
    public void setTestMethodsVisible(boolean visible) {
        for (Map.Entry<Integer, RowSpec> entry : testOnlyRows.entrySet()) {
            metricsPaneLayout.setRowSpec(entry.getKey(), visible ? entry.getValue() : EMPTY_ROW_SPEC);
        }
        metricsPane.invalidate();
    }
}
