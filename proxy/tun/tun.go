package tun

import "github.com/Dreamacro/clash/dns"

// TunAdapter hold the state of tun/tap interface
type TunAdapter interface {
	Close()
	DeviceURL() string
	// Create creates dns server on tun device
	ReCreateDNSServer(resolver *dns.Resolver, addr string) error
	DNSListen() string
}
