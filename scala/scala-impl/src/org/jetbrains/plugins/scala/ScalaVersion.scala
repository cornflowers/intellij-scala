package org.jetbrains.plugins.scala

import com.intellij.lang.Language
import org.jetbrains.plugins.scala.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.util.HashBuilder._

final class ScalaVersion(
  val languageLevel: ScalaLanguageLevel,
  val minorSuffix: String
) extends Ordered[ScalaVersion] {

  def major: String = languageLevel.getVersion

  def minor: String = major + "." + minorSuffix

  @inline def isScala2: Boolean = languageLevel.isScala2
  @inline def isScala3: Boolean = languageLevel.isScala3
  @inline def language: Language = languageLevel.getLanguage

  lazy val minorVersion: project.Version = project.Version(minorSuffix)

  override def compare(that: ScalaVersion): Int =
    (languageLevel, minorVersion) compare (that.languageLevel, that.minorVersion)

  def withMinor(newMinorSuffix: String): ScalaVersion = new ScalaVersion(languageLevel, newMinorSuffix)
  def withMinor(newMinorSuffix: Int): ScalaVersion = withMinor(newMinorSuffix.toString)

  override def equals(other: Any): Boolean = other match {
    case that: ScalaVersion =>
      that.languageLevel == languageLevel &&
        that.minorSuffix == minorSuffix
    case _ => false
  }

  override def hashCode(): Int = languageLevel #+ minorSuffix

  override def toString: String = s"ScalaVersion($minor)"
}

//noinspection TypeAnnotation
object ScalaVersion {

  def Latest: LatestScalaVersions.type = LatestScalaVersions

  def default: ScalaVersion =
    LatestScalaVersions.all.find(_.languageLevel == ScalaLanguageLevel.getDefault).get

  private val versionRegex = """(\d)\.(\d\d?)\.(.+)""".r

  /**
   * @param versionString minor scala version in format 2.13.2
   */
  def fromString(versionString: String): Option[ScalaVersion] =
    versionString match {
      case versionRegex(_, _, z) =>
        val level = ScalaLanguageLevel.findByVersion(versionString)
        level.map(new ScalaVersion(_, z))
      case _                     => None
    }
}

object LatestScalaVersions {

  // duplicated here to refer with `ScalaVersion.` prefix
  val Scala_2_9  = new ScalaVersion(ScalaLanguageLevel.Scala_2_9, "3")
  val Scala_2_10 = new ScalaVersion(ScalaLanguageLevel.Scala_2_10, "7")
  val Scala_2_11 = new ScalaVersion(ScalaLanguageLevel.Scala_2_11, "12")
  val Scala_2_12 = new ScalaVersion(ScalaLanguageLevel.Scala_2_12, "17")
  val Scala_2_13 = new ScalaVersion(ScalaLanguageLevel.Scala_2_13, "10")
  val Scala_3_0  = new ScalaVersion(ScalaLanguageLevel.Scala_3_0, "2")
  val Scala_3_1  = new ScalaVersion(ScalaLanguageLevel.Scala_3_1, "3")
  val Scala_3_2  = new ScalaVersion(ScalaLanguageLevel.Scala_3_2, "1")

  val Scala_3: ScalaVersion = Scala_3_2

  val all: Seq[ScalaVersion] = Seq(
    Scala_2_9,
    Scala_2_10,
    Scala_2_11,
    Scala_2_12,
    Scala_2_13,
    Scala_3_0,
    Scala_3_1,
    Scala_3_2
  )
}
