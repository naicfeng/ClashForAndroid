package tun

import (
	"errors"
	"github.com/kr328/tun2socket/binding"
	"github.com/kr328/tun2socket/redirect"
	"net"
)

type udpPacket struct {
	payload  []byte
	endpoint *binding.Endpoint
	send     redirect.UDPSender
	recycle  func([]byte)
}

func (conn *udpPacket) Data() []byte {
	return conn.payload
}

func (conn *udpPacket) WriteBack(b []byte, addr net.Addr) (n int, err error) {
	if addr == nil {
		addr = &net.UDPAddr{
			IP:   conn.endpoint.Target.IP,
			Port: int(conn.endpoint.Target.Port),
			Zone: "",
		}
	}

	udpAddr, ok := addr.(*net.UDPAddr)
	if !ok {
		return 0, errors.New("Invalid udp address")
	}

	ep := &binding.Endpoint{
		Source: binding.Address{
			IP:   udpAddr.IP,
			Port: uint16(udpAddr.Port),
		},
		Target: conn.endpoint.Source,
	}

	return len(b), conn.send(b, ep)
}

func (conn *udpPacket) LocalAddr() net.Addr {
	return &net.UDPAddr{
		IP:   conn.endpoint.Source.IP,
		Port: int(conn.endpoint.Source.Port),
		Zone: "",
	}
}

func (conn *udpPacket) Drop() {
	conn.recycle(conn.payload)
}
