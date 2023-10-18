package com.atlassian.clover.idea;

import com.atlassian.clover.util.FileUtils;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestCase;

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class LibrarySupportIdeaTest extends IdeaTestCase {
    public static final String LIB_NAME = "CloverLibrarySupportTestLibraryName";

    private Library testLibrary;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testLibrary = createTestLibrary();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteTestLibrary(testLibrary);
        super.tearDown();
    }

    public void testAddRemoveLibrary() {
        Module module = getModule();
        assertEquals(0, countEntries(module, LIB_NAME));
        assertTrue(wrapAdd(module));
        assertEquals(1, countEntries(module, LIB_NAME));
        assertFalse(wrapAdd(module));
        assertEquals(1, countEntries(module, LIB_NAME));

        assertTrue(wrapRemove(module));
        assertEquals(0, countEntries(module, LIB_NAME));
        assertFalse(wrapRemove(module));
        assertEquals(0, countEntries(module, LIB_NAME));


    }

    public void testOrderedAddLibrary() {
        final Module module = getModule();

        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable();
        final Library existing = ApplicationManager.getApplication().runWriteAction(new Computable<Library>() {
            @Override
            public Library compute() {
                return table.createLibrary("Existing Library");
            }
        });
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                LibrarySupport.addLibraryTo(existing, module);
            }
        });

        wrapAdd(module);

        final OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        assertEquals(testLibrary, ((LibraryOrderEntry)orderEntries[0]).getLibrary());
    }

    public void testOrderExistingLibrary() {
        final Module module = getModule();

        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable();
        wrapAdd(module);
        final Library existing = ApplicationManager.getApplication().runWriteAction(new Computable<Library>() {
            @Override
            public Library compute() {
                return table.createLibrary("Existing Library");
            }
        });
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                LibrarySupport.addLibraryTo(existing, module);
            }
        });

        // check preconditions
        final OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        assertEquals(existing, ((LibraryOrderEntry)orderEntries[0]).getLibrary());

        wrapAdd(module);
        final OrderEntry[] orderEntries2 = ModuleRootManager.getInstance(module).getOrderEntries();
        assertEquals(testLibrary, ((LibraryOrderEntry)orderEntries2[0]).getLibrary());

    }

    private boolean isIdea13_0_x() {
        return ApplicationInfo.getInstance().getMajorVersion().equals("13") &&
                ApplicationInfo.getInstance().getMinorVersion().startsWith("0");
    }

    public void testGetValidatedCloverLibrary_NormalBehaviour() throws Exception {
        // running test inside write action because getValidatedCloverLibrary can call createCloverLibrary which is a
        // write operation; in real application we call getValidatedCloverLibrary from the addCloverLibrary which is
        // encapsulated already
        ApplicationTestHelper.runWriteAction(new ApplicationTestHelper.Action() {
            @Override
            public void run() throws Exception {
                if (!isIdea13_0_x()) { // normal behaviour for all versions != IDEA 13.0.x
                    Library library = LibrarySupport.getValidatedCloverLibrary();
                    VirtualFile[] virtualFiles = library.getRootProvider().getFiles(OrderRootType.CLASSES);
                    assertEquals(1, virtualFiles.length);

                    // check that the library path has matches the location of the CoverageRecorder.class, i.e.:
                    // recorderClassUrl=file:/{virtualFiles[0].getPath()}/com_atlassian_clover/CoverageRecorder.class
                    final Class recorderClass = com_atlassian_clover.CoverageRecorder.class;
                    final String path = "/" + recorderClass.getName().replace('.', '/') + ".class";
                    final URL recorderClassUrl = recorderClass.getResource(path);
                    assertTrue(recorderClassUrl.toString().contains(virtualFiles[0].getPath()));

                    // cleanup
                    deleteTestLibrary(library);
                }
            }
        });
    }

    public void testGetValidatedCloverLibrary_Workaround() throws Exception {
        // running test inside write action because getValidatedCloverLibrary can call createCloverLibrary which is a
        // write operation; in real application we call getValidatedCloverLibrary from the addCloverLibrary which is
        // encapsulated already
        ApplicationTestHelper.runWriteAction(new ApplicationTestHelper.Action() {
            @Override
            public void run() throws Exception {
                if (isIdea13_0_x()) { // workaround is for IDEA 13.0.x only
                    Library library = LibrarySupport.getValidatedCloverLibrary();
                    VirtualFile[] virtualFiles = library.getRootProvider().getFiles(OrderRootType.CLASSES);
                    assertEquals(1, virtualFiles.length);

                    // check that the library path is located in the temporary directory, i.e.:
                    // virtualFiles[0].getPath={java.io.tmpdir}/clover-idea.jar
                    // ignore file separator ("\" vs "/") and a letter case ("C:" vs "c:" on windows) in this test
                    assertThat(FileUtils.getNormalizedPath(virtualFiles[0].getPath()).toLowerCase(Locale.ENGLISH),
                            startsWith(
                                    FileUtils.getNormalizedPath(System.getProperty("java.io.tmpdir")).toLowerCase(Locale.ENGLISH)));

                    // cleanup
                    deleteTestLibrary(library);
                }
            }
        });
    }

    public void testReorder() {
        OrderEntry[] empty = new OrderEntry[0];
        assertFalse(LibrarySupport.reorderEntries(empty, null));

        OrderEntry o1 = mock(OrderEntry.class, "OrderEntry 1");
        OrderEntry o2 = mock(OrderEntry.class, "OrderEntry 2");
        OrderEntry o3 = mock(OrderEntry.class, "OrderEntry 3");
        OrderEntry o4 = mock(OrderEntry.class, "OrderEntry 4");
        OrderEntry[] one = new OrderEntry[] { o1 };

        assertFalse(LibrarySupport.reorderEntries(one, null));
        assertSame(o1, one[0]);
        assertFalse(LibrarySupport.reorderEntries(one, o2));
        assertSame(o1, one[0]);
        assertFalse(LibrarySupport.reorderEntries(one, o1)); // no reordering done, really
        assertSame(o1, one[0]);

        OrderEntry[] two = new OrderEntry[] { o1, o2 };

        assertFalse(LibrarySupport.reorderEntries(two, o3));
        assertEquals(Arrays.asList(o1, o2), Arrays.asList(two));
        assertFalse(LibrarySupport.reorderEntries(two, o1));
        assertEquals(Arrays.asList(o1, o2), Arrays.asList(two));
        assertTrue(LibrarySupport.reorderEntries(two, o2));
        assertEquals(Arrays.asList(o2, o1), Arrays.asList(two));

        OrderEntry[] four = new OrderEntry[] {o1, o2, o3, o4};
        assertFalse(LibrarySupport.reorderEntries(four, mock(OrderEntry.class, "OrderEntry 5")));
        assertEquals(Arrays.asList(o1, o2, o3, o4), Arrays.asList(four));
        assertFalse(LibrarySupport.reorderEntries(four, o1));
        assertEquals(Arrays.asList(o1, o2, o3, o4), Arrays.asList(four));

        assertTrue(LibrarySupport.reorderEntries(four, o2));
        assertEquals(Arrays.asList(o2, o1, o3, o4), Arrays.asList(four));

        four = new OrderEntry[] {o1, o2, o3, o4};
        assertTrue(LibrarySupport.reorderEntries(four, o3));
        assertEquals(Arrays.asList(o3, o1, o2, o4), Arrays.asList(four));

        four = new OrderEntry[] {o1, o2, o3, o4};
        assertTrue(LibrarySupport.reorderEntries(four, o4));
        assertEquals(Arrays.asList(o4, o1, o2, o3), Arrays.asList(four));
    }

    private boolean wrapAdd(final Module module) {
        return ApplicationManager.getApplication().runWriteAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return LibrarySupport.addLibraryTo(testLibrary, module);
            }
        });
    }

    private boolean wrapRemove(final Module module) {
        return ApplicationManager.getApplication().runWriteAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return LibrarySupport.removeLibraryFrom(module, LIB_NAME);
            }
        });
    }


    private static int countEntries(Module module, String entry) {
        int count = 0;
        for (OrderEntry o : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry.equals(o.getPresentableName())) {
                count++;
            }
        }
        return count;
    }

    private static Library createTestLibrary() {
        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable();
        return ApplicationManager.getApplication().runWriteAction(new Computable<Library>() {
            @Override
            public Library compute() {
                return table.createLibrary(LIB_NAME);
            }
        });
    }

    private static void deleteTestLibrary(final Library lib) {
        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                table.removeLibrary(lib);
            }
        });

    }
}
