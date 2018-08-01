package ch.guengel.webtools.services

import io.vertx.core.Future
import io.vertx.core.http.HttpClientResponse

class ScanService(server: String, port: Int) : BaseService(server, port) {

    fun scanHost(host: String, portSpec: String = ""): Future<String> {
        var basePath = "/v1/scan/$host"
        if (portSpec.isNotEmpty()) {
            basePath += "/$portSpec"
        }

        val result: Future<String> = Future.future()

        client.getNow(basePath) { httpClientResponse ->
            logger.info("Scan {} completed", httpClientResponse.request().absoluteURI())
            when {
                httpClientResponse.statusCode() != 200 -> handleHttpError(httpClientResponse, result)
                else -> getScanResult(httpClientResponse, result)
            }
        }
        return result
    }

    private fun getScanResult(
        response: HttpClientResponse,
        result: Future<String>
    ): Future<String> {
        response.bodyHandler {
            result.complete(it.toString())
        }
        return result
    }


}