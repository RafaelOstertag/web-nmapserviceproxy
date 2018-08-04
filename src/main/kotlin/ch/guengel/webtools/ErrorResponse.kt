package ch.guengel.webtools

import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

fun errorResponse(message: String?): String {
    return json {
        obj(
            "reason" to (message ?: "reason unknown")
        )
    }.encode()
}