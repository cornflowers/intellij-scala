package org.jetbrains.plugins.scala.testingSupport.scalatest.base

trait ScalaTestSpecialCharactersTest extends ScalaTestTestCase {

  private val className = "SpecialCharactersTest"
  private val fileName = className + ".scala"

  private val commaTestPath = TestNodePath("[root]", className, "Comma , test", "should contain , comma")
  private val exclamationTestPath = TestNodePath("[root]", className, "! test", "should contain !")
  private val tickTestPath = TestNodePath("[root]", className, "tick ' test", "should contain '")
  private val tildeTestPath = TestNodePath("[root]", className, "tilde ~ test", "should contain ~")
  private val backtickTestPath = TestNodePath("[root]", className, "backtick ` test", "should contain `")

  private val className1 = "ClassInPackageWithReservedKeywordsTest"
  private val packageName1 = "myPackage.type.implicit"
  private val classFullName1 = packageName1 + s".$className1"
  private val classFilePath1 = packageName1.replace(".", "/") + s"/$className1.scala"
  private val classTestTreePath1 = TestNodePath("[root]", className1, "test", "should work")

  addSourceFile(s"$className.scala",
    s"""$ImportsForFlatSpec
       |
       |class $className extends $FlatSpecBase with GivenWhenThen {
       |  "Comma , test" should "contain , comma" in {
       |  }
       |
       |  "! test" should "contain !" in {
       |  }
       |
       |  "tick ' test" should "contain '" in {
       |  }
       |
       |  "backtick ` test" should "contain `" in {
       |  }
       |
       |  "tilde ~ test" should "contain ~" in {
       |  }
       |}""".stripMargin
  )

  addSourceFile(classFilePath1,
    s"""package myPackage.`type`.`implicit`
       |
       |$ImportsForFlatSpec
       |
       |class $className1 extends $FlatSpecBase {
       |
       |  "test" should "work" in {}
       |}""".stripMargin
  )

  def testComma(): Unit =
    runTestByLocation(loc(fileName, 3, 3),
      assertConfigAndSettings(_, className, "Comma , test should contain , comma"),
      root => assertResultTreeHasExactNamedPath(root, commaTestPath)
    )

  def testExclamation(): Unit =
    runTestByLocation(loc(fileName, 6, 3),
      assertConfigAndSettings(_, className, "! test should contain !"),
      root => assertResultTreeHasExactNamedPath(root, exclamationTestPath)
    )

  def testTick(): Unit =
    runTestByLocation(loc(fileName, 9, 3),
      assertConfigAndSettings(_, className, "tick ' test should contain '"),
      root => assertResultTreeHasExactNamedPath(root, tickTestPath)
    )

  def testTilde(): Unit =
    runTestByLocation(loc(fileName, 15, 3),
      assertConfigAndSettings(_, className, "tilde ~ test should contain ~"),
      root => assertResultTreeHasExactNamedPath(root, tildeTestPath)
    )

  def testBacktick(): Unit =
    runTestByLocation(loc(fileName, 12, 3),
      assertConfigAndSettings(_, className, "backtick ` test should contain `"),
      root => assertResultTreeHasExactNamedPath(root, backtickTestPath)
    )

  def testClassInPackageWithReservedKeywordInName(): Unit =
    runTestByLocation(loc(classFilePath1, 6, 10),
      assertConfigAndSettings(_, classFullName1, "test should work"),
      root => assertResultTreeHasExactNamedPath(root, classTestTreePath1)
    )
}
