package org.openclover.core.util;

import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.cfg.Interval;
import org.openclover.runtime.Logger;
import org.openclover.runtime.RecorderLogging;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.TestCaseInfo;

import java.text.DateFormat;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class RegistryDumper {
    private static int indent;

    public static void main(String[] args) throws Exception {
        RecorderLogging.init();
        if (args.length != 3) {
            printUsage();
        } else {
            CloverDatabase db = CloverDatabase.loadWithCoverage(
                    args[2],
                    new CoverageDataSpec(new Interval(args[1]).getValueInMillis()));

            if (args[0].equalsIgnoreCase("pretty")) {
                printPretty(db, false);
            } else if (args[0].equalsIgnoreCase("prettyfull")) {
                printPretty(db, true);
            } else if (args[0].equalsIgnoreCase("csv")) {
                printCSV(db);
            } else {
                System.err.println("Unknown format");
            }
        }
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("java " + RegistryDumper.class.getName() + " pretty|prettyfull|csv span database");
    }

    private static void printPretty(CloverDatabase db, final boolean full) {
        Clover2Registry reg = db.getRegistry();
        Logger.getInstance().info("Loaded registry at " + reg.getRegistryFile());
        Logger.getInstance().info("Init String: " + reg.getInitstring());
        Logger.getInstance().info("Version: " + reg.getVersion());
        Logger.getInstance().info("Coverage Data Length: " + reg.getDataLength());
        Logger.getInstance().info("Instrumentation History:");
        for (Object o : reg.getInstrHistory()) {
            Logger.getInstance().info(o.toString());
        }

        reg.getProject().visitFiles(fileInfo -> {
            Logger.getInstance().info("File " + fileInfo.getPackagePath());
            indent++;
            Logger.getInstance().info(indent("Physical file:" + ((FullFileInfo)fileInfo).getPhysicalFile().getAbsolutePath()));
            Logger.getInstance().info(indent("Encoding: " + fileInfo.getEncoding()));
            Logger.getInstance().info(indent("Checksum: " + fileInfo.getChecksum()));
            Logger.getInstance().info(indent("File Size: " + fileInfo.getFilesize()));
            Logger.getInstance().info(indent("Line Count: " + fileInfo.getLineCount()));
            Logger.getInstance().info(indent("NC Line Count: " + fileInfo.getNcLineCount()));
            Logger.getInstance().info(indent("Timestamp: " + DateFormat.getDateTimeInstance().format(fileInfo.getTimestamp())));
            Logger.getInstance().info(indent("Slot Index: " + fileInfo.getDataIndex()));
            Logger.getInstance().info(indent("Slot Length: " + fileInfo.getDataLength()));
            Logger.getInstance().info(indent("Class count: " + classCount(fileInfo)));
            Logger.getInstance().info(indent("Method count: " + methodCount(fileInfo)));
            Logger.getInstance().info(indent("Test count: " + testCount(fileInfo)));
            Logger.getInstance().info(indent("Statement count: " + statementCount(fileInfo)));
            Logger.getInstance().info(indent("Branch count: " + branchCount(fileInfo)));
            Logger.getInstance().info(indent("Max version supported: " + ((FullFileInfo) fileInfo).getMaxVersion()));
            Logger.getInstance().info(indent("Min version supported: " + ((FullFileInfo) fileInfo).getMinVersion()));
            Logger.getInstance().info(indent("Classes:"));
            indent++;
            for (ClassInfo classInfo : fileInfo.getClasses()) {
                Logger.getInstance().info(indent("Class " + classInfo.getQualifiedName()));
                Logger.getInstance().info(indent("Statement count: " + statementCount(classInfo)));
                Logger.getInstance().info(indent("Branch count: " + branchCount(classInfo)));

                indent++;
                for (final MethodInfo methodInfo : classInfo.getMethods()) {
                    if (full) {
                        Logger.getInstance().info(indent(methodInfo.getName()));
                        indent++;
                        Logger.getInstance().info(indent(methodInfo.toString()));
                        if (methodInfo.isTest()) {
                            TestCaseInfo testCase = ((FullClassInfo) classInfo).getTestCase(methodInfo.getQualifiedName());
                            if (testCase != null) {
                                Logger.getInstance().info(indent(indent(testCase.toString())));
                            }
                        }
                        Logger.getInstance().info(indent("Statement count: " + methodInfo.getStatements().size()));
                        Logger.getInstance().info(indent("Branch count: " + methodInfo.getBranches().size()));

                        final List<SourceInfo> stmtsAndBranches = newArrayList();
                        stmtsAndBranches.addAll(methodInfo.getBranches());
                        stmtsAndBranches.addAll(methodInfo.getStatements());

                        stmtsAndBranches.sort((sourceInfo1, sourceInfo2) -> {
                            final int startLine1 = sourceInfo1.getStartLine();
                            final int startCol1 = sourceInfo1.getStartColumn();
                            final int startLine2 = sourceInfo2.getStartLine();
                            final int startCol2 = sourceInfo2.getStartColumn();

                            if (startLine1 < startLine2) {
                                return -1;
                            } else if (startLine1 > startLine2) {
                                return 1;
                            } else {
                                return Integer.compare(startCol1, startCol2);
                            }
                        });

                        indent++;
                        for (SourceInfo stmtOrBranch : stmtsAndBranches) {
                            String line = stmtOrBranch instanceof StatementInfo ?
                                    describeStatement((StatementInfo) stmtOrBranch) :
                                    describeBranch((BranchInfo) stmtOrBranch);
                            Logger.getInstance().info(indent(line));
                        }
                        indent--;
                    } else {
                        Logger.getInstance().info(indent(methodInfo.toString()));
                    }
                    indent--;
                }
                indent--;
            }
            indent--;
            indent--;
        });
    }

    private static String describeBranch(BranchInfo branch) {
        return String.format("branch region=%d:%d-%d:%d, relativeDataIndex=%d, complexity=%d, context=%s, true hits=%d, false hits=%d",
                branch.getStartLine(), branch.getStartColumn(), branch.getEndLine(), branch.getEndColumn(),
                branch.getDataIndex(), branch.getComplexity(), branch.getContext().toString(),
                branch.getTrueHitCount(), branch.getFalseHitCount());
    }

    private static String describeStatement(StatementInfo stmt) {
        return String.format("statement region=%d:%d-%d:%d, relativeDataIndex=%d, complexity=%d, context=%s, hits=%d",
                stmt.getStartLine(), stmt.getStartColumn(), stmt.getEndLine(), stmt.getEndColumn(),
                stmt.getDataIndex(), stmt.getComplexity(), stmt.getContext().toString(),
                stmt.getHitCount());
    }

    private static int methodCount(FileInfo fileInfo) {
        int methodCount = 0;
        for (ClassInfo classInfo : fileInfo.getClasses()) {
            methodCount += classInfo.getMethods().size();
        }
        return methodCount;
    }

    private static int testCount(FileInfo fileInfo) {
        int testCount = 0;
        for (ClassInfo classInfo : fileInfo.getClasses()) {
            testCount += ((FullClassInfo) classInfo).getTestCases().size();
        }
        return testCount;
    }

    private static int classCount(FileInfo fileInfo) {
        return fileInfo.getClasses().size();
    }

    private static int statementCount(FileInfo fileInfo) {
        int statementCount = 0;
        for (ClassInfo classInfo : fileInfo.getClasses()) {
            statementCount += statementCount(classInfo);
        }
        return statementCount;
    }

    private static int statementCount(ClassInfo classInfo) {
        int statementCount = 0;
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            statementCount += methodInfo.getStatements().size();
        }
        return statementCount;
    }

    private static int branchCount(FileInfo fileInfo) {
        int branchCount = 0;
        for (ClassInfo classInfo : fileInfo.getClasses()) {
            branchCount += branchCount(classInfo);
        }
        return branchCount;
    }

    private static int branchCount(ClassInfo classInfo) {
        int branchCount = 0;
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            branchCount += methodInfo.getBranches().size();
        }
        return branchCount;
    }

    private static String indent(String line) {
        for (int i = 0; i < indent; i++) {
            System.out.print("\t");
        }
        return line;
    }

    public static void printCSV(CloverDatabase db) {
        Clover2Registry reg = db.getRegistry();
        Logger.getInstance().info("Init String,Version,Coverage Data Length");
        Logger.getInstance().info(
            reg.getInitstring() + "," +
            reg.getVersion() + "," +
            reg.getDataLength() + "\n");
        Logger.getInstance().info("File,Encoding,Checksum,File Size,Line Count,NC Line Count,Timestamp,Slot Index,Slot Length,Class Count, Method Count, Statement Count,Branch Count");

        reg.getProject().visitFiles(fileInfo -> Logger.getInstance().info(
            fileInfo.getPackagePath() + "," +
            fileInfo.getEncoding() + "," +
            fileInfo.getChecksum() + "," +
            fileInfo.getFilesize() + "," +
            fileInfo.getLineCount() + "," +
            fileInfo.getNcLineCount() + "," +
            DateFormat.getDateTimeInstance().format(fileInfo.getTimestamp()) + "," +
            fileInfo.getDataIndex() + "," +
            fileInfo.getDataLength() + "," +
            classCount(fileInfo) + "," +
            methodCount(fileInfo) + "," +
            statementCount(fileInfo) + "," +
            branchCount(fileInfo) + ","));
    }
}
