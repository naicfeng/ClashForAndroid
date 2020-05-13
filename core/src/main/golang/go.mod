module github.com/kr328/cfa

go 1.14

require (
	github.com/Dreamacro/clash v0.0.0 // local
	github.com/kr328/tun2socket v0.0.0-20200511061008-edd2b9608763
	github.com/miekg/dns v1.1.29
)

replace github.com/Dreamacro/clash => ./clash
