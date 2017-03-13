package com.atlassian.clover.spi.reporters.html.source;

import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.StackTraceInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;

import java.util.Collections;
import java.util.List;

public class LineRenderInfo {
    private boolean hilight;

    private FullClassInfo classStart;
    private MethodInfo methodStart;
    private boolean filtered = false;
    private StackTraceInfo.TraceEntry[] failedStackEntries;
    private List<TestCaseInfo> testHits = Collections.emptyList();

    private String coverageStr = "";
    private String msg = "";
    private String src = "";

    private String lineNumberCSS = SourceReportCss.LINE_COUNT_CLASS + " "  + SourceReportCss.NO_HILIGHT_CLASS;
    private String coverageCountCSS = SourceReportCss.COVERAGE_COUNT_CLASS + " " + SourceReportCss.NO_HILIGHT_CLASS;
    private String sourceCSS = SourceReportCss.SRC_LINE_CLASS;
    private String testHitCSS = "";

    public LineRenderInfo() {
        this("");
    }
    
    public LineRenderInfo(String coverageStr) {
        this.coverageStr = coverageStr;
    }

    public void setHilight(boolean hilight) {
        this.hilight = hilight;
    }

    public void setClassStart(FullClassInfo classStart) {
        this.classStart = classStart;
    }

    public void setMethodStart(MethodInfo methodStart) {
        this.methodStart = methodStart;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public void setFailedStackEntries(StackTraceInfo.TraceEntry[] failedStackEntries) {
        this.failedStackEntries = failedStackEntries;
    }

    public void setTestHits(List<TestCaseInfo> testHits) {
        this.testHits = testHits;
    }

    public void setCoverageStr(String coverageStr) {
        this.coverageStr = coverageStr;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setLineNumberCSS(String lineNumberCSS) {
        this.lineNumberCSS = lineNumberCSS;
    }

    public void setCoverageCountCSS(String coverageCountCSS) {
        this.coverageCountCSS = coverageCountCSS;
    }

    public void setSourceCSS(String sourceCSS) {
        this.sourceCSS = sourceCSS;
    }

    public void setTestHitCSS(String testHitCSS) {
        this.testHitCSS = testHitCSS;
    }

    public boolean getHilight() {
        return hilight;
    }

    public String getCoverageStr() {
        return coverageStr;
    }

    public String getMsg() {
        return msg;
    }

    public FullClassInfo getClassStart() {
        return classStart;
    }

    public MethodInfo getMethodStart() {
        return methodStart;
    }

    public StackTraceInfo.TraceEntry[] getFailedStackEntries() {
        return failedStackEntries;
    }

    public String getSrc() {
        return src;
    }

    public String getLineNumberCSS() {
        return lineNumberCSS;
    }

    public String getCoverageCountCSS() {
        return coverageCountCSS;
    }

    public String getSourceCSS() {
        return sourceCSS;
    }

    public String getTestHitCSS() {
        return testHitCSS;
    }

    public List<TestCaseInfo> getTestHits() {
        return testHits;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public boolean isHilight() {
        return hilight;
    }
}
