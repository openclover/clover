package org.openclover.core.instr.java;

import clover.antlr.CharScanner;
import clover.antlr.CommonHiddenStreamToken;
import clover.antlr.Token;
import clover.antlr.TokenStreamException;
import clover.antlr.TokenStreamHiddenTokenFilter;
import org.jetbrains.annotations.NotNull;
import org.openclover.core.Contract;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig;
import org.openclover.core.context.ContextStore;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org_openclover_runtime.CloverVersionInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;


/**
 * A token stream that preserves whitespace tokens in hidden tokens,
 * and maintains a linked list of tokens it has seen, and provides
 * methods to insert instrumenting tokens into that list.
 *
 * @version 1.0
 * @see clover.antlr.TokenStreamHiddenTokenFilter
 */
public class CloverTokenStreamFilter extends TokenStreamHiddenTokenFilter {

    /**
     * a marker to indicate add at the front when dumping token list to mark it as instrumented
     */
    public static final String MARKER_PREFIX = "/* $$ This file has been instrumented by OpenClover ";
    public static final String MARKER = MARKER_PREFIX + CloverVersionInfo.RELEASE_NUM + "#" + CloverVersionInfo.BUILD_STAMP + " $$ */";

    private static final String DIRECTIVE_PREFIX = "CLOVER:";
    private static final int DIRECTIVE_LENGTH = DIRECTIVE_PREFIX.length();
    private static final String DIRECTIVE_ON = "ON";
    private static final String DIRECTIVE_OFF = "OFF";
    private static final String DIRECTIVE_FLUSH = "FLUSH";
    private static final String DIRECTIVE_LAMBDA_VOID = "VOID";
    private static final String DIRECTIVE_CASE_YIELD = "YIELD";

    private CloverToken last = null;
    private CloverToken first = null;

    private String filePath;


    public static void guardAgainstDoubleInstrumentation(File orig, BufferedReader bin) throws IOException, CloverException {
        Contract.check(bin.markSupported(), "Must use a markSupporting Reader when instrumenting");

        final int markerLength = CloverTokenStreamFilter.MARKER_PREFIX.length();
        bin.mark(markerLength);
        final char[] chars = new char[markerLength];
        final int charCount = bin.read(chars, 0, markerLength);
        if (charCount == markerLength) {
            final String maybeMarker = new String(chars);
            if (maybeMarker.equals(CloverTokenStreamFilter.MARKER_PREFIX)) {
                throw new CloverException("Double instrumentation detected: " + orig.getAbsolutePath() +
                        " appears to have already been instrumented by OpenClover.");
            }
        }
        bin.reset();
    }


    /**
     * A filtering token stream that hides java comments
     * and whitespace from the parser, and builds a list
     * of tokens that it has seen.
     *
     * @param filePath path to original source, for reporting errors
     * @param input    the CharScanner to pull tokens from
     * @see clover.antlr.TokenStreamHiddenTokenFilter
     */
    public CloverTokenStreamFilter(String filePath, CharScanner input) {
        super(input);
        this.filePath = filePath;
        input.setTokenObjectClass(CloverToken.class.getName());
        hide(JavaTokenTypes.WS);
        hide(JavaTokenTypes.SL_COMMENT);
        hide(JavaTokenTypes.ML_COMMENT);

    }

    private int countNewLines(String s) {
        int res = 0;

        for (int i = 0; i < s.length(); ) {
            // check for dos newlines
            if (i < s.length() - 1 && s.charAt(i) == '\r' && s.charAt(i + 1) == '\n') {
                res++;
                i++;
            }
            // check for unix & mac newlines
            else if (s.charAt(i) == '\r' || s.charAt(i) == '\n') {
                res++;
            }
            i++;
        }
        return res;
    }

    private void scanForDirectives(Token tok, InstrumentationState state) {
        String text = tok.getText();
        if (text == null) {
            return;
        }

        int startDirective = text.indexOf(DIRECTIVE_PREFIX);

        int curLine = tok.getLine();
        while (startDirective >= 0) {
            curLine += countNewLines(text.substring(0, startDirective));
            final String rest = processDirective(state, text, startDirective, curLine);

            startDirective = rest.indexOf(DIRECTIVE_PREFIX);
            text = rest;
        }
    }

    @NotNull
    private String processDirective(InstrumentationState state, String text, int startDirective, int curLine) {
        final String rest = text.substring(startDirective + DIRECTIVE_LENGTH);
        if (rest.startsWith(DIRECTIVE_ON)) {
            Logger.getInstance().debug(filePath + ":" + curLine + ": switching OpenClover instrumentation ON as per directive");
            state.setInstrEnabled(true);
            state.setInstrContext(state.getInstrContext().clear(ContextStore.CONTEXT_CLOVER_OFF));
        } else if (rest.startsWith(DIRECTIVE_OFF)) {
            Logger.getInstance().debug(filePath + ":" + curLine + ": switching OpenClover instrumentation OFF as per directive");
            state.setInstrContext(state.getInstrContext().set(ContextStore.CONTEXT_CLOVER_OFF));
            state.setInstrEnabled(false);
        } else if (rest.startsWith(DIRECTIVE_FLUSH)) {
            Logger.getInstance().debug(filePath + ":" + curLine + ": inserting flush as per directive");
            state.setNeedsFlush(true);
        } else if (rest.startsWith(DIRECTIVE_LAMBDA_VOID)) {
            final FullMethodInfo methodInfo = (FullMethodInfo) state.getSession().getCurrentMethod();
            if (methodInfo != null && methodInfo.isLambda()) {
                Logger.getInstance().debug(filePath + ":" + curLine + ": declaring lambda expression as void");
                methodInfo.setVoidReturnType(true);
            } else {
                Logger.getInstance().debug(filePath + ":" + curLine + ": could not declare lambda expression as void since there's no method stack");
            }
        } else if (rest.startsWith(DIRECTIVE_CASE_YIELD)) {
            Logger.getInstance().debug(filePath + ":" + curLine + ": inserting return as per directive");
            state.setNeedsYieldKeyword(true);
        } else {
            Logger.getInstance().warn(filePath + ":" + curLine + ": ignoring unknown OpenClover directive");
        }
        return rest;
    }

    /**
     * @see clover.antlr.TokenStream#nextToken
     **/
    @Override
    public Token nextToken() throws TokenStreamException {

        CloverToken next = (CloverToken) super.nextToken();
        next.setFilter(this);

        if (last != null) {
            // we may not be at the end of the list because the instrumenter may have added trivial tokens, so move
            //  fwd till we find the end
            while (last.getNext() != null) {
                last = last.getNext();
            }
            next.setPrev(last);
            last.setNext(next);
        } else {
            first = next;
        }
        last = next;

        return next;
    }


    /**
     * process directives, set final emitter state. This leaves the token stream ready for output
     */
    public void instrument(FileStructureInfo structure, FullFileInfo fileInfo, InstrumentationSession session, JavaInstrumentationConfig cfg) {
        InstrumentationState state = new InstrumentationState(session, fileInfo, structure, cfg);
        scanHiddens(getInitialHiddenToken(), state);
        CloverToken curr = first;
        while (curr != null) {
            if (curr.hasEmitters()) {
                curr.initEmitters(state);
            }
            scanHiddens(curr.getHiddenAfter(), state);
            curr = curr.getNext();
        }
    }


    /**
     * dump the token list, including hidden whitespace and comment tokens
     *
     * @param outWriter a <code>Writer</code> to output the token list to
     */
    public void write(Writer outWriter) throws IOException {
        PrintWriter out = new PrintWriter(outWriter);
        out.print(MARKER);
        dumpHiddens(out, getInitialHiddenToken());
        CloverToken curr = first;

        while (curr != null) {
            curr.triggerPreEmitters(out);
            String str = curr.getText();

            // deal with EOF token
            if (str != null) {
                out.print(str);
            }
            curr.triggerPostEmitters(out);
            dumpHiddens(out, curr.getHiddenAfter());
            curr = curr.getNext();
        }
    }

    public boolean isEOLTerminated() {

        if (last != null) {
            // at the EOF now, go back one and search for any hidden whitespace after it
            CloverToken beforeEOF = last.getPrev();
            if (beforeEOF != null && beforeEOF.getHiddenAfter() != null) {
                CommonHiddenStreamToken curr = beforeEOF.getHiddenAfter();
                CommonHiddenStreamToken prev = curr;
                while (curr != null) {
                    prev = curr;
                    curr = curr.getHiddenAfter();
                }
                // prev now holds the last piece of whitespace in the file, which could be a terminating EOL
                return prev.getText().endsWith("\n") || prev.getText().endsWith("\r");
            }
        }
        return false;
    }

    /**
     * helper method to dump any a list of zero or more
     * hidden tokens
     */
    private void dumpHiddens(PrintWriter out, CommonHiddenStreamToken tok) {
        while (tok != null) {
            out.print(tok.getText());
            tok = tok.getHiddenAfter();
        }
    }

    private void scanHiddens(CommonHiddenStreamToken tok, InstrumentationState state) {
        while (tok != null) {
            int type = tok.getType();
            if (JavaTokenTypes.SL_COMMENT == type ||
                    JavaTokenTypes.ML_COMMENT == type) {
                scanForDirectives(tok, state);
            }
            tok = tok.getHiddenAfter();
        }
    }
}
