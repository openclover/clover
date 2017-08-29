package org.openclover.ci

import com.atlassian.clover.api.ci.CIOptions
import com.atlassian.clover.api.ci.Integrator
import spock.lang.Specification
import spock.lang.Unroll

import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.CoreMatchers.not
import static spock.util.matcher.HamcrestSupport.that

/**
 * Test for {@link org.openclover.ci.MavenIntegrator}
 */
class MavenIntegratorSpec extends Specification {

    static def clv = MavenIntegrator.PREFIX

    static def standardReportOptions = [
            "-Dmaven.clover.generateHtml=true",
            "-Dmaven.clover.generateJson=false",
            "-Dmaven.clover.generatePdf=false",
            "-Dmaven.clover.generateXml=true"
    ]

    @Unroll
    def "test insertAfterAllOf for #expectedList #putIfNotFound"() {
        setup:
        def list = ["a", "b", "c", "d"]
        MavenIntegrator.insertAfterAllOf(list, afterAllOf, "z", putIfNotFound)

        expect:
        list == expectedList

        where:
        afterAllOf        | putIfNotFound || expectedList
        [] as Set         | true          || ["z", "a", "b", "c", "d"]   // no after - before first
        ["e", "f"] as Set | true          || ["z", "a", "b", "c", "d"]   // afterAllOf not found - insert before first
        ["e", "f"] as Set | false         || ["a", "b", "c", "d"]        // afterAllOf not found - don't add element
        ["a"] as Set      | true          || ["a", "z", "b", "c", "d"]   // insert after first
        ["a", "c"] as Set | true          || ["a", "b", "c", "z", "d"]   // insert after non-continous afterAllOf
        ["d"] as Set      | true          || ["a", "b", "c", "d", "z"]   // add as last
    }

    @Unroll
    def "test insertBeforeAnyOf for #expectedList #putIfNotFound"() {
        setup:
        def list = ["a", "b", "c", "d"]
        MavenIntegrator.insertBeforeAnyOf(list, beforeAnyOf, "z", putIfNotFound)

        expect:
        list == expectedList

        where:
        beforeAnyOf       | putIfNotFound || expectedList
        [] as Set         | true          || ["a", "b", "c", "d", "z"]   // no beforeAnyOf - add after last
        ["e", "f"] as Set | true          || ["a", "b", "c", "d", "z"]   // beforeAnyOf not found - add after last
        ["e", "f"] as Set | false         || ["a", "b", "c", "d"]        // beforeAnyOf not found - don't add element
        ["a"] as Set      | true          || ["z", "a", "b", "c", "d"]   // put before first
        ["b", "d"] as Set | true          || ["a", "z", "b", "c", "d"]   // put before non-continous beforeAnyOf
        ["d"] as Set      | true          || ["a", "b", "c", "z", "d"]   // insert before last
    }

    @Unroll
    def "enable clover (#args)"() {
        setup:
        Integrator mvn = Integrator.Factory.newMavenIntegrator(builder.build())
        mvn.decorateArguments(args)

        expect:
        // clover:setup shall be placed after the "clean" and before other targets
        // clover:aggregate, clover:clover shall be placed after other targets/phases
        args == expectedArgs

        where:
        args                                 | builder
        ["clean", "test"]                    | newBuilder().fullClean(true)
        ["clean", "test", "verifier:verify"] | newBuilder()
        ["verify"]                           | newBuilder()
        ["help:help"]                        | newBuilder()

        expectedArgs << [
                ["clean", "${clv}setup", "test", "${clv}aggregate", "${clv}clover"] + standardReportOptions, // standard test build
                ["clean", "${clv}setup", "test", "verifier:verify", "${clv}aggregate", "${clv}clover"] + standardReportOptions, // extra goals after testing
                ["${clv}setup", "verify", "${clv}aggregate", "${clv}clover"] + standardReportOptions, // no clean called, different build phase (which also calls tests)
                ["${clv}setup", "help:help", "verify", "${clv}aggregate", "${clv}clover"] + standardReportOptions, // no clean, no tests - add "verify" phase by default
        ]

    }

    static def goalsBefore = ["clean", "${clv}setup", "${clv}optimize"]
    static def goalsAfter = ["${clv}snapshot", "${clv}aggregate", "${clv}clover"]

    @Unroll
    def "enable clover with test optimization (#args)"() {
        setup:
        Integrator mvn = Integrator.Factory.newMavenIntegrator(newBuilder().optimize(true).build())
        mvn.decorateArguments(args)

        expect:
        // clover:optimize shall be called before test/integration-test/verify/deploy etc
        args == expectedArgs

        where:
        args                          || expectedArgs
        ["clean", "test"]             || goalsBefore + ["test"] + goalsAfter + standardReportOptions
        ["clean", "package"]          || goalsBefore + ["package"] + goalsAfter + standardReportOptions
        ["clean", "integration-test"] || goalsBefore + ["integration-test"] + goalsAfter + standardReportOptions
        ["clean", "verify"]           || goalsBefore + ["verify"] + goalsAfter + standardReportOptions
        ["clean", "install"]          || goalsBefore + ["install"] + goalsAfter + standardReportOptions
        ["clean", "deploy"]           || goalsBefore + ["deploy"] + goalsAfter + standardReportOptions
    }

    @Unroll
    def "enable clover with historical reports (#args)"() {
        setup:
        Integrator mvn = Integrator.Factory.newMavenIntegrator(builder.build());
        mvn.decorateArguments(args)

        expect:
        that args, hasItems(containsArgs.toArray())

        where:
        args      | builder
        ["clean"] | newBuilder().historical(false)
        ["clean"] | newBuilder().historical(true)
        ["clean"] | newBuilder().historical(true).historyDir(new File("abc"))

        containsArgs << [
                ["clean"],
                ["-Dmaven.clover.generateHistorical=true", "-Dmaven.clover.historyDir=.cloverhistory", "${clv}save-history".toString()],
                ["-Dmaven.clover.generateHistorical=true", "-Dmaven.clover.historyDir=" + new File("abc").absolutePath, "${clv}save-history".toString()]
        ]
    }

    @Unroll
    def "enable clover with various options (#builder)"() {
        setup:
        Integrator mvn = Integrator.Factory.newMavenIntegrator(builder.build());
        mvn.decorateArguments(args)

        expect:
        that args, hasItems(containsArgs.toArray())

        where:
        args          | builder                                          || containsArgs
        ["clean"]     | newBuilder().licenseCert("MYLICENSECERT")        || ["-Dmaven.clover.license=MYLICENSECERT"]
        ["clean"]     | newBuilder().license(new File("clover.license")) || ["-Dmaven.clover.licenseLocation=" + new File("clover.license").absolutePath]
        ["clean"]     | newBuilder().historical(true)                    || ["-Dmaven.clover.generateHistorical=true"]
        ["clean"]     | newBuilder().json(true)                          || ["-Dmaven.clover.generateJson=true"]
        ["clean"]     | newBuilder().html(false)                         || ["-Dmaven.clover.generateHtml=false"]
        ["clean"]     | newBuilder().pdf(true)                           || ["-Dmaven.clover.generatePdf=true"]
        ["clean"]     | newBuilder().xml(false)                          || ["-Dmaven.clover.generateXml=false"]
        ["help:help"] | newBuilder().fullClean(true)                     || ["help:help", "clean"]
    }

    @Unroll
    def "enable clover with various duplicated options (#args) and builder (#builder)"() {
        setup:
        Integrator mvn = Integrator.Factory.newMavenIntegrator(builder.build());
        mvn.decorateArguments(args)

        expect:
        that args, not(hasItems(dosentContain.toArray()))
        that args, hasItems(containsArgs.toArray())

        where:
        args << [
                ["clean", "-Dmaven.clover.license=MYLICENSECERT2"],
                ["clean", licenseLocationProperty("clover2.license")],
                ["clean", "-Dmaven.clover.generateHistorical=false"],
                ["clean", "-Dmaven.clover.generateJson=true"],
                ["clean", "--define", "maven.clover.generateHtml=false"],
                ["clean", "-Dmaven.clover.generatePdf=true"],
                ["clean", "-Dmaven.clover.generateXml=false"],
                ["help:help"]]

        builder << [
                newBuilder().licenseCert("MYLICENSECERT"),
                newBuilder().license(new File("clover.license")),
                newBuilder().historical(true),
                newBuilder().json(false),
                newBuilder().html(true),
                newBuilder().pdf(false),
                newBuilder().xml(true),
                newBuilder().fullClean(true)]

        containsArgs << [
                ["-Dmaven.clover.license=MYLICENSECERT2"],      // ["clean", "-Dmaven.clover.license=MYLICENSECERT2"]       | licenseCert("MYLICENSECERT")
                [licenseLocationProperty("clover2.license")],   // ["clean", licenseLocationProperty("clover2.license")]    | license(new File("clover.license"))
                ["-Dmaven.clover.generateHistorical=false"],    // ["clean", "-Dmaven.clover.generateHistorical=false"]     | historical(true)
                ["-Dmaven.clover.generateJson=true"],           // ["clean", "-Dmaven.clover.generateJson=true"]            | json(false)
                ["--define", "maven.clover.generateHtml=false"],// ["clean", "--define", "maven.clover.generateHtml=false"] | html(true)
                ["-Dmaven.clover.generatePdf=true"],            // ["clean", "-Dmaven.clover.generatePdf=true"]             | pdf(false)
                ["-Dmaven.clover.generateXml=false"],           // ["clean", "-Dmaven.clover.generateXml=false"]            | xml(true)
                ["help:help", "clean"]]                         // ["help:help"]                                            | fullClean(true)

        dosentContain << [
                ["-Dmaven.clover.license=MYLICENSECERT"],    // ["clean", "-Dmaven.clover.license=MYLICENSECERT2"]      | licenseCert("MYLICENSECERT")
                [licenseLocationProperty("clover1.license")],// ["clean", licenseLocationProperty("clover2.license")]   | license(new File("clover.license"))
                ["-Dmaven.clover.generateHistorical=true"],  // ["clean", "-Dmaven.clover.generateHistorical=false"]    | historical(true)
                ["-Dmaven.clover.generateJson=false"],       // ["clean", "-Dmaven.clover.generateJson=true"]           | newBuilder().json(false)
                ["-Dmaven.clover.generateHtml=true"],        // ["clean", "--define", "maven.clover.generateHtml=false"]| newBuilder().html(true)
                ["-Dmaven.clover.generatePdf=false"],        // ["clean", "-Dmaven.clover.generatePdf=true"]            | newBuilder().pdf(false)
                ["-Dmaven.clover.generateXml=true"],         // ["clean", "-Dmaven.clover.generateXml=false"]           | newBuilder().xml(true)
                [""]]                                        // ["help:help"]                                           | newBuilder().fullClean(true)
    }

    private static String licenseLocationProperty(final String licenseName) {
        "-Dmaven.clover2.licenseLocation=" + new File(licenseName).absolutePath
    }

    private static CIOptions.Builder newBuilder() {
        new CIOptions.Builder()
    }
}
