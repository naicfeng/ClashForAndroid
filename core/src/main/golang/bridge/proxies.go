package bridge

import (
	"sync"

	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/adapters/provider"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

type ProxyItem struct {
	Name  string
	Type  string
	Delay int
}

type ProxyGroupItem struct {
	Name    string
	Type    string
	Current string
	Delay   int

	providers []provider.ProxyProvider
}

type ProxyGroupCollection interface {
	Add(proxy *ProxyGroupItem) bool
}

type ProxyCollection interface {
	Add(proxy *ProxyItem) bool
}

func (p *ProxyGroupItem) QueryAllProxies(collection ProxyCollection) {
	for _, v := range p.providers {
		for _, p := range v.Proxies() {
			collection.Add(
				&ProxyItem{
					Name:  p.Name(),
					Type:  p.Type().String(),
					Delay: int(p.LastDelay()),
				},
			)
		}
	}
}

func StartUrlTest(group string, callback DoneCallback) {
	go func() {
		defer callback.Done()

		p := tunnel.Proxies()[group]

		pi, ok := p.(*outbound.Proxy)
		if !ok {
			return
		}

		group, ok := pi.ProxyAdapter.(outboundgroup.ProxyGroup)
		if !ok {
			return
		}

		providers := group.GetProxyProviders()

		wg := &sync.WaitGroup{}
		wg.Add(len(providers))

		for _, v := range providers {
			go func(p provider.ProxyProvider) {
				p.HealthCheck()
				wg.Done()
			}(v)
		}

		wg.Wait()
	}()
}

func QueryAllProxyGroups(collection ProxyGroupCollection) {
	ps := tunnel.Proxies()

	for _, p := range ps {
		pi, ok := p.(*outbound.Proxy)
		if !ok {
			continue
		}

		group, ok := pi.ProxyAdapter.(outboundgroup.ProxyGroup)
		if !ok {
			continue
		}

		collection.Add(&ProxyGroupItem{
			Name:      group.Name(),
			Type:      group.Type().String(),
			Current:   group.Now(),
			Delay:     0,
			providers: group.GetProxyProviders(),
		})
	}
}

func SetSelectedProxy(name, proxy string) bool {
	p := tunnel.Proxies()[name]
	if p == nil {
		log.Infoln("Set %s: Not such proxy group", name)
		return false
	}

	pb, ok := p.(*outbound.Proxy)
	if !ok {
		log.Infoln("Set %s: Not a proxy object", name)
		return false
	}

	selector, ok := pb.ProxyAdapter.(*outboundgroup.Selector)
	if !ok {
		log.Infoln("Set %s: Not a selector group", name)
		return false
	}

	selected := selector.Now()
	if selected == proxy {
		log.Infoln("Set " + name + " -> " + proxy)
		return true
	}

	if err := selector.Set(proxy); err != nil {
		log.Infoln("Set %s: %s", name, err.Error())
		return false
	}

	for _, conn := range tunnel.DefaultManager.Snapshot().Connections {
		for _, p := range conn.Chain() {
			if p == name {
				_ = conn.Close()
				break
			}
		}
	}

	log.Infoln("Set " + name + " -> " + proxy)

	return true
}
