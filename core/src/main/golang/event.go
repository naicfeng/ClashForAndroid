package main

//#include "event.h"
import "C"
import (
	"sync"
	"unsafe"
)

type EventWaiter struct {
	result chan struct{}
}

var idLock = sync.Mutex{}
var currentId = int64(0)
var ids = map[int64]*EventWaiter{}

//export answerEvent
func answerEvent(id int64) {
	idLock.Lock()
	defer idLock.Unlock()

	waiter, ok := ids[id]
	if ok {
		close(waiter.result)
		delete(ids, id)
	}
}

func sendEvent(t C.event_type_t, token uint64, payload string) *EventWaiter {
	idLock.Lock()
	defer idLock.Unlock()

	currentId++

	id := currentId
	r := &EventWaiter{make(chan struct{})}

	ids[id] = r

	p := append([]byte(payload), 0)
	e := allocCEvent(len(p))

	e.id = C.int64_t(id)
	e._type = t
	e.token = C.int64_t(token)

	C.send_event(e, unsafe.Pointer(&p[0]), C.size_t(len(p)))

	return r
}

func allocCEvent(payloadLength int) *C.event_t {
	return (*C.event_t)(C.malloc(C.sizeof_event_t + C.size_t(payloadLength)))
}

