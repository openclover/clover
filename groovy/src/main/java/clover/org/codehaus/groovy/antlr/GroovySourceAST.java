/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package clover.org.codehaus.groovy.antlr;

import clover.antlr.CommonAST;
import clover.antlr.Token;
import clover.antlr.collections.AST;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

/**
 * We have an AST subclass so we can track source information.
 * Very odd that ANTLR doesn't do this by default.
 *
 * @author Mike Spille
 * @author Jeremy Rayner groovy@ross-rayner.com
 */
public class GroovySourceAST extends CommonAST implements Comparable, SourceInfo {
    private int line;
    private int col;
    private int lineLast;
    private int colLast;
    private String snippet;

    public GroovySourceAST() {
    }

    public GroovySourceAST(Token t) {
        super(t);
    }

    @Override
    public void initialize(AST ast) {
        super.initialize(ast);
        line = ast.getLine();
        col = ast.getColumn();
        if (ast instanceof GroovySourceAST) {
        	GroovySourceAST node = (GroovySourceAST)ast;
        	lineLast = node.getLineLast();
            colLast = node.getColumnLast();
        }
    }

    @Override
    public void initialize(Token t) {
        super.initialize(t);
        line = t.getLine();
        col = t.getColumn();
        if (t instanceof SourceInfo) {
            SourceInfo info = (SourceInfo) t;
            lineLast = info.getLineLast();
            colLast  = info.getColumnLast();
        }
    }

    public void setLast(Token last) {
        lineLast = last.getLine();
        colLast = last.getColumn();
    }

    @Override
    public int getLineLast() {
        return lineLast;
    }

    @Override
    public void setLineLast(int lineLast) {
        this.lineLast = lineLast;
    }

    @Override
    public int getColumnLast() {
        return colLast;
    }

    @Override
    public void setColumnLast(int colLast) {
        this.colLast = colLast;
    }

    @Override
    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public int getLine() {
        return (line);
    }

    @Override
    public void setColumn(int column) {
        this.col = column;
    }

    @Override
    public int getColumn() {
        return (col);
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public int compareTo(Object object) {
        if (object == null) {
            return 0;
        }
        if (!(object instanceof AST)) {
            return 0;
        }
        AST that = (AST) object;

        if (this.getLine() < that.getLine()) {
            return -1;
        }
        if (this.getLine() > that.getLine()) {
            return 1;
        }

        return Integer.compare(this.getColumn(), that.getColumn());
    }

    public GroovySourceAST childAt(int position) {
        List list = newArrayList();
        AST child = this.getFirstChild();
        while (child != null) {
            list.add(child);
            child = child.getNextSibling();
        }
        try {
            return (GroovySourceAST)list.get(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public GroovySourceAST childOfType(int type) {
        AST child = this.getFirstChild();
        while (child != null) {
            if (child.getType() == type) { return (GroovySourceAST)child; }
            child = child.getNextSibling();
        }
        return null;
    }

    public List<GroovySourceAST> childrenOfType(int type) {
        List<GroovySourceAST> result = newArrayList();
        AST child = this.getFirstChild();
        while (child != null) {
            if (child.getType() == type) { result.add((GroovySourceAST) child); }
            child = child.getNextSibling();
        }
        return result;
    }

}
