package reactivemongo.play

import scala.compiletime.testing.{ typeCheckErrors, Error }

import org.specs2.execute.{ TypecheckErrors, TypecheckResult, TypecheckSuccess }

object TestUtils:

  inline def typecheck(inline code: String): TypecheckResult =
    typeCheckErrors(code).headOption match {
      case Some(err @ Error(_, _, _, _)) =>
        TypecheckErrors(List(err))

      case _ =>
        TypecheckSuccess
    }
