package config

var defaultFakeIPFilter = []string{
	// stun services
	"+.stun.*.*",
	"+.stun.*.*.*",
	"+.stun.*.*.*.*",

	// Google Voices
	"lens.l.google.com",
	"stun.l.google.com",

	// Nintendo Switch
	"*.n.n.srv.nintendo.net",
}
