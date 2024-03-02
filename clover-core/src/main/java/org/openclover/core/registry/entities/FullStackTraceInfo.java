package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.StackTraceEntry;
import org.openclover.core.api.registry.StackTraceInfo;
import org.openclover.core.api.registry.TestCaseInfo;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.core.util.Lists.newArrayList;

public class FullStackTraceInfo implements StackTraceInfo {

    private final List<StackTraceEntry> entries = newArrayList();
    private TestCaseInfo originatingTest;

    public FullStackTraceInfo(TestCaseInfo originatingTest, String fullTrace) {

        this.originatingTest = originatingTest;

        LineNumberReader lineReader = new LineNumberReader(new StringReader(fullTrace));
        try {
            String line = lineReader.readLine();
            StackTraceEntry prev = null;
            int id = 0;
            while (line != null) {
                StackTraceEntry cur = new StackTraceEntryImpl(this, id++, prev, line);
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

    @Override
    public TestCaseInfo getOriginatingTest() {
        return originatingTest;
    }

    @Override
    public void setOriginatingTest(TestCaseInfo originatingTest) {
        this.originatingTest = originatingTest;
    }

    @Override
    public void resolve(ProjectInfo proj) {
        for (StackTraceEntry stackTraceEntry : entries) {
            stackTraceEntry.resolve(proj);
        }
    }

    @Override
    public List<StackTraceEntry> getEntries() {
        return entries; 
    }

    public static class StackTraceEntryImpl implements StackTraceEntry {


        private static final String FILE_REGEXP = "[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*\\.java:([0-9]+)";
        private static final Pattern TRACE_LINE_PATTERN = Pattern.compile("((([\\p{Alpha}\\$_][\\p{Alnum}\\$_]*\\.)*[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*)\\.(?:\\<)?[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*(?:\\>)?)\\((" + FILE_REGEXP + "|Unknown Source)\\)");
        private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(FILE_REGEXP);


        private StackTraceInfo parentTrace;
        private int id; //local only to the parent trace
        private String line;
        private StackTraceEntry up;
        private StackTraceEntry down;

        // these are possibly filled upon resolve(p)
        private WeakReference<FullFileInfo> containingFile = new WeakReference<>(null);
        private int lineNum = -1;
        private String linePrefix;
        private String linkableLineSegment;

        public StackTraceEntryImpl(StackTraceInfo parentTrace, int id, StackTraceEntry up, String line) {
            this.parentTrace = parentTrace;
            this.id = id;
            this.up = up;
            this.line = line;
        }

        @Override
        public StackTraceInfo getParentTrace() {
            return parentTrace;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getLine() {
            return line;
        }

        @Override
        public String getLinePrefix() {
            return linePrefix;
        }

        @Override
        public String getLinkableLineSegment() {
            return linkableLineSegment;
        }

        @Override
        public StackTraceEntry getUp() {
            return up;
        }

        @Override
        public StackTraceEntry getDown() {
            return down;
        }

        @Override
        public void setDown(StackTraceEntry down) {
            this.down = down;
        }

        @Override
        public FullFileInfo getContainingFile() {
            return containingFile.get();
        }

        @Override
        public int getLineNum() {
            return lineNum;
        }

        @Override
        public boolean isResolved() {
            return containingFile.get() != null;
        }

        @Override
        public boolean resolve(ProjectInfo proj) {
            Matcher matcher = TRACE_LINE_PATTERN.matcher(line);
            boolean resolved = false;
            if (matcher.find()) {
                linePrefix = line.substring(0,matcher.start());
                linkableLineSegment = line.substring(matcher.start());
                String fqcn = matcher.group(2).replace('$','.');
                ClassInfo clazz = proj.findClass(fqcn);

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
