package com.atlassian.clover.util;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.Logger;
import com.atlassian.clover.RecorderLogging;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.registry.entities.BaseFileInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.FileInfoVisitor;
import com.atlassian.clover.registry.entities.TestCaseInfo;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

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
        reg.getProject().visitFiles(new FileInfoVisitor() {
            @Override
            public void visitFileInfo(BaseFileInfo file) {
                Logger.getInstance().info("File " + file.getPackagePath());
                indent++;
                Logger.getInstance().info(indent("Physical file:" + ((FullFileInfo)file).getPhysicalFile().getAbsolutePath()));
                Logger.getInstance().info(indent("Encoding: " + file.getEncoding()));
                Logger.getInstance().info(indent("Checksum: " + file.getChecksum()));
                Logger.getInstance().info(indent("File Size: " + file.getFilesize()));
                Logger.getInstance().info(indent("Line Count: " + file.getLineCount()));
                Logger.getInstance().info(indent("NC Line Count: " + file.getNcLineCount()));
                Logger.getInstance().info(indent("Timestamp: " + DateFormat.getDateTimeInstance().format(file.getTimestamp())));
                Logger.getInstance().info(indent("Slot Index: " + file.getDataIndex()));
                Logger.getInstance().info(indent("Slot Length: " + file.getDataLength()));
                Logger.getInstance().info(indent("Class count: " + classCount(file)));
                Logger.getInstance().info(indent("Method count: " + methodCount(file)));
                Logger.getInstance().info(indent("Test count: " + testCount(file)));
                Logger.getInstance().info(indent("Statement count: " + statementCount(file)));
                Logger.getInstance().info(indent("Branch count: " + branchCount(file)));
                Logger.getInstance().info(indent("Max version supported: " + ((FullFileInfo) file).getMaxVersion()));
                Logger.getInstance().info(indent("Min version supported: " + ((FullFileInfo) file).getMinVersion()));
                Logger.getInstance().info(indent("Classes:"));
                indent++;
                for (ClassInfo classInfo : file.getClasses()) {
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

                            Collections.sort(stmtsAndBranches, new Comparator<SourceInfo>() {
                                @Override
                                public int compare(SourceInfo o1, SourceInfo o2) {
                                    final int startLine1 = o1.getStartLine();
                                    final int startCol1 = o1.getStartColumn();
                                    final int startLine2 = o2.getStartLine();
                                    final int startCol2 = o2.getStartColumn();

                                    if (startLine1 < startLine2) {
                                        return -1;
                                    } else if (startLine1 > startLine2) {
                                        return 1;
                                    } else if (startCol1 < startCol2) {
                                        return -1;
                                    } else if (startCol1 > startCol2) {
                                        return 1;
                                    } else {
                                        return 0;
                                    }
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
            }
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
            Integer.toString(reg.getDataLength()) + "\n");
        Logger.getInstance().info("File,Encoding,Checksum,File Size,Line Count,NC Line Count,Timestamp,Slot Index,Slot Length,Class Count, Method Count, Statement Count,Branch Count");
        reg.getProject().visitFiles(new FileInfoVisitor() {
            @Override
            public void visitFileInfo(BaseFileInfo file) {
                Logger.getInstance().info(
                    file.getPackagePath() + "," +
                    file.getEncoding() + "," +
                    file.getChecksum() + "," +
                    file.getFilesize() + "," +
                    file.getLineCount() + "," +
                    file.getNcLineCount() + "," +
                    DateFormat.getDateTimeInstance().format(file.getTimestamp()) + "," +
                    file.getDataIndex() + "," +
                    Integer.toString(file.getDataLength()) + "," +
                    Integer.toString(classCount(file)) + "," +
                    Integer.toString(methodCount(file)) + "," +
                    Integer.toString(statementCount(file)) + "," +
                    Integer.toString(branchCount(file)) + ",");
            }
        });
    }
}
