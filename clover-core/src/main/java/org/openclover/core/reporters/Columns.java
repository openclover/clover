package org.openclover.core.reporters;

import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.FileInfoRegion;
import org.openclover.core.registry.entities.BaseFileInfo;
import org.openclover.core.registry.entities.BasePackageInfo;
import org.openclover.core.registry.entities.BaseProjectInfo;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.registry.metrics.FileMetrics;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.core.reporters.html.HtmlReportUtil;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.core.util.Sets.newHashSet;

/**
 * A holder of Column objects. Columns are added (usually via
 * ant), with the addXXX methods. Adding order is preserved.
 * <p/>
 * If no pkgColumns are added, then {@link Format#getDefaultColumns} can be used.
 */
public class Columns {
    static final String SCOPE_PACKAGE = "package";
    static final String SCOPE_CLASS = "class";
    static final String SCOPE_METHOD = "method";

    /**
     * Given a column class name, this method will create a new column object of that type.
     * @param columnType the class name of the column to instantiate
     * @return a new column object
     */
    private static Column createColumn(String columnType) throws CloverException {

        try {
            final String name =
                            Columns.class.getName() + "$" +
                            columnType.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                            columnType.substring(1);
            Class colClass = Class.forName(name);
            return (Column) colClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            Logger.getInstance().debug(e.getMessage(), e);
        }
        throw new CloverException("Invalid column name: '" + columnType + "'");
    }

    /**
     * @return if the column name is valid or not
     */
    public static boolean isValidColumnName(String columnName) {
        try {
            final String name =
                Columns.class.getName() + "$" +
                columnName.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                columnName.substring(1);
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    public static double getColumnValue(String name, String format, BlockMetrics m) throws CloverException {
        final Column col = createColumn(name);
        col.setFormat(format);
        col.init(m);
        return col.getNumber().doubleValue();
    }

    public static class Expression extends Column {
        final static ColumnFormat.ErrorColumnFormat ERROR_COLUMN_FORMAT = new ColumnFormat.ErrorColumnFormat("-");
        private String expr;
        private String title;

        public Expression() {
            formatter = new ColumnFormat.FloatColumnFormat();
        }

        public Expression(Column col) {
            super(col);
            final Expression expression = (Expression) col;
            expr = expression.expr;
            formatter = col.formatter;
            title = col.getTitle();
        }

        public void addText(String text) {
            expr = text;
        }

        public void setName(String title) {
            this.title = title;
        }

        public void setTitle(String title) {
            setName(title);
        }

        @Override
        public Column copy() {
            return new Expression(this);
        }

        @Override
        public void init(BlockMetrics value) throws CloverException {
            try {
                double result = ExpressionEvaluator.eval(expr, value, getTitle(value));
                data = new ColumnData((float) result);
            } catch (CloverException e) {
                Logger.getInstance().warn(e.getMessage(), e.getCause());
                throw e;
            } catch (ClassCastException e) {
                Logger.getInstance().warn(
                        "Expression: '" + expr + "' contains columns that are not unavailable at this scope.");
                Logger.getInstance().debug(e.getMessage(), e);
                this.data = new ColumnData(-1f);
                this.formatter = ERROR_COLUMN_FORMAT;
            }
        }

        @Override
        public String getTitle(BlockMetrics value) {
            return this.title == null ? this.expr : this.title;
        }

        @Override
        public String getHelp() {
            return this.expr;
        }
    }

    public static class SUM extends Expression {
        public SUM() {
            addText("Complexity^2 * ((1 - %CoveredElements/100)^3) + Complexity");
            setTitle("SUM");
        }

        @Override
        public String getHelp() {
            return "Scientifically Untested Metric";
        }
    }

    public static class TotalBranches extends TotalColumn {
        public TotalBranches() { }

        public TotalBranches(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalBranches(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumBranches());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Branches");
        }

        @Override
        public String getHelp() {
            return "A branch is any part of the code where a decision has been made. e.g. if elseif, ? :, for, while, switch.";
        }
    }

    public static class CoveredBranches extends CoverageColumn {
        public CoveredBranches() { }

        public CoveredBranches(Column col) {
            super(col);
        }


        @Override
        public Column copy() {
            return new CoveredBranches(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumCoveredBranches(), value.getPcCoveredBranches());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Covered Branches");
        }

        @Override
        public String getHelp() {
            return "A covered branch is a branch that has been followed during testing.";
        }
    }


    public static class UncoveredBranches extends CoverageColumn {
        public UncoveredBranches() { }

        public UncoveredBranches(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new UncoveredBranches(this);
        }

        @Override
        public void init(BlockMetrics value) {
            final float pcvalue = value.getPcCoveredBranches() < 0 ? -1 : 1.0f - value.getPcCoveredBranches();
            setValues(value.getNumBranches() - value.getNumCoveredBranches(), pcvalue);
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Uncovered Branches");
        }

        @Override
        public String getHelp() {
            return "An uncovered branch is a branch that has not been followed during testing.";
        }
    }


    public static class TotalStatements extends TotalColumn {
        public TotalStatements() { }

        public TotalStatements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalStatements(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumStatements());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Statements");
        }

        @Override
        public String getHelp() {
            return "The total number of statements.";
        }
    }

    public static class CoveredStatements extends CoverageColumn {
        public CoveredStatements() { }

        public CoveredStatements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new CoveredStatements(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumCoveredStatements(), value.getPcCoveredStatements());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Covered Statements");
        }

        @Override
        public String getHelp() {
            return "Statements that were executed at least once.";
        }
    }

    public static class UncoveredStatements extends CoverageColumn {

        public UncoveredStatements() { }

        public UncoveredStatements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new UncoveredStatements(this);
        }

        @Override
        public void init(BlockMetrics value) {
            final float pcvalue = value.getPcCoveredStatements() < 0 ? -1 : 1.0f - value.getPcCoveredStatements();
            setValues(value.getNumStatements() - value.getNumCoveredStatements(), pcvalue);
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Uncovered Statements");
        }

        @Override
        public String getHelp() {
            return "Statements that were not executed during testing.";
        }
    }

    public static class TotalMethods extends TotalColumn {
        public TotalMethods() { }

        public TotalMethods(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalMethods(this);
        }

        @Override
        public void init(BlockMetrics value) {
            if (value instanceof ClassMetrics) {
                ClassMetrics cm = (ClassMetrics) value;
                setValues(cm.getNumMethods());
            } else {
                setValues(1);
            }
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Methods");
        }

        @Override
        public String getHelp() {
            return "The total number of methods of all scopes including both static and non-static methods.";
        }
    }

    public static class CoveredMethods extends CoverageColumn {
        public CoveredMethods() { }

        public CoveredMethods(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new CoveredMethods(this);
        }

        @Override
        public void init(BlockMetrics value) {
            ClassMetrics cm = (ClassMetrics) value;
            setValues(cm.getNumCoveredMethods(), cm.getPcCoveredMethods(), value.isEmpty());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Covered Methods");
        }

        @Override
        public String getHelp() {
            return "The amount of methods that were entered at least once during testing.";
        }
    }


    public static class UncoveredMethods extends CoverageColumn {
        public UncoveredMethods() { }

        public UncoveredMethods(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new UncoveredMethods(this);
        }

        @Override
        public void init(BlockMetrics value) {
            ClassMetrics cm = (ClassMetrics) value;
            final float pcvalue = cm.getPcCoveredMethods() < 0 ? -1 : 1.0f - cm.getPcCoveredMethods();
            setValues(cm.getNumMethods() - cm.getNumCoveredMethods(), pcvalue, value.isEmpty());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Uncovered Methods");
        }

        @Override
        public String getHelp() {
            return "The amount of methods that were not entered during testing.";
        }
    }

    abstract static class PercentageColumn extends CoverageColumn {
        public PercentageColumn() { }

        public PercentageColumn(Column col) {
            super(col);
        }

        public void setCustomPositiveClass(String className) {
            ((PcColumnData)getColumnData()).setCustomPositiveClass(className);
        }
        public void setCustomNegativeClass(String className) {
            ((PcColumnData)getColumnData()).setCustomNegativeClass(className);
        }
    }

    public static class TotalPercentageCovered extends PercentageColumn {
        public TotalPercentageCovered() { }

        public TotalPercentageCovered(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalPercentageCovered(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumCoveredElements(), value.getPcCoveredElements(), value.isEmpty());
        }


        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("TOTAL Coverage");
        }

        @Override
        public String getHelp() {
            return "The amount of code that was hit at least once during testing.";
        }
    }

    abstract static class PercentageContribution extends PercentageColumn {
        protected PercentageContribution() { }

        protected PercentageContribution(Column col) {
            super(col);
        }

        protected void setCustomClasses() {
            setCustomPositiveClass("contribBarPositive");
            setCustomNegativeClass("contribBarNegative");
        }
    }

    public static class PercentageCoveredContribution extends PercentageContribution {
        public PercentageCoveredContribution() { }

        public PercentageCoveredContribution(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new PercentageCoveredContribution(this);
        }

        @Override
        public void init(BlockMetrics value) {
            final HasMetrics owner = value.getOwner();
            final ProjectInfo project = getProjectFor(owner);
            final BlockMetrics projectMetrics =
                project == null ? null : project.getMetrics();

            setValues(
                value.getNumCoveredElements(),
                projectMetrics == null || (projectMetrics.getNumCoveredElements() == 0.0f)
                    ? 0.0f
                    : ((float)value.getNumCoveredElements() / (float)projectMetrics.getNumCoveredElements()),
                value.isEmpty());
            setCustomClasses();
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Covered Contribution");
        }

        @Override
        public String getHelp() {
            return "The amount of code that was hit at least once during testing as a percentage of the project\\'s total";
        }
    }

    public static class PercentageUncoveredContribution  extends PercentageContribution {
        public PercentageUncoveredContribution() { }

        public PercentageUncoveredContribution(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new PercentageUncoveredContribution(this);
        }

        @Override
        public void init(BlockMetrics value) {
            final HasMetrics owner = value.getOwner();
            final ProjectInfo project = getProjectFor(owner);
            final BlockMetrics projectMetrics =
                project == null ? null : project.getMetrics();

            setValues(
                value.getNumCoveredElements(),
                projectMetrics == null || (projectMetrics.getNumUncoveredElements() == 0.0f)
                    ? 0.0f
                    : ((float)value.getNumUncoveredElements() / (float)projectMetrics.getNumUncoveredElements()),
                value.isEmpty());
            setCustomClasses();
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Uncovered Contribution");
        }

        @Override
        public String getHelp() {
            return "The amount of code that was not hit during testing as a percentage of the project\\'s total";
        }
    }

    private static ProjectInfo getProjectFor(HasMetrics owner) {
        return owner instanceof FileInfoRegion
            ? ((FileInfoRegion)owner).getContainingFile().getContainingPackage().getContainingProject()
            : owner instanceof BaseFileInfo
                ? ((BaseFileInfo)owner).getContainingPackage().getContainingProject()
                : owner instanceof BasePackageInfo
                    ? ((BasePackageInfo)owner).getContainingProject()
                    : owner instanceof BaseProjectInfo
                        ? (BaseProjectInfo)owner
                        : null;
    }

    public static class TotalElements extends TotalColumn {
        public TotalElements() { }

        public TotalElements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalElements(this);
        }


        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumElements());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Elements");
        }

        @Override
        public String getHelp() {
            return "The total amount of branches + statements.";
        }
    }

    public static class CoveredElements extends CoverageColumn {
        public CoveredElements() { }

        public CoveredElements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new CoveredElements(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getNumCoveredElements(), value.getPcCoveredElements());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Covered Elements");
        }

        @Override
        public String getHelp() {
            return "The number of statements and branches that were covered during testing.";
        }
    }

    public static class UncoveredElements extends CoverageColumn {

        public UncoveredElements() { }

        public UncoveredElements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new UncoveredElements(this);
        }

        @Override
        public void init(BlockMetrics value) {
            final float pcvalue = value.getPcCoveredElements() < 0 ? -1 : 1.0f - value.getPcCoveredElements();
            setValues(value.getNumElements() - value.getNumCoveredElements(), pcvalue);
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Uncovered Elements");
        }

        @Override
        public String getHelp() {
            return "The number of statements and branches that were not covered during testing.";
        }
    }

    public static class AvgClassesPerFile extends AverageColumn {

        public AvgClassesPerFile() { }

        @Override
        public Column copy() {
            return new AvgClassesPerFile(this);
        }


        public AvgClassesPerFile(Column col) {
            super(col);
        }

        @Override
        public void init(BlockMetrics value) {
            PackageMetrics metrics = (PackageMetrics) value;
            setValues(metrics.getAvgClassesPerFile());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Avg Classes / File");
        }


        @Override
        public String getHelp() {
            return "The average number of classes in each file.";
        }
    }

    public static class AvgMethodsPerClass extends AverageColumn {


        public AvgMethodsPerClass() { }

        public AvgMethodsPerClass(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new AvgMethodsPerClass(this);
        }

        @Override
        public void init(BlockMetrics value) {
            FileMetrics metrics = (FileMetrics) value;
            setValues(metrics.getAvgMethodsPerClass());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Avg Methods / Class");
        }

        @Override
        public String getHelp() {
            return "The average number of methods in each class.";
        }
    }


    public static class AvgStatementsPerMethod extends AverageColumn {


        public AvgStatementsPerMethod() { }

        public AvgStatementsPerMethod(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new AvgStatementsPerMethod(this);
        }

        @Override
        public void init(BlockMetrics value) {
            ClassMetrics metrics = (ClassMetrics) value;
            setValues(metrics.getAvgStatementsPerMethod());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Avg Statements / Method");
        }

        @Override
        public String getHelp() {
            return "The average number of statements per method.";
        }
    }

    public static class TotalPackages extends TotalColumn {

        public TotalPackages() { }

        public TotalPackages(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalPackages(this);
        }

        @Override
        public void init(BlockMetrics value) {
            if (value instanceof ProjectMetrics) {
                ProjectMetrics metrics = (ProjectMetrics) value;
                setValues(metrics.getNumPackages());
            } else {
                setValues(0);
            }
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Packages");
        }

        @Override
        public String getHelp() {
            return "The total number of packages in this project.";
        }
    }


    public static class TotalFiles extends TotalColumn {

        public TotalFiles() { }

        public TotalFiles(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalFiles(this);
        }

        @Override
        public void init(BlockMetrics value) {
            PackageMetrics metrics = (PackageMetrics) value;
            setValues(metrics.getNumFiles());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Files");
        }

        @Override
        public String getHelp() {
            return "The total number of files in this package or project.";
        }
    }

    public static class TotalClasses extends TotalColumn {

        public TotalClasses() { }

        public TotalClasses(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalClasses(this);
        }

        @Override
        public void init(BlockMetrics value) {
            FileMetrics metrics = (FileMetrics) value;
            setValues(metrics.getNumClasses());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Total Classes");
        }

        @Override
        public String getHelp() {
            return "The total number of classes.";
        }
    }

    public static class LineCount extends TotalColumn {

        public LineCount() { }

        public LineCount(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new LineCount(this);
        }

        @Override
        public void init(BlockMetrics value) {
            FileMetrics metrics = (FileMetrics) value;
            setValues(metrics.getLineCount());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Lines");
        }

        @Override
        public String getHelp() {
            return "Total number of lines.";
        }
    }

    public static class NcLineCount extends TotalColumn {

        public NcLineCount() { }

        public NcLineCount(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new NcLineCount(this);
        }

        @Override
        public void init(BlockMetrics value) {
            FileMetrics metrics = (FileMetrics) value;
            setValues(metrics.getNcLineCount());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("NC Lines");
        }


        @Override
        public String getHelp() {
            return "Total number of non-comment lines of code.";
        }
    }

    public static class Complexity extends TotalColumn {

        public Complexity() {
            super();
        }

        public Complexity(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new Complexity(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getComplexity());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return "Complexity";
        }


        @Override
        public String getHelp() {
            return "Cyclomatic complexity is a measure of the number of paths in your code.";
        }
    }

    public static class AvgMethodComplexity extends AverageColumn {


        public AvgMethodComplexity() { }

        public AvgMethodComplexity(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new AvgMethodComplexity(this);
        }

        @Override
        public void init(BlockMetrics value) {
            ClassMetrics metrics = (ClassMetrics) value;
            setValues(metrics.getAvgMethodComplexity());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Average Method Complexity");
        }


        @Override
        public String getHelp() {
            return "The average number of paths per method.";
        }
    }

    public static class ComplexityDensity extends AverageColumn {

        public ComplexityDensity() { }

        public ComplexityDensity(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new ComplexityDensity(this);
        }

        @Override
        public void init(BlockMetrics value) {
            setValues(value.getComplexityDensity());
        }

        @Override
        public String getTitle(BlockMetrics value)  {
            return formatter.formatTitle("Complexity Density");
        }

        @Override
        public String getHelp() {
            return "Complexity Density is the average number of paths in your code per statement.";
        }
    }

    public static class ComplexityToCoverage extends TotalColumn {


        public ComplexityToCoverage() { }

        public ComplexityToCoverage(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new ComplexityToCoverage(this);
        }

        @Override
        public void init(BlockMetrics value) {
            boolean isEmpty = value.getPcCoveredElements() == 0;
            setValues(isEmpty ? -1 : (int)(value.getComplexity() / value.getPcCoveredElements()));
        }

        @Override
        public String getTitle(BlockMetrics value) {
            return formatter.formatTitle("Complexity / Coverage");
        }
    }

    public static class TotalChildren extends TotalColumn {


        public TotalChildren() { }

        public TotalChildren(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new TotalChildren(this);
        }

        @Override
        public void init(BlockMetrics value) {
            ClassMetrics metrics = (ClassMetrics) value;
            setValues(metrics.getNumChildren());
        }

        @Override
        public String getTitle(BlockMetrics value) {
            ClassMetrics metrics = (ClassMetrics) value;
            if (metrics == null) {
                return "children";
            }
            HtmlRenderingSupportImpl support = new HtmlRenderingSupportImpl();
            final String title =  support.capitalize(metrics.getChildType());
            final String suffix = title.endsWith("s") ? "es" : "s";
            return title + suffix;
        }
    }

    public static class FilteredElements extends PercentageColumn {

        public FilteredElements() { }

        public FilteredElements(Column col) {
            super(col);
        }

        @Override
        public Column copy() {
            return new FilteredElements(this);
        }

        @Override
        public void init(BlockMetrics value) {
            final HasMetrics hasMetrics = value.getOwner();
            final BlockMetrics rawMetrics = hasMetrics.getRawMetrics();
            final boolean isFiltered;
            final float pcFiltered;
            final int totalFiltered;

            if (rawMetrics == null) {
                isFiltered = false;
                pcFiltered = 0f;
                totalFiltered = 0;
            } else {
                isFiltered = HtmlReportUtil.hasFilteredMetrics(hasMetrics);
                pcFiltered = HtmlReportUtil.getPercentageFiltered(hasMetrics);
                totalFiltered = rawMetrics.getNumElements() - hasMetrics.getMetrics().getNumElements();
            }

            setValues(totalFiltered, pcFiltered, isFiltered);
            setCustomPositiveClass("barFiltered");
            setCustomNegativeClass("barNonFiltered");
        }

        @Override
        public String getTitle(BlockMetrics value) {
            return formatter.formatTitle("Filtered");
        }

        @Override
        public String getHelp() {
            return "The percentage of elements that have been filtered from this report.";
        }

    }

    /**
     * Holds a list of &lt;Column &gt; objects.
     */
    private final List<Column> projectColumns = newLinkedList();
    private final List<Column> pkgColumns = newLinkedList();
    private final List<Column> classColumns = newLinkedList();
    private final List<Column> methodColumns = newLinkedList();

    public void addConfiguredTotalPercentageCovered(TotalPercentageCovered column) {
        addGlobalColumn(column);
    }

    public void addConfiguredPercentageCoveredContribution(PercentageCoveredContribution column) {
        addGlobalColumn(column);
    }

    public void addConfiguredPercentageUncoveredContribution(PercentageUncoveredContribution column) {
        addGlobalColumn(column);
    }

    public void addConfiguredTotalBranches(TotalBranches column) {
        addGlobalColumn(column);
    }

    public void addConfiguredCoveredBranches(CoveredBranches column) {
        addGlobalColumn(column);
    }

    public void addConfiguredUncoveredBranches(UncoveredBranches column) {
        addGlobalColumn(column);
    }

    public void addConfiguredTotalMethods(TotalMethods column) {
        addPkgColumn(column);
        addClassColumn(column);
    }

    public void addConfiguredCoveredMethods(CoveredMethods column) {
        addPkgColumn(column);
        addClassColumn(column);
    }

    public void addConfiguredUncoveredMethods(UncoveredMethods column) {
        addPkgColumn(column);
        addClassColumn(column);
    }

    public void addConfiguredTotalStatements(TotalStatements column) {
        addGlobalColumn(column);
    }

    public void addConfiguredCoveredStatements(CoveredStatements column) {
        addGlobalColumn(column);
    }

    public void addConfiguredUncoveredStatements(UncoveredStatements column) {
        addGlobalColumn(column);
    }

    public void addConfiguredTotalElements(TotalElements column) {
        addGlobalColumn(column);
    }

    public void addConfiguredCoveredElements(CoveredElements column) {
        addGlobalColumn(column);
    }

    public void addConfiguredUncoveredElements(UncoveredElements column) {
        addGlobalColumn(column);
    }

    public void addConfiguredAvgClassesPerFile(AvgClassesPerFile column) {
        addPkgColumn(column);
    }

    public void addConfiguredAvgMethodsPerClass(AvgMethodsPerClass column) {
        addPkgColumn(column);
    }

    public void addConfiguredAvgStatementsPerMethod(AvgStatementsPerMethod column) {
        addGlobalMethodColumn(column, new TotalStatements(column));
    }

    public void addConfiguredTotalPackages(TotalPackages column) {
        projectColumns.add(column);
    }

    public void addConfiguredTotalFiles(TotalFiles column) {
        addPkgColumn(column);
    }

    public void addConfiguredTotalClasses(TotalClasses column) {
        addPkgColumn(column);
    }

    public void addConfiguredLineCount(LineCount column) {
        addPkgColumn(column);
    }

    public void addConfiguredNcLineCount(NcLineCount column) {
        addPkgColumn(column);
    }

    public void addConfiguredComplexity(Complexity column) {
        addGlobalColumn(column);
    }

    public void addConfiguredAvgMethodComplexity(AvgMethodComplexity column) {
        addGlobalMethodColumn(column, new Complexity(column));
    }

    private void addGlobalMethodColumn(Column column, Column methodColumn) {
        addPkgColumn(column);
        addClassColumn(column);
        addMethodColumn(methodColumn);
    }

    public void addConfiguredComplexityDensity(ComplexityDensity column) {
        addGlobalColumn(column);
    }

    public void addConfiguredTotalChildren(TotalChildren column) {
        addPkgColumn(column);
        addClassColumn(column);
        addMethodColumn(new TotalStatements(column));
    }

    public void addConfiguredComplexityCoverage(ComplexityToCoverage column) {
        addGlobalColumn(column);
    }

    public void addConfiguredExpression(Expression column) throws CloverException {
        // ensure the expression is valid
        ExpressionEvaluator.parse(column.expr, column.title);
        addGlobalColumn(column);
    }

    private void insertColumn(Column column) {
         if (column.getScope() == null ||
                 scopeContains(column, SCOPE_METHOD)) {
            addPkgColumn(column);
            addClassColumn(column);
            addMethodColumn(column);
        } else if (scopeContains(column, SCOPE_CLASS)) {
            addPkgColumn(column);
            addClassColumn(column);
        } else if (scopeContains(column, SCOPE_PACKAGE)) {
            addPkgColumn(column);
        }
    }

    private boolean scopeContains(Column column, String scope) {
        return column.getScope().contains(scope);
    }

    public void addConfiguredSum(SUM column) {
        addGlobalColumn(column);
    }

    public void addFilteredElements(FilteredElements column) {
        addGlobalColumn(column);
    }

    private void addGlobalColumn(Column column) {
        insertColumn(column);
    }

    private void addClassColumn(Column column) {
        classColumns.add(column);
    }

    private void addPkgColumn(Column column) {
        pkgColumns.add(column);
    }

    private void addMethodColumn(Column column) {
        methodColumns.add(column);
    }



    public Set<Column> getProjectColumns() {
        // since ProjectMetrics extends from MethodMetrics, add all metrics
        Set<Column> allColumns = newHashSet(projectColumns);
        allColumns.addAll(pkgColumns);
        allColumns.addAll(classColumns);
        allColumns.addAll(methodColumns);
        return allColumns;
    }

    public List<Column> getPkgColumns() {
        return newLinkedList(pkgColumns);
    }

    public List<Column> getClassColumns() {
        return newLinkedList(classColumns);
    }

    public List<Column> getMethodColumns() {
        return methodColumns;
    }

    public List<Column> getProjectColumnsCopy() {
        return copyColumns(newLinkedList(getProjectColumns()));
    }

    public List<Column> getClassColumnsCopy() {
        return copyColumns(classColumns);
    }

    public List<Column> getMethodColumnsCopy() {
        return copyColumns(methodColumns);
    }

    public static List<Column> getAllColumns() {
        final List<Column> allColumns = newLinkedList();
        final Class[] classes = Columns.class.getClasses();
        for (Class aClass : classes) {
            if (Column.class.isAssignableFrom(aClass) &&
                    !Expression.class.isAssignableFrom(aClass)) { // don't add expressions
                try {
                    Column col = (Column) aClass.getDeclaredConstructor().newInstance();
                    if (!PercentageColumn.class.isAssignableFrom(aClass)) {
                        col.setFormat("raw");
                    }
                    allColumns.add(col);

                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                         InstantiationException e) {
                    Logger.getInstance().warn("Could not add column: " + aClass, e);
                }
            }
        }
        return allColumns;
    }

    private List<Column> copyColumns(List<Column> cols) {
        // make a deep copy of these
        List<Column> columns = new ArrayList<>(cols.size());
        for (Column column : cols) {
            columns.add(column.copy());
        }
        return columns;
    }

}
