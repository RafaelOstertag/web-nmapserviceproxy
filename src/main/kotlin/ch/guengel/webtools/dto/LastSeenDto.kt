package ch.guengel.webtools.dto

import java.time.OffsetDateTime

data class LastSeenDto(val ip: String, val timesSeen: Int, val from: OffsetDateTime, val to: OffsetDateTime)