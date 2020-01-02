package ch.guengel.webtools

import ch.guengel.webtools.utils.printGitVersion
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("application")

fun main(args: Array<String>) {
    printGitVersion()
    val environment = commandLineEnvironment(args)
    embeddedServer(Netty, environment).start(wait = true)

//    with(HttpServer) {
//        addRoute("/v1/scan/:scanTarget") { routingContext ->
//            val ipRequestingScan = routingContext.getClientIp()
//            val scanTarget = routingContext.pathParam("scanTarget")
//
//            if (isScanTargetBlacklisted(scanTarget)) {
//                logger.error("Scan target $scanTarget is black listed")
//                routingContext.fail(HttpException(403, "Host must not be scanned"))
//                return@addRoute
//            }
//
//            logger.info("Inquiry number of occurrences for {}", ipRequestingScan)
//
//            serviceDiscovery
//                .getService("lastseen")
//                .compose {
//                    val result = Future.future<LastSeenService>()
//                    result.complete(LastSeenService(it.host, it.port))
//                    result
//                }.compose {
//                    it.isIpWithinTimeConstraint(ipRequestingScan).compose { success ->
//                        if (!success) {
//                            throw HttpException(400, "Too many scans. Try again later.")
//                        }
//                        serviceDiscovery.getService("nmap")
//                    }
//                }.compose {
//                    val result = Future.future<ScanService>()
//                    result.complete(ScanService(it.host, it.port))
//                    result
//                }.compose {
//                    it.scanHost(scanTarget, getPorts(routingContext))
//                }.setHandler {
//                    when {
//                        it.failed() -> routingContext.fail(it.cause())
//                        else -> routingContext
//                            .response()
//                            .contentTypeJson()
//                            .setStatusCode(200)
//                            .end(it.result())
//                    }
//                }
//        }
//        start(getHttpPort())
//    }
}
