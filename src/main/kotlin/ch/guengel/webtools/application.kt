package ch.guengel.webtools

import ch.guengel.webtools.servicediscovery.Consul
import ch.guengel.webtools.servicediscovery.ServiceDiscovery
import ch.guengel.webtools.services.LastSeenService
import ch.guengel.webtools.services.ScanService
import io.vertx.core.Future
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.slf4j.LoggerFactory

private val serviceDiscovery: ServiceDiscovery = Consul("gizmo.kruemel.home", 8500)
private val logger = LoggerFactory.getLogger("application")

private fun errorResponse(message: String?): String {
    return json {
        obj(
            "reason" to (message ?: "reason unknown")
        )
    }.encode()
}

fun main(args: Array<String>) {
    printGitVersion()
    with(HttpServer) {
        addRoute("/v1/scan/:scanTarget") { routingContext ->
            val ipRequestingScan = routingContext.getClientIp()
            val scanTarget = routingContext.pathParam("scanTarget")

            if (isScanTargetBlacklisted(scanTarget)) {
                logger.error("Scan target $scanTarget is black listed")
                routingContext
                    .response()
                    .contentTypeJson()
                    .setStatusCode(403)
                    .end(errorResponse("Host must not be scanned"))
                return@addRoute
            }

            logger.info("Inquiry number of occurrences for {}", ipRequestingScan)

            serviceDiscovery
                .getService("lastseen")
                .compose {
                    val result = Future.future<LastSeenService>()
                    result.complete(LastSeenService(it.host, it.port))
                    result
                }.compose {
                    it.isIpWithinTimeConstraint(ipRequestingScan).compose { success ->
                        if (!success) {
                            throw HttpException(400, "Too many scans")
                        }
                        serviceDiscovery.getService("nmap")
                    }
                }.compose {
                    val result = Future.future<ScanService>()
                    result.complete(ScanService(it.host, it.port))
                    result
                }.compose {
                    it.scanHost(scanTarget, getPorts(routingContext))
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

private fun getPorts(routingContext: RoutingContext): String {
    val portsParameter = routingContext.queryParam("ports")
    return if (portsParameter.size != 0) portsParameter[0] else ""
}