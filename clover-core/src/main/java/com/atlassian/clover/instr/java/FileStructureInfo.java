package com.atlassian.clover.instr.java;

import com.atlassian.clover.api.registry.MethodInfo;

import java.io.File;
import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;

/**
 *
 */
public class FileStructureInfo {


    private String packageName = "";
    private File file = null;
    private boolean suppressFallthroughWarnings = false;
    private List<Marker> statements = newLinkedList();
    private List<MethodMarker> methods = newLinkedList();


    public FileStructureInfo(File file) {
        this.file = file;
    }

    public static class Marker {
        private CloverToken start;
        private CloverToken end;
        private String normalisedString;

        public Marker(CloverToken start, CloverToken end) {
            this.start = start;
            this.end = end;
        }

        public CloverToken getStart() {
            return start;
        }

        public void setStart(CloverToken start) {
            this.start = start;
        }

        public CloverToken getEnd() {
            return end;
        }

        public void setEnd(CloverToken end) {
            this.end = end;
        }

        public String getNormalisedString() {
            if (normalisedString == null) {
                normalisedString = TokenListUtil.getNormalisedSequence(getStart(), getEnd());
            }
            return normalisedString;
        }
    }

    public static class MethodMarker extends Marker {


        private CloverToken endSig;
        private String normalisedSignature;
        private MethodEntryInstrEmitter entryEmitter;

        public MethodMarker(MethodEntryInstrEmitter entryEmitter, CloverToken start, CloverToken endSig, CloverToken end) {
            super(start,end);
            this.entryEmitter = entryEmitter;
            this.endSig = endSig;
        }

        public MethodInfo getMethod() {
            return entryEmitter.getMethod();
        }

        public CloverToken getEndSig() {
            return endSig;
        }

        public void setEndSig(CloverToken endSig) {
            this.endSig = endSig;
        }

        public String getNormalisedSignature() {
            if (normalisedSignature == null) {
                normalisedSignature = TokenListUtil.getNormalisedSequence(getStart(), getEndSig());
            }
            return normalisedSignature;
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public File getFile() {
        return file;
    }

    public boolean isSuppressFallthroughWarnings() {
        return suppressFallthroughWarnings;
    }

    public void setSuppressFallthroughWarnings(boolean suppressFallthroughWarnings) {
        this.suppressFallthroughWarnings = suppressFallthroughWarnings;
    }

    public void addMethodMarker(MethodEntryInstrEmitter method, CloverToken start, CloverToken endSig, CloverToken end) {
        methods.add(new MethodMarker(method, start, endSig, end));
    }

    public int getNumMethodMarkers() {
        return methods.size();
    }

    public MethodMarker getMethodMarker(int i) {
        return methods.get(i);
    }

    public int getNumStatementMarkers() {
        return statements.size();
    }

    public Marker getStatementMarker(int i) {
        return statements.get(i);
    }

    public void addStatementMarker(CloverToken start, CloverToken end) {
        statements.add(new Marker(start, end));
    }
}
