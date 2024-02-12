package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.core.instr.java.FileInstrumentationSource;
import org.openclover.core.instr.java.InstrumentationSource;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.runtime.api.CloverException;

import java.io.CharArrayWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryInstrumenter extends BaseInstrumenter {
    private Map originalToInstrumentedSource;

    public InMemoryInstrumenter(CloverProject project, Clover2Registry registry, InstrumentationProjectPathMap pathMap, IProgressMonitor monitor, int buildKind) throws CoreException {
        super(monitor, pathMap, project, registry, buildKind);
        originalToInstrumentedSource = new LinkedHashMap();
    }

    @Override
    protected void instrumentSource(IFile originalFile) throws CloverException, CoreException {
        maybeInitialiseInstrumentation();

        removeMarkers(originalFile);
        monitor.subTask("Instrumenting " + originalFile.getFullPath());

        CharArrayWriter out = new CharArrayWriter(2056);
        try {
            final InstrumentationSource input = new FileInstrumentationSource(originalFile.getLocation().toFile(),
                    originalFile.getCharset());
            instrumenter.instrument(input, out, originalFile.getCharset());
            originalToInstrumentedSource.put(originalFile.getLocation().toFile().getAbsolutePath(), out.toCharArray());
        } catch (Exception e) {
            addInstrumentationFailure(originalFile, e);
        }

        hasInstrumented = true;
        
        monitor.worked(1);
    }

    @Override
    public Iterator fileNamesAsCompilerArg() {
        return new Iterator() {
            private Iterator iter = originalToInstrumentedSource.keySet().iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Object next() {
                return iter.next() + "[UTF-8]";
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected void copySource(IFile originalFile) throws CloverException, CoreException {
        //TODO: implement
        throw new UnsupportedOperationException();
    }

    @Override
    public char[] getContentsFor(String filename) {
        return (char[]) originalToInstrumentedSource.get(filename);
    }

    @Override
    public boolean willRenderContentsFor(String filename) {
        return originalToInstrumentedSource.containsKey(filename);
    }
}
