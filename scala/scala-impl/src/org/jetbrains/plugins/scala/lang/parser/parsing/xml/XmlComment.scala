package org.jetbrains.plugins.scala.lang.parser.parsing.xml

import org.jetbrains.plugins.scala.lang.lexer.ScalaXmlTokenTypes
import org.jetbrains.plugins.scala.lang.parser.{ErrMsg, ScalaElementType}
import org.jetbrains.plugins.scala.lang.parser.parsing.ParsingRule
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder

/*
 * XmlComment ::= <!-- comment -->
 */
object XmlComment extends ParsingRule {
  override def parse(implicit builder: ScalaPsiBuilder): Boolean = {
    val commentMarker = builder.mark()
    builder.getTokenType match {
      case ScalaXmlTokenTypes.XML_COMMENT_START => builder.advanceLexer()
      case _ =>
        commentMarker.drop()
        return false
    }
    while (builder.getTokenType!=ScalaXmlTokenTypes.XML_COMMENT_END && builder.getTokenType != null) {
      if (builder.getTokenType == ScalaXmlTokenTypes.XML_BAD_CHARACTER) builder error ErrMsg("xml.wrong.character")
      builder.advanceLexer()
    }
    builder.getTokenType match {
      case ScalaXmlTokenTypes.XML_COMMENT_END => builder.advanceLexer()
      case _ => builder error ErrMsg("xml.comment.end.expected")
    }
    commentMarker.done(ScalaElementType.XML_COMMENT)
    true
  }
}