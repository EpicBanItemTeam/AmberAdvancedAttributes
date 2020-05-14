package team.ebi.aaa.util.impl

trait ChainingUtil {
  implicit class RichObject[A](value: A) {
    def tap[U](f: A => U): A = {
      f(value)
      value
    }

    def pipe[B](f: A => B): B = f(value)
  }
}
