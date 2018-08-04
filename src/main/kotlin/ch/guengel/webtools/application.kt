package ch.guengel.webtools

import ch.guengel.webtools.servicediscovery.Consul
import ch.guengel.webtools.servicediscovery.ServiceDiscovery
import ch.guengel.webtools.services.LastSeenService
import ch.guengel.webtools.services.ScanService
import io.vertx.core.Future
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

private val serviceDiscovery: ServiceDiscovery = Consul("gizmo.kruemel.home", 8500)
private val logger = LoggerFactory.getLogger("application")

private fun getHttpPort(): Int {
    return System.getenv("HTTP_PORT")?.toInt() ?: 8080
}

fun main(args: Array<String>) {
    printGitVersion()
    with(HttpServer) {
        addRoute("/v1/scan/:scanTarget") { routingContext ->
            val ipRequestingScan = routingContext.getClientIp()
            val scanTarget = routingContext.pathParam("scanTarget")

            if (isScanTargetBlacklisted(scanTarget)) {
                logger.error("Scan target $scanTarget is black listed")
                routingContext.fail(HttpException(403, "Host must not be scanned"))
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
                        it.failed() -> routingContext.fail(it.cause())
                        else -> routingContext
                            .response()
                            .contentTypeJson()
                            .setStatusCode(200)
                            .end(it.result())
                    }
                }
        }
        start(getHttpPort())
    }

}

private fun getPorts(routingContext: RoutingContext): String {
    val portsParameter = routingContext.queryParam("ports")
    return if (portsParameter.size != 0) portsParameter[0] else ""
}