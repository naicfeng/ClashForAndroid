#pragma once

#if __cplusplus
extern "C" {
#endif

static const int MODE_UNKNOWN = -1;
static const int MODE_DIRECT = 0;
static const int MODE_GLOBAL = 1;
static const int MODE_RULE = 2;
static const int MODE_SCRIPT = 3;

typedef struct general_t {
    int mode;
    int http_port;
    int socks_port;
    int redirect_port;
    int mixed_port;
} general_t;

#if __cplusplus
};
#endif