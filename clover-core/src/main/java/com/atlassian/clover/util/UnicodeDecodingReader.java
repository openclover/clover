package com.atlassian.clover.util;

import com.atlassian.clover.Logger;

import java.io.Reader;
import java.io.IOException;
import java.io.FilterReader;
import java.io.CharArrayWriter;

public class UnicodeDecodingReader extends FilterReader {

    private Logger log = Logger.getInstance();
    private int leftInBuffer = 0;
    private char [] buffer;

    private boolean lookahead = false;
    private int la;
    private int escapeCount = 0;

    public UnicodeDecodingReader(Reader in) {
        super(in);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(char[] chars, int off, int len) throws IOException {

        for (int i = 0; i < len; i++) {
            int c = read();
            if (c < 0) {
                return i == 0 ? -1 : i;
            } else {
                chars[i + off] = (char)c;
            }
        }
        return len;
    }

    @Override
    public int read() throws IOException {
        int c;

        if (leftInBuffer > 0) {
            c = buffer[buffer.length - leftInBuffer];
            leftInBuffer--;
            return c;
        }

        if (lookahead) {
            lookahead = false;
            c = la;
        } else {
            c = super.read();
        }
        if (c == '\\') {
            escapeCount++;
            if (escapeCount % 2 == 1) {

                int la1 = super.read();

                lookahead = true;
                la = la1;

                if (la1 == 'u') {
                    // escapeseq holds the full escape sequence except for the initial '\'
                    CharArrayWriter escapeSeq = new CharArrayWriter(6);
                    escapeSeq.write('u');

                    escapeCount = 0;
                    final char [] hex = new char[4];

                    c = super.read();
                    while (c == 'u') {
                        escapeSeq.write('u');
                        c = super.read();
                    }

                    int digits = 0;
                    if (c >= 0) {
                        do {
                            hex[digits++] = (char)c;
                            c = super.read();
                        } while (c >= 0 && digits < 4);
                    }
                    la = c;

                    escapeSeq.write(hex, 0, digits);
                    final String hexStr = new String(hex, 0, digits);

                    if (digits == 4) {
                        try {
                            int charCode = Integer.parseInt(hexStr, 16);
                            // restrict the range of translation to antlr's char vocabulary.
                            // leave chars outside this range untranslated.
                            if (charCode >= 0x0003 && charCode <= 0xFFFE) {
                                char translated = (char)charCode;
                                if (Character.isDefined(translated)) {
                                    // special case #1: do not translate escape sequence for CR and LF; a reason is that such
                                    // sequence could be used in javadocs and it would produce higher number of source lines
                                    // than we actually have (see CLOV-1131)
                                    // special case #2: do not translate escape sequence for a backslash; a reason is that this
                                    // character is used for quoting another characters; examples:
                                    // char SINGLE_QUOTE = '\u005c''; (see CLOV-1305)
                                    // {@code '\u005Cu0020'} in javadocs (see CLOV-1431)
                                    if (charCode != 0x000A && charCode != 0x000D && charCode != 0x005C) {
                                        return translated;
                                    } else {
                                        log.debug("Omitted translation of unicode escape sequence: \\u" + hexStr);
                                    }
                                } else {
                                    log.verbose("No character defined for unicode escape: \\u" + hexStr);
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.verbose("Malformed unicode escape: \\u" + hexStr);
                        }
                    } else {
                        log.verbose("End of stream reached inside unicode escape");
                    }
                    // translation failed. set the output mode to buffered, and the contents of the buffer to escapeseq
                    buffer = escapeSeq.toCharArray();
                    leftInBuffer = buffer.length;

                    return '\\';
                }
            }
        } else {
            escapeCount = 0;
        }

        return c;
    }

}
