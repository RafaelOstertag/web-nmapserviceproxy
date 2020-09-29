package ch.guengel.webtools

import ch.guengel.webtools.utils.printGitVersion
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("application")

fun main(args: Array<String>) {
    printGitVersion()
    val environment = commandLineEnvironment(args)
    embeddedServer(Netty, environment).start(wait = true)
}
