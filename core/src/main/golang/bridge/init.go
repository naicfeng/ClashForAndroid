package bridge

import (
	"github.com/Dreamacro/clash/component/mmdb"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/config"
	"sync"
)

var (
	logCallback  LogCallback
	logSubscribe sync.Once
)

type LogCallback interface {
	OnLogEvent(level, payload string)
}

func InitCore(geoipDatabase[] byte, homeDir string, version string) {
	dataClone := make([]byte, len(geoipDatabase))
	copy(dataClone, geoipDatabase)

	mmdb.LoadFromBytes(dataClone)
	C.SetHomeDir(homeDir)
	config.ApplicationVersion = version

	Reset()

	log.Infoln("Initialed")
}

func Reset() {
	config.LoadDefault()
	tunnel.DefaultManager.ResetStatistic()
}

func SetLogCallback(callback LogCallback) {
	logSubscribe.Do(func() {
		go func() {
			sub := log.Subscribe()
			defer log.UnSubscribe(sub)

			for {
				elm := <-sub
				l := elm.(*log.Event)

				if l.LogLevel < log.Level() {
					continue
				}

				if cb := logCallback; cb != nil {
					cb.OnLogEvent(l.LogLevel.String(), l.Payload)
				}
			}
		}()
	})

	logCallback = callback
}
