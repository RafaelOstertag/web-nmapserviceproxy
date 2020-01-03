package ch.guengel.webtools.services

import ch.guengel.webtools.dto.NmapDto
import ch.guengel.webtools.isScanTargetBlacklisted
import ch.guengel.webtools.servicediscovery.Service
import ch.guengel.webtools.servicediscovery.ServiceDiscovery
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NmapService(private val serviceDiscovery: ServiceDiscovery,
                  private val timeConstraint: String,
                  private val maxOccurrences: Int,
                  private val useIpBlacklist: Boolean = true) {
    fun scan(requestingIp: String, host: String, portSpec: String? = null): NmapDto {
        if (useIpBlacklist && isScanTargetBlacklisted(host)) {
            logger.error("Scan target '$host' is blacklisted")
            throw HostBlacklistedException("Not allowed")
        }
        val lastSeenService = getLastSeenService()
        if (!isIpWithinTimeConstraint(requestingIp, lastSeenService)) {
            logger.error("IP '$requestingIp' made to many scans")
            throw TooManyScansException("Too many scans. Try again later.")
        }

        val nmapService = getNmapService()
        try {
            return nmapService.scan(host, portSpec)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw NmapServiceException("Error", e)
        }
    }

    private fun getLastSeenService(): LastSeenGrpcService = resolveService("lastseen").let {
        LastSeenGrpcService(it.host, it.port)
    }

    private fun isIpWithinTimeConstraint(ip: String, lastSeenService: LastSeenGrpcService): Boolean {
        lastSeenService.updateLastSeen(ip)
        return lastSeenService.getLastSeen(ip, timeConstraint).timesSeen <= maxOccurrences
    }

    private fun getNmapService(): NmapGrpcService = resolveService("nmap").let {
        NmapGrpcService(it.host, it.port)
    }

    private fun resolveService(name: String): Service {
        try {
            logger.info("Discover service $name")
            return serviceDiscovery.getService(name)
        } catch (e: Exception) {
            val errorMessage = "Error discovering '$name'"
            logger.error(errorMessage, e)
            throw NmapServiceException(errorMessage, e)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(NmapService::class.java)
    }
}

open class NmapServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TooManyScansException(message: String, cause: Throwable? = null) : NmapServiceException(message, cause)
class HostBlacklistedException(message: String, cause: Throwable? = null) : NmapServiceException(message, cause)
