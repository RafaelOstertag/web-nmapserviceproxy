package ch.guengel.webtools.servicediscovery

import com.google.common.net.HostAndPort
import com.orbitz.consul.model.health.ServiceHealth
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Consul(private val consulHost: String) : ServiceDiscovery {
    private data class CacheEntity<T>(val item: T, val added: Date = Date())

    private val maxCacheEntryAge = 5 * 60 * 1_000L

    val consulClient = com.orbitz.consul.Consul.builder().withHostAndPort(HostAndPort.fromString(consulHost)).build()
    private val serviceCache = ConcurrentHashMap<String, CacheEntity<Service>>()

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

    override fun getService(name: String): Service {
        val fromCache = getFromCache(name)
        if (fromCache != null) {
            logger.info("Serve service {} from cache", name)
            return fromCache
        }

        logger.info("Query consul '{}'", consulHost)

        val healthyServiceInstances: List<ServiceHealth> = lookupService(name)
        throwIfServiceNotFound(healthyServiceInstances, name)

        val service = extractServiceFromConsulResponse(healthyServiceInstances)
        addToCache(name, service)

        logger.info("Successfully queried consul '{}'", consulHost)
        return service
    }

    private fun lookupService(name: String): List<ServiceHealth> {
        val healthClient = consulClient.healthClient()
        val healthyServiceInstances: List<ServiceHealth>
        try {
            healthyServiceInstances = healthClient.getHealthyServiceInstances(name).response
        } catch (e: Exception) {
            logger.error("Error querying consul for '$name'", e)
            throw ServiceLookupException(name, e)
        }
        return healthyServiceInstances
    }

    private fun throwIfServiceNotFound(healthyServiceInstances: List<ServiceHealth>,
                                       name: String) {
        if (healthyServiceInstances.isEmpty()) {
            logger.error("No healthy instances for $name found")
            throw ServiceNotFoundException(name)
        }
    }

    private fun extractServiceFromConsulResponse(healthyServiceInstances: List<ServiceHealth>): Service {
        assert(!healthyServiceInstances.isEmpty()) { "Healthy Service instances must not be empty" }
        val serviceHealth = healthyServiceInstances[0]
        return with(serviceHealth.service) {
            Service(address, port)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(Consul::class.java)
    }
}