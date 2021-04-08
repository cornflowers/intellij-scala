package org.jetbrains.plugins.scala.lang.typeInference.generated

import org.jetbrains.plugins.scala.base.libraryLoaders.ScalaSDKLoader
import org.jetbrains.plugins.scala.lang.typeInference.TypeInferenceTestBase

/**
 * @author Alefas
 * @since 05.09.12
 */
class TypeInferenceScalaReflectTest extends TypeInferenceTestBase {
  //This class was generated by build script, please don't change this
  override def folderPath: String = super.folderPath + "scalaReflect/"

  override protected def librariesLoaders = Seq(ScalaSDKLoader(includeScalaReflect = true))

  def testSCL4662(): Unit = doTest()

  def testSCL5592(): Unit = doTest()

  def testSCL5790(): Unit = doTest()

  def testSCL5871(): Unit = doTest()
}