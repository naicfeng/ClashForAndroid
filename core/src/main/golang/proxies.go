package main

import (
	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
	"unsafe"
)

/*
#cgo CFLAGS: -Werror
#include "buffer.h"
#include "proxies.h"
 */
import "C"

//export setSelector
func setSelector(group C.const_string_t, selected C.const_string_t) C.int {
	g := C.GoString(group)
	s := C.GoString(selected)

	p := tunnel.Proxies()[g]
	if p == nil {
		log.Warnln("Set selector failure: %s not found", g)

		return -1
	}

	pw, ok := p.(*outbound.Proxy)
	if !ok {
		log.Warnln("Set selector failure: %s not valid group", g)

		return -1
	}

	adapter, ok := pw.ProxyAdapter.(outboundgroup.ProxyGroup)
	if !ok {
		log.Warnln("Set selector failure: %s not valid group", g)

		return -1
	}

	selector, ok := adapter.(*outboundgroup.Selector)
	if !ok {
		log.Warnln("Set selector failure: %s not selector", g)

		return -1
	}

	if err := selector.Set(s); err != nil {
		log.Warnln("Set selector failure: %s not in %s", s, g)

		return -1
	}

	log.Infoln("Set %s -> %s", g, s)

	return 0
}

//export queryProxyGroups
func queryProxyGroups() *C.proxy_group_list_t {
	stringPool := make([]byte, 0, 4096)
	proxies := tunnel.Proxies()
	groups := make([]outboundgroup.ProxyGroup, 0, len(proxies))

	for _, p := range proxies {
		pw, ok := p.(*outbound.Proxy)
		if !ok {
			continue
		}

		adapter, ok := pw.ProxyAdapter.(outboundgroup.ProxyGroup)
		if !ok {
			continue
		}

		groups = append(groups, adapter)
	}

	result := allocCProxyGroupList(len(groups))
	groupIndex := 0
	for _, group := range groups {
		ps := make([]constant.Proxy, 0)
		for _, provider := range group.GetProxyProviders() {
			ps = append(ps, provider.Proxies()...)
		}

		g := allocCProxyGroup(len(ps))

		g.base.name_index = C.long(len(stringPool))
		g.base.proxy_type = typeToProxyTypeC(group.Type())
		g.base.delay = 0

		stringPool = append(stringPool, group.Name()...)
		stringPool = append(stringPool, 0)

		proxyIndex := 0
		for _, proxy := range ps {
			p := indexCProxyGroupElement(g, proxyIndex)

			p.name_index = C.long(len(stringPool))
			p.proxy_type = typeToProxyTypeC(proxy.Type())
			p.delay = C.long(proxy.LastDelay())

			stringPool = append(stringPool, proxy.Name()...)
			stringPool = append(stringPool, 0)

			if proxy.Name() == group.Now() {
				g.now = C.int(proxyIndex)
			}

			proxyIndex++
		}

		setCProxyGroupListElement(result, groupIndex, g)

		groupIndex++
	}

	result.string_pool = (*C.char)(C.CBytes(stringPool))

	return result
}



func typeToProxyTypeC(t constant.AdapterType) C.proxy_type_t {
	switch t {
	case constant.Direct:
		return C.Direct
	case constant.Reject:
		return C.Reject

	case constant.Shadowsocks:
		return C.Shadowsocks
	case constant.ShadowsocksR:
		return C.ShadowsocksR
	case constant.Snell:
		return C.Snell
	case constant.Socks5:
		return C.Socks5
	case constant.Http:
		return C.Http
	case constant.Vmess:
		return C.Vmess
	case constant.Trojan:
		return C.Trojan

	case constant.Relay:
		return C.Relay
	case constant.Selector:
		return C.Selector
	case constant.Fallback:
		return C.Fallback
	case constant.URLTest:
		return C.URLTest
	case constant.LoadBalance:
		return C.LoadBalance

	default:
		return C.Unknown
	}
}

func allocCProxyGroup(proxiesSize int) *C.proxy_group_t {
	result := (*C.proxy_group_t)(C.malloc(C.sizeof_proxy_group_t + C.sizeof_proxy_t * C.size_t(proxiesSize)))

	result.proxies_size = C.int(proxiesSize)

	return result
}

func allocCProxyGroupList(groupSize int) *C.proxy_group_list_t {
	result := (*C.proxy_group_list_t)(C.malloc(C.sizeof_proxy_group_list_t + C.sizeof_long * C.size_t(groupSize)))

	result.size = C.int(groupSize)

	return result
}

//noinspection GoVetUnsafePointer
func setCProxyGroupListElement(list *C.proxy_group_list_t, index int, element *C.proxy_group_t) {
	address := uintptr(unsafe.Pointer(list))

	offset := address + uintptr(C.sizeof_proxy_group_list_t) + uintptr(index) * uintptr(C.sizeof_long)

	*(**C.proxy_group_t)(unsafe.Pointer(offset)) = element
}

//noinspection GoVetUnsafePointer
func indexCProxyGroupElement(group *C.proxy_group_t, index int) *C.proxy_t {
	address := uintptr(unsafe.Pointer(group))

	offset := address + uintptr(C.sizeof_proxy_group_t) + uintptr(index) * uintptr(C.sizeof_proxy_t)

	return (*C.proxy_t)(unsafe.Pointer(offset))
}