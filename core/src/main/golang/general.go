package main

//#include "buffer.h"
//#include "general.h"
import "C"

import (
	"github.com/Dreamacro/clash/proxy"
	"github.com/Dreamacro/clash/tunnel"
)

//export setProxyMode
func setProxyMode(mode C.int) {
	switch mode {
	case C.MODE_DIRECT:
		tunnel.SetMode(tunnel.Direct)
	case C.MODE_GLOBAL:
		tunnel.SetMode(tunnel.Global)
	case C.MODE_RULE:
		tunnel.SetMode(tunnel.Rule)
	}
}

//export queryGeneral
func queryGeneral(general *C.general_t) {
	m := tunnel.Mode()
	ports := proxy.GetPorts()

	switch m {
	case tunnel.Direct:
		general.mode = C.MODE_DIRECT
	case tunnel.Global:
		general.mode = C.MODE_GLOBAL
	case tunnel.Rule:
		general.mode = C.MODE_RULE
	}

	general.http_port = C.int(ports.Port)
	general.socks_port = C.int(ports.SocksPort)
	general.mixed_port = C.int(ports.MixedPort)
	general.redirect_port = C.int(ports.RedirPort)
}