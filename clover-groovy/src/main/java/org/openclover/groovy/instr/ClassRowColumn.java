package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;

class ClassRowColumn {
    ClassNode classRef;
    int row;
    int column;

    private ClassRowColumn(ClassNode clazz, int row, int column) {
        this.classRef = clazz;
        this.row = row;
        this.column = column;
    }

    static ClassRowColumn of(ClassNode clazz, int row, int column) {
        return new ClassRowColumn(clazz, row, column);
    }
}
