package org.openclover.perfmon;

import org.openclover.core.instr.java.Instrumenter;

import java.io.File;
import java.io.FilenameFilter;

class InstrumentingFilter implements FilenameFilter {
    protected final Instrumenter instrumenter;
    protected final File instrDir;
    protected final boolean logInstr;
    protected final int max;
    protected int done;

    public InstrumentingFilter(Instrumenter instrumenter, File outputDir, int max, boolean logInstr) {
        this.instrumenter = instrumenter;
        this.instrDir = outputDir;
        this.logInstr = logInstr;
        this.max = max;
        this.done = 0;
    }

    public boolean accept(File dir, String name) {
        final File file = new File(dir, name);
        if (name.endsWith(".java") && done < max) {
            try {
                if (logInstr) {
                    System.out.println("Instrumenting " + file);
                }
                instrument(file);
                done++;
            } catch (Exception e) {
                System.err.println("Failed to instrument file " + file + ": " + e);
                e.printStackTrace();
            }
        } else if (file.isDirectory()) {
            file.listFiles(this);
        }
        return true;
    }

    protected void instrument(File file) throws Exception {
        instrumenter.instrument(file, instrDir, null);
    }
}
