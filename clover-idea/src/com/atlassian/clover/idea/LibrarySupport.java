package com.atlassian.clover.idea;

import com.atlassian.clover.idea.util.MiscUtils;
import com.atlassian.clover.util.FileUtils;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This class is used to manage the clover library.
 */
public class LibrarySupport {

    public static final String LIBRARY_NAME = "Clover IDEA Plugin";

    private static final Logger LOG = Logger.getInstance(LibrarySupport.class.getName());

    private LibrarySupport() {
    }

    /**
     * @return Clover library or null if not present
     */
    public static Library lookupCloverLibrary() {
        return getAppLibraryTable().getLibraryByName(LIBRARY_NAME);
    }

    /**
     * @return newy created Clover library
     */
    public static Library createCloverLibrary() {
        final LibraryTable.ModifiableModel tableModel = getAppLibraryTable().getModifiableModel();
        final Library cloverLibrary = tableModel.createLibrary(LIBRARY_NAME);
        tableModel.commit();
        return cloverLibrary;
    }

    /**
     * @return a Clover library with current plugin jar.
     */
    public static Library getValidatedCloverLibrary() {
        Library library = lookupCloverLibrary();
        if (library == null) {
            library = createCloverLibrary();
        }

        final VirtualFile classesBase = getCloverClassBase();
        if (classesBase == null) {
            // error, failed to find clover.jar, don't update the library and it as is
            LOG.info("Unable to update '" + LIBRARY_NAME + "' classpath.");
            return library;
        }

        final Library.ModifiableModel libraryModel = library.getModifiableModel();
        final String[] currentFiles = libraryModel.getUrls(OrderRootType.CLASSES);

        boolean found = false;
        boolean dirty = false;

        for (String currentFile : currentFiles) {
            if (currentFile.equals(classesBase.getUrl())) {
                found = true;
            } else {
                libraryModel.removeRoot(currentFile, OrderRootType.CLASSES);
                dirty = true;
            }
        }
        if (!found) {
            libraryModel.addRoot(classesBase, OrderRootType.CLASSES);
            dirty = true;
        }
        if (dirty) {
            libraryModel.commit();
        }

        return library;
    }

    private static boolean isCopyCloverJarWorkaroundNeeded() {
        // a workaround is needed for IDEA [13.0.0-13.1.0)
        return ApplicationInfo.getInstance().getMajorVersion().equals("13")
                && ApplicationInfo.getInstance().getMinorVersion().startsWith("0");
    }

    @Nullable
    public static VirtualFile getCloverClassBase() {
        try {
            // get location of the Clover JAR
            final URL targetUrl = copyCloverJarIfNeccessary();
            // return the target location as a VirtualFile, may return null if VFS has not been refreshed
            VirtualFile targetVf = VfsUtil.findFileByURL(targetUrl);
            if (targetVf == null) {
                // VirtualFileManager.refreshAndFindFileByUrl must run inside a write action
                final VirtualFile[] vf = new VirtualFile[1];
                MiscUtils.invokeWriteActionAndWait(new Runnable() {
                    @Override
                    public void run() {
                        LOG.debug("Clover: VfsUtil.findFileByURL(\"" + targetUrl + "\") returned null. Refreshing VFS.");
                        // VirtualFileManager expects "file:///path/to/clover.jar" and not "file:/path/to/clover.jar"
                        String targetUrlFix = targetUrl.toString().replace("file:/", "file:///");
                        vf[0] = VirtualFileManager.getInstance().refreshAndFindFileByUrl(targetUrlFix);
                    }
                });
                targetVf = vf[0];

                if (targetVf == null) {
                    LOG.info("Failed to locate Clover JAR '" + targetUrl + "'.");
                }
            }
            return targetVf;
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static URL copyCloverJarIfNeccessary() {
        try {
            // determine source/target location for Clover JAR
            final URL baseUrl = getSourceCloverJarUrl();
            final File sourceFile = getSourceCloverJarFile(baseUrl);
            final File targetFile = getTargetCloverJarFile();
            final URL targetUrl = getTargetCloverJarUrl(baseUrl, targetFile);

            // copy JAR to a different location
            copyCloverJarIfNecessary(sourceFile, targetFile);
            return targetUrl;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void copyCloverJarIfNecessary(File sourceFile, File targetFile) throws IOException {
        if (isCopyCloverJarWorkaroundNeeded()) {
            // copy clover-idea.jar to a directory outside plugins dir - it's a workaround for
            // https://jira.atlassian.com/browse/CLOV-1395 and http://youtrack.jetbrains.com/issue/IDEA-118928
            if (!sourceFile.isDirectory()) {
                // case for a Clover JAR file
                // optimization: copy only if file differs to avoid unnecessary copying before every build
                if (!targetFile.isFile() || sourceFile.length() != targetFile.length()
                        || sourceFile.lastModified() != targetFile.lastModified()) {
                    targetFile.delete();  // delete existing one (especially if it's a directory)
                    FileUtils.fileCopy(sourceFile, targetFile);
                    targetFile.setLastModified(sourceFile.lastModified());
                }
            } else {
                // case for classes stored in a directory (development / debugging)
                FileUtils.dirCopy(sourceFile, targetFile, true);
            }
        }
    }

    /**
     * Returns an URL to the clover.jar (or a directory if classes are unpacked)
     * @return URL
     */
    private static URL getSourceCloverJarUrl() throws MalformedURLException {
        // trick: find a path to the CoverageRecorder class using getResource() method
        @SuppressWarnings({"UnnecessaryFullyQualifiedName"}) // I want this one here
        final Class recorderClass = com_atlassian_clover.CoverageRecorder.class;
        final String path = "/" + recorderClass.getName().replace('.', '/') + ".class";
        final URL recorderClassUrl = recorderClass.getResource(path);
        // now crop the package path part - we'll end up with a root directory or a JAR archive
        return new URL(recorderClassUrl.toString().replace(path, "/"));
    }

    /**
     * Returns an URL pointing to a location into which a clover.jar shall be copied.
     * It either points to the <code>originalUrl</code> or to a <code>targetFile</code> (workaround
     * for the IDEA13).
     *
     * @param originalUrl original location of the clover.jar (typically in config/plugins directory)
     * @param targetFile  alternative location for the clover.jar (typically in temporary directory)
     * @return URL targetUrl
     */
    private static URL getTargetCloverJarUrl(URL originalUrl, File targetFile) throws URISyntaxException, MalformedURLException {
        if (isCopyCloverJarWorkaroundNeeded()) {
            return targetFile.toURI().toURL();
        } else {
            // not a faulty IDEA13 so we can take a JAR from plugins' directory
            return originalUrl;
        }
    }

    /**
     * Converts a URL into a File
     *
     * @param sourceURL URL pointing to clover.jar (or a directory)
     * @return File pointing to clover.jar (or a directory)
     * @throws URISyntaxException
     */
    private static File getSourceCloverJarFile(URL sourceURL) throws URISyntaxException {
        final URI baseUri = "jar".equals(sourceURL.toURI().getScheme())
                // "jar:file:/...jar!/" case - crop "jar:" at the beginning and "!/" at the end
                ? new URI(
                StringUtils.removeEnd(sourceURL.toURI().getSchemeSpecificPart(), "!/")
                        .replace(" ", "%20"))                // OS X fails to parse space
                // "file:/...somedir/" case
                : sourceURL.toURI();
        return new File(baseUri);
    }

    /**
     * Returns a temporary location for a clover.jar
     * @return File new location
     */
    private static File getTargetCloverJarFile() {
        return new File(FileUtils.getJavaTempDir(), "clover-idea.jar");
    }

    /**
     * Add specified library to the module. Returns true if the module root model has been changed.
     * @param lib library to be added
     * @param module module we're adding to
     * @return boolean - true if model has changed
     */
    public static boolean addLibraryTo(Library lib, Module module) {
        LOG.debug("addLibraryTo(" + lib.getName() + ", " + module.getName() + ");");
        final OrderEntry[] existingEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        if (existingEntries.length > 0 && existingEntries[0] instanceof LibraryOrderEntry && lib.equals(((LibraryOrderEntry)existingEntries[0]).getLibrary())) {
            return false;
        }

        // note: it's mandatory to call commit() or dispose() for ModifiableRootModel
        final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();

        final LibraryOrderEntry existingEntry = rootModel.findLibraryOrderEntry(lib);
        if (existingEntry != null) {
            final OrderEntry[] orderEntries = rootModel.getOrderEntries();
            reorderEntries(orderEntries, existingEntry);
            rootModel.rearrangeOrderEntries(orderEntries);
        } else {
            LOG.debug("Library not found in module. Adding. +++++++++++++");

            final LibraryOrderEntry orderEntry = rootModel.addLibraryEntry(lib);

            final OrderEntry[] orderEntries = rootModel.getOrderEntries();
            if (reorderEntries(orderEntries, orderEntry)) {
                rootModel.rearrangeOrderEntries(orderEntries);
            }

            logDebugPrintCompilationClasses(module, rootModel);
        }

        rootModel.commit();
        return true;
    }

    /**
     * Returns a list of virtual files containing compilation classes for specified module. We use reflections
     * due to differences between IDEA11 and IDEA12 API.
     * @param module - idea module
     * @param rootModel - must be a reference to ModuleRootManager.getInstance(module).getModifiableModel()
     * @return VirtualFile[]
     */
    private static VirtualFile[] getCompilationClasses(Module module, ModifiableRootModel rootModel) {
        VirtualFile[] vf;

        try {
            // try IDEA12+ API:
            // OrderEnumerator orderEnumerator = OrderEnumerator.orderEntries(module);
            // vf = orderEnumerator.getAllSourceRoots();
            Method orderEntries = Class.forName("com.intellij.openapi.roots.OrderEnumerator").getMethod("orderEntries", Module.class);
            Object orderEnumerator = orderEntries.invoke(null, module);
            Method getAllSourceRoots = orderEnumerator.getClass().getMethod("getAllSourceRoots");
            vf = (VirtualFile[])getAllSourceRoots.invoke(orderEnumerator);
        } catch (Exception ex12) {
            // if fails, try IDEA9-IDEA11 API:
            // vf = rootModel.getOrderedRoots(OrderRootType.COMPILATION_CLASSES)
            try {
                Method getOrderedRoots = ModifiableRootModel.class.getMethod("getOrderedRoots", OrderRootType.class);
                Field COMPILATION_CLASSES = OrderRootType.class.getField("COMPILATION_CLASSES");
                vf = (VirtualFile[])getOrderedRoots.invoke(rootModel, COMPILATION_CLASSES.get(null));
            } catch (Exception ex11) {
                throw new UnsupportedOperationException("Failed to get list of classes roots via reflections", ex11);
            }
        }

        return vf;
    }

    /**
     * Print list of classes for compilation for the given module (and its rootModel).
     */
    private static void logDebugPrintCompilationClasses(Module module, ModifiableRootModel rootModel) {
        LOG.debug("ModuleRootModel compilation classes: ");
        VirtualFile[] vf = getCompilationClasses(module, rootModel);
        for (int i = 0; i < vf.length; i++) {
            final VirtualFile f = vf[i];
            LOG.debug("" + i + ": " + f.getPresentableUrl());
        }
    }


    /*private*/ static boolean reorderEntries(OrderEntry[] entries, OrderEntry makeFirst) {
        int i = entries.length - 1;
        while (i >=0 && entries[i] != makeFirst) {
            i--;
        }
        if (i < 1) {
            return false;
        }
        // i = index of makeFirst in the array
        for (; i > 0; i--) {
            entries[i] = entries[i-1];
        }
        entries[0] = makeFirst;
        return true;
    }

    public static boolean removeCloverLibraryFrom(final Module module) {
        return removeLibraryFrom(module, LIBRARY_NAME);
    }

    // returns true if the module root model has been changed
    public static boolean removeLibraryFrom(final Module module, final String libraryName) {
        if (containsLibrary(module, libraryName)) {
            final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            for (OrderEntry entry : rootModel.getOrderEntries()) {
                if (libraryName.equals(entry.getPresentableName())) {
                    rootModel.removeOrderEntry(entry);
                }
            }
            rootModel.commit();
            return true;
        }

        return false;
    }

    private static boolean containsLibrary(Module module, String libraryName) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (libraryName.equals(entry.getPresentableName())) {
                return true;
            }
        }
        return false;
    }

    private static LibraryTable getAppLibraryTable() {
        return LibraryTablesRegistrar.getInstance().getLibraryTable();
    }
}
