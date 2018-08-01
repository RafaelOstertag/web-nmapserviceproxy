package ch.guengel.webtools

class HttpException(val statusCode: Int, statusMessage: String?) : RuntimeException(statusMessage)

class UnexpectedJsonException : RuntimeException("Unexpected JSON")