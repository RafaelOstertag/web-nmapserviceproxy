package ch.guengel.webtools.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

// This test requires `portscanner-staging' to be running
@EnabledIfEnvironmentVariable(named = "LOCAL_ENVIRONMENT", matches = "[Yy][Ee][Ss]|[Tt][Rr][Uu][Ee]")
internal class LastSeenGrpcServiceIT {
    private val lastSeenGrpcService = LastSeenGrpcService("localhost", 8080)

    @Test
    fun `update IP`() {
        val updateLastSeen = lastSeenGrpcService.updateLastSeen("1.1.1.1")
        assertEquals("1.1.1.1", updateLastSeen.ip)

        val lastSeen = lastSeenGrpcService.getLastSeen("1.1.1.1")
        assertEquals("1.1.1.1", lastSeen.ip)

        val lastSeenWithSince = lastSeenGrpcService.getLastSeen("1.1.1.1", "30s")
        assertEquals("1.1.1.1", lastSeenWithSince.ip)
    }

    @Test
    fun `query non-existing IP`() {
        assertThrows(LastSeenIpNotFoundException::class.java) {
            lastSeenGrpcService.getLastSeen("should-not-exist")
        }
    }
}