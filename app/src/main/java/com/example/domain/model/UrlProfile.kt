package com.example.domain.model

data class UrlProfile(
    val id: Long = 0L,
    val name: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val authorization: String? = null,
    val cookies: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toHeadersMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (!userAgent.isNullOrBlank()) map["User-Agent"] = userAgent
        if (!referer.isNullOrBlank()) map["Referer"] = referer
        if (!authorization.isNullOrBlank()) map["Authorization"] = authorization
        if (!cookies.isNullOrBlank()) map["Cookie"] = cookies
        map.putAll(customHeaders)
        return map
    }
}
