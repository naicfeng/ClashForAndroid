#pragma once

#if __cplusplus
extern "C" {
#endif

typedef enum proxy_type_t {
    Direct, Reject, Socks5, Http, Shadowsocks, Vmess, Snell, Trojan, Selector, Fallback, LoadBalance, URLTest, Relay, Unknown
} proxy_type_t;

typedef struct proxy_t {
    long name_index;
    proxy_type_t proxy_type;
    long delay;
} proxy_t;

typedef struct proxy_group_t {
    proxy_t base;
    int now;
    int proxies_size;
    proxy_t proxies[];
} proxy_group_t;

typedef struct proxy_group_list_t {
    int size;
    char *string_pool;
    proxy_group_t *groups[];
} proxy_group_list_t;

#if __cplusplus
};
#endif

