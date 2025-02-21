package org.jetbrains.plugins.scala.compiler.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.lang3.StringUtils
import org.jetbrains.jps.incremental.scala.MessageKind
import org.jetbrains.plugins.scala.compiler.highlighting.ExternalHighlighting.{Pos, PosRange}
import org.jetbrains.plugins.scala.compiler.{CompilerEvent, CompilerEventListener}
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.project.template.FileExt

import scala.annotation.unused

@unused("registered in scala-plugin-common.xml")
private class UpdateCompilerGeneratedStateListener(project: Project)
  extends CompilerEventListener {

  import UpdateCompilerGeneratedStateListener.HandleEventResult
  
  override def eventReceived(event: CompilerEvent): Unit = {
    val oldState = CompilerGeneratedStateManager.get(project)

    val handleEventResult: Option[HandleEventResult] = event match {
      case CompilerEvent.CompilationStarted(_, _) =>
        val newHighlightOnCompilationFinished = oldState.toHighlightingState.filesWithHighlightings
        val newState = oldState.copy(highlightOnCompilationFinished = newHighlightOnCompilationFinished)
        Some(HandleEventResult(newState, Set.empty, informWolf = false))
      case CompilerEvent.MessageEmitted(compilationId, _, msg) =>
        for {
          text <- Option(msg.text)
          source <- msg.source
          virtualFile <- source.toVirtualFile
        } yield {
          val fromOpt = Pos.fromPosInfo(msg.from)
          val rangeOpt = fromOpt.map { fromPos =>
            val toPos = Pos.fromPosInfo(msg.to).getOrElse(fromPos)
            PosRange(fromPos, toPos)
          }
          val highlighting = ExternalHighlighting(
            highlightType = kindToHighlightInfoType(msg.kind, text),
            message = text,
            rangeOpt
          )
          val fileState = FileCompilerGeneratedState(compilationId, Set(highlighting))
          val newState = replaceOrAppendFileState(oldState, virtualFile, fileState)

          HandleEventResult(
            newState = newState,
            toHighlight = Set(virtualFile).filterNot(oldState.highlightOnCompilationFinished(_)),
            informWolf = false
          )
        }
      case CompilerEvent.ProgressEmitted(_, _, progress) =>
        val newState = oldState.copy(progress = progress)
        Some(HandleEventResult(newState, Set.empty, informWolf = false))
      case CompilerEvent.CompilationFinished(compilationId, _, sources) =>
        val vFiles = for {
          source <- sources
          virtualFile <- source.toVirtualFile
        } yield virtualFile
        val emptyState = FileCompilerGeneratedState(compilationId, Set.empty)
        val newState = vFiles.foldLeft(oldState) { case (acc, file) =>
          replaceOrAppendFileState(acc, file, emptyState)
        }.copy(progress = 1.0, highlightOnCompilationFinished = Set.empty)
        val toHighlight = vFiles.filter(oldState.highlightOnCompilationFinished(_))
        Some(HandleEventResult(newState, toHighlight, informWolf = true))
      case _ =>
        None
    }
    
    handleEventResult.foreach { case HandleEventResult(newState, toHighlight, informWolf) =>
      CompilerGeneratedStateManager.update(project, newState)

      //don't proceed with ProgressEmitted
      if (toHighlight.nonEmpty || informWolf) {
        val highlightingState = newState.toHighlightingState
        updateHighlightings(toHighlight, highlightingState)

        if (informWolf)
          ExternalHighlighters.informWolf(project, highlightingState)
      }
    }
  }

  private def kindToHighlightInfoType(kind: MessageKind, text: String): HighlightInfoType = kind match {
    case MessageKind.Error if isErrorMessageAboutWrongRef(text) =>
      HighlightInfoType.WRONG_REF
    case MessageKind.Error =>
      HighlightInfoType.ERROR
    case MessageKind.Warning =>
      HighlightInfoType.WARNING
    case MessageKind.Info =>
      HighlightInfoType.WEAK_WARNING
    case _ =>
      HighlightInfoType.INFORMATION
  }

  private def isErrorMessageAboutWrongRef(text: String): Boolean =
    StringUtils.startsWithIgnoreCase(text, "value") && text.contains("is not a member of") ||
      StringUtils.startsWithIgnoreCase(text, "not found:") ||
      StringUtils.startsWithIgnoreCase(text, "cannot find symbol")

  private def replaceOrAppendFileState(oldState: CompilerGeneratedState,
                                       file: VirtualFile,
                                       fileState: FileCompilerGeneratedState): CompilerGeneratedState = {
    val newFileState = oldState.files.get(file) match {
      case Some(oldFileState) if oldFileState.compilationId == fileState.compilationId =>
        oldFileState.withExtraHighlightings(fileState.highlightings)
      case _ =>
        fileState
    }
    val newFileStates = oldState.files.updated(file, newFileState)
    oldState.copy(files = newFileStates)
  }

  private def updateHighlightings(virtualFiles: Set[VirtualFile], state: HighlightingState): Unit = try {
    val filteredVirtualFiles = ExternalHighlighters.filterFilesToHighlightBasedOnFileLevel(virtualFiles, project)
    for {
      editor <- EditorFactory.getInstance.getAllEditors
      editorProject <- Option(editor.getProject)
      if editorProject == project
      vFile <- editor.getDocument.virtualFile
      if filteredVirtualFiles contains vFile
    } ExternalHighlighters.applyHighlighting(project, editor, state)
  } catch {
    //don't know what else we can do if compilation was cancelled at this stage
    //probably just don't show updated highlightings
    case _: ProcessCanceledException =>
      //ignore
  }
}

object UpdateCompilerGeneratedStateListener {

  private case class HandleEventResult(newState: CompilerGeneratedState,
                                       toHighlight: Set[VirtualFile],
                                       informWolf: Boolean)
}
