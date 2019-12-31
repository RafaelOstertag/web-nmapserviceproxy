package ch.guengel.webtools.modules

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CallLogging
import org.slf4j.event.Level

fun Application.callLogging() {
    log.info("Setup Call Logging Module")
    install(CallLogging) {
        level = Level.INFO
    }
}