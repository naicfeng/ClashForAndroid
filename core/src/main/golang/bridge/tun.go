package bridge

import (
	"net"
	"syscall"

	"github.com/Dreamacro/clash/component/dialer"
	"github.com/kr328/cfa/tun"
)

type TunCallback interface {
	OnCreateSocket(fd int)
	OnStop()
}

var callback TunCallback

func init() {
	dialer.DialerHook = onNewDialer
	dialer.ListenConfigHook = onNewListenConfig
}

func onNewDialer(dialer *net.Dialer) error {
	dialer.Control = onNewSocket
	return nil
}

func onNewListenConfig(listen *net.ListenConfig) error {
	listen.Control = onNewSocket
	return nil
}

func onNewSocket(_, _ string, c syscall.RawConn) error {
	if cb := callback; cb != nil {
		_ = c.Control(func(fd uintptr) {
			cb.OnCreateSocket(int(fd))
		})
	}

	return nil
}

func StartTunDevice(fd, mtu int, gateway, mirror, dns string, cb TunCallback) error {
	callback = cb

	return tun.StartTunDevice(fd, mtu, gateway, mirror, dns)
}

func StopTunDevice() {
	tun.StopTunDevice()

	if c := callback; c != nil {
		c.OnStop()
	}

	callback = nil
}
