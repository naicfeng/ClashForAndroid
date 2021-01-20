#pragma once

#if __cplusplus
extern "C" {
#endif

typedef struct dns_override_t {
    int override_dns;
    const char *append_dns;
} dns_override_t;

#if __cplusplus
};
#endif