package org.jetbrains.plugins.scala.worksheet.integration.repl

import org.jetbrains.plugins.scala.util.RevertableChange.withModifiedRegistryValue
import org.jetbrains.plugins.scala.util.assertions.StringAssertions.assertIsBlank
import org.jetbrains.plugins.scala.util.runners.{RunWithJdkVersions, RunWithScalaVersions, TestJdkVersion, TestScalaVersion}
import org.jetbrains.plugins.scala.worksheet.WorksheetUtils
import org.jetbrains.plugins.scala.worksheet.actions.topmenu.RunWorksheetAction.RunWorksheetActionResult.WorksheetRunError
import org.jetbrains.plugins.scala.worksheet.integration.WorksheetIntegrationBaseTest.TestRunResult
import org.jetbrains.plugins.scala.worksheet.processor.WorksheetCompiler.WorksheetCompilerResult
import org.junit.Assert.assertEquals

@RunWithScalaVersions(Array(TestScalaVersion.Scala_2_12))
class WorksheetReplIntegration_Scala_2_12_Test extends WorksheetReplIntegration_Scala_2_11_Test {

  override def testRestoreErrorPositionsInOriginalFile(): Unit =
    withModifiedRegistryValue(WorksheetUtils.ContinueOnFirstFailure, newValue = true).run {
      val expectedCompilerOutput =
        """Error:(2, 7) not found: value unknown1
          |unknown1
          |Error:(8, 1) not found: value unknownVar
          |unknownVar = 23 +
          |Error:(11, 14) not found: value unknownVar
          |val $ires0 = unknownVar
          |Error:(12, 5) not found: value unknown3
          |unknown3 +
          |Error:(13, 7) not found: value unknown4
          |unknown4
          |Error:(17, 9) not found: value unknown5
          |unknown5
          |Error:(22, 5) not found: value unknown6
          |unknown6; val z =
          |Error:(23, 7) not found: value unknown7
          |unknown7
          |Error:(29, 5) not found: value unknown8
          |unknown8
          |Error:(35, 5) not found: value unknown9
          |unknown9
          |Error:(39, 5) not found: value unknown10
          |unknown10
          |Error:(48, 5) not found: value unknown11
          |unknown11
          |Error:(50, 7) not found: value unknown12
          |unknown12
          |Error:(59, 7) not found: value unknown13
          |unknown13
          |Error:(62, 3) not found: value unknown14
          |unknown14""".stripMargin

      val TestRunResult(editor, evaluationResult) =
        doRenderTestWithoutCompilationChecks2(RestoreErrorPositionsInOriginalFileCode, output => assertIsBlank(output))
      assertEquals(WorksheetRunError(WorksheetCompilerResult.CompilationError), evaluationResult)
      assertCompilerMessages(editor)(expectedCompilerOutput)
    }

  // TODO: why is there this strange error
  //  Error:(10, 14) not found: value unknownVar
  //  val $ires0 = unknownVar
  //  ?
  @RunWithScalaVersions(Array(TestScalaVersion.Scala_2_12_12, TestScalaVersion.Scala_2_12_6))
  @RunWithJdkVersions(Array(TestJdkVersion.JDK_11))
  def testRestoreErrorPositionsInOriginalFile_ExtraScalaVersions(): Unit =
    testRestoreErrorPositionsInOriginalFile()
}
