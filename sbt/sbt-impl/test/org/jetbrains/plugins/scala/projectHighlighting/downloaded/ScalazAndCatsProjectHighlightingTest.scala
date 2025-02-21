package org.jetbrains.plugins.scala.projectHighlighting.downloaded

import org.jetbrains.plugins.scala.HighlightingTests
import org.jetbrains.plugins.scala.projectHighlighting.base.GithubRepositoryWithRevision
import org.junit.experimental.categories.Category

@Category(Array(classOf[HighlightingTests]))
class ScalazAndCatsProjectHighlightingTest extends GithubSbtAllProjectHighlightingTest {

  override protected def githubRepositoryWithRevision: GithubRepositoryWithRevision =
    GithubRepositoryWithRevision("fosskers", "scalaz-and-cats", "e35a79297e06fafa7c76dda4bd74862131f2d37b")

  //"scalaz-and-cats" project has extra "scala" folder which contains the actual project
  override def getTestProjectPath: String = s"${super.getTestProjectPath}/scala"
}
