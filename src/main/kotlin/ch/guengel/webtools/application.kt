package ch.guengel.webtools

import ch.guengel.webtools.services.LastSeenService
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.slf4j.LoggerFactory

private val lastSeenService = LastSeenService("gizmo.kruemel.home", 40001)
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
            lastSeenService.isIpWithinTimeConstraint(ipRequestingScan).setHandler {
                when {
                    it.failed() -> routingContext
                        .response()
                        .contentTypeJson()
                        .setStatusCode(500)
                        .setStatusMessage("Internal server error")
                        .end(errorResponse(it.cause().message))
                    it.result() == true -> routingContext
                        .response()
                        .setStatusCode(200)
                        .end("Would now send scan request for $ipToScan")
                    it.result() == false -> routingContext
                        .response()
                        .contentTypeJson()
                        .setStatusCode(400)
                        .end(errorResponse("Too many scans"))
                }
            }
        }
        start()
    }

}