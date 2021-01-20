#pragma once

#include <stdint.h>

#if __cplusplus
extern "C" {
#endif

typedef struct buffer_t {
    void *buffer;
    int length;
} buffer_t;

typedef struct const_buffer_t {
    const void *buffer;
    int length;
} const_buffer_t;

typedef const char *const_string_t;

#if __cplusplus
};
#endif