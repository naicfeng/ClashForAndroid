package main

//#include "event.h"
import "C"
import (
	"fmt"
	"github.com/Dreamacro/clash/log"
	"sync"
)

var logLocker sync.Mutex
var closeChan chan struct{}

//export enableLogReport
func enableLogReport() {
	logLocker.Lock()
	defer logLocker.Unlock()


	if closeChan != nil {
		close(closeChan)
	}

	closeChan = make(chan struct{})

	go func(closed chan struct{}) {
		subscriber := log.Subscribe()
		defer log.UnSubscribe(subscriber)
		defer log.Infoln("Log broadcast disabled")

		for {
			select {
			case item := <-subscriber:
				msg := item.(*log.Event)

				if msg.LogLevel < log.Level() {
					continue
				}

				<- sendEvent(C.LOG_RECEIVED, 0, fmt.Sprintf("%s:%s", msg.LogLevel.String(), msg.Payload)).result
			case <-closeChan:
				return
			}
		}
	}(closeChan)
}

//export disableLogReport
func disableLogReport() {
	logLocker.Lock()
	defer logLocker.Unlock()

	if closeChan != nil {
		close(closeChan)

		closeChan = nil
	}
}
