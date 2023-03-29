package com.atlassian.clover.ant.groovy

import com.atlassian.clover.test.junit.JavaExecutorMixin
import junit.framework.Test

@Mixin (JavaExecutorMixin)
class AntProjectSimulacrum {
    public static String DEBUG_OPTIONS = ""//-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5009"

    String methodName
    String testVersionedName
    String antVersion
    File antJar
    String groovyVersion
    File groovyAllJar
    List<File> additionalGroovyJars = []
    File cloverRuntimeJar
    File cloverRepkgRuntimeJar
    Test test
    File buildXml

    List<File> calcAntClasspath() {
        [ antJar ]
    }

    def getName() {
      return testVersionedName
    }

    void executeTargets(File workingDir, String ... targets) {
        if (buildXml == null) {
            buildXml = buildProjectArtifacts(workingDir)
        }
        String classpath = [calcAntClasspath(), calcRepkgJarPath(), additionalGroovyJars]
                .flatten()
                .findAll { it != null }
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
        def src = test.metaClass.getProperty(test, methodName + "Src")
        src.each {entry ->
            new File(workingDir, (String) entry.key).with {
                it.parentFile.mkdirs()
                it.createNewFile()
                writeStringToFile(it, (String) entry.value)
            }
        }
        def buildxml = File.createTempFile("build", ".xml", workingDir)
        def body = test.metaClass.getProperty(test, methodName + "XML")
        def proj = """
        <project>
          <path id="groovy.path">
            <pathelement path="${groovyAllJar.getAbsolutePath()}"/>
          </path>
          <path id="clover.path">
            <pathelement path="${cloverRuntimeJar.getAbsolutePath()}"/>
          </path>

          <taskdef name="groovyc" classname="org.codehaus.groovy.ant.Groovyc" classpathref="groovy.path"/>
          <taskdef resource="cloverlib.xml" classpathref="clover.path"/>
          <property name="workDir" location="${workingDir.getAbsolutePath()}"/>
          ${body}
        </project>
      """
        writeStringToFile(buildxml, proj)
        return buildxml
    }

    private void writeStringToFile(File file, String value) {
        def fout = new FileWriter(file)
        fout.write(value)
        fout.close()
    }
}
