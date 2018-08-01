package ch.guengel.webtools

import ch.guengel.webtools.services.LastSeenService
import ch.guengel.webtools.services.ScanService
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.slf4j.LoggerFactory

private val lastSeenService = LastSeenService("gizmo.kruemel.home", 40001)
private val scanService = ScanService("gizmo.kruemel.home", 40000)
private val logger = LoggerFactory.getLogger("application")

private fun errorResponse(message: String?): String {
    return json {
        obj(
            "reason" to (message ?: "reason unknown")
        )
    }.encode()
}

fun main(args: Array<String>) {
    with(HttpServer) {
        addRoute("/v1/scan/:ip") { routingContext ->
            val ipRequestingScan = routingContext.getClientIp()
            val ipToScan = routingContext.pathParam("ip")

            logger.info("Inquiry number of occurrences for {}", ipRequestingScan)

            lastSeenService.isIpWithinTimeConstraint(ipRequestingScan).compose { success ->
                if (!success) {
                    throw HttpException(400, "Too many scans")
                }
                scanService.scanHost(ipToScan)
            }.setHandler {
                when {
                    it.failed() -> {
                        routingContext
                            .response()
                            .contentTypeJson()

                        val exception = it.cause()
                        if (exception is HttpException) {
                            routingContext
                                .response()
                                .setStatusCode(exception.statusCode)
                        } else {
                            routingContext.response().setStatusCode(500)
                        }
                            .end(errorResponse(it.cause().message))
                    }
                    else -> routingContext
                        .response()
                        .contentTypeJson()
                        .setStatusCode(200)
                        .end(it.result())
                }
            }
        }
        start()
    }

}