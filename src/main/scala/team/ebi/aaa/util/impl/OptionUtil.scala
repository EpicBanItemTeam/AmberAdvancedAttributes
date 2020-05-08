package team.ebi.aaa.util.impl

import java.util.{Optional, OptionalDouble, OptionalInt, OptionalLong}

trait OptionUtil {

  implicit class AsJava[A >: Null](value: Option[A]) {
    def asJava: Optional[A] = value match {
      case Some(elem) => Optional.ofNullable(elem);
      case None => Optional.empty[A]
    }
  }

  implicit class AsJavaDouble(value: Option[Double]) {
    def asJava: OptionalDouble = value match {
      case Some(elem) => OptionalDouble.of(elem)
      case None => OptionalDouble.empty
    }
  }

  implicit class AsJavaInt(value: Option[Int]) {
    def asJava: OptionalInt = value match {
      case Some(elem) => OptionalInt.of(elem)
      case None => OptionalInt.empty
    }
  }

  implicit class AsJavaLong(value: Option[Long]) {
    def asJava: OptionalLong = value match {
      case Some(elem) => OptionalLong.of(elem)
      case None => OptionalLong.empty
    }
  }

  implicit class AsScala[A >: Null](value: Optional[A]) {
    def asScala: Option[A] = if (value.isPresent) Some(value.get) else None
  }

  implicit class AsScalaDouble(value: OptionalDouble) {
    def asScala: Option[Double] = if (value.isPresent) Some(value.getAsDouble) else None
  }

  implicit class AsScalaInt(value: OptionalInt) {
    def asScala: Option[Int] = if (value.isPresent) Some(value.getAsInt) else None
  }

  implicit class AsScalaLong(value: OptionalLong) {
    def asScala: Option[Long] = if (value.isPresent) Some(value.getAsLong) else None
  }
}
