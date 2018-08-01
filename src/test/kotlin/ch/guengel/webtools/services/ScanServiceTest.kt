package ch.guengel.webtools.services

import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class ScanServiceTest {

    @Test
    fun `scan host without port spec`(testContext: TestContext) {
        val async = testContext.async()

        val scanService = ScanService(server = "gizmo.kruemel.home", port = 40000)
        scanService.scanHost("gizmo.kruemel.home").setHandler {
            when {
                it.failed() -> testContext.fail("Test failed with " + it.cause())
                else -> {
                    testContext.assertTrue(it.succeeded())
                    testContext.assertFalse(it.result().isEmpty())
                }
            }
            async.complete()
        }
        async.awaitSuccess()
    }

    @Test
    fun `scan host with port spec`(testContext: TestContext) {
        val async = testContext.async()

        val scanService = ScanService(server = "gizmo.kruemel.home", port = 40000)
        scanService.scanHost("gizmo.kruemel.home", "22").setHandler {
            when {
                it.failed() -> testContext.fail("Test failed with " + it.cause())
                else -> {
                    testContext.assertTrue(it.succeeded())
                    testContext.assertFalse(it.result().isEmpty())
                }
            }
            async.complete()
        }
        async.awaitSuccess()
    }

    @Test
    fun `scan non existing host`(testContext: TestContext) {
        val async = testContext.async()

        val scanService = ScanService(server = "gizmo.kruemel.home", port = 40000)
        scanService.scanHost("not-exist.kruemel.home").setHandler {
            testContext.assertFalse(it.failed())
            testContext.assertTrue(it.succeeded())
            async.complete()
        }
        async.awaitSuccess()
    }

    @Test
    fun `scan host with illegal port spec`(testContext: TestContext) {
        val async = testContext.async()

        val scanService = ScanService(server = "gizmo.kruemel.home", port = 40000)
        scanService.scanHost("gizmo.kruemel.home", "12--12").setHandler {
            testContext.assertTrue(it.failed())
            testContext.assertFalse(it.succeeded())
            async.complete()
        }
        async.awaitSuccess()
    }
}