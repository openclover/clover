package org.openclover.eclipse.core.views;

import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import com.atlassian.clover.registry.metrics.ClassMetrics;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.coverageexplorer.CoverageViewSettings;
import org.openclover.eclipse.core.views.coverageexplorer.MetricsRawCellRenderer;
import org.openclover.eclipse.core.views.widgets.ListeningRenderer;
import com.atlassian.clover.util.MetricsFormatUtils;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import java.util.Comparator;

public abstract class BuiltinMetricsColumnDefinition
    extends BuiltinColumnDefinition
    implements NumericColumnDefinition {
    
    protected static final Double NOT_AVAILABLE_DOUBLE = -1.0d;

    private final MetricsComparator appOnlyComparator = new MetricsComparator(MetricsScope.APP_ONLY);
    private final MetricsComparator testOnlyComparator = new MetricsComparator(MetricsScope.TEST_ONLY);
    private final MetricsComparator fullComparator = new MetricsComparator(MetricsScope.FULL);
    private final Column prototype;

    public BuiltinMetricsColumnDefinition(Column prototype, String abbreviatedTitle, int requiredIndex, int style) {
        super("Metrics" + prototype.getName(), requiredIndex, style, prototype.getTitle(), abbreviatedTitle, prototype.getHelp());
        this.prototype = prototype;
    }

    @Override
    public String getLabel(ExplorerViewSettings settings, MetricsScope scope, ILabelProvider delegate, Object target) {
        BlockMetrics metrics = scope.getMetricsFor(target);
        if (metrics != null) {
            try {
                synchronized(prototype) {
                    prototype.init(metrics);
                    return format(prototype.getNumber());
                }
            } catch (Exception e) {
            }
        }
        return MetricsFormatUtils.NO_METRICS_LABEL;
    }

    @Override
    public Number getValue(MetricsScope scope, Object object) {
        BlockMetrics metrics = scope.getMetricsFor(object);
        if (metrics != null) {
            try {
                synchronized(prototype) {
                    prototype.init(metrics);
                    return prototype.getNumber();
                }
            } catch (Exception e) {
            }
        }
        return NOT_AVAILABLE_DOUBLE;
    }

    @Override
    public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
        return new MetricsRawCellRenderer((Tree)composite, settings, settings.getTreeColumnSettings(), this) {
            @Override
            protected String formatValue(Object item) {
                return getLabel(viewSettings, viewSettings.getMetricsScope(), null, item);
            }
        };
    }

    protected abstract String format(Number value);

    public Column getPrototype() {
        return prototype;
    }

    private static ClassMetrics toClassMetrics(BlockMetrics metrics1) {
        return metrics1 instanceof ClassMetrics ? (ClassMetrics)metrics1 : null;
    }

    private static BlockMetrics toMetrics(CoverageViewSettings settings, Object value) {
        return settings.getMetricsScope().getMetricsFor(value);
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

    public class MetricsComparator implements Comparator {
        private MetricsScope scope;

        public MetricsComparator(MetricsScope scope) {
            this.scope = scope;
        }

        @Override
        public int compare(Object object1, Object object2) {
            if (object1.getClass() == object2.getClass()) {
                Double value1 = getValue(scope, object1).doubleValue();
                Double value2 = getValue(scope, object2).doubleValue();
                return value1.compareTo(value2);
            } else {
                return ExplorerViewComparator.compareType(object1, object2);
            }
        }

    }
}
