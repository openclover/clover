package com.atlassian.clover.util;

import java.io.FilterWriter;
import java.io.Writer;
import java.io.IOException;

public class UnicodeEncodingWriter extends FilterWriter {

    public UnicodeEncodingWriter(Writer out) {
        super(out);
    }

    @Override
    public void write(int c) throws IOException
    {
        if (c <= 0x7F) {
            super.write(c);
        }
        else {
            super.write('\\');
            super.write('u');
            final String hex = Integer.toHexString(c);

            for (int i = hex.length(); i < 4; i++) {
                super.write('0');
            }
            write(hex);
        }
    }

    @Override
    public void write(char[] buf, int off, int len) throws IOException
    {
        for(int i = off; i < off + len; i++) {
            write(buf[i]);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException
    {
        write(str.toCharArray(), off, len);
    }

}
