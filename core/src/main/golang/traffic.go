package main

/*
#include "traffic.h"
 */
import "C"
import "github.com/Dreamacro/clash/tunnel"

//export querySpeed
func querySpeed(r *C.traffic_t) {
	u, d := tunnel.DefaultManager.Now()

	r.upload = C.int64_t(u)
	r.download = C.int64_t(d)
}

//export queryBandwidth
func queryBandwidth(r *C.traffic_t) {
	u := tunnel.DefaultManager.UploadTotal()
	d := tunnel.DefaultManager.DownloadTotal()

	r.upload = C.int64_t(u)
	r.download = C.int64_t(d)
}