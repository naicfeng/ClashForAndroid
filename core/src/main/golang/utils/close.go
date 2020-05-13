package utils

import "io"

func CloseSilent(closer io.Closer) {
	_ = closer.Close()
}
