package shade.memcached.internals

import concurrent.atomic.Atomic
import akka.dispatch._

sealed trait PartialResult[+T]
case class FinishedResult[T](result: Either[Throwable, Result[T]]) extends PartialResult[T]
case class FutureResult[T](result: Future[Result[T]]) extends PartialResult[T]
case object NoResultAvailable extends PartialResult[Nothing]

final class MutablePartialResult[T] {
  def tryComplete(result: Either[Throwable, Result[T]]) =
    _result.compareAndSet(NoResultAvailable, FinishedResult(result))

  def tryCompleteWith(result: Future[Result[T]]) =
    _result.compareAndSet(NoResultAvailable, FutureResult(result))

  def completePromise(key: String, promise: Promise[Result[T]]) {
    _result.get match {
      case FinishedResult(result) =>
        promise.tryComplete(result)
      case FutureResult(result) =>
        result.onComplete { case r => promise.tryComplete(r) }
      case NoResultAvailable =>
        promise.tryComplete(Right(FailedResult(key, IllegalCompleteStatus)))
    }
  }

  private[this] val _result =
    Atomic[PartialResult[T]](NoResultAvailable)
}