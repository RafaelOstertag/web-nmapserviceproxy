package ch.guengel.webtools.services

import ch.guengel.webtools.dto.NmapDto
import ch.guengel.webtools.isScanTargetBlacklisted
import ch.guengel.webtools.servicediscovery.Service
import ch.guengel.webtools.servicediscovery.ServiceDiscovery
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NmapService(private val serviceDiscovery: ServiceDiscovery,
                  private val timeConstraint: String,
                  private val maxOccurrences: Int,
                  private val useIpBlacklist: Boolean = true) {
    private val resolveDispatcher = newSingleThreadContext("service-discovery")
    private val lastSeenService = getLastSeenService()
    private val nmapService = getNmapService()

    fun scan(requestingIp: String, host: String, portSpec: String? = null): NmapDto {
        if (useIpBlacklist && isScanTargetBlacklisted(host)) {
            logger.error("Scan target '$host' is blacklisted")
            throw HostBlacklistedException("Not allowed")
        }

        if (!isIpWithinTimeConstraint(requestingIp, lastSeenService)) {
            logger.error("IP '$requestingIp' made to many scans")
            throw TooManyScansException("Too many scans. Try again later.")
        }

        try {
            return nmapService.scan(host, portSpec)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw NmapServiceException("Error", e)
        }
    }

    private fun getLastSeenService(): LastSeenGrpcService = runBlocking(resolveDispatcher) {
        resolveService("lastseen").let {
            LastSeenGrpcService(it.host, it.port)
        }
    }

    private fun isIpWithinTimeConstraint(ip: String, lastSeenService: LastSeenGrpcService): Boolean {
        lastSeenService.updateLastSeen(ip)
        return lastSeenService.getLastSeen(ip, timeConstraint).timesSeen <= maxOccurrences
    }

    private fun getNmapService(): NmapGrpcService = runBlocking(resolveDispatcher) {
        resolveService("nmap").let {
            NmapGrpcService(it.host, it.port)
        }
    }

    private suspend fun resolveService(name: String): Service {
        var retries = 0
        var sleep = 1000L

        while (true) {
            try {
                logger.info("Trying to discover service $name")
                val service = serviceDiscovery.getService(name)
                logger.info("Successfully resolved service $name")
                return service
            } catch (e: Exception) {
                retries++
                logger.warn("Could not resolve $name. Reason ${e.message}. Retry $retries in ${sleep}ms")
                delay(sleep)
                sleep *= 2
            }
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(NmapService::class.java)
    }
}

open class NmapServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TooManyScansException(message: String, cause: Throwable? = null) : NmapServiceException(message, cause)
class HostBlacklistedException(message: String, cause: Throwable? = null) : NmapServiceException(message, cause)
