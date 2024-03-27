package org.openclover.eclipse.core.compiler;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.ClassFilePool;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.openclover.eclipse.core.projects.builder.BaseInstrumenter;
import org.openclover.eclipse.core.projects.builder.InstrumentationProjectPathMap;
import org.openclover.eclipse.core.projects.builder.PathUtils;
import org.openclover.runtime.CloverNames;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;
import static org.openclover.eclipse.core.CloverPlugin.logError;

/**
 * This lives in org.eclipse.jdt.internal.compiler.batch to allow package-protected access
 */
public class CloverCompiler extends Main {
    private static final String RECORDER_CLASS_INTERFIX = "$" + CloverNames.CLOVER_PREFIX;
    private static final int CLASSFILE_BUFFER_SIZE = 4096;
    private static final ReleaseInvoker RELEASE_INVOKER;

    static {
        ReleaseInvoker invoker = ReleaseInvoker.UNAVAILABLE;
        try {
            invoker = new ReflectionReleaseInvoker();
        } catch (Throwable t) {
            logError("Unable to release any classes from the pool during compilation", t);
        }
        RELEASE_INVOKER = invoker;
    }

    
    private interface ReleaseInvoker {
        ReleaseInvoker UNAVAILABLE = (compiler, classFile) -> {};

        void release(CloverCompiler compiler, ClassFile classFile);
    }

    private static class ReflectionReleaseInvoker implements ReleaseInvoker {
        //TODO: By not using weakrefs are we in practice causing big memory retention here?
        private final Field ClassFile_isShared;
        private final Field Main_batchCompiler;
        private final Field Compiler_lookupEnvironment;
        private final Field LookupEnvironment_classFilePool;
        private final Method ClassFilePool_release;

        private ReflectionReleaseInvoker() throws NoSuchMethodException, NoSuchFieldException {
            ClassFile_isShared = ClassFile.class.getDeclaredField("isShared");
            ClassFile_isShared.setAccessible(true);
            Main_batchCompiler = Main.class.getDeclaredField("batchCompiler");
            Main_batchCompiler.setAccessible(true);
            Compiler_lookupEnvironment = org.eclipse.jdt.internal.compiler.Compiler.class.getDeclaredField("lookupEnvironment");
            Compiler_lookupEnvironment.setAccessible(true);
            LookupEnvironment_classFilePool = LookupEnvironment.class.getDeclaredField("classFilePool");
            LookupEnvironment_classFilePool.setAccessible(true);
            ClassFilePool_release = ClassFilePool.class.getDeclaredMethod("release", ClassFile.class);
            ClassFilePool_release.setAccessible(true);
        }

        @Override
        public void release(CloverCompiler compiler, ClassFile classFile) {
            try {
                //if (classFile.isShared)
                if (ClassFile_isShared.getBoolean(classFile)) {
                    final Object batchCompiler = Main_batchCompiler.get(compiler);
                    final Object lookupEnvironment = Compiler_lookupEnvironment.get(batchCompiler);
                    final Object classFilePool = LookupEnvironment_classFilePool.get(lookupEnvironment);
                    ClassFilePool_release.invoke(classFilePool, classFile);
                }
            } catch (Throwable e) {
                logError("Unable to release class " + classFile + " from pool", e);
            }
        }
    }

    private final BaseInstrumenter instrumenter;
    private final InstrumentationProjectPathMap pathMap;
    private final IProgressMonitor monitor;
    private final Map<IContainer, Set<String>> dirsToRecorderClassNames;
    private final Map<IContainer, Set<String>> dirsToRecorderClassBaseNames;

    public CloverCompiler(PrintWriter out, PrintWriter err, BaseInstrumenter instrumenter,
                          InstrumentationProjectPathMap pathMap, IProgressMonitor monitor) {
        super(out, err, false, null, null);
        this.instrumenter = instrumenter;
        this.pathMap = pathMap;
        this.monitor = monitor;
        this.dirsToRecorderClassNames = newHashMap();
        this.dirsToRecorderClassBaseNames = newHashMap();
    }

    @Override
    public boolean compile(String[] strings) {
        boolean result = super.compile(strings);
        try {
            removeOldRecorderClasses();
        } catch (CoreException e) {
            logError("Failed removing some old recorder classes", e);
        }
        return result;
    }

    @Override
    public void outputClassFiles(CompilationResult unitResult) {
        if (!((unitResult == null) || (unitResult.hasErrors() && !this.proceedOnError))) {
            if (!monitor.isCanceled()) {
                try {
                    ClassFile[] classFiles = unitResult.getClassFiles();
                    CompilationUnit compilationUnit = (CompilationUnit)unitResult.compilationUnit;
                    String cuFileName = new String(compilationUnit.getFileName());
                    IPath destinationPath = pathMap.getOutputRootForWorkignAreaSourceResource(new Path(cuFileName));
                    IResource currentDestination = ensureOutputContainerCreated(destinationPath);

                    if (currentDestination instanceof IContainer) {
                        for (ClassFile classFile : classFiles) {
                            String baseName = new String(classFile.fileName()).replace('/', File.separatorChar);
                            String classFileName = baseName + ".class";
                            try {
                                writeToDisk(
                                        (IContainer) currentDestination,
                                        classFileName,
                                        classFile);
                                RELEASE_INVOKER.release(this, classFile);
                            } catch (IOException e) {
                                logError("Unable to write class for file " + classFileName, e);
                            }
                        }
                    } else {
                        logError(
                            "Unable to write class file as container "
                                + (destinationPath == null
                                ? ""
                                : " " + destinationPath.toOSString() + " ")
                                + "doesn't exist");
                    }
                } catch (Exception e) {
                    logError("Unable to write classes for source file " + new String(unitResult.getFileName()), e);
                }
            }
        }
    }

    @Override
    public CompilationUnit[] getCompilationUnits() /* throws InvalidInputException - not declared since Eclipse 3.5 */ {
        CompilationUnit[] result = null;
        try {
            result = super.getCompilationUnits(); /* throws InvalidInputException in Eclipse 3.4 */
            for (int i = 0; i < result.length; i++) {
                final String filename = new String(result[i].getFileName());
                if (instrumenter.willRenderContentsFor(filename)) {
                    final char[] customContents = instrumenter.getContentsFor(filename);
                    result[i] = new CompilationUnit(null, filenames[i], "UTF-8") {
                        @Override
                        public char[] getContents() {
                            return customContents;
                        }
                    };
                }
            }
        } catch (Exception ex) {
            logError("Exception caught: "  + ex);
        }
        return result;
    }

    private IContainer ensureOutputContainerCreated(IPath workspaceRelativePath) {
        try {
            IContainer container = PathUtils.containerFor(workspaceRelativePath);
            if (container instanceof IProject) {
                return container;
            } else {
                IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(workspaceRelativePath);
                if (folder != null) {
                    return (IFolder)PathUtils.makeDerivedFoldersFor(folder);
                } else {
                    logError("Unable to create output folder " + workspaceRelativePath);
                }
            }
        } catch (Exception e) {
            logError("Unable to create output folder " + workspaceRelativePath, e);
        }
        return null;
    }

    private boolean isRecorderClass(String baseName) {
        return baseName.contains(RECORDER_CLASS_INTERFIX);
    }

    private void removeOldRecorderClasses() throws CoreException {
        for (Map.Entry<IContainer, Set<String>> entry : dirsToRecorderClassNames.entrySet()) {
            final IContainer container = entry.getKey();
            final Set<String> newRecorderClasses = entry.getValue();
            container.accept(proxy -> {
                if ((proxy.getType() == IResource.FOLDER || proxy.getType() == IResource.PROJECT) && proxy.getName().equals(container.getName())) {
                    return true;
                } else if (proxy.getType() == IResource.FILE) {
                    final String name = proxy.getName();
                    if (name.indexOf(RECORDER_CLASS_INTERFIX) > 0 && !newRecorderClasses.contains(name)) {
                        Set<String> recorderBaseClasses = dirsToRecorderClassBaseNames.get(container);
                        if (recorderBaseClasses != null) {
                            for (String recorderBaseClass : recorderBaseClasses) {
                                if (name.indexOf(recorderBaseClass) == 0) {
                                    proxy.requestResource().delete(true, null);
                                    break;
                                }
                            }
                        }
                    }
                }
                return false;
            }, IResource.DEPTH_ONE);
        }
    }

    private void writeToDisk(IContainer outputRoot, String packageFileName, ClassFile classFile) throws IOException, CoreException {
        final IFile file = outputRoot.getFile(new Path(packageFileName));

        final boolean isRecorder = isRecorderClass(file.getName());

        if (isRecorder) {
            registerRecorderClass(file);
        }

        if (!file.getParent().exists()) {
            PathUtils.makeFoldersFor(file);
        }

        long timestamp = System.currentTimeMillis();
        if (!file.exists()) {
            writeClassToFile(file, true, classFile, isRecorder);
        } else {
            timestamp = file.getLocalTimeStamp();
            writeClassToFile(file, false, classFile, isRecorder);
        }
        file.setLocalTimeStamp(timestamp);
    }

    private void writeClassToFile(IFile file, boolean create, ClassFile classFile, boolean isRecorder) throws IOException, CoreException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(CLASSFILE_BUFFER_SIZE);
        try (baos) {
            final BufferedOutputStream bos = new BufferedOutputStream(baos, CLASSFILE_BUFFER_SIZE);
            if (isRecorder) {
                writeRawClassTo(classFile, baos);
            } else {
                writeSanitisedClassTo(classFile, baos);
            }
            bos.flush();
            if (create) {
                file.create(
                        new ByteArrayInputStream(baos.toByteArray()),
                        true,
                        null);
            } else {
                file.setContents(
                        new ByteArrayInputStream(baos.toByteArray()),
                        true,
                        true,
                        null);
            }
        }
    }

    private OutputStream writeRawClassTo(ClassFile classFile, OutputStream os) throws IOException {
        os.write(classFile.header, 0, classFile.headerOffset);
        os.write(classFile.contents, 0, classFile.contentsOffset);
        return os;
    }

    private void writeSanitisedClassTo(ClassFile classFile, OutputStream os) throws IOException {
        os.write(
            removeCloverInnerClassReferece(
                ((ByteArrayOutputStream)writeRawClassTo(classFile, new ByteArrayOutputStream(CLASSFILE_BUFFER_SIZE))).toByteArray()));
    }

    private void registerRecorderClass(IFile file) {
        registerRecorderName(file);
        registerRecorderBaseNames(file);
    }

    private void registerRecorderBaseNames(IFile file) {
        Set<String> recorderBaseNames = dirsToRecorderClassBaseNames.computeIfAbsent(file.getParent(), k -> newHashSet());
        recorderBaseNames.add(
            file.getName().substring(
                0,
                file.getName().indexOf(RECORDER_CLASS_INTERFIX) + RECORDER_CLASS_INTERFIX.length()));
    }

    private void registerRecorderName(IFile file) {
        Set<String> recorderNames = dirsToRecorderClassNames.computeIfAbsent(file.getParent(), k -> newHashSet());
        recorderNames.add(file.getName());
    }

    public byte[] removeCloverInnerClassReferece(byte[] classBytes) throws IOException {
        final ClassReader reader = new ClassReader(new ByteArrayInputStream(classBytes));
        final ClassWriter writer = new ClassWriter(0);
        reader.accept(new RecorderInnerClassRemover(Opcodes.ASM5, writer), 0);
        return writer.toByteArray();
    }

    public static class RecorderInnerClassRemover extends ClassVisitor {
        public RecorderInnerClassRemover(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (!name.contains(CloverNames.CLOVER_RECORDER_PREFIX)) {
                super.visitInnerClass(name, outerName, innerName, access);
            }
        }
    }

}
