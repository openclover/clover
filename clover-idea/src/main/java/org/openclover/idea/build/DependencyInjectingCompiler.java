package org.openclover.idea.build;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.SourceInstrumentingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.util.MiscUtils;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DependencyInjectingCompiler implements SourceInstrumentingCompiler {
    @Override
    @NotNull
    public ProcessingItem[] getProcessingItems(final CompileContext context) {
        // getFiles() iterates the workspace/project file index; since IDEA 2026 this runs on a
        // background pooled thread (not the EDT) and requires read access - so the whole thing,
        // including collecting the files, must be wrapped in a read action.
        return ApplicationManager.getApplication().runReadAction((Computable<ProcessingItem[]>) () -> {
            final VirtualFile[] files = context.getCompileScope().getFiles(StdFileTypes.JAVA, true);
            final ProcessingItem[] items = new ProcessingItem[files.length];
            for (int i = 0; i < files.length; i++) {
                final VirtualFile file = files[i];
                final boolean included = ProjectInclusionDetector.processFile(context.getProject(), file).isIncluded();
                items[i] = new CloverDependencyProcessingItem(file, new CloverValidityState(included));
            }
            return items;
        });
    }

    @Override
    public ProcessingItem[] process(final CompileContext context, final ProcessingItem[] items) {
        // OpenClover instruments sources exclusively through the external build process (JPS),
        // so we always  touch the affected source files to force their recompilation (and re-instrumentation)
        final List<VirtualFile> toTouch = new ArrayList<>(items.length);

        ApplicationManager.getApplication().runReadAction(() -> {
            for (final ProcessingItem item : items) {
                final VirtualFile vf = item.getFile();
                final PsiFile file = PsiManager.getInstance(context.getProject()).findFile(vf);
                if (file instanceof PsiJavaFile) {
                    toTouch.add(vf);
                }
            }
        });

        // force recompilation of files for which an inclusion/exclusion setting has changed: refresh
        // the source files (to simulate that they have changed), because *.class removal is ignored by
        // the build server process (it looks in source roots only - see
        // org.jetbrains.jps.cmdline.BuildSession.applyFSEvent)
        MiscUtils.invokeWriteActionAndWait(() -> {
            for (final VirtualFile file : toTouch) {
                file.refresh(false, false);
            }
        });

        return items;
    }

    @Override
    @NotNull
    public String getDescription() {
        return "OpenClover dependency injecting compiler";
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
