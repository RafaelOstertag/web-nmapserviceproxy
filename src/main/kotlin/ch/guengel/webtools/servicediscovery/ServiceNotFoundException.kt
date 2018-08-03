package ch.guengel.webtools.servicediscovery

class ServiceNotFoundException(name: String) : RuntimeException("Service '$name' not found")