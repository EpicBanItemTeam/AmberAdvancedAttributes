package io.izzel.aaa.range

import scala.util.parsing.combinator._

object RangeParser extends RegexParsers {

  case class DoubleWithPercent(value: Double, percent: Double) {
    override def toString: String = f"$value%+f${"-++".charAt(percent.signum + 1)}${percent.abs}%f%%"
  }

  case class Value(lower: DoubleWithPercent, upper: DoubleWithPercent) {
    override def toString: String = if (lower == upper) lower.toString else s"($lower)~($upper)"
  }

  private def ws: Parser[String] = opt(whiteSpace).^^(_.getOrElse(""))

  private def num: Parser[Double] = """[+-]?(?:0|[1-9]\d*)(?:\.\d+)?""".r.^^(_.toDouble)

  private def percent: Parser[DoubleWithPercent] = (num <~ "%").^^(DoubleWithPercent(0, _))

  private def numProduct: Parser[Double] = (numValue ~ rep(ws ~> ("*" | "/") ~ (ws ~> numValue))).^^ {
    case value ~ tail => tail.foldLeft(value)((a, b) => b match {
      case "*" ~ c => a * c
      case "/" ~ c => a / c
    })
  }

  private def numSum: Parser[Double] = (numProduct ~ rep(ws ~> ("+" | "-") ~ (ws ~> numProduct))).^^ {
    case value ~ tail => tail.foldLeft(value)((a, b) => b match {
      case "+" ~ c => a + c
      case "-" ~ c => a - c
    })
  }

  private def numValue: Parser[Double] = "(" ~> numSum <~ ")" | num

  private def productReverse: Parser[DoubleWithPercent] = (rep(numValue ~ (ws ~> "*")) ~ (ws ~> value)).^^ {
    case head ~ value => head.foldRight(value)((a, b) => a match {
      case c ~ "*" => DoubleWithPercent(c * b.value, c * b.percent)
    })
  }

  private def product: Parser[DoubleWithPercent] = (productReverse ~ rep(ws ~> ("*" | "/") ~ (ws ~> numValue))).^^ {
    case value ~ tail => tail.foldLeft(value)((a, b) => b match {
      case "*" ~ c => DoubleWithPercent(a.value * c, a.percent * c)
      case "/" ~ c => DoubleWithPercent(a.value / c, a.percent / c)
    })
  }

  private def sum: Parser[DoubleWithPercent] = (product ~ rep(ws ~> ("+" | "-") ~ (ws ~> product))).^^ {
    case value ~ tail => tail.foldLeft(value)((a, b) => b match {
      case "+" ~ c => DoubleWithPercent(a.value + c.value, a.percent + c.percent)
      case "-" ~ c => DoubleWithPercent(a.value - c.value, a.percent - c.percent)
    })
  }

  private def value: Parser[DoubleWithPercent] = "(" ~> sum <~ ")" | percent | numValue.^^(DoubleWithPercent(_, 0))

  def range: Parser[Value] = (sum ~ opt(ws ~> "~" ~ (ws ~> sum))).^^ {
    case value ~ tail => Value(value, tail.map(_._2).getOrElse(value))
  }
}
