package org.openclover.groovy.test.junit

import groovy.transform.CompileStatic
import org.openclover.core.CloverDatabase
import org.openclover.core.api.registry.BranchInfo
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.ElementInfo
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.api.registry.HasMetrics
import org.openclover.core.api.registry.InstrumentationInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.api.registry.SourceInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.registry.entities.BaseProjectInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.TestCaseInfo
import org.openclover.core.api.registry.HasMetricsFilter

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/** Mixin for making assertions on a clover .db file  */
@CompileStatic
trait CloverDbTestMixin {
    private File db

    File getDb() {
        db
    }

    void reserveCloverDbFile(File workingDir) {
        db = File.createTempFile("clover", ".db", workingDir)
        db.delete()
    }

    boolean assertRegistry(File db, Closure<Boolean> assertion = { true }) {
        def reg = Class.forName("org.openclover.core.registry.Clover2Registry")
                .getMethod("fromFile", [File.class] as Class[])
                .invoke(null, [db] as Object[])
        if (reg) {
            assertTrue("Assertion on registry failed: ${reg}", assertion.call(reg))
        } else {
            assertFalse "Registry failed to load from db file: ${db.absolutePath}", true
        }
        return true
    }

    boolean assertPackage(BaseProjectInfo p, Closure<Boolean> selection, Closure<Boolean> assertion = { true }) {
        PackageInfo found = p.allPackages.find(selection)
        if (found) {
            assertTrue("Assertion on package failed: ${found}", assertion.call(found))
        } else {
            assertFalse("No matching package in project. Packages found:\n" +
                    "${p.getPackages(HasMetricsFilter.ACCEPT_ALL).collect { PackageInfo it -> it.name}.join(", ")}",
                    true)
        }
        return true
    }

    boolean assertFile(PackageInfo p, Closure<Boolean> selection, Closure<Boolean> assertion = { true }) {
        FileInfo found = p.files.find(selection)
        if (found) {
            assertTrue("Assertion on file failed: ${found}", assertion.call(found))
        } else {
            assertFalse("No matching file in package ${p.name}. " +
                    "Files found:\n${p.files.collect { FileInfo it-> it.name }.join(", ")}",
                    true)
        }
        return true
    }

    boolean assertClass(FileInfo f, Closure<Boolean> selection, Closure<Boolean> assertion = { true }) {
        ClassInfo found = f.classes.find(selection)
        if (found) {
            assertTrue("Assertion on class failed: ${found}", assertion.call(found))
        } else {
            assertFalse("No matching class in file ${f.name}. " +
                    "Classes found:\n${f.classes.collect { ClassInfo it -> it.name }.join(", ")}", true)
        }
        return true
    }

    boolean assertMethod(ClassInfo c, Closure<Boolean> selection, Closure<Boolean> assertion = { true }) {
        MethodInfo found = c.methods.find(selection)
        if (found) {
            assertTrue("Assertion on method failed: ${found.toString()} hits: ${found.hitCount}", assertion.call(found))
        } else {
            assertFalse("No matching method in class ${c.name}:\n" +
                    "${c.methods.collect {"${it.toString()} hits: ${it.hitCount}"}.join("\n")}",
                    true)
        }
        return true
    }

    boolean assertStatement(MethodInfo m, Closure<Boolean> selection, Closure<Boolean> assertion = { true }) {
        StatementInfo found = m.statements.find(selection)
        if (found) {
            assertTrue("Assertion on statement failed: ${found.toString()} hits: ${found.hitCount}", assertion.call(found))
        } else {
            assertFalse("No matching statement in method ${m.name}. " +
                    "Statements found:\n${m.statements.collect { StatementInfo it -> "${it.toString()} hits: ${it.hitCount}"}.join("\n")}",
                    true)
        }
        return true
    }

    boolean assertBranch(MethodInfo m, Closure<Boolean> selection, Closure<Boolean> assertion = { true }) {
        BranchInfo found = m.branches.find(selection)
        if (found) {
            assertTrue("Assertion on branch failed: ${found.toString()} trueHits/falseHits: ${found.trueHitCount}/${found.falseHitCount}",
                    assertion.call(found))
        } else {
            assertFalse("No matching branch in method ${m.name}. Branches found:\n" +
                    "${m.branches.collect { BranchInfo it -> "${it.toString()} trueHits/falseHits: ${it.trueHitCount}/${it.falseHitCount}"}.join("\n")}".toString(),
                    true)
        }
        return true
    }

    boolean assertTestCase(FullClassInfo c, Closure selection, Closure<Boolean> assertion = { true }) {
        TestCaseInfo found = c.testCases.find(selection)
        if (found) {
            assertTrue("Assertion on test case failed: ${found.toString()}", assertion.call(found))
        } else {
            assertFalse("No matching test case in class ${c.name}. " +
                    "Test cases found:\n${c.testCases.collect { it.toString() }.join("\n")}",
                    true)
        }
        return true
    }

    boolean assertHitByTest(InstrumentationInfo covered, CloverDatabase db, FullClassInfo c, String testName) {
        boolean result = hitByTest(db, c, testName).call(covered)
        assertTrue("${covered} was not hit by test ${testName}".toString(), result)
        return true
    }

    boolean assertNotHitByTest(InstrumentationInfo covered, CloverDatabase db, FullClassInfo c, String testName) {
        boolean result = hitByTest(db, c, testName).call(covered)
        assertFalse("${covered} was hit by test ${testName}".toString(), result)
        return true
    }

    Closure<Boolean> at(int startLine, int startColumn, int endLine, int endColumn) {
        return { SourceInfo it -> it.startLine == startLine &&
                    it.startColumn == startColumn &&
                    it.endLine == endLine &&
                    it.endColumn == endColumn
        }
    }

    Closure<Boolean> isDefaultPackage = { PackageInfo it -> it.isDefault() }

    Closure<Boolean> complexity(int cpx) {
        return { ElementInfo it -> it.complexity == cpx }
    }

    Closure<Boolean> hits(int hitCount) {
        return { ElementInfo it -> it.hitCount == hitCount }
    }

    Closure<Boolean> uncoveredElements(int uncovered) {
      return { HasMetrics it -> it.metrics.numUncoveredElements == uncovered }
    }

    Closure<Boolean> pcUncoveredElements(float pcUncovered) {
      return { HasMetrics it -> it.metrics.pcUncoveredElements == pcUncovered }
    }

    Closure<Boolean> hits(int trueHitCount, int falseHitCount) {
        return { BranchInfo it -> it.trueHitCount == trueHitCount && it.falseHitCount == falseHitCount }
    }

    Closure<Boolean> simplyNamed(String name) {
        return { MethodInfo it -> it.simpleName == name }
    }

    Closure<Boolean> named(String name) {
        return { HasMetrics it -> it.name == name }
    }

    Closure<Boolean> and(Closure<Boolean>... others) {
        return { subject -> others.inject(true) { result, other -> result && other.call(subject) } }
    }

    Closure<Boolean> or(Closure<Boolean>... others) {
        return { subject -> others.inject(true) { result, other -> result || other.call(subject) } }
    }

    Closure<Boolean> hitByTest(CloverDatabase db, FullClassInfo c, String testName) {
        return { InstrumentationInfo it ->
            db.getCoverageData()
                    .getHitsFor(c.getTestCase(testName))
                    .get(it.dataIndex)
        }
    }

    Closure<Boolean> notHitByTest(CloverDatabase db, FullClassInfo c, String testName) {
        return { InstrumentationInfo it ->
            !db.getCoverageData()
                    .getHitsFor(c.getTestCase(testName))
                    .get(it.dataIndex)
        }
    }
}