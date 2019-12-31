package ch.guengel.webtools.dto

data class NmapPort(val number: Int, val state: String, val name: String)
data class NmapDto(val state: String,
                   val addresses: List<String>,
                   val hostnames: List<String>,
                   val ports: List<NmapPort>)