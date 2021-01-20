#pragma once

#include <stdint.h>
#include <stddef.h>

#if __cplusplus
extern "C" {
#endif

typedef enum {
    NEW_SOCKET, TUN_STOP, COMPLETE, LOG_RECEIVED
} event_type_t;

typedef struct event_t {
    int64_t id;
    int64_t token;
    event_type_t type;
    char payload[];
} event_t;

typedef void (*event_handler_t)(const event_t *event);

void set_event_handler(event_handler_t handler);
void send_event(event_t *event, const void *payload, size_t payload_length);
void answer_event(const event_t *event);

#if __cplusplus
};
#endif