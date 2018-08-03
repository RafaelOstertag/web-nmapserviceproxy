package ch.guengel.webtools.servicediscovery

import io.vertx.core.Future

interface ServiceDiscovery {
    fun getService(name: String): Future<Service>
}

