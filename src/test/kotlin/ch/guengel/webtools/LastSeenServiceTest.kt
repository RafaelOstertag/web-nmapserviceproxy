package ch.guengel.webtools

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

data class Occurrences(val ip: String, val from: String, val to: String, val timesSeen: Int)

val port = 50001

@RunWith(VertxUnitRunner::class)
class LastSeenServiceTest {

    var server: HttpServer? = null

    @Before
    fun `set up`(testContext: TestContext) {
        val async = testContext.async()
        val vertx = Vertx.vertx()
        fun makeOccurrences(occurrences: Int): String? {
            return json {
                obj(
                    "ip" to "127.0.0.1",
                    "timesSeen" to occurrences
                )
            }.encode()
        }

        fun router(): Router {
            val router = Router.router(vertx)

            router.route(HttpMethod.PUT, "/v1/lastseen/127.0.0.1").handler {
                val response = it.response()
                response.putHeader("content-type", "application/json")
                response.end()
            }
            router.route(HttpMethod.PUT, "/v1/lastseen/127.0.0.2").handler {
                it.response().setStatusCode(400).end()
            }
            router.route(HttpMethod.PUT, "/v1/lastseen/127.0.0.3").handler {
                it.response().setStatusCode(404).end()
            }
            // When `?since=...` is present, return four occurrences. If absent return status code 400.
            router.route(HttpMethod.GET, "/v1/lastseen/127.0.0.1").handler {
                val queryParam = it.queryParam("since")
                if (queryParam.size == 0) {
                    it.response().setStatusCode(400).end()
                } else {
                    it.response()
                        .putHeader("content-type", "application/json")
                        .end(makeOccurrences(4))
                }

            }
            // When `?since=...` is present, return ten occurrences. If absent return status code 400.
            router.route(HttpMethod.GET, "/v1/lastseen/127.0.0.2").handler {
                val queryParam = it.queryParam("since")
                if (queryParam.size == 0) {
                    it.response().setStatusCode(400).end()
                } else {
                    it.response()
                        .putHeader("content-type", "application/json")
                        .end(makeOccurrences(10))
                }
            }

            return router
        }

        server = vertx
            .createHttpServer()
            .requestHandler { router().accept(it) }
            .listen(
                port
            ) {
                async.complete()
            }
    }

    @After
    fun `tear down`(testContext: TestContext) {
        val async = testContext.async()
        server?.close {
            async.complete()
        }
    }

    @Test
    fun `submit ip happy path`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        val updateLastSeen = lastSeenService.updateLastSeen("127.0.0.1")
        updateLastSeen.setHandler { event ->
            testContext.assertTrue(event.succeeded())
            testContext.assertFalse(event.failed())
            async.complete()
        }
        async.awaitSuccess()
    }

    @Test
    fun `submit ip endpoint not found`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        val updateLastSeen = lastSeenService.updateLastSeen("127.0.0.3")
        updateLastSeen.setHandler { event ->
            testContext.assertFalse(event.succeeded())
            testContext.assertTrue(event.failed())
            async.complete()
        }
        async.awaitSuccess()
    }

    @Test
    fun `submit ip endpoint error`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        val updateLastSeen = lastSeenService.updateLastSeen("127.0.0.2")
        updateLastSeen.setHandler { event ->
            testContext.assertFalse(event.succeeded())
            testContext.assertTrue(event.failed())
            async.complete()
        }
        async.awaitSuccess()
    }

    @Test
    fun `test constraint ok`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        lastSeenService.ipFulfilsConstraints("127.0.0.1", "5m", 5)
            .setHandler {
                when {
                    it.failed() -> testContext.fail("Future failed " + it.cause())
                    it.succeeded() -> testContext.assertTrue(it.result())
                }
                async.complete()
            }

        async.awaitSuccess()
    }

    @Test
    fun `test constraint not ok`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        lastSeenService.ipFulfilsConstraints("127.0.0.2", "5m", 1)
            .setHandler {
                when {
                    it.failed() -> testContext.fail("Future failed " + it.cause())
                    it.succeeded() -> testContext.assertFalse(it.result())
                }
                async.complete()
            }

        async.awaitSuccess()
    }

    @Test
    fun `test constraint on non existing ip`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        lastSeenService.ipFulfilsConstraints("127.0.0.3", "5m", 1)
            .setHandler {
                testContext.assertFalse(it.succeeded())
                testContext.assertTrue(it.failed())
                async.complete()
            }

        async.awaitSuccess()
    }

    @Test
    fun `test is within time constraint`(testContext: TestContext) {
        val async = testContext.async()
        val lastSeenService = LastSeenService(server = "localhost", port = port)
        lastSeenService.isIpWithinTimeConstraint("127.0.0.1")
            .setHandler {
                when {
                    it.failed() -> testContext.fail("Future failed " + it.cause())
                    it.succeeded() -> testContext.assertTrue(it.result())
                }
                async.complete()
            }
        async.awaitSuccess()
    }
}