package org.openclover.core.registry.metrics

import org.junit.Test
import org.openclover.core.api.registry.HasMetrics

import static org.junit.Assert.assertEquals

class MetricsTest {

    HasMetricsNode owner = new HasMetricsNode() {
        String getName() {
            return null
        }

        org.openclover.core.api.registry.BlockMetrics getMetrics() {
            return null
        }

        org.openclover.core.api.registry.BlockMetrics getRawMetrics() {
            return null
        }

        void setMetrics(org.openclover.core.api.registry.BlockMetrics metrics) {
            
        }

        String getChildType() {
            return null
        }

        boolean isEmpty() {
            return true
        }

        int getNumChildren() {
            return 0
        }

        HasMetricsNode getChild(int i) {
            return null
        }

        int getIndexOfChild(HasMetricsNode child) {
            return 0
        }

        boolean isLeaf() {
            return false
        }

        void setComparator(Comparator<HasMetrics> cmp) {
            
        }
    }

    @Test
    void testBlockMetrics() {
        BlockMetrics m1 = new BlockMetrics(owner)
        MetricsHelper.setBlockMetrics(m1, 20, 10, 20, 5, 15, 1, 1, 0, 0, 0.10f)
        checkBlockMetrics(m1, 0.5f, 0.25f, 0.375f, 0.75f, 1.0f, 0.10, 0.10)

        BlockMetrics m2 = new BlockMetrics(owner)
        MetricsHelper.setBlockMetrics(m2, 10, 10, 20, 10, 10, 1, 0, 1, 0, 0.20f)
        m1.add(m2)
        checkBlockMetrics(m1, 0.666f, 0.375f, 0.5f, 0.833f, 0.5f, 0.30, 0.15)
    }

    @Test
    void testClassMetrics() {
        ClassMetrics m1 = new ClassMetrics(owner)
        MetricsHelper.setClassMetrics(m1, 100, 50, 10, 10, 30, 10, 4, 3, 3, 1.0f, 10, 5)
        checkClassMetrics(m1, 0.5f, 1.0f, 0.542f, 0.3f, 0.4f, 1.0, 0.1, 3.0f, 10f)
        ClassMetrics m2 = new ClassMetrics(owner)
        MetricsHelper.setClassMetrics(m2, 100, 50, 10, 10, 30, 5, 0, 1, 4, 0.5f, 10, 5)
        m1.add(m2)
        checkClassMetrics(m1, 0.5f, 1.0f, 0.542f, 0.3f, 0.266f, 1.5, 0.1, 3.0f, 10f)
    }

    @Test
    void testFileMetrics() {
        FileMetrics m1 = new FileMetrics(owner)
        MetricsHelper.setFileMetrics(m1, 100, 75, 50, 10, 20, 10, 4, 3, 3, 1.0f, 10, 9, 1, 222, 111)
        checkFileMetrics(m1, 0.75f, 0.2f, 0.587f, 0.2f, 0.4f, 1.0, 0.1, 2.0f, 10f, 10f)
        FileMetrics m2 = new FileMetrics(owner)
        MetricsHelper.setFileMetrics(m2, 100, 75, 50, 10, 20, 5, 0, 1, 4, 0.5f, 10, 9, 1, 444, 333)
        m1.add(m2)
        checkFileMetrics(m1, 0.75f, 0.2f, 0.587f, 0.2f, 0.266f, 1.5, 0.1, 2.0f, 10f, 10f)
    }

    @Test
    void testPackageMetrics() {
        PackageMetrics m1 = new PackageMetrics(owner)
        MetricsHelper.setPackageMetrics(m1, 300, 250, 100, 90, 40, 10, 4, 3, 3, 1.0f, 20, 15, 4, 222, 111, 3)
        checkPackageMetrics(m1, 0.833f, 0.9f, 0.845f, 0.133f, 0.4f, 1.0, 0.1, 2.0f, 15, 5, 1.333f)
    }

    @Test
    void testProjectMetrics() {
        ProjectMetrics m1 = new ProjectMetrics(owner)
        MetricsHelper.setProjectMetrics(m1, 300, 250, 100, 90, 40, 10, 4, 3, 3, 1.0f, 20, 15, 4, 222, 111, 3, 2)
        checkProjectMetrics(m1, 0.833f, 0.9f, 0.845f, 0.133f, 0.4f, 1.0, 0.1, 2.0f, 15, 5, 1.333f, 2, 1.5f)
    }

    @Test
    void testMetricsType() {
        assertEquals(new BlockMetrics(owner).getType(), "block")
        assertEquals(new ClassMetrics(owner).getType(), "class")
        assertEquals(new FileMetrics(owner).getType(), "file")
        assertEquals(new PackageMetrics(owner).getType(), "package")
        assertEquals(new ProjectMetrics(owner).getType(), "project")
    }

    private void checkBlockMetrics(BlockMetrics m, float pcs, float pcb, float pce, float cd, float ptp, double tet, double ate) {
        assertEquals("PcCoveredStatements", pcs, m.getPcCoveredStatements(), 0.001f)
        assertEquals("PcCoveredBranches", pcb, m.getPcCoveredBranches(), 0.001f)
        assertEquals("PcCoveredElements", pce, m.getPcCoveredElements(), 0.001f)
        assertEquals("ComplexityDensity", cd, m.getComplexityDensity(), 0.001f)
        assertEquals("PcTestPasses", ptp, m.getPcTestPasses(), 0.001f)
        assertEquals("TestExecutionTime", tet, m.getTestExecutionTime(), 0.001f)
        assertEquals("AvgTestExecutionTime", ate, m.getAvgTestExecutionTime(), 0.001f)
    }

    private void checkClassMetrics(ClassMetrics m, float pcs, float pcb, float pce, float cd, float ptp, double tet,
                                   double ate, float acmp, float asm) {
        checkBlockMetrics(m, pcs, pcb, pce, cd, ptp, tet, ate)
        assertEquals("AvgMethodComplexity", acmp, m.getAvgMethodComplexity(), 0.001f)
        assertEquals("AvgStatementsPerMethod", asm, m.getAvgStatementsPerMethod(), 0.001f)
    }

    private void checkFileMetrics(FileMetrics m, float pcs, float pcb, float pce, float cd, float ptp, double tet,
                                  double ate, float acmp, float asm, float mpc) {
        checkClassMetrics(m, pcs, pcb, pce, cd, ptp, tet, ate, acmp, asm)
        assertEquals("AvgMethodsPerClass", mpc, m.getAvgMethodsPerClass(), 0.001f)
    }

    private void checkPackageMetrics(PackageMetrics m, float pcs, float pcb, float pce, float cd, float ptp, double tet,
                                     double ate, float acmp, float asm, float mpc, float cpf) {
        checkFileMetrics(m, pcs, pcb, pce, cd, ptp, tet, ate, acmp, asm, mpc)
        assertEquals("AvgClassesPerFile", cpf, m.getAvgClassesPerFile(), 0.001f)
    }

    private void checkProjectMetrics(ProjectMetrics m, float pcs, float pcb, float pce, float cd, float ptp, double tet,
                                     double ate, float acmp, float asm, float mpc, float cpf, float cpp, float cfp) {
        checkPackageMetrics(m, pcs, pcb, pce, cd, ptp, tet, ate, acmp, asm, mpc, cpf)
        assertEquals("AvgClassesPerPackage", cpp, m.getAvgClassesPerPackage(), 0.001f)
        assertEquals("AvgFilesPerPackage", cfp, m.getAvgFilesPerPackage(), 0.001f)
    }

}
