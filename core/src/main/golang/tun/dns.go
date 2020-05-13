package tun

import (
	"encoding/binary"
	"github.com/Dreamacro/clash/component/resolver"
	"github.com/Dreamacro/clash/dns"
	"github.com/kr328/cfa/utils"
	"github.com/kr328/tun2socket/binding"
	"github.com/kr328/tun2socket/redirect"
	D "github.com/miekg/dns"
	"io"
	"net"
	"sync"
	"time"
)

const (
	defaultDNSReadTimeout = time.Second * 10
)

var lock sync.Mutex
var hijackAddress net.IP
var dnsHandler dns.Handler

func setHijackAddress(hijackAddr net.IP) {
	lock.Lock()
	defer lock.Unlock()

	hijackAddress = hijackAddr
}

func InitialResolver() {
	lock.Lock()
	defer lock.Unlock()

	rawResolver := resolver.DefaultResolver
	if rawResolver == nil {
		dnsHandler = nil
		return
	}
	r, ok := rawResolver.(*dns.Resolver)
	if !ok || r == nil {
		dnsHandler = nil
		return
	}

	dnsHandler = dns.NewHandler(r)
}

func hijackTCPDNS(conn net.Conn, endpoint *binding.Endpoint) bool {
	if endpoint.Target.Port != 53 {
		return false
	}

	if dnsHandler == nil {
		return false
	}

	if !hijackAddress.Equal(net.IPv4zero) && !hijackAddress.Equal(endpoint.Target.IP) {
		return false
	}

	go func() {
		defer utils.CloseSilent(conn)

		for {
			if err := conn.SetReadDeadline(time.Now().Add(defaultDNSReadTimeout)); err != nil {
				return
			}

			var length uint16
			if err := binary.Read(conn, binary.BigEndian, &length); err != nil {
				return
			}

			data := make([]byte, length)
			msg := &D.Msg{}

			_, err := io.ReadFull(conn, data)
			if err != nil {
				return
			}

			if err := msg.Unpack(data); err != nil || len(msg.Question) == 0 {
				return
			}

			handler := dnsHandler
			if handler == nil {
				return
			}

			handler(&tcpWriter{
				conn:     conn,
				endpoint: endpoint,
			}, msg)
		}
	}()

	return true
}

func hijackDNS(payload []byte, endpoint *binding.Endpoint, sender redirect.UDPSender, recycle func([]byte)) bool {
	if endpoint.Target.Port != 53 {
		return false
	}

	if dnsHandler == nil {
		return false
	}

	if !hijackAddress.Equal(net.IPv4zero) && !hijackAddress.Equal(endpoint.Target.IP) {
		return false
	}

	go func() {
		msg := &D.Msg{}
		if err := msg.Unpack(payload); err != nil {
			return
		}

		handler := dnsHandler
		handler(&udpWriter{
			endpoint: endpoint,
			sender:   sender,
		}, msg)

		recycle(payload)
	}()

	return true
}

type tcpWriter struct {
	conn     net.Conn
	endpoint *binding.Endpoint
}

func (r *tcpWriter) LocalAddr() net.Addr {
	return &net.TCPAddr{
		IP:   r.endpoint.Target.IP,
		Port: int(r.endpoint.Target.Port),
		Zone: "",
	}
}

func (r *tcpWriter) RemoteAddr() net.Addr {
	return &net.TCPAddr{
		IP:   r.endpoint.Source.IP,
		Port: int(r.endpoint.Source.Port),
		Zone: "",
	}
}

func (r *tcpWriter) Write(b []byte) (int, error) {
	if len(b) > 65535 {
		return 0, io.ErrShortBuffer
	}

	var length [2]byte
	binary.BigEndian.PutUint16(length[:], uint16(len(b)))

	n, err := (&net.Buffers{length[:], b}).WriteTo(r.conn)

	return int(n), err
}

func (r *tcpWriter) Close() error {
	return nil
}

func (r *tcpWriter) WriteMsg(d *D.Msg) error {
	msg, err := d.Pack()
	if err != nil {
		return err
	}

	_, err = r.Write(msg)

	return err
}

func (r *tcpWriter) TsigStatus() error {
	// Unsupported
	return nil
}

func (r *tcpWriter) TsigTimersOnly(bool) {
	// Unsupported
}

func (r *tcpWriter) Hijack() {
	// Unsupported
}

type udpWriter struct {
	endpoint *binding.Endpoint
	sender   redirect.UDPSender
}

func (r *udpWriter) LocalAddr() net.Addr {
	return &net.UDPAddr{
		IP:   r.endpoint.Target.IP,
		Port: int(r.endpoint.Target.Port),
		Zone: "",
	}
}

func (r *udpWriter) RemoteAddr() net.Addr {
	return &net.UDPAddr{
		IP:   r.endpoint.Source.IP,
		Port: int(r.endpoint.Source.Port),
		Zone: "",
	}
}

func (r *udpWriter) WriteMsg(d *D.Msg) error {
	msg, err := d.Pack()
	if err != nil {
		return err
	}

	_, err = r.Write(msg)

	return err
}

func (r *udpWriter) Write(msg []byte) (int, error) {
	ep := &binding.Endpoint{
		Source: r.endpoint.Target,
		Target: r.endpoint.Source,
	}

	return len(msg), r.sender(msg, ep)
}

func (r *udpWriter) Close() error {
	return nil
}

func (r *udpWriter) TsigStatus() error {
	// Unsupported
	return nil
}

func (r *udpWriter) TsigTimersOnly(bool) {
	// Unsupported
}

func (r *udpWriter) Hijack() {
	// Unsupported
}
