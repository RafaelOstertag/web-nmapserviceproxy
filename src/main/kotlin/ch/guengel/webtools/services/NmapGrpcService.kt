package ch.guengel.webtools.services

import ch.guengel.nmapservice.NmapGrpc
import ch.guengel.nmapservice.NmapOuterClass
import ch.guengel.webtools.dto.NmapDto
import ch.guengel.webtools.dto.NmapPort
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NmapGrpcService(host: String, port: Int) {
    private val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    private val blockingStub: NmapGrpc.NmapBlockingStub = NmapGrpc.newBlockingStub(channel)

    fun scan(host: String, portSpec: String? = null): NmapDto {
        val nmapScanRequest = NmapOuterClass.ScanRequest.newBuilder().run {
            this.host = host
            if (portSpec != null) this.portSpec = portSpec
            build()
        }

        try {
            logger.info("Query NmapService for host '$host' with '$portSpec' port specification")
            val response = blockingStub.scan(nmapScanRequest)
            return toNmapDto(response)
        } catch (e: StatusRuntimeException) {
            val errorMessage = "Error querying NmapService for host '$host' with '$portSpec' port specification"
            logger.error(errorMessage, e)
            when (e.status.code) {
                Status.INVALID_ARGUMENT.code -> throw IllegalArgumentException(e.message?.removePrefix("INVALID_ARGUMENT: ")
                        ?: "no reason", e)
                else -> throw NmapException(errorMessage, e)
            }
        }
    }

    private fun toNmapDto(response: NmapOuterClass.ScanReply): NmapDto =
            response
                    .portsList
                    .map { NmapPort(it.number, it.state, it.name) }
                    .let {
                        NmapDto(response.state, response.addressesList, response.hostnamesList, it)
                    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(NmapGrpcService::class.java)
    }
}

class NmapException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)