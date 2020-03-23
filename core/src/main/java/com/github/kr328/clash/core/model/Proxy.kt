package com.github.kr328.clash.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Proxy(
    val name: String,
    val type: Type,
    val delay: Long
) {
    enum class Type {
        SELECT,
        URL_TEST,
        FALLBACK,
        DIRECT,
        REJECT,
        SHADOWSOCKS,
        SHADOWSOCKSR,
        SNELL,
        SOCKS5,
        HTTP,
        VMESS,
	TORJAN,
        LOAD_BALANCE,
        UNKNOWN;

        override fun toString(): String {
            return when (this) {
                SELECT -> TYPE_SELECT
                URL_TEST -> TYPE_URL_TEST
                FALLBACK -> TYPE_FALLBACK
                DIRECT -> TYPE_DIRECT
                REJECT -> TYPE_REJECT
                SHADOWSOCKS -> TYPE_SHADOWSOCKS
                SHADOWSOCKSR-> TYPE_SHADOWSOCKSR
                SNELL -> TYPE_SNELL
                SOCKS5 -> TYPE_SOCKS5
                HTTP -> TYPE_HTTP
                VMESS -> TYPE_VMESS
		TORJAN-> TYPE_TORJAN
                LOAD_BALANCE -> TYPE_LOAD_BALANCE
                UNKNOWN -> TYPE_UNKNOWN
            }
        }

        companion object {
            fun fromString(type: String): Type {
                return when (type) {
                    TYPE_SELECT -> SELECT
                    TYPE_URL_TEST -> URL_TEST
                    TYPE_FALLBACK -> FALLBACK
                    TYPE_DIRECT -> DIRECT
                    TYPE_REJECT -> REJECT
                    TYPE_SHADOWSOCKS -> SHADOWSOCKS
                    TYPE_SHADOWSOCKSR -> SHADOWSOCKSR
                    TYPE_SNELL -> SNELL
                    TYPE_SOCKS5 -> SOCKS5
                    TYPE_HTTP -> HTTP
                    TYPE_VMESS -> VMESS
		    TYPE_TORJAN -> TORJAN
                    TYPE_LOAD_BALANCE -> LOAD_BALANCE
                    TYPE_UNKNOWN -> UNKNOWN
                    else -> UNKNOWN
                }
            }
        }
    }

    companion object {
        private const val TYPE_SELECT = "Selector"
        private const val TYPE_URL_TEST = "URLTest"
        private const val TYPE_FALLBACK = "Fallback"
        private const val TYPE_DIRECT = "Direct"
        private const val TYPE_REJECT = "Reject"
        private const val TYPE_SHADOWSOCKS = "Shadowsocks"
        private const val TYPE_SHADOWSOCKSR = "ShadowsocksR"
        private const val TYPE_SNELL = "Snell"
        private const val TYPE_SOCKS5 = "Socks5"
        private const val TYPE_HTTP = "Http"
        private const val TYPE_VMESS = "Vmess"
	private const val TYPE_TORJAN = "Trojan"
        private const val TYPE_LOAD_BALANCE = "LoadBalance"
        private const val TYPE_UNKNOWN = "Unknown"

    }
}
