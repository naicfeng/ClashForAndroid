package tun

import (
	"fmt"
	"net"
	"net/url"
	"strings"

	adapters "github.com/Dreamacro/clash/adapters/inbound"
	"github.com/Dreamacro/clash/component/socks5"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/proxy/tun/dev"
	tun "github.com/Dreamacro/clash/tunnel"

	"encoding/binary"

	"github.com/google/netstack/tcpip"
	"github.com/google/netstack/tcpip/adapters/gonet"
	"github.com/google/netstack/tcpip/header"
	"github.com/google/netstack/tcpip/network/ipv4"
	"github.com/google/netstack/tcpip/network/ipv6"
	"github.com/google/netstack/tcpip/stack"
	"github.com/google/netstack/tcpip/transport/tcp"
	"github.com/google/netstack/tcpip/transport/udp"
	"github.com/google/netstack/waiter"
)

// tunAdapter is the wraper of tun
type tunAdapter struct {
	device  dev.TunDevice
	ipstack *stack.Stack

	dnsserver *DNSServer
}

// NewTunProxy create TunProxy under Linux OS.
func NewTunProxy(deviceURL string) (TunAdapter, error) {

	var err error

	url, err := url.Parse(deviceURL)
	if err != nil {
		return nil, fmt.Errorf("Invalid tun device url: %v", err)
	}

	tundev, err := dev.OpenTunDevice(*url)
	if err != nil {
		return nil, fmt.Errorf("Can't open tun: %v", err)
	}

	ipstack := stack.New(stack.Options{
		NetworkProtocols:   []stack.NetworkProtocol{ipv4.NewProtocol(), ipv6.NewProtocol()},
		TransportProtocols: []stack.TransportProtocol{tcp.NewProtocol(), udp.NewProtocol()},
	})

	tl := &tunAdapter{
		device:  tundev,
		ipstack: ipstack,
	}

	linkEP, err := tundev.AsLinkEndpoint()
	if err != nil {
		return nil, fmt.Errorf("Unable to create virtual endpoint: %v", err)
	}

	if err := ipstack.CreateNIC(1, linkEP); err != nil {
		return nil, fmt.Errorf("Fail to create NIC in ipstack: %v", err)
	}

	// IPv4 0.0.0.0/0
	subnet, _ := tcpip.NewSubnet(tcpip.Address(strings.Repeat("\x00", 4)), tcpip.AddressMask(strings.Repeat("\x00", 4)))
	ipstack.AddAddressRange(1, ipv4.ProtocolNumber, subnet)

	// IPv6 [::]/0
	subnet, _ = tcpip.NewSubnet(tcpip.Address(strings.Repeat("\x00", 16)), tcpip.AddressMask(strings.Repeat("\x00", 16)))
	ipstack.AddAddressRange(1, ipv6.ProtocolNumber, subnet)

	// TCP handler
	// maximum number of half-open tcp connection set to 1024
	// receive buffer size set to 20k
	tcpFwd := tcp.NewForwarder(ipstack, 20*1024, 1024, func(r *tcp.ForwarderRequest) {
		var wq waiter.Queue
		ep, err := r.CreateEndpoint(&wq)
		if err != nil {
			log.Warnln("Can't create TCP Endpoint in ipstack: %v", err)
			r.Complete(true)
			return
		}
		r.Complete(false)

		conn := gonet.NewConn(&wq, ep)
		target := getAddr(ep.Info().(*tcp.EndpointInfo).ID)
		tun.Add(adapters.NewSocket(target, conn, C.TUN, C.TCP))

	})
	ipstack.SetTransportProtocolHandler(tcp.ProtocolNumber, tcpFwd.HandlePacket)

	// UDP handler
	ipstack.SetTransportProtocolHandler(udp.ProtocolNumber, tl.udpHandlePacket)

	log.Infoln("Tun adapter have interface name: %s", tundev.Name())
	return tl, nil

}

// Close close the TunAdapter
func (t *tunAdapter) Close() {
	t.device.Close()
	if t.dnsserver != nil {
		t.dnsserver.Stop()
	}
	t.ipstack.Close()
}

// IfName return device URL of tun
func (t *tunAdapter) DeviceURL() string {
	return t.device.URL()
}

func (t *tunAdapter) udpHandlePacket(r *stack.Route, id stack.TransportEndpointID, pkt tcpip.PacketBuffer) bool {

	hdr := header.UDP(pkt.Data.First())
	if int(hdr.Length()) > pkt.Data.Size() {
		// Malformed packet.
		t.ipstack.Stats().UDP.MalformedPacketsReceived.Increment()
		return true
	}
	pkt.Data.TrimFront(header.UDPMinimumSize)

	target := getAddr(id)

	packet := &fakeConn{
		id:      id,
		r:       r,
		payload: pkt.Data.ToView(),
	}
	tun.AddPacket(adapters.NewPacket(target, packet, C.SOCKS))

	return true
}

func getAddr(id stack.TransportEndpointID) socks5.Addr {
	ipv4 := id.LocalAddress.To4()

	// get the big-endian binary represent of port
	port := make([]byte, 2)
	binary.BigEndian.PutUint16(port, id.LocalPort)

	if ipv4 != "" {
		addr := make([]byte, 1+net.IPv4len+2)
		addr[0] = socks5.AtypIPv4
		copy(addr[1:1+net.IPv4len], []byte(ipv4))
		addr[1+net.IPv4len], addr[1+net.IPv4len+1] = port[0], port[1]
		return addr
	} else {
		addr := make([]byte, 1+net.IPv6len+2)
		addr[0] = socks5.AtypIPv6
		copy(addr[1:1+net.IPv6len], []byte(id.LocalAddress))
		addr[1+net.IPv6len], addr[1+net.IPv6len+1] = port[0], port[1]
		return addr
	}

}
