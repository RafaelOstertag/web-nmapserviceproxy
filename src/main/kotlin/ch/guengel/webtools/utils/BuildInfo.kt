package ch.guengel.webtools.utils

import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.util.*

private val logger = LoggerFactory.getLogger("build-info")

data class MavenInfo(
        val groupId: String,
        val artifactId: String,
        val version: String
) {
    constructor() : this("n/a", "n/a", "n/a")
}

data class BuildInfo(val gitInfo: Map<Any, Any>, val mavenInfo: MavenInfo)

fun readBuildInfo(): BuildInfo {
    val gitInfo = readGitVersion().toMap()
    val mavenInfo = readMavenInfo("/build.info")
    return BuildInfo(gitInfo, mavenInfo)
}

fun readMavenInfo(resourcePath: String): MavenInfo {
    try {
        return object {}.javaClass.getResourceAsStream(resourcePath).use {
            val inputStreamReader = InputStreamReader(it)
            val mavenInfo = Properties()
            mavenInfo.load(inputStreamReader)
            return@use MavenInfo(
                    mavenInfo.getProperty("project.groupId"),
                    mavenInfo.getProperty("project.artifactId"),
                    mavenInfo.getProperty("project.version")
            )
        }
    } catch (e: NullPointerException) {
        logger.warn("Cannot read ${resourcePath}. Return empty maven info")
        return MavenInfo()
    }
}
