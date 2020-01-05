package ch.guengel.webtools.services

import ch.guengel.lastseenservice.LastSeenGrpc
import ch.guengel.lastseenservice.Lastseen
import ch.guengel.webtools.dto.LastSeenDto
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

class LastSeenGrpcService(host: String, port: Int) {
    private val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    private val blockingStub = LastSeenGrpc.newBlockingStub(channel)

    fun getLastSeen(ip: String, since: String? = null): LastSeenDto {
        val lastSeenRequest = Lastseen.LastSeenRequest.newBuilder().run {
            this.ip = ip
            if (since != null) this.since = since
            build()
        }

        try {
            logger.info("Query LastSeenService for IP $ip")
            val response = blockingStub.getLastSeen(lastSeenRequest)
            return toLastSeenDto(response)
        } catch (e: StatusRuntimeException) {
            logger.error("Error querying occurrences of IP '$ip'", e)
            when (e.status.code) {
                Status.NOT_FOUND.code -> throw LastSeenIpNotFoundException(ip)
                else -> throw LastSeenException("Error querying LastSeen service", e)
            }
        }
    }

    fun updateLastSeen(ip: String): LastSeenDto {
        val updateLastSeenRequest = Lastseen.UpdateLastSeenRequest.newBuilder().run {
            setIp(ip)
            build()
        }
        try {
            logger.info("Update LastSeenService IP $ip")
            val response = blockingStub.updateLastSeen(updateLastSeenRequest)
            return toLastSeenDto(response)
        } catch (e: StatusRuntimeException) {
            val errorMessage = "Error updating IP '$ip' in LastSeenService"
            logger.error(errorMessage, e)
            throw LastSeenException(errorMessage, e)
        }
    }

    private fun toLastSeenDto(response: Lastseen.LastSeenReply): LastSeenDto =
            LastSeenDto(response.ip,
                    response.timesSeen,
                    OffsetDateTime.parse(response.from),
                    OffsetDateTime.parse(response.to))

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(LastSeenGrpcService::class.java)
    }
}

open class LastSeenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class LastSeenIpNotFoundException(ip: String, cause: Throwable? = null) : LastSeenException("IP '$ip' not found", cause)
