package ch.guengel.webtools

fun main(args: Array<String>) {
    with(HttpServer) {
        addRoute("/test") { routingContext ->
            routingContext.response().end(routingContext.getClientIp())
        }
        start()
    }

}