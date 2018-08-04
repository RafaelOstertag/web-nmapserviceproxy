package ch.guengel.webtools

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.CorsHandler
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

    private val corsHandler by lazy {
        logger.info("Initializing CORS handler")
        CorsHandler.create("*")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Access-Control-Allow-Origin")
    }

    init {
        router
            .route()
            .handler(LoggerHandler.create()::handle)
            .handler(corsHandler)
            .failureHandler(this::defaultFailureHandler)
    }

    fun defaultFailureHandler(routingContext: RoutingContext) {
        var status: Int
        var phrase: String

        val contextFailure = routingContext.failure()
        when {
            contextFailure is HttpException -> {
                status = contextFailure.statusCode
                phrase = contextFailure.message ?: "unknown"
            }
            contextFailure is RuntimeException -> {
                status = 500
                phrase = contextFailure.message ?: "unkown"
            }
            else -> {
                status = routingContext.statusCode()
                phrase = HttpResponseStatus.valueOf(status).reasonPhrase()
            }
        }

        routingContext.response()
            .contentTypeJson()
            .setStatusCode(status)
            .end(errorResponse(phrase))
    }

    fun addRoute(path: String, handler: (RoutingContext) -> Unit) {
        logger.info("Add route for '{}'", path)
        router
            .route(path)
            .handler(handler)
    }

    fun start(port: Int = 8080) {
        logger.info("Starting server on port {}", port)

        // This will be called if no other route handles the request
        router.route()
            .handler {
                it.fail(404)
            }
            .failureHandler(::defaultFailureHandler)

        server
            .requestHandler(router::accept)
            .listen(port)
    }
}

fun RoutingContext.getClientIp(): String {
    val header = this.request().getHeader("x-forwarded-for")
    val ip = header?.split("\\s*,\\s*".toRegex())?.get(0)
    return ip ?: this.request().connection().remoteAddress().host()
}

fun HttpServerResponse.contentTypeJson(): HttpServerResponse {
    this.putHeader("Content-Type", "application/json")
    return this
}
