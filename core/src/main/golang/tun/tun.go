package tun

import (
	"errors"
	adapters "github.com/Dreamacro/clash/adapters/inbound"
	"github.com/Dreamacro/clash/component/socks5"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/tun2socket"
	"github.com/kr328/tun2socket/binding"
	"github.com/kr328/tun2socket/redirect"
	"net"
	"os"
	"sync"
	"syscall"

	"github.com/Dreamacro/clash/log"
)

const (
	maxUdpPacketSize = 65535
)

var adapter *tun2socket.Tun2Socket
var mutex sync.Mutex

func StartTunDevice(fd, mtu int, gateway, mirror, dnsAddress string) error {
	mutex.Lock()
	defer mutex.Unlock()

	if adapter != nil {
		adapter.Close()
		adapter = nil

		log.Infoln("Android tun stopped")
	}

	gatewayIP, gatewayNet, err := net.ParseCIDR(gateway)
	_, ipv4Loopback, _ := net.ParseCIDR("127.0.0.0/8")
	mirrorIP := net.ParseIP(mirror)

	if err != nil || mirrorIP == nil || !gatewayNet.Contains(mirrorIP) {
		return errors.New("invalid gateway or mirror")
	}

	udpPool := sync.Pool{New: func() interface{} {
		return make([]byte, maxUdpPacketSize)
	}}
	udpRecycle := func(bytes []byte) {
		if cap(bytes) == maxUdpPacketSize {
			udpPool.Put(bytes[:maxUdpPacketSize])
		}
	}

	file := os.NewFile(uintptr(fd), "/dev/tun")
	_ = syscall.SetNonblock(fd, true)

	adapter = tun2socket.NewTun2Socket(file, mtu, gatewayIP, mirrorIP.To4())

	adapter.SetLogger(&ClashLogger{})
	adapter.SetClosedHandler(func() {
		StopTunDevice()
	})
	adapter.SetAllocator(func(length int) []byte {
		if length <= maxUdpPacketSize {
			return udpPool.Get().([]byte)[:length]
		}
		return make([]byte, length)
	})
	adapter.SetTCPHandler(func(conn net.Conn, endpoint *binding.Endpoint) {
		if gatewayNet.Contains(endpoint.Target.IP) || ipv4Loopback.Contains(endpoint.Target.IP) {
			_ = conn.Close()
			return
		}

		if hijackTCPDNS(conn, endpoint) {
			return
		}

		addr := socks5.ParseAddrToSocksAddr(&net.TCPAddr{
			IP:   endpoint.Target.IP,
			Port: int(endpoint.Target.Port),
			Zone: "",
		})

		tunnel.Add(adapters.NewSocket(addr, conn, C.SOCKS))
	})
	adapter.SetUDPHandler(func(payload []byte, endpoint *binding.Endpoint, sender redirect.UDPSender) {
		if gatewayNet.Contains(endpoint.Target.IP) || ipv4Loopback.Contains(endpoint.Target.IP) {
			udpRecycle(payload)
			return
		}

		if hijackDNS(payload, endpoint, sender, udpRecycle) {
			return
		}

		addr := socks5.ParseAddrToSocksAddr(&net.TCPAddr{
			IP:   endpoint.Target.IP,
			Port: int(endpoint.Target.Port),
			Zone: "",
		})
		pkt := &udpPacket{
			payload:  payload,
			endpoint: endpoint,
			send:     sender,
			recycle:  udpRecycle,
		}

		tunnel.AddPacket(adapters.NewPacket(addr, pkt, C.SOCKS))
	})

	setHijackAddress(net.ParseIP(dnsAddress))
	InitialResolver()

	adapter.Start()

	log.Infoln("Android tun started")

	return nil
}

func StopTunDevice() {
	mutex.Lock()
	defer mutex.Unlock()

	if adapter != nil {
		adapter.Close()
		adapter = nil

		log.Infoln("Android tun stopped")
	}
}
