package org.jetbrains.plugins.scala.lang.refactoring.ui

import com.intellij.codeInsight.daemon.impl.JavaReferenceImporter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.ui.CodeFragmentTableCellEditorBase
import org.jetbrains.plugins.scala.ScalaFileType

class ScalaCodeFragmentTableCellEditor(project: Project)
  extends CodeFragmentTableCellEditorBase(project, ScalaFileType.INSTANCE) {

  override def stopCellEditing: Boolean = {
    val editor: Editor = myEditorTextField.getEditor
    if (editor != null) {
      val offset = editor.getCaretModel.getOffset
      new JavaReferenceImporter().computeAutoImportAtOffset(editor, myCodeFragment, offset, true).getAsBoolean
    }
    super.stopCellEditing
  }

}
