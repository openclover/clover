package org.openclover.core.registry.entities;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static org.openclover.core.util.Lists.newArrayList;

public class StackTraceInfo {


    private List<TraceEntry> entries = newArrayList();
    private TestCaseInfo originatingTest;

    public StackTraceInfo(TestCaseInfo originatingTest, String fullTrace) {

        this.originatingTest = originatingTest;

        LineNumberReader lineReader = new LineNumberReader(new StringReader(fullTrace));
        try {
            String line = lineReader.readLine();
            TraceEntry prev = null;
            int id = 0;
            while (line != null) {
                TraceEntry cur = new TraceEntry(this, id++, prev, line);
                entries.add(cur);
                if (prev != null) {
                    prev.setDown(cur);
                }
                prev = cur;
                line = lineReader.readLine();
            }
        } catch (IOException e) {
            // will never occur
        }
    }

    public TestCaseInfo getOriginatingTest() {
        return originatingTest;
    }

    public void setOriginatingTest(TestCaseInfo originatingTest) {
        this.originatingTest = originatingTest;
    }

    public void resolve(FullProjectInfo proj) {
        for (TraceEntry traceEntry : entries) {
            traceEntry.resolve(proj);
        }
    }

    public List getEntries() {
        return entries; 
    }

    public static class TraceEntry {


        private static final String FILE_REGEXP = "[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*\\.java:([0-9]+)";
        private static final Pattern TRACE_LINE_PATTERN = Pattern.compile("((([\\p{Alpha}\\$_][\\p{Alnum}\\$_]*\\.)*[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*)\\.(?:\\<)?[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*(?:\\>)?)\\((" + FILE_REGEXP + "|Unknown Source)\\)");
        private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(FILE_REGEXP);


        private StackTraceInfo parentTrace;
        private int id; //local only to the parent trace
        private String line;
        private TraceEntry up;
        private TraceEntry down;

        // these are possibly filled upon resolve(p)
        private WeakReference<FullFileInfo> containingFile = new WeakReference<>(null);
        private int lineNum = -1;
        private String linePrefix;
        private String linkableLineSegment;

        public TraceEntry(StackTraceInfo parentTrace, int id, TraceEntry up, String line) {
            this.parentTrace = parentTrace;
            this.id = id;
            this.up = up;
            this.line = line;
        }

        public StackTraceInfo getParentTrace() {
            return parentTrace;
        }

        public int getId() {
            return id;
        }

        public String getLine() {
            return line;
        }

        public String getLinePrefix() {
            return linePrefix;
        }

        public String getLinkableLineSegment() {
            return linkableLineSegment;
        }

        public TraceEntry getUp() {
            return up;
        }

        public TraceEntry getDown() {
            return down;
        }

        public void setDown(TraceEntry down) {
            this.down = down;
        }

        public FullFileInfo getContainingFile() {
            return containingFile.get();
        }

        public int getLineNum() {
            return lineNum;
        }

        public boolean isResolved() {
            return containingFile.get() != null;
        }

        public boolean resolve(FullProjectInfo proj) {
            Matcher matcher = TRACE_LINE_PATTERN.matcher(line);
            boolean resolved = false;
            if (matcher.find()) {
                linePrefix = line.substring(0,matcher.start());
                linkableLineSegment = line.substring(matcher.start());
                String fqcn = matcher.group(2).replace('$','.');
                FullClassInfo clazz = (FullClassInfo)proj.findClass(fqcn);

                if (clazz != null) {
                    final FullFileInfo fileInfo = (FullFileInfo)clazz.getContainingFile();
                    containingFile = new WeakReference<>(fileInfo);
                    String lineStr = matcher.group(4);
                    if (LINE_NUMBER_PATTERN.matcher(lineStr).matches()) {
                        lineNum = Integer.parseInt(matcher.group(5));
                        fileInfo.addFailStackEntry(lineNum, this);
                    }
                    resolved = true;
                }
            }
            return resolved;
        }
    }



}
