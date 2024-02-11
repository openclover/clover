package com.atlassian.clover.registry.format;

import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.api.registry.CloverRegistryException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 *
 */
public class UpdatableRegFilePrinter {

    public static void printFile(final File registryFile, final Writer out) throws CloverException, IOException {
            final UpdatableRegFile regFile = new UpdatableRegFile(registryFile);
            out.write("Registry name=" + regFile.getName() + " version=" + regFile.getVersion() + " \n");

            regFile.readContents(contents -> {
                //Sessions are ordered newest to oldest
                for (InstrSessionSegment sessionSegment : contents.getSessions()) {
                    out.write("\tSession startTs=" + sessionSegment.getStartTs() + " endTs=" + sessionSegment.getEndTs() + " version=" + sessionSegment.getVersion() + "\n");

                    for (FileInfoRecord fileInfoRec : sessionSegment.getFileInfoRecords()) {
                         out.write("\t\tFile name=" + fileInfoRec.getName() + " package=" + fileInfoRec.getPackageName() + "\n");
                    }
                }
                out.flush();
            });
    }

    public static void printFile(final File registryFile) throws IOException, CloverException {
        printFile(registryFile, new OutputStreamWriter(System.out));
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
        } else {
            try {
                printFile(new File(args[0]));
            } catch (IOException | CloverException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("java -cp clover.jar com.atlassian.clover.registry.format.UpdatableRegFilePrinter <clover.db>");
    }

}

