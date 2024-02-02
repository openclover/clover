package com.atlassian.clover.idea.build;

import com.atlassian.clover.Logger;
import com.atlassian.clover.idea.ProjectPlugin;
import com.atlassian.clover.idea.util.MiscUtils;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.SourceInstrumentingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DependencyInjectingCompiler implements SourceInstrumentingCompiler {
    @Override
    @NotNull
    public ProcessingItem[] getProcessingItems(final CompileContext context) {
        final VirtualFile[] files = context.getCompileScope().getFiles(StdFileTypes.JAVA, true);
        final ProcessingItem[] items = new ProcessingItem[files.length];
        ApplicationManager.getApplication().runReadAction(() -> {
            for (int i = 0; i < files.length; i++) {
                final VirtualFile file = files[i];
                final boolean included = ProjectInclusionDetector.processFile(context.getProject(), file).isIncluded();
                items[i] = new CloverDependencyProcessingItem(file, new CloverValidityState(included));
            }

        });
        return items;
    }

    @Override
    public ProcessingItem[] process(final CompileContext context, final ProcessingItem[] items) {
        if (isClassicBuild(context.getProject())) {
            return processForClassicBuild(context, items);
        } else {
            return processForExternalBuild(context, items);
        }
    }

    protected ProcessingItem[] processForClassicBuild(final CompileContext context, final ProcessingItem[] items) {
        final List<VirtualFile> toDelete = new ArrayList<>(items.length * 2); // most files contain 1 top level class

        ApplicationManager.getApplication().runReadAction(() -> {
            for (final ProcessingItem item : items) {
                final VirtualFile vf = item.getFile();
                final PsiFile file = PsiManager.getInstance(context.getProject()).findFile(vf);
                if (file instanceof PsiJavaFile) {
                    // in classic build - remove *.class files associated with given source file
                    final Module m = context.getModuleByFile(vf);
                    final PsiJavaFile pjf = (PsiJavaFile) file;
                    final VirtualFile outputDirectory = context.getModuleOutputDirectory(m);
                    final VirtualFile outputTestDirectory = context.getModuleOutputDirectoryForTests(m);
                    for (final PsiClass psiClass : pjf.getClasses()) {
                        final String name = psiClass.getQualifiedName();
                        if (name == null) {
                            continue;
                        }
                        final String path = name.replace('.', '/') + ".class";

                        if (outputDirectory != null) {
                            final VirtualFile classFile = outputDirectory.findFileByRelativePath(path);
                            if (classFile != null) {
                                toDelete.add(classFile);
                            }
                        }
                        if (outputTestDirectory != null) {
                            // in case of leftovers
                            final VirtualFile classFile = outputTestDirectory.findFileByRelativePath(path);
                            if (classFile != null) {
                                toDelete.add(classFile);
                            }
                        }
                    }
                }
            }
        });

        // force recompilation of files for which an inclusion/exclusion setting has changed
        MiscUtils.invokeWriteActionAndWait(() -> {
            // in case of a classic build delete *.class files in order to force recompilation
            for (final VirtualFile file : toDelete) {
                try {
                    file.delete(DependencyInjectingCompiler.this);
                } catch (IOException e) {
                    Logger.getInstance().info("Cannot delete output file " + file, e);
                }
            }
        });

        return items;
    }

    protected ProcessingItem[] processForExternalBuild(final CompileContext context, final ProcessingItem[] items) {
        final List<VirtualFile> toTouch = new ArrayList<>(items.length);

        ApplicationManager.getApplication().runReadAction(() -> {
            for (final ProcessingItem item : items) {
                final VirtualFile vf = item.getFile();
                final PsiFile file = PsiManager.getInstance(context.getProject()).findFile(vf);
                if (file instanceof PsiJavaFile) {
                    // in case of external build - touch source files
                    toTouch.add(vf);
                }
            }
        });

        // force recompilation of files for which an inclusion/exclusion setting has changed
        MiscUtils.invokeWriteActionAndWait(() -> {
            // in case of external build, we must refresh source file (to simulate that it has changed),
            // because *.class removal is being ignored by the build server process (it looks in source roots only
            // - see org.jetbrains.jps.cmdline.BuildSession.applyFSEvent)
            for (final VirtualFile file : toTouch) {
                file.refresh(false, false);
            }
        });

        return items;
    }

    /**
     * Return true if it's a "classic" build (available till IDEA 13), false if it's an external build
     * (introduced in IDEA 12)
     * @param project current project
     * @return true for classic build, false for external build
     */
    private boolean isClassicBuild(final Project project) {
        // IDEA 11 and older - classic build only
        // IDEA 14 and newer - external build only
        // IDEA 12-13 - classic or external, depending on workspace setting
        try {
            // call CompilerWorkspaceConfiguration.useOutOfProcessBuild() - method is available only in IDEA 12-13
            Method useOutOfProcessBuild = CompilerWorkspaceConfiguration.class.getMethod("useOutOfProcessBuild");
            Object ret = useOutOfProcessBuild.invoke(CompilerWorkspaceConfiguration.getInstance(project));
            return !(Boolean)ret;
        } catch (Exception ex) {
            // no method? as we don't support IDEA 11 and older it must be IDEA 14+, assuming external build
            Logger.getInstance().debug("Exception caught when trying to check if external build is enabled. "
                    + "Assuming that an external build is used. ", ex);
            return false;
        }
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Clover dependency injecting compiler";
    }

    @Override
    public boolean validateConfiguration(CompileScope scope) {
        final Module[] modules = scope.getAffectedModules();
        return modules.length > 0 && ProjectPlugin.getPlugin(modules[0].getProject()) != null;
    }

    @Override
    public ValidityState createValidityState(DataInput in) throws IOException {
        return CloverValidityState.read(in);
    }
}
