package org.openclover.core.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.zip.Adler32;
import java.util.zip.Checksum;


public class ChecksummingReader extends FilterReader {
    private final Checksum checksum;

    public ChecksummingReader(Reader in) {
        super(in);
        checksum = new Adler32();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Read all characters till end of the stream. As checksum is being calculated during read,
     * you can get the checksum after this method returns.
     */
    public void readAllCharacters() throws IOException {
        while (read() != -1) {
            // intentionally empty
        }
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
        int c = super.read();
        if (c >= 0 && c != 0xD) { // ignore '\r' chars, so checksum is calculated using normalized '\n' eols
            checksum.update(c >>> 8);
            checksum.update(c);
        }

        return c;
    }

    public long getChecksum() {
        return checksum.getValue();
    }
}
