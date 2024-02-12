package org.openclover.eclipse.core.views;

import clover.antlr.RecognitionException;
import clover.antlr.collections.AST;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.openclover.core.registry.metrics.BlockMetrics;
import org.openclover.core.reporters.CalcTreeWalker;
import org.openclover.core.reporters.ExpressionEvaluator;
import org.openclover.core.util.MetricsFormatUtils;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.coverageexplorer.MetricsPcCellRenderer;
import org.openclover.eclipse.core.views.coverageexplorer.MetricsRawCellRenderer;
import org.openclover.eclipse.core.views.widgets.ListeningRenderer;
import org.openclover.runtime.api.CloverException;

import java.util.Comparator;

public class CustomColumnDefinition
    extends ColumnDefinition
    implements NumericColumnDefinition {
    
    public static final int RAW_FORMAT = 0;
    public static final int PC_FORMAT = 1;
    private static int INDEX = 0;

    public static String idForIndex(int index) {
        return "Custom:" + index;
    }

    public static synchronized int nextIndex() {
        return INDEX++;
    }

    private final String expression;
    private final AST expressionAST;
    private final CalcTreeWalker expressionWalker;
    private final int format;
    private final Comparator appOnlyComparator;
    private final Comparator testOnlyComparator;
    private final Comparator fullComparator;

    public CustomColumnDefinition(String id, String title, String abbreviatedTitle, String expression, int alignment, int format) throws CloverException {
        super(id, ANY_COLUMN, alignment, title, abbreviatedTitle, "");
        this.format = (format == RAW_FORMAT || format == PC_FORMAT) ? format : RAW_FORMAT;
        this.expression = expression;
        this.expressionAST = ExpressionEvaluator.parse(expression, title).getAST();
        this.expressionWalker = new CalcTreeWalker();
        this.appOnlyComparator = new MyComparator(MetricsScope.APP_ONLY);
        this.testOnlyComparator = new MyComparator(MetricsScope.TEST_ONLY);
        this.fullComparator = new MyComparator(MetricsScope.FULL);
        try {
            this.expressionWalker.validate(this.expressionAST);
        } catch (RecognitionException e) {
            throw new CloverException(e);
        }
    }

    public CustomColumnDefinition(String title, String abbreviatedTitle, String expression, int aligment, int format) throws CloverException {
        this(idForIndex(nextIndex()), title, abbreviatedTitle, expression, aligment, format);
    }

    public int getFormat() {
        return format;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public boolean isCustom() {
        return true;
    }

    public double calculate(BlockMetrics metrics) throws RecognitionException {
        return new CalcTreeWalker().expr(expressionAST, metrics);
    }

    @Override
    public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object element) {
        BlockMetrics metrics = scope.getMetricsFor(element);
        if (metrics != null) {
            try {
                if (format == CustomColumnDefinition.RAW_FORMAT) {
                    return MetricsFormatUtils.formatMetricsDecimal(calculate(metrics));
                } else {
                    return MetricsFormatUtils.formatMetricsPercent(calculate(metrics) / 100.0d);
                }
            } catch (Exception e) {
                return MetricsFormatUtils.ERROR_METRICS_LABEL;
            }
        } else {
            return MetricsFormatUtils.NO_METRICS_LABEL;
        }
    }

    @Override
    public Number getValue(MetricsScope scope, Object element) {
        BlockMetrics metrics = scope.getMetricsFor(element);
        if (metrics != null) {
            try {
                Double result = format == CustomColumnDefinition.RAW_FORMAT
                        ? calculate(metrics)
                        : calculate(metrics) / 100.0d;
                return (result.isNaN() || result.isInfinite()) ? NOT_AVAILABLE_DOUBLE : result;
            } catch (Exception e) {
                // swallow
            }
        }
        return NOT_AVAILABLE_DOUBLE;
    }

    @Override
    public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
        ListeningRenderer renderer = null;
        if (format == CustomColumnDefinition.PC_FORMAT) {
            renderer = new MetricsPcCellRenderer((Tree)composite, settings, settings.getTreeColumnSettings(), this);
        } else if (format == CustomColumnDefinition.RAW_FORMAT) {
            renderer = new MetricsRawCellRenderer((Tree)composite, settings, settings.getTreeColumnSettings(), this) {
                @Override
                protected String formatValue(Object item) {
                    double value = valueFor(item);
                    return MetricsFormatUtils.formatMetricsDecimal(value);
                }

                @Override
                protected double valueFor(Object data) {
                    return
                        ((NumericColumnDefinition)column).getValue(
                            viewSettings.getMetricsScope(),
                            data).doubleValue();
                }
            };
        }
        return renderer;
    }

    @Override
    public Comparator getComparator(ExplorerViewSettings settings, MetricsScope scope) {
        if (scope == MetricsScope.APP_ONLY) {
            return appOnlyComparator;
        } else if (scope == MetricsScope.TEST_ONLY) {
            return testOnlyComparator;
        } else {
            return fullComparator;
        }
    }
    
    private class MyComparator implements Comparator {
        private MetricsScope scope;

        public MyComparator(MetricsScope scope) {
            this.scope = scope;
        }

        @Override
        public int compare(Object object1, Object object2) {
            return ExplorerViewComparator.compareCustomColumn(scope, CustomColumnDefinition.this, object1, object2);
        }
    }
}
