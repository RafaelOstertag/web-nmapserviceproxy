package ch.guengel.webtools.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

// This test requires `portscanner-staging' to be running
@EnabledIfEnvironmentVariable(named = "LOCAL_ENVIRONMENT", matches = "[Yy][Ee][Ss]|[Tt][Rr][Uu][Ee]")
internal class NmapGrpcServiceIT {
    private val nmapGrpcService = NmapGrpcService("localhost", 8081)

    @Test
    fun `scan without port specification`() {
        val result = nmapGrpcService.scan("www.guengel.ch")

        assertEquals("up", result.state)
        assertFalse(result.addresses.isEmpty())
        assertFalse(result.hostnames.isEmpty())
        assertFalse(result.ports.isEmpty())
    }

    @Test
    fun `scan with port specification`() {
        val result = nmapGrpcService.scan("www.guengel.ch", "443,80")

        assertEquals("up", result.state)
        assertFalse(result.addresses.isEmpty())
        assertFalse(result.hostnames.isEmpty())
        assertEquals(2, result.ports.size)
    }
}