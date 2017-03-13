package com.atlassian.clover.eclipse.core.reports.reporters;

import com.atlassian.clover.reporters.html.HtmlReporter;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.Notification;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;

public abstract class MemoryDebugReporter {
    private static final double PERCENTAGE_MEM_THRESHOLD = 0.95d;
    private static final int ONE_MEG = 1024 * 1024;

    public static void mainImpl(MemoryDebugReporter reporter, String[] args) {
        try {
            reporter.runReport(args);
        } finally {
            reporter.stopMemoryDebugging();
        }
    }

    private static MemoryPoolMXBean findTenuredGenPool() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
                return pool;
            }
        }
        System.err.println("Could not find tenured space to report on");
        return null;
    }

    private ScheduledExecutorService reporterService;

    public MemoryDebugReporter() {
        final NotificationEmitter emitter = (NotificationEmitter)ManagementFactory.getMemoryMXBean();
        final MemoryPoolMXBean tenuredGenPool = findTenuredGenPool();
        if (emitter != null && tenuredGenPool != null) {
            long maxMemory = tenuredGenPool.getUsage().getMax();
            long warningThreshold = (long) (maxMemory * PERCENTAGE_MEM_THRESHOLD);
            tenuredGenPool.setUsageThreshold(warningThreshold);

            emitter.addNotificationListener(new NotificationListener() {
                @Override
                public void handleNotification(Notification notification, Object hb) {
                    if (notification.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                        long maxMemory = tenuredGenPool.getUsage().getMax();
                        long usedMemory = tenuredGenPool.getUsage().getUsed();
                        System.out.println(
                            "LOW MEMORY WARNING - " + tenuredMemoryDescription(usedMemory, maxMemory));
                        System.out.println("Stack traces for running threads:");
                        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                            Thread thread = entry.getKey();
                            System.out.println(thread);
                            StackTraceElement[] elements = entry.getValue();
                            for (StackTraceElement element : elements) {
                                System.out.println("at " + element);
                            }
                            System.out.println();
                        }
                    }
                }
            }, null, null);

            reporterService = Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
            reporterService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    long maxMemory = tenuredGenPool.getUsage().getMax();
                    long usedMemory = tenuredGenPool.getUsage().getUsed();
                    System.out.println(tenuredMemoryDescription(usedMemory, maxMemory));
            }}, 5, 5, TimeUnit.SECONDS);
        } else {
            System.err.println("Unable to find memory MX bean or memory pool MX bean or both");
        }
    }

    private String tenuredMemoryDescription(long usedMemory, long maxMemory) {
        return "Tenured generation: [" + (int)(usedMemory / ONE_MEG) + "m used out of " + (int)(maxMemory / ONE_MEG) + "m]";
    }

    public abstract void runReport(String[] args);

    private void stopMemoryDebugging() {
        reporterService.shutdown();
    }

    public final static class HTMLReporter extends MemoryDebugReporter {
        @Override
        public void runReport(String[] args) {
            HtmlReporter.main(args);
        }

        public static void main(String[] args) {
            MemoryDebugReporter.mainImpl(new HTMLReporter(), args);
        }
    }

    public final static class XMLReporter extends MemoryDebugReporter {
        @Override
        public void runReport(String[] args) {
            com.atlassian.clover.reporters.xml.XMLReporter.main(args);
        }

        public static void main(String[] args) {
            MemoryDebugReporter.mainImpl(new XMLReporter(), args);
        }
    }

    public final static class PDFReporter extends MemoryDebugReporter {
        @Override
        public void runReport(String[] args) {
            com.atlassian.clover.reporters.pdf.PDFReporter.main(args);
        }

        public static void main(String[] args) {
            MemoryDebugReporter.mainImpl(new PDFReporter(), args);
        }
    }
}

