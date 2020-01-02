package ch.guengel.webtools.modules

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ForwardedHeaderSupport
import io.ktor.features.XForwardedHeaderSupport

fun Application.forwardedHeaderSupport() {
    log.info("Setup Forwarded Header Support Module")
    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)
}