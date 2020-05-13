package com.github.kr328.clash.service.clash.module

import com.github.kr328.clash.core.Clash

class DnsInjectModule : Module() {
    var dnsOverride: Boolean = false
        set(value) {
            Clash.setDnsOverrideEnabled(value)
            field = value
        }
    var appendDns: List<String> = emptyList()
        set(value) {
            Clash.appendDns(value)
            field = value
        }

    override suspend fun onStart() {
        Clash.setDnsOverrideEnabled(dnsOverride)
        Clash.appendDns(appendDns)
    }

    override suspend fun onStop() {
        Clash.setDnsOverrideEnabled(false)
        Clash.appendDns(emptyList())
    }

}