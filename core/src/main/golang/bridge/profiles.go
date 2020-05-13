package bridge

import (
	"strings"

	"github.com/kr328/cfa/config"
)

func ResetDnsAppend(dns string) {
	if len(dns) == 0 {
		config.NameServersAppend = make([]string, 0)
	} else {
		config.NameServersAppend = strings.Split(dns, ",")
	}
}

func SetDnsOverrideEnabled(enabled bool) {
	if enabled {
		config.DnsPatch = config.OptionalDnsPatch
	} else {
		config.DnsPatch = nil
	}
}

func LoadProfileFile(path, baseDir string, callback DoneCallback) {
	go func() {
		call(config.LoadFromFile(path, baseDir), callback)
	}()
}

func DownloadProfileAndCheck(url, output, baseDir string, callback DoneCallback) {
	go func() {
		call(config.PullRemote(url, output, baseDir), callback)
	}()
}

func ReadProfileAndCheck(fd int, output, baseDir string, callback DoneCallback) {
	go func() {
		call(config.PullLocal(fd, output, baseDir), callback)
	}()
}

func call(err error, callback DoneCallback) {
	if err != nil {
		callback.DoneWithError(err)
	} else {
		callback.Done()
	}
}
