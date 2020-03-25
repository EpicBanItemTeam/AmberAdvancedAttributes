package io.izzel.aaa.range

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.{TypeSerializer, TypeSerializers}

import scala.util.parsing.combinator._

object DoubleRange extends RegexParsers {

  TypeSerializers.getDefaultSerializers.registerType(TypeToken.of(classOf[Value]), new TypeSerializer[Value] {
    override def serialize(t: TypeToken[_], o: Value, v: ConfigurationNode): Unit = v.setValue(o.toString)

    override def deserialize(t: TypeToken[_], v: ConfigurationNode): Value = parseAll(range, v.getString("")) match {
      case Failure(msg, _) => throw new ObjectMappingException(msg)
      case Success(result, _) => result
    }
  })

  case class DoubleWithPercent(value: Double, percent: Double) {
    override def toString: String = f"$value%+f ${"-++".charAt(percent.signum + 1)} ${percent.abs}%f%%"
  }

  case class Value(lower: DoubleWithPercent, upper: DoubleWithPercent) {
    override def toString: String = if (lower == upper) lower.toString else s"($lower) ~ ($upper)"
  }

  override def skipWhitespace: Boolean = false

  private def ws1: Parser[String] = whiteSpace

  private def ws: Parser[String] = opt(whiteSpace).^^(_.getOrElse(""))

  private def num: Parser[Double] = """[+-]?(?:0|[1-9]\d*)(?:\.\d+)?""".r.^^(_.toDouble)

  private def percent: Parser[DoubleWithPercent] = (num <~ "%").^^(DoubleWithPercent(0, _))

  private def numProduct: Parser[Double] = (numValue ~ rep(ws ~> ("*" | "/") ~ (ws ~> numValue))).^^ {
    case value ~ tail => tail.foldLeft(value) {
      case (a, "*" ~ c) => a * c
      case (a, "/" ~ c) => a / c
    }
  }

  private def numSum: Parser[Double] = (numProduct ~ rep(ws1 ~> ("+" | "-") ~ (ws1 ~> numProduct))).^^ {
    case value ~ tail => tail.foldLeft(value) {
      case (a, "+" ~ c) => a + c
      case (a, "-" ~ c) => a - c
    }
  }

  private def numValue: Parser[Double] = "(" ~> ws ~> numSum <~ ws <~ ")" | num

  private def productReverse: Parser[DoubleWithPercent] = (rep(numValue ~ (ws ~> "*")) ~ (ws ~> value)).^^ {
    case head ~ value => head.foldRight(value) {
      case (c ~ "*", b) => DoubleWithPercent(c * b.value, c * b.percent)
    }
  }

  private def product: Parser[DoubleWithPercent] = (productReverse ~ rep(ws ~> ("*" | "/") ~ (ws ~> numValue))).^^ {
    case value ~ tail => tail.foldLeft(value) {
      case (a, "*" ~ c) => DoubleWithPercent(a.value * c, a.percent * c)
      case (a, "/" ~ c) => DoubleWithPercent(a.value / c, a.percent / c)
    }
  }

  private def sum: Parser[DoubleWithPercent] = (product ~ rep(ws1 ~> ("+" | "-") ~ (ws1 ~> product))).^^ {
    case value ~ tail => tail.foldLeft(value) {
      case (a, "+" ~ c) => DoubleWithPercent(a.value + c.value, a.percent + c.percent)
      case (a, "-" ~ c) => DoubleWithPercent(a.value - c.value, a.percent - c.percent)
    }
  }

  private def value: Parser[DoubleWithPercent] = "(" ~> ws ~> sum <~ ws <~ ")" | percent | numValue.^^(DoubleWithPercent(_, 0))

  def range: Parser[Value] = (sum ~ opt(ws1 ~> "~" ~ (ws1 ~> sum))).^^ {
    case value ~ tail => Value(value, tail.map(_._2).getOrElse(value))
  }
}
