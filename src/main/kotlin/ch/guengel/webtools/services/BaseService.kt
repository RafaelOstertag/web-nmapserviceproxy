package ch.guengel.webtools.services

import ch.guengel.webtools.HttpException
import ch.guengel.webtools.Runtime.vertx
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientResponse
import io.vertx.kotlin.core.http.HttpClientOptions
import org.slf4j.LoggerFactory

open class BaseService(server: String, port: Int) {
    protected val logger = LoggerFactory.getLogger("Service")
    protected val client: HttpClient

    init {
        logger.info("Initializing HTTP client")
        val options = HttpClientOptions()
            .setDefaultHost(server)
            .setDefaultPort(port)
            .setLogActivity(true)

        client = vertx.createHttpClient(options)
    }

    protected fun handleHttpError(
        response: HttpClientResponse,
        future: Future<out Any>
    ) {
        val statusCode = response.statusCode()
        logger.error(
            "Received {} while {} {}: {}",
            statusCode,
            response.request().method(),
            response.request().absoluteURI(),
            response.statusMessage()
        )

        if (response.getHeader("content-type").toLowerCase().contains("application/json")) {
            response.bodyHandler {
                val jsonObject = it.toJsonObject()
                val reason = jsonObject.getString("reason") ?: jsonObject.getString("message") ?: "unknown"
                future.fail(HttpException(statusCode, reason))
            }
        } else {
            future.fail(HttpException(statusCode, response.statusMessage()))
        }
    }

}
