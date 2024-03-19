package foo.bar

//How a HelloWorld program should resemble once instrumented.
//Used through "scalac -Ybrowse:parsing InstrHelloWorld.scala" to check AST

class InstrHelloWorld {
  def main(args: Array[String]) {
    __CLR2_6_0.R.inc(0);println("Hello, world")
  }

  def foo: String = ""
}

object __CLR2_6_0 {
  val R: _root_.org_openclover_runtime.CoverageRecorder = _root_.org_openclover_runtime.Clover.createRecorder("", 0L, 0L, 0)
}
