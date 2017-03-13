package com.atlassian.clover.api.optimization

import clover.com.google.common.collect.Lists
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.api.CloverException
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.optimization.Snapshot
import com.atlassian.clover.optimization.TestOptimizationBase
import org.junit.Before
import org.junit.Test

class TestOptimizerTest extends TestOptimizationBase {

    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Override
    void tearDown() throws Exception {
        super.tearDown()
    }

    @Test
    void testOptimize() throws CloverException, IOException {

        // run the tests
        runNoAppClassTest_testMain()
        runAppClass2Test_testMain()
        runAppClass23Test_testMain()
        runAppClass234Test_testMain()

        // store the snapshot
        final CloverDatabase db = CloverDatabase.loadWithCoverage(registry.getInitstring(), new CoverageDataSpec())        
        final Snapshot snapshot = Snapshot.generateFor(db)
        snapshot.store()

        // check that optimize includes 0 tests.
        // for testing, use StringOptimizable, since classes are fake.
        List<Optimizable> classes = Lists.newLinkedList()
        classes.add(new StringOptimizable(THIS_PACKAGE + '.AppClass2Test'))
        classes.add(new StringOptimizable(THIS_PACKAGE + '.AppClass23Test'))
        classes.add(new StringOptimizable(THIS_PACKAGE + '.AppClass234Test'))

        final TestOptimizer optimizer = new TestOptimizer(registry.getInitstring(), snapshot.getLocation())

        final List<Optimizable> optimizedClasses =  optimizer.optimize(classes)
        assertEquals(0, optimizedClasses.size())


        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        addClassWithSingleMethod(session, new ContextSet(), THIS_PACKAGE, "AppClass2345Test", "testMain", true) // add a new test.
        final FullFileInfo fInfo = (FullFileInfo) appClass4_main.getContainingFile()

        session.enterFile(fInfo.getContainingPackage().getName(), fInfo.getPhysicalFile(), 10, 8, System.currentTimeMillis(), 100, 42)
        session.exitFile()

        session.finishAndApply()
        registry.saveAndOverwriteFile()

        classes.add(new StringOptimizable(THIS_PACKAGE + ".AppClass2345Test"))

        final List<Optimizable> optimizedClasses2 =  optimizer.optimize(classes)
        assertEquals(2, optimizedClasses2.size())

    }

}
