package ch.guengel.webtools.services

import ch.guengel.webtools.dto.NmapDto
import ch.guengel.webtools.isScanTargetBlacklisted
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NmapService(private val lastSeenService: LastSeenGrpcService,
                  private val nmapService: NmapGrpcService,
        private val timeConstraint: String,
                  private val maxOccurrences: Int,
                  private val useIpBlacklist: Boolean = true) {

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

    private fun isIpWithinTimeConstraint(ip: String, lastSeenService: LastSeenGrpcService): Boolean {
        lastSeenService.updateLastSeen(ip)
        return lastSeenService.getLastSeen(ip, timeConstraint).timesSeen <= maxOccurrences
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(NmapService::class.java)
    }
}

open class NmapServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TooManyScansException(message: String, cause: Throwable? = null) : NmapServiceException(message, cause)
class HostBlacklistedException(message: String, cause: Throwable? = null) : NmapServiceException(message, cause)
