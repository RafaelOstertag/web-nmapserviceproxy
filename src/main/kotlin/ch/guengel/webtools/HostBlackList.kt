package ch.guengel.webtools

private val blackList = Regex(
    "(?:.*(?:\\.home|\\.?guengel\\.ch)|(?:127\\.|192\\.168\\.|10\\.).*|)"
)

fun isScanTargetBlacklisted(target: String): Boolean {
    return blackList.matches(target)
}