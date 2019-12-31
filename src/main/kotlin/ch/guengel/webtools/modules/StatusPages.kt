package ch.guengel.webtools.modules

import ch.guengel.webtools.services.HostBlacklistedException
import ch.guengel.webtools.services.NmapServiceException
import ch.guengel.webtools.services.TooManyScansException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import io.ktor.application.*
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext

private const val UNKNOWN_REASON = "unknown reason"

data class ErrorResponse(val statusCode: Int, val reason: String)

fun Application.statusPages() {
    log.info("Setup Status Pages")
    install(StatusPages) {
        exception<IllegalArgumentException> { cause ->
            badRequest(cause)
        }

        exception<JsonParseException> { cause ->
            badRequest(cause)
        }

        exception<JsonMappingException> { cause ->
            badRequest(cause)
        }

        exception<TooManyScansException> { cause ->
            val message = ErrorResponse(HttpStatusCode.BadRequest.value, cause.message ?: UNKNOWN_REASON)
            call.respond(HttpStatusCode.BadRequest, message)
        }

        exception<HostBlacklistedException> { cause ->
            val message = ErrorResponse(HttpStatusCode.Forbidden.value, cause.message ?: UNKNOWN_REASON)
            call.respond(HttpStatusCode.Forbidden, message)
        }

        exception<IllegalArgumentException> { cause ->
            val message = ErrorResponse(HttpStatusCode.BadRequest.value, cause.message ?: UNKNOWN_REASON)
            call.respond(HttpStatusCode.BadRequest, message)
        }

        exception<NmapServiceException> { cause ->
            val message = ErrorResponse(HttpStatusCode.BadRequest.value, cause.message ?: UNKNOWN_REASON)
            call.respond(HttpStatusCode.BadRequest, message)
        }

        exception<Throwable> { cause ->
            val message = ErrorResponse(HttpStatusCode.InternalServerError.value, cause.message ?: UNKNOWN_REASON)
            call.respond(HttpStatusCode.InternalServerError, message)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.badRequest(
        cause: Throwable
) {
    val message = ErrorResponse(HttpStatusCode.BadRequest.value, cause.message ?: UNKNOWN_REASON)
    call.respond(HttpStatusCode.BadRequest, message)
}
