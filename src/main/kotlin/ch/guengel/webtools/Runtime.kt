package ch.guengel.webtools

import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

object Runtime {
    private val logger = LoggerFactory.getLogger("Runtime")
    val vertx: Vertx by lazy {
        logger.info("Initializing Vertx")
        Vertx.vertx()
    }
}