package org.openclover.core.api.instrumentation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.ModifiersInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.registry.entities.FullBranchInfo;
import org.openclover.core.registry.entities.FullStatementInfo;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.core.spi.lang.LanguageConstruct;

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
