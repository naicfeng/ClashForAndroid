#pragma once

#include <pthread.h>
#include <vector>
#include <map>
#include <functional>
#include <cstdint>
#include <string>

#include "event.h"

#define DEFAULT_EVENT_QUEUE_PROCESSES 8

class EventQueue {
public:
    typedef std::function<void (const event_t *event)> Handler;

public:
    EventQueue();

public:
    void enqueueEvent(const event_t *event);
    const event_t *dequeueEvent();

public:
    void registerHandler(event_type_t type, uint64_t token, const Handler& handler);
    void unregisterHandler(event_type_t type, uint64_t token);
    Handler findHandler(event_type_t type, uint64_t token);

public:
    uint64_t obtainToken();

public:
    static EventQueue *getInstance();

public:
    inline bool isClosed() {
        return closed;
    }

private:
    bool closed;
    std::vector<const event_t*> queue;
    std::map<event_type_t, std::map<uint64_t, Handler>> handlers;
    pthread_mutex_t lock;
    pthread_cond_t condition;
    uint64_t currentToken;

public:
    static EventQueue *instance;
};