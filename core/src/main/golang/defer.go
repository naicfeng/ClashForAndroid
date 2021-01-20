package main

import (
	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/adapters/provider"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/config"
	"sync"
)

//#include "buffer.h"
//#include "event.h"
import "C"

//export downloadProfileFromFd
func downloadProfileFromFd(fd int, base C.const_string_t, output C.const_string_t, callbackId uint64) {
	b := C.GoString(base)
	o := C.GoString(output)

	go func() {
		err := config.DownloadFd(fd, o, b)
		if err != nil {
			sendEvent(C.COMPLETE, callbackId, err.Error())
		} else {
			sendEvent(C.COMPLETE, callbackId, "")
		}
	}()
}

//export downloadProfileFromUrl
func downloadProfileFromUrl(url C.const_string_t, base C.const_string_t, output C.const_string_t, callbackId uint64) {
	u := C.GoString(url)
	b := C.GoString(base)
	o := C.GoString(output)

	go func() {
		err := config.DownloadUrl(u, o, b)
		if err != nil {
			sendEvent(C.COMPLETE, callbackId, err.Error())
		} else {
			sendEvent(C.COMPLETE, callbackId, "")
		}
	}()
}

//export loadProfile
func loadProfile(path C.const_string_t, base C.const_string_t, callbackId uint64) {
	p := C.GoString(path)
	b := C.GoString(base)

	go func() {
		err := config.LoadFromFile(p, b)
		if err != nil {
			sendEvent(C.COMPLETE, callbackId, err.Error())
		} else {
			sendEvent(C.COMPLETE, callbackId, "")
		}
	}()
}

//export performHealthCheck
func performHealthCheck(group C.const_string_t, callbackId uint64) {
	g := C.GoString(group)

	go func() {
		p := tunnel.Proxies()[g]
		if p == nil {
			sendEvent(C.COMPLETE, callbackId,  "No such proxy group")

			log.Warnln("Perform health check failure: %s not found", g)

			return
		}

		pw, ok := p.(*outbound.Proxy)
		if !ok {
			sendEvent(C.COMPLETE, callbackId, "Invalid group")

			log.Warnln("Perform health check failure: %s not valid group", g)

			return
		}

		adapter, ok := pw.ProxyAdapter.(outboundgroup.ProxyGroup)
		if !ok {
			sendEvent(C.COMPLETE, callbackId, "Invalid group")

			log.Warnln("Perform health check failure: %s not valid group", g)

			return
		}

		providers := adapter.GetProxyProviders()
		wg := &sync.WaitGroup{}

		wg.Add(len(providers))

		for _, p := range providers {
			go func(p provider.ProxyProvider) {
				p.HealthCheck()

				wg.Done()
			}(p)
		}

		wg.Wait()

		sendEvent(C.COMPLETE, callbackId, "")
	}()
}