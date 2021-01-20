package main

import (
	"github.com/Dreamacro/clash/component/mmdb"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/config"
	"unsafe"
)

/*
#cgo CFLAGS: -O3
#cgo LDFLAGS: -llog
#include "buffer.h"
#include "malloc.h"

extern void log_info(const char *msg);
extern void log_error(const char *msg);
extern void log_warn(const char *msg);
extern void log_debug(const char *msg);
extern void log_verbose(const char *msg);
 */
import "C"

func main() {
	panic("Only for linking")
}

func init() {
	r := make(chan struct{})

	go func() {
		sub := log.Subscribe()
		defer log.UnSubscribe(sub)

		close(r)

		for item := range sub {
			msg := item.(*log.Event)

			if msg.LogLevel < log.Level() {
				continue
			}

			cPayload := C.CString(msg.Payload)

			switch msg.LogLevel {
			case log.INFO:
				C.log_info(cPayload)
			case log.ERROR:
				C.log_error(cPayload)
			case log.WARNING:
				C.log_warn(cPayload)
			case log.DEBUG:
				C.log_debug(cPayload)
			case log.SILENT:
				C.log_verbose(cPayload)
			}

			C.free(unsafe.Pointer(cPayload))
		}
	}()

	<- r
}

//export initialize
func initialize(database *C.const_buffer_t, home, version C.const_string_t) {
	databaseData := C.GoBytes(database.buffer, database.length)
	homeData := C.GoString(home)
	versionData := C.GoString(version)

	mmdb.LoadFromBytes(databaseData)
	constant.SetHomeDir(homeData)
	config.ApplicationVersion = versionData
}

//export reset
func reset() {
	config.LoadDefault()
	tunnel.DefaultManager.ResetStatistic()
}