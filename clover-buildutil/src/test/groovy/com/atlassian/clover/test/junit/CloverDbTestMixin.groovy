package com.atlassian.clover.test.junit

/** Mixin for making assertsions on a clover .db file  */
trait CloverDbTestMixin {
    protected File db

    void reserveCloverDbFile() {
        db = File.createTempFile("clover", ".db", workingDir)
        db.delete()
    }

    boolean assertRegistry(File db, Closure assertion = { true }) {
        def reg = Class.forName("com.atlassian.clover.registry.Clover2Registry").getMethod("fromFile", [File.class] as Class[]).invoke(null, [db] as Object[])
        if (reg) {
            assertTrue("Assertion on registry failed: ${reg}", assertion.call(reg))
        } else {
            assertFalse "Registry failed to load from db file: ${db.absolutePath}", true
        }
        return true
    }

    boolean assertPackage(p, Closure selection, Closure assertion = { true }) {
        def found = p.allPackages.find(selection)
        if (found) {
            assertTrue("Assertion on package failed: ${found}", assertion.call(found))
        } else {
            assertFalse "No matching package in project. Packages found:\n${p.packages.collect {it.name}.join(", ")}", true
        }
        return true
    }

    boolean assertFile(p, Closure selection, Closure assertion = { true }) {
        def found = p.files.find(selection)
        if (found) {
            assertTrue("Assertion on file failed: ${found}", assertion.call(found))
        } else {
            assertFalse "No matching file in package ${p.name}. Files found:\n${p.files.collect {it.name}.join(", ")}", true
        }
        return true
    }

    boolean assertClass(f, Closure selection, Closure assertion = { true }) {
        def found = f.classes.find(selection)
        if (found) {
            assertTrue("Assertion on class failed: ${found}", assertion.call(found))
        } else {
            assertFalse "No matching class in file ${f.name}. Classes found:\n${f.classes.collect {it.name}.join(", ")}", true
        }
        return true
    }

    boolean assertMethod(c, Closure selection, Closure assertion = { true }) {
        def found = c.methods.find(selection)
        if (found) {
            assertTrue("Assertion on method failed: ${found.toString()} hits: ${found.hitCount}", assertion.call(found))
        } else {
            assertFalse "No matching method in class ${c.name}:\n${c.methods.collect {"${it.toString()} hits: ${it.hitCount}"}.join("\n")}", true
        }
        return true
    }

    boolean assertStatement(m, Closure selection, Closure assertion = { true }) {
        def found = m.statements.find(selection)
        if (found) {
            assertTrue("Assertion on statement failed: ${found.toString()} hits: ${found.hitCount}", assertion.call(found))
        } else {
            assertFalse "No matching statement in method ${m.name}. Statements found:\n${m.statements.collect {"${it.toString()} hits: ${it.hitCount}"}.join("\n")}", true
        }
        return true
    }

    boolean assertBranch(m, Closure selection, Closure assertion = { true }) {
        def found = m.branches.find(selection)
        if (found) {
            assertTrue("Assertion on branch failed: ${found.toString()} trueHits/falseHits: ${found.trueHitCount}/${found.falseHitCount}", assertion.call(found))
        } else {
            assertFalse "No matching branch in method ${m.name}. Branches found:\n${m.branches.collect {"${it.toString()} trueHits/falseHits: ${it.trueHitCount}/${it.falseHitCount}"}.join("\n")}", true
        }
        return true
    }

    boolean assertTestCase(c, Closure selection, Closure assertion = { true }) {
        def found = c.testCases.find(selection)
        if (found) {
            assertTrue("Assertion on test case failed: ${found.toString()}", assertion.call(found))
        } else {
            assertFalse "No matching test case in class ${c.name}. Test cases found:\n${c.testCases.collect { it.toString() }.join("\n")}", true
        }
        return true
    }

    boolean assertHitByTest(covered, db, c, String testName) {
        assertTrue("${covered} was not hit by test ${testName}", hitByTest(db, c, testName).call(covered))
        return true
    }

    boolean assertNotHitByTest(covered, db, c, String testName) {
        assertFalse("${covered} was hit by test ${testName}", hitByTest(db, c, testName).call(covered))
        return true
    }

    Closure at(startLine, startColumn, endLine, endColumn) {
        return { it.startLine == startLine && it.startColumn == startColumn && it.endLine == endLine && it.endColumn == endColumn }
    }

    Closure complexity(cpx) {
        return { it.complexity == cpx }
    }

    Closure hits(int hitCount) {
        return { it.hitCount == hitCount }
    }

    Closure uncoveredElements(int uncovered) {
      return { it.metrics.numUncoveredElements == uncovered }
    }

    Closure pcUncoveredElements(float pcUncovered) {
      return { it.metrics.pcUncoveredElements == pcUncovered }
    }


    Closure hits(int trueHitCount, int falseHitCount) {
        return { it.trueHitCount == trueHitCount && it.falseHitCount == falseHitCount }
    }

    Closure simplyNamed(String name) {
        return { it.simpleName == name }
    }

    Closure named(String name) {
        return { it.name == name }
    }

    Closure and(Closure... others) {
        return { subject -> others.inject(true) { result, other -> result && other.call(subject) } }
    }

    Closure hitByTest(db, c, String testName) {
        return { db.getCoverageData().getHitsFor(c.getTestCase(testName)).get(it.dataIndex) }
    }

    Closure notHitByTest(db, c, String testName) {
        return { !db.getCoverageData().getHitsFor(c.getTestCase(testName)).get(it.dataIndex) }
    }
}