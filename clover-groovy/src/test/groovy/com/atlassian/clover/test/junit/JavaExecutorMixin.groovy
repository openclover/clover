package com.atlassian.clover.test.junit

import groovy.transform.CompileStatic

/** Mixin for executing a Java process  */
@CompileStatic
trait JavaExecutorMixin {
    Result launchJavap(String params) {
      launchCmd("${[System.getenv("JAVA_HOME"), 'bin', 'javap'].join(File.separator)} ${params}")
    }

    Result launchJava(String params) {
      launchCmd("${[System.getenv("JAVA_HOME"), 'bin', 'java'].join(File.separator)} ${params}")
    }

    Result launchCmd(String cmd) {
      System.out.println("Executing:")
      System.out.println(cmd)

      def sout = new StringBuffer()
      def serr = new StringBuffer()
      def process = cmd.execute()
      process.consumeProcessOutput(sout, serr)
      int exitCode = process.waitFor()
      Result resultVal = new Result(stdErr: serr, stdOut: sout, exitCode: exitCode)
      System.out.println("exit code: = " + resultVal.exitCode)

      System.out.println("stdout: = '" + resultVal.stdOut + "'")
      System.out.println("stderr: = '" + resultVal.stdErr + "'")

      return resultVal
    }
}

class Result {
    String stdErr, stdOut
    int exitCode
}
