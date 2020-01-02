package ch.guengel.webtools.utils

import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("git-info")

fun printGitVersion() {
    try {
        readGitVersion().forEach { name, value -> logger.info("$name: $value") }
    } catch (e: Exception) {
        // Ignore
    }
}

fun readGitVersion(): Properties {
    try {
        val properties = Properties()
        val gitPropertiesStream = object {}::class.java.getResourceAsStream("/git.properties")
        gitPropertiesStream.use {
            properties.load(it)

        }
        return properties
    } catch (e: Exception) {
        logger.warn("Unable to read git.properties: {}", e.message ?: "null")
        throw e
    }
}