package team.ebi.aaa.data

import com.google.common.reflect.TypeToken
import com.google.gson.JsonPrimitive
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.{ObjectMappingException, serialize}

import scala.util.Random
import scala.util.parsing.combinator.RegexParsers

object DoubleRange {

  sealed trait FixedDouble

  case class Absolute(value: Double) extends FixedDouble {
    override def toString: String = value.formatted("%+f")
  }

  case class Relative(value: Double) extends FixedDouble {
    override def toString: String = (value * 100).formatted("%+f%%")
  }

  case class Probability(value: Double) extends FixedDouble {
    override def toString: String = (value * 100).formatted("%f%%")
  }

  case class Value[T <: FixedDouble](lower: T, upper: T, probability: Probability) {
    override def toString: String = {
      val suffix = if (probability.value < 1) " : " + probability.toString else ""
      val prefix = if (lower == upper) lower.toString else s"$lower ~ $upper"
      prefix + suffix
    }

    def average: Value[T] = this match {
      case Value(Absolute(a), Absolute(b), _) => Value(Absolute((a + b) / 2), Absolute((a + b) / 2), Probability(1)).asInstanceOf[Value[T]]
      case Value(Relative(a), Relative(b), _) => Value(Relative((a + b) / 2), Relative((a + b) / 2), Probability(1)).asInstanceOf[Value[T]]
    }

    def apply(rand: Random): Double => Double = this match {
      case Value(Absolute(a), Absolute(b), p) => if (rand.nextDouble() >= p.value) identity else {
        val chosen = if (rand.nextBoolean()) a + (b - a) * rand.nextDouble() else b - (b - a) * rand.nextDouble()
        input => input + math.max(-input, chosen)
      }
      case Value(Relative(a), Relative(b), p) => if (rand.nextDouble() >= p.value) identity else {
        val chosen = if (rand.nextBoolean()) a - (a - b) * rand.nextDouble() else b + (a - b) * rand.nextDouble()
        input => input * math.max(0, 1 + chosen)
      }
    }
  }

  private object Parsers extends RegexParsers {
    override def skipWhitespace: Boolean = false

    def ws: Parser[String] = opt(whiteSpace).^^(_.getOrElse(""))

    def zero: Parser[Double] = """(?:0)(?:\.0+)?""".r.^^(_.toDouble)

    def unsigned: Parser[Double] = """(?:0|[1-9]\d*)(?:\.\d+)?""".r.^^(_.toDouble)

    def signed: Parser[Double] = """[+-](?:0|[1-9]\d*)(?:\.\d+)?""".r.^^(_.toDouble)

    def absolute: Parser[Double] = zero | signed | failure("signed value expected here")

    def with_~[T](in: Parser[T]): Parser[Option[T]] = opt(ws ~> "~" ~ (ws ~> in)).^^(_.map(_._2))

    def with_:[T](in: Parser[T]): Parser[Option[T]] = opt(ws ~> ":" ~ (ws ~> in)).^^(_.map(_._2))

    def probability: Parser[Double] = (unsigned <~ "%" | failure("unsigned value expected here")).^^(_ / 100)

    def relative: Parser[Double] = ((zero | signed) <~ "%" | failure("signed value expected here")).^^(_ / 100)

    def range[T <: FixedDouble](in: Parser[T]): Parser[Value[T]] = (in ~ with_~(in) ~ with_:(probability)).^^ {
      case lower ~ Some(upper) ~ option => Value(lower, upper, Probability(option.getOrElse(1)))
      case lower ~ None ~ option => Value(lower, lower, Probability(option.getOrElse(1)))
    }

    def range: Parser[Value[_]] = phrase(ws ~> (range(relative.^^(Relative)) | range(absolute.^^(Absolute))) <~ ws)
  }

  private object TypeSerializer extends serialize.TypeSerializer[Value[_]] {
    private def parse(s: String): Value[_] = Parsers.parse(Parsers.range, s) match {
      case Parsers.NoSuccess(msg, _) => throw new ObjectMappingException(s"$msg (raw string: ${new JsonPrimitive(s)}")
      case Parsers.Success(result, _) => result
    }

    override def serialize(t: TypeToken[_], o: Value[_], v: ConfigurationNode): Unit = v.setValue(o.toString)

    override def deserialize(t: TypeToken[_], v: ConfigurationNode): Value[_] = parse(v.getString(""))
  }

  serialize.TypeSerializers.getDefaultSerializers.registerType(TypeToken.of(classOf[Value[_]]), TypeSerializer)
}
