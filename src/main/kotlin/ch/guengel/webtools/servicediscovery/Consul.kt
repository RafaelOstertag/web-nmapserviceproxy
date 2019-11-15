package ch.guengel.webtools.servicediscovery

import ch.guengel.webtools.Runtime
import io.vertx.core.Future
import io.vertx.ext.consul.ConsulClient
import io.vertx.kotlin.ext.consul.ConsulClientOptions
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class Consul(private val consulHost: String, private val consulPort: Int) : ServiceDiscovery {
    private data class CacheEntity<T>(val item: T, val added: Date = Date())

    private val maxCacheEntryAge = 5 * 60 * 1_000L

    val logger = LoggerFactory.getLogger("Service Discovery")
    val consulClient: ConsulClient
    private val serviceCache = ConcurrentHashMap<String, CacheEntity<Service>>()

    init {
        val options = ConsulClientOptions()
        options.host = consulHost
        options.port = consulPort
        consulClient = ConsulClient.create(Runtime.vertx, options)
    }

    private fun ageInMillisOfCacheEntity(cacheEntity: CacheEntity<out Any>): Long {
        return Date().time - cacheEntity.added.time
    }

    private fun getFromCache(serviceName: String): Service? {
        val cacheEntry = serviceCache[serviceName] ?: return null
        val age = ageInMillisOfCacheEntity(cacheEntry)
        return if (age > maxCacheEntryAge) null else cacheEntry.item
    }

    private fun addToCache(serviceName: String, service: Service) {
        serviceCache.put(serviceName, CacheEntity(service))
    }

    override fun getService(name: String): Future<Service> {
        val serviceResult: Future<Service> = Future.future<Service>()

        val fromCache = getFromCache(name)
        if (fromCache != null) {
            logger.info("Serve service {} from cache", name)
            serviceResult.complete(fromCache)
            return serviceResult
        }

        logger.info("Query consul '{}:{}'", consulHost, consulPort)
        consulClient.healthServiceNodes(name, true) {
            when {
                it.failed() -> {
                    logger.error("Error query consul for service {}: {}", name, it.cause().message)
                    serviceResult.fail(it.cause())
                }
                it.succeeded() -> {
                    val result = it.result().list
                    if (result.size == 0) {
                        logger.error("Unable to find service {} on {}:{}", name, consulHost, consulPort)
                        serviceResult.fail(ServiceNotFoundException(name))
                    } else {
                        with(result[0]) {
                            val address = this.service.address
                            val port = this.service.port
                            logger.info("Service {} is reachable under {}:{}", name, address, port)
                            val service = Service(host = this.service.address, port = this.service.port)
                            addToCache(name, service)
                            serviceResult.complete(service)
                        }
                    }
                }
            }
        }

        return serviceResult
    }
}