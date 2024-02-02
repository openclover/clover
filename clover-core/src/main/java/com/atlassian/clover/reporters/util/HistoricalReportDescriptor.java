package com.atlassian.clover.reporters.util;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.cfg.Percentage;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.ColumnFormat;
import com.atlassian.clover.reporters.Historical;
import com.atlassian.clover.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Lists.newLinkedList;

/**
 * convenience class that interprets a report config and gathers data required for historical report renderers
 */
public class HistoricalReportDescriptor {
    /**
     * use to log messages *
     */
    private final Logger log;

    private Historical histCfg;
    private SortedMap<Long, HasMetrics> models;
    private List<MoversDescriptor> moversDescriptors;
    private List<AddedDescriptor> addedDescriptors;
    private HasMetrics subjectMetrics;
    private Long firstTS;
    private Long lastTS;
    private boolean enoughForMovers;

    public HistoricalReportDescriptor(CloverReportConfig cfg)
            throws CloverException {
        log = Logger.getInstance();
        histCfg = (Historical) cfg;
        if (histCfg == null) {
            throw new CloverException("Invalid report config");
        }
        moversDescriptors = new ArrayList<>(histCfg.getMovers().size());
        addedDescriptors = new ArrayList<>(histCfg.getAdded().size());
    }

    public boolean gatherHistoricalModels() throws IOException {
        File[] historyFiles = histCfg.getHistoryFiles();
        if (historyFiles == null) {
            historyFiles = FileUtils.listMatchingFilesForDir(
                    histCfg.getHistoryDir(),
                    CloverNames.HISTPOINT_PREFIX + ".*" + CloverNames.HISTPOINT_SUFFIX);
        }

        if (isPackageLevel()) {
            models = HistoricalSupport.getPackageMetricsForRange(getPackage(),
                    historyFiles,
                    histCfg.getFromTS().getTime(),
                    histCfg.getToTS().getTime());
        } else {
            models = HistoricalSupport.getProjectMetricsForRange(historyFiles,
                    histCfg.getFromTS().getTime(),
                    histCfg.getToTS().getTime());
        }
        int numts = models.keySet().size();

        if (numts == 0) {
            // zero datapoints makes for historical disappointment
            Logger.getInstance().debug("No historical data found. No report can be generated.");
            return false;
        }

        enoughForMovers = numts > 1;

        firstTS = models.firstKey();
        lastTS = models.lastKey();
        try {
            subjectMetrics = HistoricalSupport.getFullMetrics((HistoricalSupport.HasMetricsWrapper) models.get(lastTS), getPackage());

            if (showMovers()) {
                for (Historical.Movers movers : histCfg.getMovers()) {
                    MoversDescriptor moversDescriptor = new MoversDescriptor(movers);
                    moversDescriptor.gatherMovers();
                    moversDescriptors.add(moversDescriptor);
                }
                for (Historical.Added added : histCfg.getAdded()) {
                    AddedDescriptor addedDescriptor = new AddedDescriptor(added);
                    addedDescriptor.gatherMovers();
                    addedDescriptors.add(addedDescriptor);
                }
            }
            return true;
        } catch (Exception e) {
            final String msg = "An error occured reading historical data (" + e.getClass().getName() + ":" + e.getMessage() + ")";
            Logger.getInstance().debug(msg, e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean showOverview() {
        return histCfg.getOverview() != null;
    }

    public boolean showMovers() {
        return enoughForMovers && histCfg.getMovers().size() > 0;
    }

    public boolean isPackageLevel() {
        return getPackage() != null;
    }

    public String getPackage() {
        return histCfg.getPackage();
    }


    public String getSubjectName() {
        if (isPackageLevel()) {
            return subjectMetrics.getName();
        } else {
            return ""; //##HACK
        }
    }

    @SuppressWarnings("unused") // historical.vm
    public String getSubjectType() {
        if (isPackageLevel()) {
            return "Package";
        } else {
            return "Project";
        }
    }

    public HasMetrics getSubjectMetrics() {
        return subjectMetrics;
    }

    public long getFirstTimestamp() {
        return firstTS;
    }

    public long getLastTimestamp() {
        return lastTS;
    }

    public SortedMap getHistoricalModels() {
        return models;
    }

    public List<MoversDescriptor> getMoversDescriptors() {
        return moversDescriptors;
    }

    public List<AddedDescriptor> getAddedDescriptors() {
        return addedDescriptors;
    }

    private Long getFirstTSAfter(List<Long> timestamps, Long lastTS, Interval requested) {
        Collections.reverse(timestamps);
        Long firstTS = timestamps.get(1); // the default is the second last timestamp

        if (requested != null) {
            long requestedTS = lastTS - requested.getValueInMillis();

            if (requestedTS >= lastTS) {
                log.warn("Ignoring interval setting of " + requested + ". ");
            } else {
                for (Long ts : timestamps) {
                    if (ts < requestedTS) {
                        break;
                    } else {
                        firstTS = ts;
                    }
                }
            }
        }
        if (firstTS.equals(lastTS)) {
            firstTS = timestamps.get(1);
        }

        return firstTS;
    }

    public class AddedDescriptor extends MoversDescriptor {

        public AddedDescriptor(Historical.Movers movers) {
            this.movers = movers;
        }

        @Override
        protected List<MetricsDiffSummary> getPositiveMovers(
                List<MetricsDiffSummary> moverClasses, int lastMover, int range) {

            // ensure we have the top n and bottom n classes in this list
            if (moverClasses.size() <= range) { // if we have less added than range, return them all.
                moverClasses.sort(MetricsDiffSummary.INVERSE_DIFF_COMP);
                return removeAllEmpty(moverClasses);
            } else { // otherwise, get the top and bottom N.
                final List<MetricsDiffSummary> topN = getPositiveMoversInner(moverClasses, lastMover, range, true);
                topN.sort(MetricsDiffSummary.INVERSE_DIFF_COMP);
                final List<MetricsDiffSummary> bottomN = getBottomMovers(moverClasses, range);
                final List<MetricsDiffSummary> result = new ArrayList<>(topN.size() + bottomN.size());
                result.addAll(topN);
                result.addAll(bottomN);

                return removeAllEmpty(result);
            }
        }

        private List<MetricsDiffSummary> removeAllEmpty(List<MetricsDiffSummary> result) {
            // remove any empties
            final List<MetricsDiffSummary> nonEmpty = newLinkedList();
            for (MetricsDiffSummary diffSummary : result) {
                if (!diffSummary.getCurrentClassInfo().getMetrics().isEmpty()) {
                    nonEmpty.add(diffSummary);
                }
            }
            return nonEmpty;
        }

        protected List<MetricsDiffSummary> getBottomMovers(List<MetricsDiffSummary> moverClasses, int range) {
            // ensure there are no repeats. return an empty list if range < moverClasses.size()
            int toIndex = moverClasses.size() > range ? range : moverClasses.size();
            return moverClasses.subList(0, toIndex);
        }

        @Override
        protected List<MetricsDiffSummary> getMoverClasses(HasMetrics fromMetrics, Column column) throws CloverException {
            return HistoricalSupport.getClassesMetricsDifference(
                    fromMetrics, subjectMetrics, movers.getThreshold(), column, false);
        }

    }

    public class MoversDescriptor {
        Historical.Movers movers;
        private Interval moversInterval;
        private List<MetricsDiffSummary> positiveMovers;
        private List<MetricsDiffSummary> negativeMovers;

        public MoversDescriptor() {

        }

        public MoversDescriptor(Historical.Movers movers) {
            this.movers = movers;
        }

        public void gatherMovers() throws Exception {
            // find the interval to use to calculate movers
            final Interval requested = movers.getInterval();
            final Long firstTS = getFirstTSAfter(newLinkedList(models.keySet()), lastTS, requested);
            moversInterval = calcActualInterval(lastTS, firstTS, requested);
            HasMetrics fromMetrics = HistoricalSupport.getFullMetrics((HistoricalSupport.HasMetricsWrapper) models.get(firstTS),
                    getPackage());
            Column column = movers.getColumn();
            List<MetricsDiffSummary> moverClasses = getMoverClasses(fromMetrics, column);

            if (column.getFormat() instanceof ColumnFormat.PercentageColumnFormat) {
                movers.setMaxWidth(100);
            } else {
                movers.setMaxWidth(column.getNumber().intValue());
            }

            int lastMover = moverClasses.size() - 1;
            int range = movers.getRange();

            if (moverClasses.size() > 0) {
                positiveMovers = getPositiveMovers(moverClasses, lastMover, range);
                negativeMovers = getNegativeMovers(moverClasses, range);
            } else {
                positiveMovers = newArrayList();
                negativeMovers = newArrayList();
            }
        }

        protected List<MetricsDiffSummary> getNegativeMovers(
                final List<MetricsDiffSummary> moverClasses,
                final int range) {
            MetricsDiffSummary diff = moverClasses.get(0);
            final List<MetricsDiffSummary> negativeMovers = newArrayList();
            if (diff.getPcDiff() < 0) {
                int i = 0;
                final Iterator<MetricsDiffSummary> bottom = moverClasses.iterator();
                while (i < range && bottom.hasNext()) {
                    diff = bottom.next();
                    if (diff.getPcDiff() >= 0) {
                        break;
                    }

                    negativeMovers.add(diff);
                    i++;
                }
            }
            return negativeMovers;
        }

        protected List<MetricsDiffSummary> getPositiveMovers(
                final List<MetricsDiffSummary> moverClasses,
                int lastMover, int range) {
            return getPositiveMoversInner(moverClasses, lastMover, range, false);
        }

        protected List<MetricsDiffSummary> getPositiveMoversInner(
                final List<MetricsDiffSummary> moverClasses,
                int lastMover, int range, boolean includeZero) {
            MetricsDiffSummary diff = moverClasses.get(lastMover);
            final List<MetricsDiffSummary> positiveMovers = newArrayList();
            if (diff.getPcDiff() > 0 || (includeZero && diff.getPcDiff() == 0)) {
                int i = 0;
                ListIterator<MetricsDiffSummary> top = moverClasses.listIterator(lastMover + 1);
                while (i < range && top.hasPrevious()) {
                    diff = top.previous();
                    if (diff.getPcDiff() < 0 || (!includeZero && diff.getPcDiff() == 0)) {
                        break;
                    }
                    positiveMovers.add(diff);
                    i++;
                }
            }
            return positiveMovers;
        }

        protected List<MetricsDiffSummary> getMoverClasses(HasMetrics fromMetrics, Column column)
                throws CloverException {
            return HistoricalSupport.getClassesMetricsDifference(
                    fromMetrics, subjectMetrics, movers.getThreshold(), column, true);
        }

        private Interval calcActualInterval(Long lastTS, Long firstTS, Interval requested) {
            Interval actual = new Interval((lastTS - firstTS) / 1000, Interval.UNIT_SECOND);
            if (requested != null && !actual.equals(requested)) {
                log.info("movers interval adjusted to " + actual.toSensibleString());
            } else {
                log.info("using movers interval of " + actual.toSensibleString());
            }

            return actual;
        }

        public Interval getRequestedInterval() {
            if (showMovers()) {
                Interval interval = movers.getInterval();
                if (interval == null) {
                    // user hasn't requested an interval
                    // so return the actual interval that will be used.
                    interval = getActualInterval();
                }
                return interval;
            } else {
                return null;
            }
        }

        public int getMaxWidth() {
            return movers.getMaxWidth();
        }

        public List<MetricsDiffSummary> getGainers() {
            return positiveMovers;
        }

        public List<MetricsDiffSummary> getLosers() {
            return negativeMovers;
        }

        public Interval getActualInterval() {
            return moversInterval;
        }

        public Interval getInterval() {
            return movers.getInterval();
        }

        public Percentage getThreshold() {
            return movers.getThreshold();
        }

        public Column getColumn() {
            return movers.getColumn();
        }

        public int getRange() {
            return movers.getRange();
        }
    }
}
