package org.openclover.functest.ant.testutils

import groovy.transform.CompileStatic
import org.openclover.core.api.registry.SourceInfo

@CompileStatic
class MutableSourceRegion implements SourceInfo {
    protected int startLine
    protected int startColumn
    protected int endLine
    protected int endColumn

    MutableSourceRegion(int startLine, int startColumn) {
        this.startLine = startLine
        this.startColumn = startColumn
        this.endLine = endLine
        this.endColumn = endColumn
    }

    int getStartLine() {
        return startLine
    }

    int getStartColumn() {
        return startColumn
    }

    int getEndLine() {
        return endLine
    }

    int getEndColumn() {
        return endColumn
    }

    void setStartColumn(int startColumn) {
        this.startColumn = startColumn
    }

    void setStartLine(int startLine) {
        this.startLine = startLine
    }

    void setEndLine(int endLine) {
        this.endLine = endLine
    }

    void setEndColumn(int endColumn) {
        this.endColumn = endColumn
    }

    @Override
    boolean equals(Object o) {
        if (this == o) return true
        if (o == null || getClass() != o.getClass()) return false

        MutableSourceRegion that = (MutableSourceRegion)o

        if (endColumn != that.endColumn) return false
        if (endLine != that.endLine) return false
        if (startColumn != that.startColumn) return false
        if (startLine != that.startLine) return false

        return true
    }

    @Override
    int hashCode() {
        int result = startLine
        result = 31 * result + startColumn
        result = 31 * result + endLine
        result = 31 * result + endColumn
        return result
    }
}
