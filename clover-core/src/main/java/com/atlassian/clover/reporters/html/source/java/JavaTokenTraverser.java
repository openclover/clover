package com.atlassian.clover.reporters.html.source.java;

import clover.antlr.Token;
import clover.antlr.TokenStreamException;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.instr.java.JavaLexer;
import com.atlassian.clover.instr.java.JavaTokenTypes;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.reporters.html.source.SourceTraverser;

import java.io.Reader;

/**
 * Traverses a Java token stream, informing a JavaSourceListener about
 * the interesting aspects of it - new lines, comments, keywords, identifiers, string literals, imports, packages
 */
public final class JavaTokenTraverser implements SourceTraverser<JavaSourceListener> {
    @Override
    public void traverse(Reader sourceReader, FullFileInfo fileInfo, JavaSourceListener listener) throws TokenStreamException {
        JavaLexer lexer = new JavaLexer(sourceReader, new JavaInstrumentationConfig());

        Token token = lexer.nextToken();
        Token prev = token;
        StringBuilder currentChunk = new StringBuilder();
        StringBuilder accumName = new StringBuilder();
        boolean gatherPkgIdent = false;
        boolean gatherImportIdent = false;

        listener.onStartDocument();
        while (prev != null && JavaTokenTypes.EOF != prev.getType()) {
           if (token != null && token.getType() == prev.getType()) {
                currentChunk.append(token.getText());
            } else {
                // render the previous chunk
                if (JavaTokenTypes.WS == prev.getType()) {
                    // handle whitespace with possible newlines
                    processWhiteSpace(currentChunk.toString(), listener);
                }
                else if (JavaTokenTypes.STRING_LITERAL == prev.getType()) {
                    listener.onStringLiteral(currentChunk.toString());
                }
                else if (JavaTokenTypes.SL_COMMENT == prev.getType()) {
                    listener.onCommentChunk(currentChunk.toString());
                }
                else if (JavaTokenTypes.ML_COMMENT == prev.getType()) {
                    //multiline comment parsing for javadoc tags
                    processComment(currentChunk.toString(), listener);
                }
                else if (JavaKeywords.contains(prev.getType())) {
                    listener.onKeyword(currentChunk.toString());
                    gatherPkgIdent = (prev.getType() == JavaTokenTypes.LITERAL_package);
                    gatherImportIdent = (prev.getType() == JavaTokenTypes.LITERAL_import);
                }
                else {
                    if (gatherPkgIdent || gatherImportIdent) {
                        if (JavaTokenTypes.SEMI == prev.getType()) {
                            if (gatherImportIdent) {
                                listener.onImport(accumName.toString());
                            }
                            accumName = new StringBuilder();
                            gatherPkgIdent = false;
                            gatherImportIdent = false;
                            listener.onChunk(currentChunk.toString());
                        }
                        else if (JavaTokenTypes.DOT == prev.getType()) {
                            accumName.append(currentChunk.toString());
                            listener.onChunk(currentChunk.toString());
                        }
                        else if (JavaTokenTypes.IDENT == prev.getType()) {
                            accumName.append(currentChunk.toString());
                            if (gatherPkgIdent) {
                                listener.onPackageSegment(accumName.toString(), currentChunk.toString());
                            }
                            else {
                                listener.onImportSegment(accumName.toString(), currentChunk.toString());
                            }
                        }
                        else {
                            listener.onChunk(currentChunk.toString());
                        }
                    }
                    else if (JavaTokenTypes.IDENT == prev.getType()) {
                        // TODO: this doesn't handle fully qualified Idents
                        listener.onIdentifier(currentChunk.toString());
                    }
                    else {
                        listener.onChunk(currentChunk.toString());
                    }
                }
                currentChunk = new StringBuilder();
                currentChunk.append(token.getText());
            }
            prev = token;
            token = lexer.nextToken();
        }
        listener.onEndDocument();
    }

    public static void processWhiteSpace(String whitespace, JavaSourceListener listener) {
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < whitespace.length()) {
            boolean atNewLine = false;
            char c1 = whitespace.charAt(i);
            char c2 = (i + 1 < whitespace.length() ? whitespace.charAt(i + 1) : 0);
            if (c1 == '\r' && c2 == '\n') {
                atNewLine = true;
                i++;
            }
            else if (c1 == '\r' || c1 == '\n') {
                atNewLine = true;
            }

            if (atNewLine) {
                if (b.length() > 0) {
                    listener.onChunk(b.toString());
                    b = new StringBuilder();
                }
                listener.onNewLine();
            }
            else {
                b.append(c1);
            }
            i++;
        }
        if (b.length() > 0) {
            listener.onChunk(b.toString());
        }
    }

    public static void processComment(String comment, JavaSourceListener listener) {
        StringBuilder b = new StringBuilder();
        int i = 0;
        boolean inTag = false;
        while (i < comment.length()) {
            boolean atNewLine = false;
            char c1 = comment.charAt(i);
            char c2 = (i + 1 < comment.length() ? comment.charAt(i + 1) : 0);

            if (c1 == '\r' && c2 == '\n') {
                atNewLine = true;
                i++;
            }
            else if (c1 == '\r' || c1 == '\n') {
                atNewLine = true;
            }

            // look for javadoc tags
            if (!inTag && c1 == '@' && Character.isLetter(c2)) {
                inTag = true;
                listener.onCommentChunk(b.toString());
                b = new StringBuilder();
            }
            else if (inTag && (!Character.isLetter(c1))) {
                inTag = false;
                String tag = b.toString();
                if (JavadocTags.contains(tag.substring(1))) {
                    listener.onJavadocTag(tag);
                    b = new StringBuilder();
                }
            }

            if (atNewLine) {
                if (b.length() > 0) {
                    listener.onCommentChunk(b.toString());
                    b = new StringBuilder();
                }
                listener.onNewLine();
            }
            else {
                b.append(c1);
            }
            i++;
        }
        if (b.length() > 0) {
            String left = b.toString();
            if (inTag && JavadocTags.contains(left)) {
                listener.onJavadocTag(left);
            }
            else {
                listener.onCommentChunk(left);
            }
        }
    }
}
