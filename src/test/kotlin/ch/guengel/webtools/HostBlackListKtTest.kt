package ch.guengel.webtools

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HostBlackListKtTest {

    @Test
    fun `is guengel_dot_ch blacklisted`() {
        assertTrue(isScanTargetBlacklisted("guengel.ch"))
        assertTrue(isScanTargetBlacklisted("whatever.guengel.ch"))
    }

    @Test
    fun `is kruemel_dot_home blacklisted`() {
        assertTrue(isScanTargetBlacklisted(".kruemel.home"))
        assertTrue(isScanTargetBlacklisted("gizmo.kruemel.home"))
    }

    @Test
    fun `is 192 168 blacklisted`() {
        assertTrue(isScanTargetBlacklisted("192.168.100.1"))
        assertTrue(isScanTargetBlacklisted("192.168.232.123"))
    }

    @Test
    fun `is 10 blacklisted`() {
        assertTrue(isScanTargetBlacklisted("10.0.0.0"))
        assertTrue(isScanTargetBlacklisted("10.123.234.2"))
    }

    @Test
    fun `is 127 blacklisted`() {
        assertTrue(isScanTargetBlacklisted("127.0.0.1"))
        assertTrue(isScanTargetBlacklisted("127.255.255.255"))
    }
}