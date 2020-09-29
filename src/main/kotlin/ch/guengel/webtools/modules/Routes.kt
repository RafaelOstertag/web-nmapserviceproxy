package ch.guengel.webtools.modules

import ch.guengel.webtools.dto.NmapDto
import ch.guengel.webtools.services.LastSeenGrpcService
import ch.guengel.webtools.services.NmapGrpcService
import ch.guengel.webtools.services.NmapService
import ch.guengel.webtools.utils.readBuildInfo
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.config.ApplicationConfig
import io.ktor.features.origin
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.Logger

fun Application.routes() {
    log.info("Setting up routes")

    val config = environment.config
    val nmapService = createNmapService(log, config)

    routing {
        infoRoute()
        route("v1") {
            scanRoute(log, nmapService)
        }
    }
    log.info("Routes set up")
}

private fun Route.scanRoute(log: Logger, nmapService: NmapService) = createRouteFromPath("scan/{scanTarget}").apply {

    get {
        val scanResult = scan(nmapService)
        call.respond(HttpStatusCode.OK, scanResult)
    }
}

private fun PipelineContext<Unit, ApplicationCall>.scan(nmapService: NmapService): NmapDto {
    val scanTarget = call.parameters["scanTarget"] ?: throw IllegalArgumentException("Missing scan target")
    val ports = call.request.queryParameters["ports"]
    val requestingHost = call.request.origin.remoteHost
    val scanResult = nmapService.scan(requestingHost, scanTarget, ports)
    return scanResult
}

private fun createNmapService(log: Logger, config: ApplicationConfig): NmapService {
    val lastSeenServiceHost = config.property("services.lastseenservice.host").getString()
    log.info("Use last seen service host '$lastSeenServiceHost'")

    val nmapServiceHost = config.property("services.nmapservice.host").getString()
    log.info("Use nmap service host '$nmapServiceHost'")

    val timeConstraint = config.property("constraints.timeConstraint").getString()
    val maxOccurences = config.property("constraints.maxOccurences").getString().toInt()
    log.info("Requests per caller $maxOccurences/$timeConstraint")

    val useIpBlacklist = config.property("constraints.useIPBlacklist").getString().toBoolean()
    log.info("Use IP Blacklist: ${useIpBlacklist}")

    val lastSeenServiceHostAndPort = lastSeenServiceHost.toHostAndPort()
    val nmapServiceHostAndPort = nmapServiceHost.toHostAndPort();
    return NmapService(
            LastSeenGrpcService(lastSeenServiceHostAndPort.host,lastSeenServiceHostAndPort.port),
            NmapGrpcService(nmapServiceHostAndPort.host,nmapServiceHostAndPort.port),
            timeConstraint, maxOccurences, useIpBlacklist)
}

private fun String.toHostAndPort() : HostAndPort {
    val hostAndPortComponents = this.split(":")
    if (hostAndPortComponents.isEmpty()) {
        throw IllegalArgumentException("'$this' is not a valid host")
    }

    if (hostAndPortComponents.size < 2) {
        return HostAndPort(hostAndPortComponents[0], 8080)
    }
    return HostAndPort(hostAndPortComponents[0], hostAndPortComponents[1].toInt())
}

private fun Route.infoRoute() = createRouteFromPath("/info").apply {
    val buildInfo = readBuildInfo()
    get {
        call.respond(buildInfo)
    }
}

private data class HostAndPort(val host: String, val port: Int)