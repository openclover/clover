package org.openclover.core

import org.openclover.core.context.ContextSet
import org.openclover.core.instr.InstrumentationSessionImpl
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullBranchInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullStatementInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder
import org_openclover_runtime.Clover

import static org.openclover.core.util.Lists.newArrayList
import static org.openclover.core.util.Maps.newHashMap

class CloverTestFixture {

    private final File tmpDir

    private final Map<String, Clover2Registry> registries = newHashMap()

    /**
     * @param dir File
     */
    CloverTestFixture(File dir) {
        if (!dir.isDirectory() || !dir.canWrite()) {
            throw new IllegalArgumentException(
                    "Invalid temporary directory: isDirectory[${dir.isDirectory()}], canWrite[${dir.canWrite()}]".toString())
        }
        this.tmpDir = dir
    }

    /**
     * @return String
     * @throws IOException
     */
    String getTestCoverageDatabase() throws IOException, CloverException {
        List<Clazz> classList = newArrayList()
        classList.add(new Clazz(tmpDir, "org.test", "TestA", new Coverage(0.23f, 0.34f, 0.45f)))

        String initString = createCoverageDB()
        register(initString, classList)
        write(initString, classList)

        return initString
    }

    /**
     * Create a new coverage database.
     *
     * @return String
     * @throws IOException
     */
    String createCoverageDB() throws IOException, CloverException {
        File f = File.createTempFile("coverage", ".db", tmpDir)
        f.delete()

        String initStr = f.getAbsolutePath()

        Clover2Registry registry = getRegistry(initStr)
        registry.saveAndOverwriteFile()
        return initStr
    }

    /**
     * @param initString String
     * @param classList  List
     * @throws IOException
     */
    void register(String initString, List<Clazz> classList) throws IOException, CloverException {
        Clover2Registry registry = getRegistry(initString)
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        for (Clazz clzz : classList) {
            register(session, clzz, new ContextSet())
        }
        session.finishAndApply()
        registry.saveAndOverwriteFile()
    }

    /**
     * @param initString String
     * @param classList  List
     * @throws IOException
     */
    void write(String initString, List<Clazz> classList) throws IOException, CloverException {
        Clover2Registry registry = getRegistry(initString)
        for (Clazz clzz : classList) {
            writeCoverage(registry, clzz)
        }
    }

    void register(InstrumentationSessionImpl session, Clazz clazz, ContextSet con) throws IOException {

        session.enterFile(clazz.getPackageName(), clazz.getSourceFile(), 1, 1, 0L, 0L, 0L)
        MutableSourceRegion reg = new MutableSourceRegion(0, 0)
        clazz.mthds = new FullMethodInfo[clazz.coverage.elements]

        session.enterClass(clazz.getName(), reg, new Modifiers(), false, false, false)

        // n-1 empty methods, leaving one for the statements & branches
        for (int i = 0; i < clazz.mthds.length - 1; i++) {
            reg.setStartColumn(i)
            Modifiers mods = new Modifiers()
            mods.setMask(1)
            MethodSignature signature = new MethodSignature(null, null, null, null, mods, "method" + i, null, "void", null, null)
            clazz.mthds[i] = session.enterMethod(con, reg, signature, false)
            session.exitMethod(0, reg.getStartColumn())
        }

        if (clazz.coverage.elements > 0) {
            reg.setStartColumn(reg.getStartColumn() + 1)
            clazz.mthds[clazz.mthds.length - 1] = session.enterMethod(con, reg, new MethodSignature("nonEmptyMethod"), false)

            clazz.stmts = new FullStatementInfo[clazz.coverage.elements]
            for (int i = 0; i < clazz.stmts.length; i++) {
                clazz.stmts[i] = session.addStatement(con, reg, 1)
            }

            clazz.cnds = new FullBranchInfo[clazz.coverage.elements]
            for (int i = 0; i < clazz.cnds.length; i++) {
                clazz.cnds[i] = session.addBranch(con, reg, true, 1)
            }
            session.exitMethod(0, reg.getEndColumn())
        }
        session.exitClass(0, 0)
        session.exitFile()
    }

    /**
     * @param registry CloverRegistry
     * @param clazz    Clazz
     */
    private void writeCoverage(Clover2Registry registry, Clazz clazz) {

        Coverage coverage = clazz.getCoverage()
        if (coverage != null) {
            FixedSizeCoverageRecorder recorder = (FixedSizeCoverageRecorder) Clover.getRecorder(registry.getInitstring(),
                    registry.getVersion(), 0L, clazz.getCoverage().elements, null, null)

            Runtime.getRuntime().removeShutdownHook(recorder.getShutdownFlusher())

            int coveredStmts = (coverage.statementCoverage * clazz.coverage.elements) as float
            for (int i = 0; i < coveredStmts; i++) {
                recorder.inc(clazz.stmts[i].getDataIndex())
            }
            int coveredConditionals = (coverage.conditionalCoverage * clazz.coverage.elements) as float
            for (int i = 0; i < coveredConditionals; i++) {
                recorder.inc(clazz.cnds[i].getDataIndex())
                recorder.inc(clazz.cnds[i].getDataIndex() + 1)
            }

            int coveredMethods = (coverage.methodCoverage * clazz.coverage.elements) as float
            for (int i = 0; i < coveredMethods; i++) {
                recorder.inc(clazz.mthds[i].getDataIndex())
            }

            recorder.forceFlush()
        }
    }

    private Clover2Registry getRegistry(String initString) throws IOException, CloverException {
        if (!registries.containsKey(initString)) {
            registries.put(initString, Clover2Registry.fromInitString(initString, "test project"))
        }
        return registries.get(initString)
    }

    static class Coverage {
        private float statementCoverage = -1
        private float conditionalCoverage = -1
        private float methodCoverage = -1
        private int elements


        Coverage(float stmt, float cnd, float mthd, int elements) {
            statementCoverage = stmt
            conditionalCoverage = cnd
            methodCoverage = mthd
            this.elements = elements
        }

        Coverage(float stmt, float cnd, float mthd) {
            this(stmt, cnd, mthd, 1000)
        }

        void setStatementCoverage(float v) {
            statementCoverage = v
        }

        void setConditionalCoverage(float v) {
            conditionalCoverage = v
        }

        void setMethodCoverage(float v) {
            methodCoverage = v
        }
    }

    static class Clazz {

        private FullStatementInfo[] stmts = new FullStatementInfo[0]
        private FullBranchInfo[] cnds = new FullBranchInfo[0]
        private FullMethodInfo[] mthds = new FullMethodInfo[0]

        private String name = null
        private String pkgName = null
        private File file = null

        private Coverage coverage = null

        Clazz(File rootDir, String pkgName, String name, Coverage coverage)
                throws IOException {

            this.name = name
            this.pkgName = pkgName
            this.coverage = coverage
            File basedir = rootDir
            if (pkgName != null && pkgName.length() > 0) {
                String pkgPath = pkgName.replace('.', '/')
                basedir = new File(rootDir, pkgPath)
                basedir.mkdirs()
            }

            file = new File(basedir, name + ".java")
            file.createNewFile()
        }

        String getName() {
            return name
        }

        String getPackageName() {
            return pkgName
        }

        File getSourceFile() {
            return file
        }

        Coverage getCoverage() {
            return coverage
        }

        void setCoverage(Coverage coverage) {
            this.coverage = coverage
        }
    }
}
