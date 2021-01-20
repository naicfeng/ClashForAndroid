#pragma once

#include <stdint.h>

#if __cplusplus
extern "C" {
#endif

typedef struct traffic_t {
    int64_t upload;
    int64_t download;
} traffic_t;

#if __cplusplus
};
#endif