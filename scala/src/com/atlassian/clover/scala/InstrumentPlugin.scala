package com.atlassian.clover.scala

import com.atlassian.clover.api.instrumentation.InstrumentationSession
import com.atlassian.clover.api.registry.ContextSet
import com.atlassian.clover.registry.{FixedSourceRegion, Clover2Registry}
import com.atlassian.clover.registry.entities.MethodSignature
import org.openclover.runtime.CloverNames

import java.io.File
import tools.nsc.plugins.{PluginComponent, Plugin}
import tools.nsc._
import symtab._
import tools.nsc.ast._
import tools.nsc.util._
import tools.nsc.io._

class InstrumentPlugin(val global: Global) extends Plugin {
  val name = "cloverInstrumenter"
  val description = "instruments source code to measure code coverage"
  val components = List[PluginComponent](Instrumenter)
  var initString: String = null
  var projectName: String = "Project"

  override def processOptions(options: List[String], error: String => Unit) {
    for (option: String <- options) {
      val nvp: List[String] = List.fromString(option, '=')
      nvp(0) match {
        case com.atlassian.clover.CloverNames.PROP_INITSTRING =>
          initString = nvp(1)
        case "clover.project.name" =>
          projectName = nvp(1)
        case _ =>
          error("Invalid Clover option: " + option)
      }
    }

    if (initString == null)
      error("Clover initstring not specicified: please specify via -P:" + name + ":clover.initstring=initstring")

    println("processOptions")
  }

  private object Instrumenter extends PluginComponent {
    val global = InstrumentPlugin.this.global

    val phaseName = "clover-instrument"

    override val runsBefore: List[String] = List("namer")
    override val runsRightAfter: Option[String] = Some("parser")
    val runsAfter: List[String] = List("parser")

    def newPhase(prev: Phase) = new InstrumentPhrase(prev, InstrumentPlugin.this.initString, InstrumentPlugin.this.projectName)

    def name = phaseName

    class InstrumentPhrase(prev: Phase, initString: String, projectName: String) extends Phase(prev) {
      override def name = Instrumenter.this.phaseName

      override def run {
        val reg = Clover2Registry.createOrLoad(new File(initString), projectName)
        val session: InstrumentationSession = reg.startInstr()
        val transformer = new TreeTransformer(session)
        var lineCounter = new LineCounter()
        for (unit <- global.currentRun.units; if !unit.isJava) {
          lineCounter.traverse(unit.body)
          session.enterFile("", unit.source.file.file, lineCounter.maxLine, lineCounter.maxLine,
            unit.source.file.file.lastModified(), unit.source.file.file.length(), 0L)
          unit.body = transformer.transform(unit.body)
          session.exitFile()
          lineCounter = new LineCounter()
        }
        session.finishAndApply()
        reg.overwriteSaveToFile()
      }

      class LineCounter extends Traverser {
        var maxLine: Int = -1
        override def traverse(tree: Tree) = {
          if (tree.pos.isDefined) {
            if (maxLine < tree.pos.line) {
              maxLine = tree.pos.line
            }
          }
          super.traverse(tree)
        }
      }

      class TreeTransformer(session: InstrumentationSession) extends Transformer {
        var packages: List[String] = List()
        var currentRecName: String = null
        var greatestPos: Position = null

        override def transform(tree: Tree) = tree match {
          case pkg @ PackageDef(name, stats) =>
            import symtab.Flags._

            currentRecName = null
            packages = packages ::: List(pkg.name.toString())

            var newPkg: PackageDef = super.transform(pkg).asInstanceOf[PackageDef]
            if (currentRecName != null) {
              newPkg =
                  treeCopy.PackageDef(
                    tree,
                    pkg.pid,
                    newPkg.stats ::: List(ModuleDef(
                      Modifiers(PRIVATE),
                      newTermName(currentRecName),
                      Template(
                        List(Select(Ident("scala"), newTypeName("ScalaObject"))),
                        emptyValDef,
                        Modifiers(PRIVATE),
                        List(),
                        List(List()),
                        List(
                          ValDef(
                            Modifiers(0),
                            newTermName("R"),
                            Select(Ident("org_openclover_runtime"), newTypeName("CoverageRecorder")),
                            Apply(
                              Select(Select(Ident("org_openclover_runtime"), newTermName("Clover")), newTermName("getRecorder")),
                              List(Literal(session.getRegistry.getInitstring()), Literal(session.getVersion()), Literal(0L), Literal(session.getCurrentFileMaxIndex))))),
                        NoPosition))))
            }

            packages = packages.init
            currentRecName = null

            newPkg
          case c @ ClassDef(mods, name, tparams, impl) =>
            val startPos: Position = tree.pos

            session.enterClass(pkgName(packages ::: List(name.toString())),
              new FixedSourceRegion(startPos.line, startPos.column),
              false, false, false)

            if (currentRecName == null) {
              currentRecName = CloverNames.recorderPrefixFor(session.getCurrentFile.getDataIndex, session.getCurrentClass().getDataIndex())
            }

            val clazz = super.transform(c)
            session.exitClass(greatestPos.line, greatestPos.column)
            clazz
          case m @ ModuleDef(mods, name, impl) =>
            val startPos: Position = tree.pos

            session.enterClass(pkgName(packages ::: List(name.toString())),
              new FixedSourceRegion(startPos.line, startPos.column),
              false, false, false)

            if (currentRecName == null) {
              currentRecName = CloverNames.recorderPrefixFor(session.getCurrentFile.getDataIndex, session.getCurrentClass().getDataIndex())
            }

            val mod = super.transform(m)
            session.exitClass(greatestPos.line, greatestPos.column)
            mod
          case d @ DefDef(mods, name, tparams, vparams, tpt, impl) =>
            val startPos: Position = tree.pos
            var endPos: Position = null
            session.enterMethod(
              new ContextSet(),
              new FixedSourceRegion(startPos.line, startPos.column),
              new MethodSignature(name.toString), false)

            var defdef: DefDef = super.transform(d).asInstanceOf[DefDef]
            var defRhs: Tree = defdef.rhs match {
              case a @ Apply(fun, args) =>
                endPos = a.pos
                Block(
                  List(
                    Apply(Select(
                      Select(Ident(currentRecName), newTermName("R")), newTermName("inc")),
                      List(Literal(session.getCurrentMethod().getDataIndex())))),
                  a)
              case b @ Block(stats, expr) =>
                endPos = b.expr.pos
                Block(
                  List(
                    Apply(Select(
                      Select(Ident(currentRecName), newTermName("R")), newTermName("inc")),
                      List(Literal(session.getCurrentMethod().getDataIndex())))) ::: b.stats,
                  b.expr)
              case d @ _ =>
                endPos = d.pos
                d
            }
            defdef =
              treeCopy.DefDef(
                defdef,
                defdef.mods,
                defdef.name,
                defdef.tparams,
                defdef.vparamss,
                defdef.tpt,
                defRhs)

            session.exitMethod(endPos.line, endPos.column)
            defdef
          case t =>
            greatestPos = t.pos
            super.transform(t)
        }

        def pkgName(packages: List[String]) = packages.mkString("", ".", "")
      }
    }
  }
}