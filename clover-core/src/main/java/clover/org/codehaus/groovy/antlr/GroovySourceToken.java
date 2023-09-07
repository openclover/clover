package clover.org.codehaus.groovy.antlr;

import clover.antlr.Token;

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

/**
 * This is a Token sub class to track line information
 *
 * @author Jochen Theodorou
 */
public class GroovySourceToken extends Token implements SourceInfo{
    protected int line;
    protected String text = "";
    protected int col;
    protected int lineLast;
    protected int colLast;


    /**
     * Constructor using a token type
     *
     * @param t the type
     */
    public GroovySourceToken(int t) {
        super(t);
    }

    @Override
    public int getLine() {
        return line;
    }

    /**
     * get the source token text
     * @return the source token text
     */
    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setLine(int l) {
        line = l;
    }

    /**
     * set the source token text
     * @param s the text
     */
    @Override
    public void setText(String s) {
        text = s;
    }

    public String toString() {
        return
            "[\"" + getText() + "\",<" + type + ">,"+
            "line=" + line + ",col=" + col +
            ",lineLast=" + lineLast + ",colLast=" + colLast +
            "]";
    }

    @Override
    public int getColumn() {
        return col;
    }

    @Override
    public void setColumn(int c) {
        col = c;
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
}
