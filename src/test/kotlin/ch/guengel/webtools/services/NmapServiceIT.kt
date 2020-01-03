package ch.guengel.webtools.services

import ch.guengel.webtools.servicediscovery.Consul
import ch.guengel.webtools.servicediscovery.ServiceDiscovery
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

// This test requires `portscanner-staging' to be running
@EnabledIfEnvironmentVariable(named = "LOCAL_ENVIRONMENT", matches = "[Yy][Ee][Ss]|[Tt][Rr][Uu][Ee]")
internal class NmapServiceIT {
    private val serviceDiscovery: ServiceDiscovery = Consul("localhost:8500")

    @Test
    fun `regular scan`() {
        val nmapService = NmapService(serviceDiscovery, "2s", 3)
        val result = nmapService.scan("1.1.1.1", "8.8.8.8")

        assertTrue(result.ports.size > 2)
    }

    @Test
    fun `scan with portspec`() {
        val nmapService = NmapService(serviceDiscovery, "2s", 3)
        val result = nmapService.scan("1.1.1.2", "www.google.ch", "80,443")

        assertEquals(2, result.ports.size)
    }

    @Test
    fun `scan blacklisted host`() {
        val nmapService = NmapService(serviceDiscovery, "2s", 3)
        assertThrows(HostBlacklistedException::class.java) {
            nmapService.scan("1.1.1.3", "www.guengel.ch", "80,443")
        }
    }

    @Test
    fun `disable IP blacklist`() {
        val nmapService = NmapService(serviceDiscovery, "2s", 3, false)
        val result = nmapService.scan("1.1.1.3", "www.guengel.ch", "80,443")
        assertEquals(2, result.ports.size)
    }

    @Test
    fun `exceed scan limit`() {
        val nmapService = NmapService(serviceDiscovery, "2s", 0)
        assertThrows(TooManyScansException::class.java) {
            nmapService.scan("1.1.1.4", "8.8.8.8", "80,443")
        }
    }
}