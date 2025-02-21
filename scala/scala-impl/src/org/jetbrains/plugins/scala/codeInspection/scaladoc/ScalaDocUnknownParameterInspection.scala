package org.jetbrains.plugins.scala.codeInspection.scaladoc

import com.intellij.codeInspection._
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.{AbstractFixOnPsiElement, ScalaInspectionBundle}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.{ScParameter, ScTypeParam, ScTypeParamClause}
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScTypeAlias}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTrait}
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil
import org.jetbrains.plugins.scala.lang.scaladoc.parser.parsing.MyScaladocParsing
import org.jetbrains.plugins.scala.lang.scaladoc.psi.api.{ScDocComment, ScDocTag}

import scala.collection.mutable

class ScalaDocUnknownParameterInspection extends LocalInspectionTool {
  override def isEnabledByDefault: Boolean = true

  override def buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = {
    new ScalaElementVisitor {
      override def visitDocComment(docComment: ScDocComment): Unit = {
        val tagParams = mutable.HashMap[String, ScDocTag]()
        val tagTypeParams = mutable.HashMap[String, ScDocTag]()
        val duplicatingParams = mutable.HashSet[ScDocTag]()

        def insertDuplicating(element: Option[ScDocTag], duplicateElement: ScDocTag): Unit = {
          element.foreach(duplicatingParams ++= Set(_, duplicateElement))
        }

        def paramsDif(paramList: Iterable[ScParameter], tagParamList: Iterable[ScTypeParam]): Unit = {
          if (paramList != null) {
            for (funcParam <- paramList) {
              tagParams -= ScalaNamesUtil.clean(funcParam.name)
            }
          }
          if (tagParamList != null) {
            for (typeParam <- tagParamList) {
              tagTypeParams -= ScalaNamesUtil.clean(typeParam.name)
            }
          }
        }

        def collectDocParams(): Unit = {
          for (tagParam <- docComment.findTagsByName(Set("@param", "@tparam").contains _)) {
            if (tagParam.getValueElement != null) {
              tagParam.name match {
                case "@param" =>
                  insertDuplicating(tagParams.put(ScalaNamesUtil.clean(tagParam.getValueElement.getText),
                    tagParam.asInstanceOf[ScDocTag]), tagParam.asInstanceOf[ScDocTag])
                case "@tparam" =>
                  insertDuplicating(tagTypeParams.put(ScalaNamesUtil.clean(tagParam.getValueElement.getText),
                    tagParam.asInstanceOf[ScDocTag]), tagParam.asInstanceOf[ScDocTag])
              }
            }
          }
        }

        def registerBadParams(): Unit = {
          for ((_, badParameter) <- tagParams) {
            holder.registerProblem(holder.getManager.createProblemDescriptor(
              badParameter.getValueElement, ScalaInspectionBundle.message("unknown.tag.parameter"), true,
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly))
          }
          for ((_, badTypeParameter) <- tagTypeParams) {
            holder.registerProblem(holder.getManager.createProblemDescriptor(
              badTypeParameter.getValueElement, ScalaInspectionBundle.message("unknown.tag.type.parameter"), true,
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly))
          }
          for (duplicatingParam <- duplicatingParams) {
            holder.registerProblem(holder.getManager.createProblemDescriptor(
              duplicatingParam.getValueElement, ScalaInspectionBundle.message("one.param.or.tparam.tag.for.one.param.or.type.param.allowed"),
              true, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
              new ScalaDocDeleteDuplicatingParamQuickFix(duplicatingParam, true)))
          }
        }

        def doInspection(paramList: Iterable[ScParameter], tagParamList: Iterable[ScTypeParam]): Unit = {
          collectDocParams()
          paramsDif(paramList, tagParamList)
          registerBadParams()
        }

        val commentOwner = docComment.getOwner
        commentOwner match {
          case func: ScFunction =>
            doInspection(func.parameters, func.typeParameters)
          case clazz: ScClass =>
            val constr = clazz.constructor
            constr match {
              case Some(primaryConstr: ScPrimaryConstructor) =>
                primaryConstr.getClassTypeParameters match {
                  case Some(a: ScTypeParamClause) =>
                    doInspection(primaryConstr.parameters, a.typeParameters)
                  case None =>
                    doInspection(primaryConstr.parameters, null)
                }
              case None => registerBadParams()
            }
          case traitt: ScTrait =>
            doInspection(null, traitt.typeParameters)
          case _: ScTypeAlias => //scaladoc can't process tparams for type alias now
            for (tag <- docComment.findTagsByName(MyScaladocParsing.TYPE_PARAM_TAG)) {
              holder.registerProblem(holder.getManager.createProblemDescriptor(
                tag.getFirstChild, ScalaInspectionBundle.message("scaladoc.cant.process.tparams.for.type.alias.now"),
                true, ProblemHighlightType.WEAK_WARNING, isOnTheFly))
            }
          case _ => //we can't have params/tparams here
            for {
              tag <- docComment.findTagsByName(Set(MyScaladocParsing.PARAM_TAG, MyScaladocParsing.TYPE_PARAM_TAG).contains _)
              if tag.isInstanceOf[ScDocTag]
            } {
              holder.registerProblem(holder.getManager.createProblemDescriptor(
                tag.getFirstChild, ScalaInspectionBundle.message("param.and.tparams.tags.arnt.allowed.there"),
                true, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
                new ScalaDocDeleteDuplicatingParamQuickFix(tag.asInstanceOf[ScDocTag], false)))
            }
        }
      }
    }
  }
}


class ScalaDocDeleteDuplicatingParamQuickFix(paramTag: ScDocTag, isDuplicating: Boolean)
  extends AbstractFixOnPsiElement(
    if (isDuplicating) ScalaBundle.message("delete.duplicating.param") else ScalaBundle.message("delete.tag"), paramTag) {

  override def getFamilyName: String = FamilyName

  override protected def doApplyFix(tag: ScDocTag)
                                   (implicit project: Project): Unit = {
    tag.delete()
  }
}
