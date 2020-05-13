package config

import (
	"errors"
	"io/ioutil"

	"github.com/Dreamacro/clash/config"
	"github.com/Dreamacro/clash/hub/executor"
	"github.com/Dreamacro/clash/log"
	"github.com/kr328/cfa/tun"
)

// LoadDefault - load default configure
func LoadDefault() {
	DnsPatch = nil
	NameServersAppend = make([]string, 0)

	defaultC, err := config.Parse([]byte{})
	if err != nil {
		log.Warnln("Load Default Failure " + err.Error())
		return
	}

	executor.ApplyConfig(defaultC, true)

	tun.InitialResolver()
}

// LoadFromFile - load file
func LoadFromFile(path, baseDir string) error {
	data, err := ioutil.ReadFile(path)
	if err != nil {
		return err
	}

	cfg, err := parseConfig(data, baseDir)
	if err != nil {
		return err
	}

	for _, ns := range cfg.DNS.NameServer {
		log.Infoln("DNS: %s", ns.Addr)
	}

	executor.ApplyConfig(cfg, true)

	tun.InitialResolver()

	log.Infoln("Profile " + path + " loaded")

	return nil
}

func parseConfig(data []byte, baseDir string) (*config.Config, error) {
	raw, err := config.UnmarshalRawConfig(data)
	if err != nil {
		return nil, err
	}

	patchRawConfig(raw)

	if len(raw.Proxy) == 0 && len(raw.ProxyProvider) == 0 &&
		len(raw.ProxyOld) == 0 && len(raw.ProxyProviderOld) == 0 {
		return nil, errors.New("Empty Profile")
	}

	cfg, err := config.ParseRawConfig(raw, baseDir)
	if err != nil {
		return nil, err
	}

	patchConfig(cfg)

	return cfg, nil
}
