package com.atlassian.clover.idea;

import com.atlassian.clover.idea.util.MiscUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
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

    @Nullable
    public static VirtualFile getCloverClassBase() {
        try {
            // get location of the Clover JAR
            final URL targetUrl = getSourceCloverJarUrl();
            // return the target location as a VirtualFile, may return null if VFS has not been refreshed
            VirtualFile targetVf = VfsUtil.findFileByURL(targetUrl);
            if (targetVf == null) {
                // VirtualFileManager.refreshAndFindFileByUrl must run inside a write action
                final VirtualFile[] vf = new VirtualFile[1];
                MiscUtils.invokeWriteActionAndWait(() -> {
                    LOG.debug("Clover: VfsUtil.findFileByURL(\"" + targetUrl + "\") returned null. Refreshing VFS.");
                    // VirtualFileManager expects "file:///path/to/clover.jar" and not "file:/path/to/clover.jar"
                    String targetUrlFix = targetUrl.toString().replace("file:/", "file:///");
                    vf[0] = VirtualFileManager.getInstance().refreshAndFindFileByUrl(targetUrlFix);
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

    /**
     * Returns an URL to the clover.jar (or a directory if classes are unpacked)
     * @return URL
     */
    private static URL getSourceCloverJarUrl() {
        try {
            // trick: find a path to the CoverageRecorder class using getResource() method
            @SuppressWarnings({"UnnecessaryFullyQualifiedName"}) // I want this one here
            final Class recorderClass = com_atlassian_clover.CoverageRecorder.class;
            final String path = "/" + recorderClass.getName().replace('.', '/') + ".class";
            final URL recorderClassUrl = recorderClass.getResource(path);
            // now crop the package path part - we'll end up with a root directory or a JAR archive
            return new URL(recorderClassUrl.toString().replace(path, "/"));
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
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

            logDebugPrintCompilationClasses(module);
        }

        rootModel.commit();
        return true;
    }

    /**
     * Returns a list of virtual files containing compilation classes for specified module. We use reflections
     * due to differences between IDEA11 and IDEA12 API.
     * @param module - idea module
     * @return VirtualFile[]
     */
    private static VirtualFile[] getCompilationClasses(Module module) {
         return OrderEnumerator.orderEntries(module).getAllSourceRoots();
    }

    /**
     * Print list of classes for compilation for the given module (and its rootModel).
     */
    private static void logDebugPrintCompilationClasses(Module module) {
        LOG.debug("ModuleRootModel compilation classes: ");
        VirtualFile[] vf = getCompilationClasses(module);
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
