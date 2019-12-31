package ch.guengel.webtools.servicediscovery


interface ServiceDiscovery {
    fun getService(name: String): Service
}

data class Service(val host: String, val port: Int)