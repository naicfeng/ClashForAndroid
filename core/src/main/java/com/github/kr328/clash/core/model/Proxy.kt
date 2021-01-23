package com.github.kr328.clash.core.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
data class Proxy constructor(
    val name: String,
    val type: Type,
    val delay: Long
) {
    @Keep
    constructor(name: String, type: String, delay: Long): this(name, Type.fromString(type), delay)

    enum class Type(val text: String, val group: Boolean) {
        DIRECT("Direct", false),
        REJECT("Reject", false),

        SHADOWSOCKS("Shadowsocks", false),
        SHADOWSOCKSR("ShadowsocksR", false),
        SNELL("Snell", false),
        SOCKS5("Socks5", false),
        HTTP("Http", false),
        VMESS("Vmess", false),
        TROJAN("Trojan", false),

        RELAY("Relay", true),
        SELECT("Selector", true),
        FALLBACK("Fallback", true),
        URL_TEST("URLTest", true),
        LOAD_BALANCE("LoadBalance", true),

        UNKNOWN("Unknown", false);

        override fun toString(): String {
            return text
        }

        companion object {
            fun fromString(type: String): Type {
                return when (type) {
                    DIRECT.text -> DIRECT
                    REJECT.text -> REJECT
                    SHADOWSOCKS.text -> SHADOWSOCKS
		    SHADOWSOCKSR.text -> SHADOWSOCKSR
                    SNELL.text -> SNELL
                    SOCKS5.text -> SOCKS5
                    HTTP.text -> HTTP
                    VMESS.text -> VMESS
                    TROJAN.text -> TROJAN
                    RELAY.text -> RELAY
                    SELECT.text -> SELECT
                    FALLBACK.text -> FALLBACK
                    URL_TEST.text -> URL_TEST
                    LOAD_BALANCE.text -> LOAD_BALANCE
                    UNKNOWN.text -> UNKNOWN
                    else -> UNKNOWN
                }
            }
        }
    }
}