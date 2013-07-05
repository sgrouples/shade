package shade.tests

import shade.memcached.{FailureMode, Protocol, Configuration}
import akka.actor.ActorSystem
import shade.Memcached
import akka.util.FiniteDuration
import akka.util.duration._

trait MemcachedTestHelpers {
  val system = ActorSystem("default")
  implicit val ec = system.dispatcher

  val defaultConfig = Configuration(
    addresses = "127.0.0.1:11211",
    authentication = None,
    keysPrefix = Some("my-tests"),
    protocol = Protocol.Binary,
    failureMode = FailureMode.Retry,
    operationTimeout = 15.seconds
  )

  def createCacheObject(system: ActorSystem, prefix: String, maxRetries: Option[Int] = None, opTimeout: Option[FiniteDuration] = None, failureMode: Option[FailureMode.Value] = None): Memcached = {
    val config = defaultConfig.copy(
      keysPrefix = defaultConfig.keysPrefix.map(s => s + "-" + prefix),
      maxTransformCASRetries = maxRetries.getOrElse(1000000),
      failureMode = failureMode.getOrElse(defaultConfig.failureMode),
      operationTimeout = opTimeout.getOrElse(defaultConfig.operationTimeout)
    )

    Memcached(config, system.scheduler, ec)
  }

  def withCache[T](prefix: String, maxRetries: Option[Int] = None, failureMode: Option[FailureMode.Value] = None)(cb: Memcached => T): T = {
    val system = ActorSystem("memcached-test")
    val cache = createCacheObject(system = system, prefix = prefix, maxRetries = maxRetries, failureMode = failureMode)

    try {
      cb(cache)
    }
    finally {
      cache.shutdown()
    }
  }
}
