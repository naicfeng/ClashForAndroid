package com.github.kr328.clash.core.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val name: String,
    val type: Proxy.Type,
    val current: String,
    val proxies: List<Proxy>
) {
    @Keep
    constructor(name: String, type: String, current: String, proxies: Array<Proxy>) :
            this(name, Proxy.Type.fromString(type), current, proxies.toList())
}