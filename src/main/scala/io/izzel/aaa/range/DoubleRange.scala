package io.izzel.aaa.range

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.{TypeSerializer, TypeSerializers}

import scala.util.parsing.combinator._

object DoubleRange extends RegexParsers {

  TypeSerializers.getDefaultSerializers.registerType(TypeToken.of(classOf[Value[_]]), new TypeSerializer[Value[_]] {
    override def serialize(t: TypeToken[_], o: Value[_], v: ConfigurationNode): Unit = v.setValue(o.toString)

    override def deserialize(t: TypeToken[_], v: ConfigurationNode): Value[_] = parseAll(range, v.getString("")) match {
      case NoSuccess(msg, _) => throw new ObjectMappingException(msg)
      case Success(result, _) => result
    }
  })

  sealed trait FixedDouble

  case class Absolute(value: Double) extends FixedDouble {
    override def toString: String = value.formatted("%+f")
  }

  case class Relative(value: Double) extends FixedDouble {
    override def toString: String = value.formatted("%+f%%")
  }

  case class Value[T <: FixedDouble](lower: T, upper: T) {
    override def toString: String = if (lower == upper) lower.toString else s"$lower ~ $upper"
  }

  override def skipWhitespace: Boolean = false

  private def ws: Parser[String] = opt(whiteSpace).^^(_.getOrElse(""))

  private def unsigned: Parser[Double] = """(?:0)(?:\.0+)?""".r.^^(_.toDouble)

  private def signed: Parser[Double] = """[+-](?:0|[1-9]\d*)(?:\.\d+)?""".r.^^(_.toDouble)

  private def absolute: Parser[Absolute] = (unsigned | signed | failure("signed value expected")).^^(Absolute)

  private def relative: Parser[Relative] = ((unsigned | signed | failure("signed value expected")) <~ "%").^^(Relative)

  private def range[T <: FixedDouble](in: Parser[T]): Parser[Value[T]] = (in ~ opt(ws ~> "~" ~ (ws ~> in))).^^ {
    case lower ~ Some("~" ~ upper) => Value(lower, upper)
    case lower ~ None => Value(lower, lower)
  }

  def range: Parser[Value[_ <: FixedDouble]] = ws ~> (range(relative) | range(absolute)) <~ ws
}
