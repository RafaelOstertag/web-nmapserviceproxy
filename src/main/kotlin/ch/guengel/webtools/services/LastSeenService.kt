package ch.guengel.webtools.services

import ch.guengel.webtools.UnexpectedJsonException
import io.vertx.core.Future
import io.vertx.core.http.HttpClientResponse

class LastSeenService(server: String, port: Int) : BaseService(server, port) {

    internal fun updateLastSeen(ip: String): Future<Void> {
        logger.info("Query time constraint for {}", ip)

        val submitted: Future<Void> = Future.future()
        client.put("/v1/lastseen/$ip") { response ->
            logger.info("Submitted {} to {}", ip, response.request().absoluteURI())
            when {
                response.statusCode() != 200 -> {
                    handleHttpError(response, submitted)
                }
                else -> submitted.complete()
            }
        }.end()
        return submitted
    }

    internal fun ipFulfilsConstraints(ip: String, timeConstraint: String, maxOccurrences: Int): Future<Boolean> {
        val fulfilled: Future<Boolean> = Future.future()

        client.getNow("/v1/lastseen/$ip?since=$timeConstraint") { response ->
            when {
                response.statusCode() != 200 -> handleHttpError(response, fulfilled)
                else -> {
                    getNumberOfOccurrencesFromResponse(response).setHandler {
                        when {
                            it.succeeded() -> fulfilled.complete(it.result() <= maxOccurrences)
                            else -> fulfilled.fail(it.cause())
                        }
                    }
                }
            }
        }

        return fulfilled
    }

    private fun getNumberOfOccurrencesFromResponse(response: HttpClientResponse): Future<Int> {
        val numberOfOccurrences: Future<Int> = Future.future()
        response.bodyHandler {
            try {
                val bodyAsJson = it.toJsonObject()
                val timesSeen = bodyAsJson.getInteger("timesSeen")
                val ip = bodyAsJson.getString("ip")
                when {
                    timesSeen == null || ip == null -> numberOfOccurrences.fail(UnexpectedJsonException())
                    else -> {
                        logger.info("{} seen {} time(s) so far", ip, timesSeen)
                        numberOfOccurrences.complete(timesSeen)
                    }
                }
            } catch (e: ClassCastException) {
                numberOfOccurrences.fail(e)
            }
        }
        return numberOfOccurrences
    }

    fun isIpWithinTimeConstraint(ip: String): Future<Boolean> {
        return updateLastSeen(ip).compose {
            ipFulfilsConstraints(ip, "1m", 3)
        }
    }
}