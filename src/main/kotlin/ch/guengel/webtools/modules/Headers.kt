package ch.guengel.webtools.modules

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CORS
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpMethod

fun Application.headersModule() {
    log.info("Setup CORS")
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Delete)
        anyHost()
        allowCredentials = true
    }

    log.info("Setup default headers")
    install(DefaultHeaders)
}