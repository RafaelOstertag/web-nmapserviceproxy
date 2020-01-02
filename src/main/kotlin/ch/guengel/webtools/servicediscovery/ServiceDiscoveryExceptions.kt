package ch.guengel.webtools.servicediscovery

class ServiceNotFoundException(name: String) : RuntimeException("Service '$name' not found")
class ServiceLookupException(name: String,
                             cause: Throwable? = null) : RuntimeException("Error while looking up service '$name'",
        cause)