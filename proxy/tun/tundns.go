package tun

import (
	"fmt"
	"net"

	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/log"
	"github.com/google/netstack/tcpip"
	"github.com/google/netstack/tcpip/adapters/gonet"
	"github.com/google/netstack/tcpip/buffer"
	"github.com/google/netstack/tcpip/header"
	"github.com/google/netstack/tcpip/network/ipv4"
	"github.com/google/netstack/tcpip/network/ipv6"
	"github.com/google/netstack/tcpip/stack"
	"github.com/google/netstack/tcpip/transport/udp"
	D "github.com/miekg/dns"
)

const (
	defaultTimeout = 5
)

var (
	ipv4Zero = tcpip.Address(net.IPv4zero.To4())
	ipv6Zero = tcpip.Address(net.IPv6zero.To16())
)

// DNSServer is DNS Server listening on tun devcice
type DNSServer struct {
	*dns.Server
	resolver *dns.Resolver

	stack         *stack.Stack
	tcpListener   net.Listener
	udpEndpoint   *dnsEndpoint
	udpEndpointID *stack.TransportEndpointID
	tcpip.NICID
}

// dnsEndpoint is a TransportEndpoint that will register to stack
type dnsEndpoint struct {
	stack.TransportEndpoint
	stack    *stack.Stack
	uniqueID uint64
	server   *dns.Server
}

// Keep track of the source of DNS request
type dnsResponseWriter struct {
	s  *stack.Stack
	r  *stack.Route
	id stack.TransportEndpointID
}

func (e *dnsEndpoint) UniqueID() uint64 {
	return e.uniqueID
}

func (e *dnsEndpoint) HandlePacket(r *stack.Route, id stack.TransportEndpointID, pkt tcpip.PacketBuffer) {
	hdr := header.UDP(pkt.Data.First())
	if int(hdr.Length()) > pkt.Data.Size() {
		// Malformed packet.
		e.stack.Stats().UDP.MalformedPacketsReceived.Increment()
		return
	}
	pkt.Data.TrimFront(header.UDPMinimumSize)

	// server DNS
	var msg D.Msg
	msg.Unpack(pkt.Data.ToView())
	writer := dnsResponseWriter{s: e.stack, r: r, id: id}
	go e.server.ServeDNS(&writer, &msg)
}

func (e *dnsEndpoint) HandleControlPacket(id stack.TransportEndpointID, typ stack.ControlType, extra uint32, pkt tcpip.PacketBuffer) {
}

func (e *dnsEndpoint) Close() {
}

func (e *dnsEndpoint) Wait() {

}

func (w *dnsResponseWriter) LocalAddr() net.Addr {
	return &net.UDPAddr{IP: net.IP(w.id.LocalAddress), Port: int(w.id.LocalPort)}
}

func (w *dnsResponseWriter) RemoteAddr() net.Addr {
	return &net.UDPAddr{IP: net.IP(w.id.RemoteAddress), Port: int(w.id.RemotePort)}
}

func (w *dnsResponseWriter) WriteMsg(msg *D.Msg) error {
	b, err := msg.Pack()
	if err != nil {
		return err
	}
	_, err = w.Write(b)
	return err
}
func (w *dnsResponseWriter) TsigStatus() error {
	// Unsupported
	return nil
}
func (w *dnsResponseWriter) TsigTimersOnly(bool) {
	// Unsupported
}
func (w *dnsResponseWriter) Hijack() {
	// Unsupported
}

func (w *dnsResponseWriter) Write(b []byte) (int, error) {
	v := buffer.NewView(len(b))
	copy(v, b)
	data := v.ToVectorisedView()
	return writeUDP(w.r, data, w.id.LocalPort, w.id.RemotePort)
}

func (w *dnsResponseWriter) Close() error {
	return nil
}

// CreateDNSServer create a dns server on given netstack
func CreateDNSServer(s *stack.Stack, resolver *dns.Resolver, ip net.IP, port int, nicID tcpip.NICID) (*DNSServer, error) {

	var v4 bool
	var err error

	address := tcpip.FullAddress{NIC: nicID, Port: uint16(port)}
	if ip.To4() != nil {
		v4 = true
		address.Addr = tcpip.Address(ip.To4())
		// netstack will only reassemble IP fragments when its' dest ip address is registered in NIC.endpoints
		s.AddAddress(nicID, ipv4.ProtocolNumber, address.Addr)
	} else {
		v4 = false
		address.Addr = tcpip.Address(ip.To16())
		s.AddAddress(nicID, ipv6.ProtocolNumber, address.Addr)
	}
	if address.Addr == ipv4Zero || address.Addr == ipv6Zero {
		address.Addr = ""
	}

	handler := dns.NewHandler(resolver)
	serverIn := &dns.Server{}
	serverIn.SetHandler(handler)

	// UDP DNS
	id := &stack.TransportEndpointID{
		LocalAddress:  address.Addr,
		LocalPort:     uint16(port),
		RemotePort:    0,
		RemoteAddress: "",
	}

	// TransportEndpoint for DNS
	endpoint := &dnsEndpoint{
		stack:    s,
		uniqueID: s.UniqueID(),
		server:   serverIn,
	}

	if tcpiperr := s.RegisterTransportEndpoint(1,
		[]tcpip.NetworkProtocolNumber{
			ipv4.ProtocolNumber,
			ipv6.ProtocolNumber,
		},
		udp.ProtocolNumber,
		*id,
		endpoint,
		true,
		nicID); err != nil {
		log.Errorln("Unable to start UDP DNS on tun:  %v", tcpiperr.String())
	}

	// TCP DNS
	var tcpListener net.Listener
	if v4 {
		tcpListener, err = gonet.NewListener(s, address, ipv4.ProtocolNumber)
	} else {
		tcpListener, err = gonet.NewListener(s, address, ipv6.ProtocolNumber)
	}
	if err != nil {
		return nil, fmt.Errorf("Can not listen on tun: %v", err)
	}

	server := &DNSServer{
		Server:        serverIn,
		resolver:      resolver,
		stack:         s,
		tcpListener:   tcpListener,
		udpEndpoint:   endpoint,
		udpEndpointID: id,
		NICID:         nicID,
	}
	server.SetHandler(handler)
	server.Server.Server = &D.Server{Listener: tcpListener, Handler: server}

	go func() {
		server.ActivateAndServe()
	}()

	return server, err
}

// Stop stop the DNS Server on tun
func (s *DNSServer) Stop() {
	// shutdown TCP DNS Server
	s.Server.Shutdown()
	// remove TCP endpoint from stack
	if s.Listener != nil {
		s.Listener.Close()
	}
	// remove udp endpoint from stack
	s.stack.UnregisterTransportEndpoint(s.NICID,
		[]tcpip.NetworkProtocolNumber{
			ipv4.ProtocolNumber,
			ipv6.ProtocolNumber,
		},
		udp.ProtocolNumber,
		*s.udpEndpointID,
		s.udpEndpoint,
		s.NICID)
}

// DNSListen return the listening address of DNS Server
func (t *tunAdapter) DNSListen() string {
	if t.dnsserver != nil {
		id := t.dnsserver.udpEndpointID
		return fmt.Sprintf("%s:%d", id.LocalAddress.String(), id.LocalPort)
	}
	return ""
}

// Stop stop the DNS Server on tun
func (t *tunAdapter) ReCreateDNSServer(resolver *dns.Resolver, addr string) error {
	if addr == "" && t.dnsserver == nil {
		return nil
	}

	if addr == t.DNSListen() && t.dnsserver != nil && t.dnsserver.resolver == resolver {
		return nil
	}

	if t.dnsserver != nil {
		t.dnsserver.Stop()
		t.dnsserver = nil
		log.Debugln("Tun DNS server stoped")
	}

	var err error
	_, port, err := net.SplitHostPort(addr)
	if port == "0" || port == "" || err != nil {
		return nil
	}

	if resolver == nil {
		return fmt.Errorf("Failed to create DNS server on tun: resolver not provided")
	}

	udpAddr, err := net.ResolveUDPAddr("udp", addr)
	if err != nil {
		return err
	}

	server, err := CreateDNSServer(t.ipstack, resolver, udpAddr.IP, udpAddr.Port, 1)
	if err != nil {
		return err
	}
	t.dnsserver = server
	log.Infoln("Tun DNS server listening at: %s", addr)
	return nil
}
