package org.openclover.eclipse.core.projects.builder;

import org.openclover.runtime.api.CloverException;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.core.util.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileBasedInstrumenter extends BaseInstrumenter {
    protected final Map originalToInstrumentedFileAndEncoding;

    public FileBasedInstrumenter(CloverProject project, Clover2Registry registry, InstrumentationProjectPathMap pathMap, IProgressMonitor monitor, int buildKind) throws CoreException {
        super(monitor, pathMap, project, registry, buildKind);
        this.originalToInstrumentedFileAndEncoding = new LinkedHashMap();
    }

    @Override
    protected void copySource(IFile originalFile) throws CloverException, CoreException {
        IPath originalSourceRoot =
            instrumentationMapper.getOriginalSourceRootFor(originalFile.getFullPath());

        IPath instrumentedSourceRoot =
            instrumentationMapper.getDisplacedSourceRootFor(originalFile.getFullPath());

        IPath copyPath =
            instrumentedSourceRoot.append(
                originalFile.getFullPath().removeFirstSegments(originalSourceRoot.segmentCount()));

        File copiedFile = copyPath.toFile();
        copiedFile.getParentFile().mkdirs();
        try {
            FileUtils.fileCopy(
                originalFile.getLocation().toFile(),
                copiedFile);
        } catch (IOException e) {
            throw new CloverException("Unable to copy excluded source file for compilation", e);
        }
        originalToInstrumentedFileAndEncoding.put(originalFile, new FileNameWithEncoding(copiedFile.getAbsolutePath(), originalFile.getCharset()));
        monitor.worked(1);
    }

    @Override
    protected void instrumentSource(IFile originalFile) throws CloverException, CoreException {
        IPath instrumentedSourceRoot =
            instrumentationMapper.getDisplacedSourceRootFor(originalFile.getFullPath());

        if (instrumentedSourceRoot != null) {
            maybeInitialiseInstrumentation();

            removeMarkers(originalFile);

            monitor.subTask("Instrumenting " + originalFile.getFullPath());
            try {
                // configure and run instrumenter
                File instrumentedFile = instrumenter.instrument(
                        originalFile.getLocation().toFile(),
                        instrumentedSourceRoot.toFile(),
                        originalFile.getCharset());

                // keep "original file=>instrumented file" mapping
                FileNameWithEncoding fileNameWithEncoding = new FileNameWithEncoding(
                        instrumentedFile.getAbsolutePath(),
                        originalFile.getCharset());
                originalToInstrumentedFileAndEncoding.put(
                        originalFile.getLocation().toFile().getAbsolutePath(),
                        fileNameWithEncoding);
            } catch (CloverException e) {
                addInstrumentationFailure(originalFile, e);
            }

            hasInstrumented = true;
        }
        monitor.worked(1);
    }

    @Override
    public Iterator fileNamesAsCompilerArg() {
        return new Iterator() {
            private Iterator iter = originalToInstrumentedFileAndEncoding.values().iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Object next() {
                FileNameWithEncoding fnwi = (FileNameWithEncoding) iter.next();
                return fnwi.getFileName() + "[" + fnwi.getEncoding() + "]";
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean willRenderContentsFor(String filename) {
        return false;
    }
}
