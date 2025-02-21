package org.jetbrains.plugins.scala.worksheet.ammonite

import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.{PsiElement, ResolveState}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt
import org.jetbrains.plugins.scala.lang.psi.api.{FileDeclarationsContributor, ScalaFile}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.worksheet.ammonite.AmmoniteUtil.isAmmoniteFile

final class AmmoniteFileDeclarationsContributor extends FileDeclarationsContributor {
  import AmmoniteFileDeclarationsContributor._
  
  override def accept(holder: PsiElement): Boolean = holder match {
    case ammoniteFile: ScalaFile => isAmmoniteFile(ammoniteFile)
    case _ => false
  }

  override def processAdditionalDeclarations(processor: PsiScopeProcessor, holder: PsiElement, state: ResolveState): Unit = {
    holder match {
      case ammoniteFile: ScalaFile =>
        AmmoniteFileDeclarationsContributor.DEFAULT_BUILTINS.foreach {
          case (name, txt) =>
            ScalaPsiElementFactory.createElementFromText[PsiElement](
              s"class A { val $name: $txt = ??? }",
              holder
            )(ammoniteFile.projectContext).processDeclarations(processor, state, null, ammoniteFile)
        }

        DEFAULT_IMPORTS.foreach {
          imp =>
            val importStmt = ScalaPsiElementFactory.createElementFromText[ScImportStmt](s"import $imp", holder)(ammoniteFile.projectContext)
            importStmt.processDeclarations(processor, state, null, ammoniteFile)
        }
      case _ =>
    }
  }
}

object AmmoniteFileDeclarationsContributor {
  private val DEFAULT_IMPORTS = Seq("ammonite.main.Router._", "ammonite.runtime.tools.grep", "ammonite.runtime.tools.browse",
    "ammonite.runtime.tools.time", "ammonite.repl.tools.desugar", "ammonite.repl.tools.source") //todo more default imports ?
  private val DEFAULT_BUILTINS = Seq(("repl", "ammonite.repl.ReplAPI"), 
    ("interp", "ammonite.runtime.Interpreter with ammonite.interp.Interpreter"))
}
