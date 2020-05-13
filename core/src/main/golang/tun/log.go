package tun

import "github.com/Dreamacro/clash/log"

type ClashLogger struct{}

func (c *ClashLogger) D(format string, args ...interface{}) {
	log.Debugln(format, args...)
}

func (c *ClashLogger) I(format string, args ...interface{}) {
	log.Infoln(format, args...)
}

func (c *ClashLogger) W(format string, args ...interface{}) {
	log.Warnln(format, args...)
}

func (c *ClashLogger) E(format string, args ...interface{}) {
	log.Errorln(format, args...)
}
