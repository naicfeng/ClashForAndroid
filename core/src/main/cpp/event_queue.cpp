#include "event_queue.h"

static void *handleEventQueue(void *context) {
    auto *queue = reinterpret_cast<EventQueue*>(context);

    while ( !queue->isClosed() ) {
        const event_t *e = queue->dequeueEvent();

        EventQueue::Handler h = queue->findHandler(e->type, e->token);

        h(e);

        answer_event(e);
    }

    return nullptr;
}

EventQueue::EventQueue(): lock(), condition(), closed(false), currentToken(0) {
    instance = this;

    pthread_mutex_init(&lock, nullptr);
    pthread_cond_init(&condition, nullptr);

    for ( int i = 0 ; i < DEFAULT_EVENT_QUEUE_PROCESSES ; i++ ) {
        pthread_t tid = 0;

        if ( pthread_create(&tid, nullptr, &handleEventQueue, this) < 0 )
            abort();
    }
}

void EventQueue::enqueueEvent(const event_t *event) {
    pthread_mutex_lock(&lock);

    queue.push_back(event);

    pthread_cond_signal(&condition);

    pthread_mutex_unlock(&lock);
}

const event_t *EventQueue::dequeueEvent() {
    pthread_mutex_lock(&lock);

    while ( queue.empty() )
        pthread_cond_wait(&condition, &lock);

    auto *result = queue.back();

    queue.pop_back();

    pthread_mutex_unlock(&lock);

    return result;
}

void EventQueue::registerHandler(event_type_t type, uint64_t token, const EventQueue::Handler& handler) {
    pthread_mutex_lock(&lock);

    handlers[type][token] = handler;

    pthread_mutex_unlock(&lock);
}

void EventQueue::unregisterHandler(event_type_t type, uint64_t token) {
    pthread_mutex_lock(&lock);

    handlers[type].erase(token);

    pthread_mutex_unlock(&lock);
}

EventQueue::Handler EventQueue::findHandler(event_type_t type, uint64_t token) {
    pthread_mutex_lock(&lock);

    Handler result = handlers[type][token];

    pthread_mutex_unlock(&lock);

    if (result == nullptr)
        return [](const event_t*){};

    return result;
}

EventQueue *EventQueue::getInstance() {
    return instance;
}

uint64_t EventQueue::obtainToken() {
    pthread_mutex_lock(&lock);

    uint64_t r = currentToken++;

    pthread_mutex_unlock(&lock);

    return r;
}

EventQueue *EventQueue::instance;