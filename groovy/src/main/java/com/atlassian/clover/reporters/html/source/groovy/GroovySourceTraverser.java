package com.atlassian.clover.reporters.html.source.groovy;

import clover.org.codehaus.groovy.antlr.GroovySourceToken;
import clover.org.codehaus.groovy.antlr.parser.GroovyLexer;
import clover.org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import clover.antlr.TokenStream;
import com.atlassian.clover.Logger;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.reporters.html.source.SourceListener;
import com.atlassian.clover.reporters.html.source.SourceRenderHelper;
import com.atlassian.clover.reporters.html.source.SourceTraverser;
import com.atlassian.clover.reporters.html.source.java.JavaSourceListener;
import com.atlassian.clover.reporters.html.source.java.JavaTokenTraverser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroovySourceTraverser implements SourceTraverser {
    static List<Integer> KEYWORDS = Arrays.asList(
            GroovyTokenTypes.FINAL,
            GroovyTokenTypes.ABSTRACT,
            GroovyTokenTypes.LITERAL_package,
            GroovyTokenTypes.LITERAL_import,
            GroovyTokenTypes.LITERAL_static,
            GroovyTokenTypes.LITERAL_def,
            GroovyTokenTypes.LITERAL_class,
            GroovyTokenTypes.LITERAL_interface,
            GroovyTokenTypes.LITERAL_enum,
            GroovyTokenTypes.LITERAL_extends,
            GroovyTokenTypes.LITERAL_super,
            GroovyTokenTypes.LITERAL_void,
            GroovyTokenTypes.LITERAL_boolean,
            GroovyTokenTypes.LITERAL_byte,
            GroovyTokenTypes.LITERAL_char,
            GroovyTokenTypes.LITERAL_short,
            GroovyTokenTypes.LITERAL_int,
            GroovyTokenTypes.LITERAL_float,
            GroovyTokenTypes.LITERAL_long,
            GroovyTokenTypes.LITERAL_double,
            GroovyTokenTypes.LITERAL_as,
            GroovyTokenTypes.LITERAL_private,
            GroovyTokenTypes.LITERAL_public,
            GroovyTokenTypes.LITERAL_protected,
            GroovyTokenTypes.LITERAL_transient,
            GroovyTokenTypes.LITERAL_native,
            GroovyTokenTypes.LITERAL_threadsafe,
            GroovyTokenTypes.LITERAL_synchronized,
            GroovyTokenTypes.LITERAL_volatile,
            GroovyTokenTypes.LITERAL_default,
            GroovyTokenTypes.LITERAL_throws,
            GroovyTokenTypes.LITERAL_implements,
            GroovyTokenTypes.LITERAL_this,
            GroovyTokenTypes.LITERAL_if,
            GroovyTokenTypes.LITERAL_else,
            GroovyTokenTypes.LITERAL_while,
            GroovyTokenTypes.LITERAL_switch,
            GroovyTokenTypes.LITERAL_for,
            GroovyTokenTypes.LITERAL_in,
            GroovyTokenTypes.LITERAL_return,
            GroovyTokenTypes.LITERAL_break,
            GroovyTokenTypes.LITERAL_continue,
            GroovyTokenTypes.LITERAL_throw,
            GroovyTokenTypes.LITERAL_assert,
            GroovyTokenTypes.LITERAL_case,
            GroovyTokenTypes.LITERAL_try,
            GroovyTokenTypes.LITERAL_finally,
            GroovyTokenTypes.LITERAL_catch,
            GroovyTokenTypes.LITERAL_false,
            GroovyTokenTypes.LITERAL_new,
            GroovyTokenTypes.LITERAL_null,
            GroovyTokenTypes.LITERAL_true);

    /**
     * Traverses Groovy code token by token informing the listener on certain interesting elements.
     * The Groovy UnicodeEscapingReader is *not* used because that can harm line and column numbers
     * Textual values from the lexer are never used because these are often unescaped and won't match the original source
     * (e.g. \u4f60\u597d - 12 chars - will be rendered as ?? - 4 chars in UTF-8).
     * Instead textual values are read from the original unadulterated source using the line and column values from the tokens.
     */
    @Override
    public void traverse(Reader reader, FullFileInfo fileInfo, SourceListener sourceListener) throws Exception {
        final GroovySourceListener listener = (GroovySourceListener)sourceListener;
        List<String> lines = SourceRenderHelper.getSrcLines(fileInfo);
        //Add a single trailing single space line - final lines with newlines cause the last token to
        //to have a line number that is 1 greater than the actual number of lines 
        lines.add(" ");

        final GroovyLexer lexer = new GroovyLexer(reader);
        lexer.setWhitespaceIncluded(true);
        //Important! plumb() is required so we don't trip over string expressions ie STRING_CTOR_START -> STRING_CTOR_END
        final TokenStream stream = lexer.plumb();

        GroovySourceToken currToken = (GroovySourceToken) stream.nextToken();
        GroovySourceToken prevToken = currToken;
        GroovySourceToken firstToken = currToken;

        StringBuilder accumName = new StringBuilder();
        boolean gatherPkgIdent = false;
        boolean gatherImportIdent = false;

        listener.onStartDocument();

        while (prevToken != null && GroovyTokenTypes.EOF != prevToken.getType()) {
            //End of the token stream or a change in a new sequence of similar tokens
            if (currToken == null || currToken.getType() != prevToken.getType()) {
                String[] fragLines = getLinesFor(firstToken, prevToken, lines);
                //String literals will either be strings (staring with ', ''', " or """) or regular expressions (staring with '/')
                if (GroovyTokenTypes.STRING_LITERAL == prevToken.getType()) {
                    if (fragLines[0].charAt(0) == '/') {
                        splitNewlinesAnd(fragLines, listener, new Closure<String>() { @Override
                                                                                      public void perform(String chunk) { listener.onRegexp(chunk); } });
                    } else {
                        splitNewlinesAnd(fragLines, listener, new Closure<String>() { @Override
                                                                                      public void perform(String chunk) { listener.onStringLiteral(chunk); } });
                    }
                } else if (GroovyTokenTypes.STRING_CTOR_START == prevToken.getType()
                           || GroovyTokenTypes.STRING_CTOR_END == prevToken.getType()
                           || GroovyTokenTypes.STRING_CTOR_MIDDLE == prevToken.getType()) {
                    //A string ctor is a Groovy string expression e.g. "1 + 1 = ${1 + 1}"
                    //The string ctor start is the first non-expression part of the string
                    //The string ctor middle is any non-expression part in between expression parts
                    //The string ctor end is the final non-expression part
                    splitNewlinesAnd(fragLines, listener, new Closure<String>() { @Override
                                                                                  public void perform(String chunk) { listener.onStringLiteral(chunk); }});
                } else if (GroovyTokenTypes.SL_COMMENT == prevToken.getType()) {
                    //A single line comment can't have newlines
                    listener.onCommentChunk(fragLines[0]);
                } else if (GroovyTokenTypes.ML_COMMENT == prevToken.getType()) {
                    //Javadoc tags and multilines all taken care of
                    JavaTokenTraverser.processComment(join("\n", fragLines), listener);
                } else if (GroovyTokenTypes.NLS == prevToken.getType()) {
                    //A > 1 sequence of newline tokens will require a number of newlines to keep line numbering correct
                    final int times = Math.max(0, prevToken.getLineLast() - firstToken.getLine());
                    for(int i = 0; i < times; i++) {
                        listener.onNewLine();
                    }
                } else if (KEYWORDS.contains(prevToken.getType())) {
                    //A sequence of keywords will always each be separated by whitespace or NLS
                    listener.onKeyword(fragLines[0]);
                    //If an import or package statement, start listening for what comes after to hyperlink if possible
                    gatherPkgIdent = (prevToken.getType() == GroovyTokenTypes.LITERAL_package);
                    gatherImportIdent = (prevToken.getType() == GroovyTokenTypes.LITERAL_import);
                } else {
                    String fragment = join("\n", fragLines);
                    if (gatherPkgIdent || gatherImportIdent) {
                        if (GroovyTokenTypes.SEMI == prevToken.getType() || GroovyTokenTypes.STAR == prevToken.getType()) {
                            if (gatherImportIdent) {
                                listener.onImport(accumName.toString().replace("\\s", ""));
                            }
                            accumName = new StringBuilder();
                            gatherPkgIdent = false;
                            gatherImportIdent = false;
                            listener.onChunk(fragment);
                        } else if (GroovyTokenTypes.DOT == prevToken.getType()) {
                            accumName.append(fragment);
                            listener.onChunk(fragment);
                        } else if (GroovyTokenTypes.IDENT == prevToken.getType()) {
                            accumName.append(fragment);
                            String nameWithoutWhitespace = accumName.toString().replace("\\s", "");
                            if (gatherPkgIdent) {
                                listener.onPackageSegment(nameWithoutWhitespace, fragment);
                            } else {
                                listener.onImportSegment(nameWithoutWhitespace, fragment);
                            }
                        } else {
                            listener.onChunk(fragment);
                        }
                    } else if (GroovyTokenTypes.IDENT == prevToken.getType()) {
                        //An ident can't be split across lines
                        listener.onIdentifier(fragment);
                    } else {
                        //Whitespace, curlies etc
                        splitNewlinesAnd(fragLines, listener, new Closure<String>() { @Override
                                                                                      public void perform(String chunk) { listener.onChunk(chunk); } });
                    }
                }
                firstToken = currToken;
            }
            prevToken = currToken;
            currToken = (GroovySourceToken) stream.nextToken();
        }
        listener.onEndDocument();
    }

    private String join(String joiner, String[] lines) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            buffer.append(lines[i]);
            if (i != lines.length - 1) { 
                buffer.append(joiner);
            }
        }
        return buffer.toString();
    }

    private static void splitNewlinesAnd(String[] lines, JavaSourceListener listener, Closure<String> c) {
        int lineCount = lines.length;
        for(int i = 0; i < lineCount; i++) {
            c.perform(lines[i]);
            if (i < lineCount - 1) {
                listener.onNewLine();
            }
        }
    }

    private String[] getLinesFor(GroovySourceToken first, GroovySourceToken last, List<String> allLines) {
        try {
            List<String> lines = new ArrayList<>(allLines.subList(first.getLine() - 1, last.getLineLast()));
            String firstLine = lines.get(0);
            if (lines.size() == 1) {
                lines.set(0, firstLine.substring(first.getColumn()- 1, last.getColumnLast() - 1));
            } else {
                String lastLine = lines.get(lines.size() - 1);
                lines.set(0, firstLine.substring(first.getColumn() - 1, firstLine.length()));
                lines.set(lines.size() - 1, lastLine.substring(0, last.getColumnLast() - 1));
            }
            return lines.toArray(new String[0]);
        } catch (Exception e) {
            Logger.getInstance().verbose("Failed to grab lines for tokens", e);
            Logger.getInstance().debug("First token: " + first);
            Logger.getInstance().debug("Last token: " + last);
            Logger.getInstance().debug("Lines.size: " + allLines.size());
            throw e;
        }
    }

    private interface Closure<T> {
        void perform(T t);
    }
}