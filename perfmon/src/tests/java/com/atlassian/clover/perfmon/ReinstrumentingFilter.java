package com.atlassian.clover.perfmon;

import com.atlassian.clover.instr.java.Instrumenter;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class ReinstrumentingFilter extends InstrumentingFilter {
    public ReinstrumentingFilter(Instrumenter instrumenter, File instrDir, int maxNumToInstrument, boolean logInstr) {
        super(instrumenter, instrDir, maxNumToInstrument, logInstr);
    }

    @Override
    protected void instrument(File file) throws CloverException, IOException {
        File altered = new File(file.getParent(), file.getName() + ".new");
        copyWithLeadingWhitespace(file, altered);
        instrumenter.instrument(altered, instrDir, null);
    }

    private void copyWithLeadingWhitespace(File file, File altered) throws IOException {
        Reader in = null;
        Writer out = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(altered), "UTF-8"));

            out.write("          ");
            int b = in.read();
            while (b >= 0) {
                out.write(b);
                b = in.read();
            }
        } finally {
            IOStreamUtils.close(in);
            IOStreamUtils.close(out);
        }
    }
}
