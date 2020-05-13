package config

import (
	"github.com/Dreamacro/clash/component/fakeip"
	"github.com/Dreamacro/clash/config"
	"github.com/Dreamacro/clash/dns"
	"net/url"
)

var (
	OptionalDnsPatch  *config.RawDNS
	DnsPatch          *config.RawDNS
	NameServersAppend []string

	cachedPool *fakeip.Pool
)

func init() {
	defaultNameServers := []string{
		"223.5.5.5",
		"119.29.29.29",
		"1.1.1.1",
		"208.67.222.222",
	}

	OptionalDnsPatch = &config.RawDNS{
		Enable:     true,
		IPv6:       true,
		NameServer: defaultNameServers,
		Fallback:   []string{},
		FallbackFilter: config.RawFallbackFilter{
			GeoIP:  false,
			IPCIDR: []string{},
		},
		Listen:            ":0",
		EnhancedMode:      dns.FAKEIP,
		FakeIPRange:       "198.18.0.0/16",
		FakeIPFilter:      []string{},
		DefaultNameserver: defaultNameServers,
	}
}

func patchRawConfig(rawConfig *config.RawConfig) {
	rawConfig.DNS.FakeIPRange = "198.18.0.0/16"
	rawConfig.Experimental.Interface = ""
	rawConfig.ExternalUI = ""
	rawConfig.ExternalController = ""

	if d := DnsPatch; d != nil {
		rawConfig.DNS = *d
	} else if d := OptionalDnsPatch; d != nil {
		if !rawConfig.DNS.Enable {
			rawConfig.DNS = *d
		}
	}

	if nameServersAppend := NameServersAppend; len(nameServersAppend) > 0 {
		d := &rawConfig.DNS
		nameServers := make([]string, len(nameServersAppend)+len(d.NameServer))
		copy(nameServers, nameServersAppend)
		copy(nameServers[len(nameServersAppend):], d.NameServer)

		d.NameServer = nameServers
	}

	providers := rawConfig.ProxyProvider

	if len(rawConfig.ProxyProvider) == 0 {
		providers = rawConfig.ProxyProviderOld
	}

	for _, provider := range providers {
		path, ok := provider["path"].(string)
		if !ok {
			continue
		}

		provider["path"] = url.QueryEscape(path)
	}
}

func patchConfig(config *config.Config) {
	if config.DNS.FakeIPRange != nil {
		if c := cachedPool; c != nil {
			if config.DNS.FakeIPRange.Gateway().String() == c.Gateway().String() {
				c.OverrideHostFrom(config.DNS.FakeIPRange)
				config.DNS.FakeIPRange = c
			}
		} else {
			cachedPool = config.DNS.FakeIPRange
		}
	}
}
