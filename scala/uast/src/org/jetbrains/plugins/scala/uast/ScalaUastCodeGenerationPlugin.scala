package org.jetbrains.plugins.scala.uast

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.{PsiElement, SmartPointerManager}
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.codeInsight.intention.imports.ImportStableMemberIntention
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScImportsHolder
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScReferencePattern
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScFieldId, ScPrimaryConstructor, ScReference}
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScAssignment, ScBlockExpr}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunctionDefinition, ScValueDeclaration, ScValueOrVariable, ScValueOrVariableDefinition, ScVariableDeclaration}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.{createElementFromText, createNewLine, createReferenceFromText}
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.uast.UastContextKt.toUElement
import org.jetbrains.uast.generate.{UastCodeGenerationPlugin, UastElementFactory}
import org.jetbrains.uast.{UElement, UExpression, UField, UMethod, UParameter, UQualifiedReferenceExpression, UReferenceExpression, UastUtils}

import scala.collection.mutable

final class ScalaUastCodeGenerationPlugin extends UastCodeGenerationPlugin {
  override def getLanguage: Language =
    if (isEnabled) ScalaLanguage.INSTANCE else DummyDialect

  override def getElementFactory(project: Project): UastElementFactory = new ScalaUastElementFactory(project)

  override def bindToElement(reference: UReferenceExpression, element: PsiElement): PsiElement =
    reference
      .getSourcePsi
      .asOptionOf[ScReference]
      .map(_.bindToElement(element))
      .orNull

  override def importMemberOnDemand(reference: UQualifiedReferenceExpression): UExpression = {
    val source = reference.getSourcePsi.asOptionOf[ScReference].orNull
    if (source == null) return null

    val ptr = SmartPointerManager.createPointer(source.nameId)

    ImportStableMemberIntention.invokeOn(source)(source.getProject)

    val element = ptr.getElement

    if (element == null) null
    else toUElement(element.getParent, classOf[UExpression])
  }

  override def replace[T <: UElement](oldElement: UElement, newElement: T, elementType: Class[T]): T = {
    val oldPsi = oldElement.getSourcePsi
    if (oldPsi == null) return null.asInstanceOf[T]
    val newPsi = newElement.getSourcePsi
    if (newPsi == null) return null.asInstanceOf[T]

    toUElement(oldPsi.replace(newPsi), elementType)
  }

  override def shortenReference(reference: UReferenceExpression): UReferenceExpression = {
    val e = reference.getSourcePsi.asOptionOf[ScReference].map { ref =>
      ScImportsHolder(ref).addImport(ref)
    }.orNull

    toUElement(e, classOf[UReferenceExpression])
  }

  override def initializeField(uField: UField, uParameter: UParameter): Unit = {
    val uMethod = UastUtils.getParentOfType(uParameter, classOf[UMethod], false)
    if (uMethod == null) return

    uMethod.getSourcePsi match {
      case null =>
      case constructor: ScPrimaryConstructor =>
        implicit val ctx: ProjectContext = constructor.getProject

        def fieldSourceVariable: Option[ScValueOrVariable] = uField.getSourcePsi match {
          case v: ScValueOrVariable => Some(v)
          case id@(_: ScReferencePattern | _: ScFieldId) => id.parentOfType[ScValueOrVariable]
          case _ => None
        }

        if (uField.getName == uParameter.getName) {
          val paramPsi = uParameter.getSourcePsi
          if (paramPsi == null) return

          fieldSourceVariable.foreach { fieldPsi =>
            val scParam = ScalaPsiElementFactory.createClassParameterFromText(fieldPsi.getText, constructor)
            scParam.getModifierList.setModifierProperty(ScalaTokenTypes.kFINAL.text, false)
            scParam.getActualDefaultExpression.foreach(_.delete())
            scParam.getNode.findChildByType(ScalaTokenTypes.tASSIGN).toOption.foreach(_.getPsi.delete())
            fieldPsi.delete()
            paramPsi.replace(scParam)
          }
        } else {
          fieldSourceVariable.foreach {
            case definition: ScValueOrVariableDefinition =>
              definition.expr.foreach(_.replace(createReferenceFromText(uParameter.getName)))
            case declaration@(_: ScValueDeclaration | _: ScVariableDeclaration) =>
              declaration.replace(
                createElementFromText[ScValueOrVariableDefinition](
                  s"${declaration.getText} = ${uParameter.getName}", constructor
                )
              )
            case _ =>
          }
        }
      case fn: ScFunctionDefinition =>
        implicit val ctx: ProjectContext = fn.getProject

        def createAssignment: ScAssignment = {
          val assignmentBuilder = new mutable.StringBuilder()

          if (uField.getName == uParameter.getName)
            assignmentBuilder.append("this.")

          assignmentBuilder
            .append(uField.getName)
            .append(" = ")
            .append(uParameter.getName)

          createElementFromText[ScAssignment](assignmentBuilder.result(), fn)
        }

        fn.body.foreach {
          case block: ScBlockExpr =>
            addToBlock(block, createAssignment)
          case bodyExpr =>
            val newBody = ScalaPsiElementFactory.createBlockWithGivenExpression(bodyExpr, bodyExpr) match {
              case block: ScBlockExpr =>
                addToBlock(block, createAssignment)
                block
              case _ => return
            }
            bodyExpr.replace(newBody)
        }
      case _ =>
    }
  }

  private def addToBlock(block: ScBlockExpr, element: PsiElement)(implicit pc: ProjectContext): Unit = {
    val rBrace = block.getRBrace.orNull
    block.addBefore(element, rBrace)
    block.addBefore(createNewLine(), rBrace)
  }
}
