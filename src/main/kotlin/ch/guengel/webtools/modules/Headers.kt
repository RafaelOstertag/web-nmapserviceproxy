package ch.guengel.webtools.modules

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*

fun Application.headersModule() {
    log.info("Setup CORS")
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Delete)
        anyHost()
    }

    log.info("Setup default headers")
    install(DefaultHeaders)
}
