package com.atlassian.clover.api.instrumentation;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.ModifiersInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.registry.entities.FullBranchInfo;
import com.atlassian.clover.registry.entities.FullStatementInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public interface InstrumentationSession {
    PackageInfo enterPackage(String name);
    void exitPackage();

    FileInfo enterFile(
        String packageName, File file, int lineCount, int ncLineCount,
        long timestamp, long filesize, long checksum);
    void exitFile();

    ClassInfo enterClass(
        String name, SourceInfo region, ModifiersInfo modifiers, boolean isInterface,
        boolean isEnum, boolean isAnnotation);

    ClassInfo exitClass(int endLine, int endCol);

    MethodInfo enterMethod(@NotNull ContextSet context, @NotNull SourceInfo region, @NotNull MethodSignature signature,
                           boolean isTest, @Nullable String staticTestName,
                           boolean isLambda, int complexity, @NotNull LanguageConstruct construct);

    void exitMethod(int endLine, int endCol);

    // TODO replace by StatementInfo
    FullStatementInfo addStatement(ContextSet context, SourceInfo region, int complexity, LanguageConstruct construct);

    FullBranchInfo addBranch(ContextSet context, SourceInfo region, boolean instrumented, int complexity, LanguageConstruct construct);

    void setSourceEncoding(String encoding);

    int getCurrentIndex();
    int getCurrentFileMaxIndex();
    int getCurrentOffsetFromFile();

    long getStartTs();
    long getEndTS();
    long getVersion();

    FileInfo getCurrentFile();
    PackageInfo getCurrentPackage();

    @Nullable
    ClassInfo getCurrentClass();

    @Nullable
    MethodInfo getCurrentMethod();

    void close() throws ConcurrentInstrumentationException;
}
