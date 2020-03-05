module github.com/kr328/cfa

go 1.13

require (
	github.com/Dreamacro/clash v0.0.0 // local
)	

replace github.com/Dreamacro/clash => ./clash

replace github.com/google/netstack => github.com/comzyh/netstack v0.0.0-20191217044024-67c27819ada4
