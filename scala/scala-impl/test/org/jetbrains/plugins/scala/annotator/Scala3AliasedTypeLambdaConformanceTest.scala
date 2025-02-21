package org.jetbrains.plugins.scala
package annotator

import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestCase
import org.jetbrains.plugins.scala.{LatestScalaVersions, ScalaVersion}
import org.junit.experimental.categories.Category

@Category(Array(classOf[TypecheckerTests]))
class Scala3AliasedTypeLambdaConformanceTest extends ScalaLightCodeInsightFixtureTestCase {
  override protected def supportedIn(version: ScalaVersion): Boolean =
    version >= LatestScalaVersions.Scala_3_0

  private val context: String =
    """
      |trait Bar[A]
      |val xs: Bar[String] = ???
      |def foo[F[_], A](fa: F[A]): F[A] = fa
      |""".stripMargin

  private def doTest(code: String): Unit =
    checkTextHasNoErrors(
      s"""
         |object Test {
         |$context
         |$code
         |}
         |""".stripMargin)

  def testSimpleLambda(): Unit = doTest(
    """
      |type TL = [A] =>> Bar[A]
      |foo[TL, String](xs)
      |""".stripMargin
  )

  def testMultiListLambda(): Unit = doTest(
    """
      |type TL2 = [A] =>> [B] =>> Bar[A]
      |foo[TL2[String], Int](xs)
      |""".stripMargin
  )

  def testAliasAndLambdaBothHaveTypeParameters(): Unit = doTest(
    """
      |type Foo[A] = [B] =>> [C] =>> Bar[B]
      |foo[Foo[Int][String], Double](xs)
      |""".stripMargin
  )

  def testChainedAlias(): Unit = doTest(
    """
      |type TL3 = [A] =>> [B] =>> Int
      |type C = TL3
      |foo[C[Int], String](1)
      |""".stripMargin
  )

  def testTypeLambdaNeg(): Unit = checkHasErrorAroundCaret(
    s"""
       |object Test {
       |  $context
       |  type TL4 = [A] =>> [B] =>> Bar[B]
       |  foo[TL4[String], Int](x${CARET}s)
       |}
       |""".stripMargin
  )
}
