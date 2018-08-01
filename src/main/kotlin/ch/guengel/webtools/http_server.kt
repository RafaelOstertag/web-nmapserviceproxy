package ch.guengel.webtools

import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory

object HttpServer {
    private val logger = LoggerFactory.getLogger("HttpServer")
    private val server by lazy {
        logger.info("Create http server")
        HttpServerOptions()
        Runtime.vertx.createHttpServer()
    }

    private val router by lazy {
        logger.info("Initialize router")
        Router.router(Runtime.vertx)
    }

    init {
        val logHandler = LoggerHandler.create()
        router
            .route()
            .handler {
                logHandler.handle(it)
            }

        server.requestHandler { router.accept(it) }
    }

    private fun addHandler(handler: (HttpServerRequest) -> Unit) {
        server.requestHandler(handler)
    }

    fun addRoute(path: String, handler: (RoutingContext) -> Unit) {
        logger.info("Add route for '{}'", path)
        router.route(path).handler(handler)

        addHandler { router.accept(it) }
    }

    fun start(port: Int = 8080) {
        logger.info("Starting server on port {}", port)
        server.listen(port)
    }
}

fun RoutingContext.getClientIp(): String {
    val header = this.request().getHeader("x-forwarded-for")
    val ip = header?.split("\\s*,\\s*".toRegex())?.get(0)
    return ip ?: this.request().connection().remoteAddress().host()
}
