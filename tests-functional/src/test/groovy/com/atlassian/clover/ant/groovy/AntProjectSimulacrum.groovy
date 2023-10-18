package com.atlassian.clover.ant.groovy

import com.atlassian.clover.test.junit.JavaExecutorMixin
import groovy.transform.CompileStatic
import junit.framework.Test

@CompileStatic
class AntProjectSimulacrum implements JavaExecutorMixin {
    public static String DEBUG_OPTIONS = ""//-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5009"

    String methodName
    String testVersionedName
    File testDependenciesDir
    String antVersion
    String groovyVersion
    File cloverRuntimeJar
    File cloverRepkgRuntimeJar
    Test test
    File buildXml

    List<File> calcAntClasspath() {
        // we must put clover.jar in ant classpath to be able to load tasks from cloverlib.xml
        [ antJar, antLauncherJar, cloverRuntimeJar ]
    }

    private File getAntJar() {
        new File(testDependenciesDir, "ant-${antVersion}.jar")
    }

    private File getAntLauncherJar() {
        new File(testDependenciesDir, "ant-launcher-${antVersion}.jar")
    }

    private List<File> calcGroovyClasspath() {
        List<File> path = new ArrayList<>()
        path.add(groovyJar)
        path.add(groovyAntJar)
        path.addAll(additionalGroovyJars)
        path
    }

    private File getGroovyJar() {
        new File(testDependenciesDir, "groovy-${groovyVersion}.jar")
    }

    private File getGroovyAntJar() {
        new File(testDependenciesDir, "groovy-ant-${groovyVersion}.jar")
    }

    private List<File> getAdditionalGroovyJars() {
        File antlrJar = new File(testDependenciesDir, "antlr-2.7.7.jar")
        File asmJar = new File(testDependenciesDir, "asm-4.1.jar")
        File commonsCliJar = new File(testDependenciesDir, "commons-cli-1.2.jar")
        [ antlrJar, asmJar, commonsCliJar ]
    }

    def getName() {
      return testVersionedName
    }

    void executeTargets(File workingDir, String ... targets) {
        if (buildXml == null) {
            buildXml = buildProjectArtifacts(workingDir)
        }
        List<File> classpathWithRepckJar = calcAntClasspath() + calcRepkgJarPath()
        String classpath = classpathWithRepckJar
                .findAll { File it -> it != null }
                .collect { File it -> it.absolutePath }
                .join(File.pathSeparator)

        launchJava """-classpath ${classpath}
                ${DEBUG_OPTIONS}
                -Djava.io.tmpdir=${System.getProperty("java.io.tmpdir")}
                -Djava.awt.headless=true
                org.apache.tools.ant.launch.Launcher
                -f ${buildXml}
                -verbose
                ${targets.join(' ')}"""
    }

    File calcRepkgJarPath() {
        return cloverRepkgRuntimeJar != null
                ? (cloverRepkgRuntimeJar.absolutePath == cloverRuntimeJar.absolutePath
                        ? null
                        : cloverRepkgRuntimeJar)
                : null
    }

    private File buildProjectArtifacts(File workingDir) {
        Map<String, String> src = (Map<String, String>) test.metaClass.getProperty(test, methodName + "Src")
        src.each { Map.Entry<String, String> entry ->
            new File(workingDir, entry.key).with {
                it.parentFile.mkdirs()
                it.createNewFile()
                writeStringToFile(it, entry.value)
            }
        }
        def buildxml = File.createTempFile("build", ".xml", workingDir)
        def body = test.metaClass.getProperty(test, methodName + "XML")
        def proj = """
        <project>
          <path id="groovy.path">
            ${calcGroovyClasspath()
                .collect { "<pathelement path=\"${it.getAbsolutePath()}\"/>" }
                .join("\n")
            }
          </path>

          <taskdef name="groovyc" classname="org.codehaus.groovy.ant.Groovyc" classpathref="groovy.path"/>
          <taskdef resource="cloverlib.xml"/>
          <property name="workDir" location="${workingDir.getAbsolutePath()}"/>
          ${body}
        </project>
      """
        writeStringToFile(buildxml, proj)
        return buildxml
    }

    private static void writeStringToFile(File file, String value) {
        Writer fout = new FileWriter(file)
        fout.write(value)
        fout.close()
    }
}
