package ch.guengel.webtools

class HttpClientException(statusCode: Int, statusMessage: String?) : RuntimeException(statusMessage)

class UnexpectedJsonException : RuntimeException("Unexpected JSON")