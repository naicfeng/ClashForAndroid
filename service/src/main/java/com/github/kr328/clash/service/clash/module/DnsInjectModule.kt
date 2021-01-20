package com.github.kr328.clash.service.clash.module

import com.github.kr328.clash.core.Clash

class DnsInjectModule : Module() {
    var dnsOverride: Boolean = false
        set(value) {
            field = value

            Clash.setDnsOverride(value, appendDns)
        }
    var appendDns: List<String> = emptyList()
        set(value) {
            field = value

            Clash.setDnsOverride(dnsOverride, value)
        }

    override suspend fun onStart() {
        Clash.setDnsOverride(dnsOverride, appendDns)
    }

    override suspend fun onStop() {
        Clash.setDnsOverride(false, emptyList())
    }

}