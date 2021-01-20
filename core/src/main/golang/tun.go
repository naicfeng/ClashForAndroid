package main

import (
	"github.com/Dreamacro/clash/component/dialer"
	"github.com/kr328/cfa/tun"
	"net"
	"strconv"
	"sync"
	"syscall"
)

//#include "buffer.h"
//#include "event.h"
import "C"

var tunLock sync.Mutex

func init() {
	c := func(_, _ string, conn syscall.RawConn) error {
		return conn.Control(func(fd uintptr) {
			<- sendEvent(C.NEW_SOCKET, 0, strconv.Itoa(int(fd))).result
		})
	}

	dialer.DialerHook = func(d *net.Dialer) error {
		d.Control = c
		return nil
	}
	dialer.ListenConfigHook = func(l *net.ListenConfig) error {
		l.Control = c
		return nil
	}
}

//export startTunDevice
func startTunDevice(fd, mtu int, gateway, mirror, dns C.const_string_t, callbackId uint64) *C.char {
	stopTunDevice()

	tunLock.Lock()
	defer tunLock.Unlock()

	g := C.GoString(gateway)
	m := C.GoString(mirror)
	d := C.GoString(dns)

	err := tun.StartTunDevice(fd, mtu, g, m, d, func() {
		sendEvent(C.TUN_STOP, callbackId, "")
	})
	if err != nil {
		return C.CString(err.Error())
	}

	return nil
}

//export stopTunDevice
func stopTunDevice() {
	tunLock.Lock()
	defer tunLock.Unlock()

	tun.StopTunDevice()
}