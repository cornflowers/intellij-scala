package org.jetbrains.plugins.scala.lang.psi.api.statements

import org.jetbrains.plugins.scala.caches.BlockModificationTracker
import org.jetbrains.plugins.scala.lang.psi.api.{ScControlFlowOwner, ScalaPsiElement}
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.light.{PsiClassWrapper, StaticTraitScFunctionWrapper}
import org.jetbrains.plugins.scala.macroAnnotations.Cached

trait ScFunctionDefinition extends ScFunction with ScControlFlowOwner with ScDefinitionWithAssignment {

  def body: Option[ScExpression]

  override def hasAssign: Boolean

  def returnUsages: Set[ScExpression] = ScFunctionDefinitionExt(this).returnUsages

  override def controlFlowScope: Option[ScalaPsiElement] = body

  @Cached(BlockModificationTracker(this), this)
  def getStaticTraitFunctionWrapper(cClass: PsiClassWrapper): StaticTraitScFunctionWrapper =
    new StaticTraitScFunctionWrapper(this, cClass)
}

object ScFunctionDefinition {
  object withBody {
    def unapply(fun: ScFunctionDefinition): Option[ScExpression] = Option(fun).flatMap(_.body)
  }
  object withName {
    def unapply(fun: ScFunctionDefinition): Option[String] = Some(fun.name)
  }
}