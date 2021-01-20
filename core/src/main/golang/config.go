package main

//#include "config.h"
//#include "buffer.h"
import "C"

import (
	"github.com/kr328/cfa/config"
	"strings"
)

//export setDnsOverride
func setDnsOverride(override *C.dns_override_t) {
	overrideDns := override.override_dns != 0
	appendDns := C.GoString(override.append_dns)

	if overrideDns {
		config.DnsPatch = config.OptionalDnsPatch
	} else {
		config.DnsPatch = nil
	}

	if len(appendDns) == 0 {
		config.NameServersAppend = make([]string, 0)
	} else {
		config.NameServersAppend = strings.Split(appendDns, ",")
	}
}
