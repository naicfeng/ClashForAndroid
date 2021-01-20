#include "main.h"

#include "event_queue.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_startTunDevice(JNIEnv *env, jclass clazz, jint fd,
                                                              jint mtu, jstring gateway,
                                                              jstring mirror, jstring dns,
                                                              jobject callback) {
    UNUSED(clazz);

    Master::runWithContext<void>(env, [&](Master::Context *context) {
        const char *gatewayString = context->getString(gateway);
        const char *mirrorString = context->getString(mirror);
        const char *dnsString = context->getString(dns);

        jobject callbackGlobal = context->newGlobalReference(callback);
        uint64_t token = EventQueue::getInstance()->obtainToken();

        EventQueue::getInstance()->registerHandler(NEW_SOCKET, token, [callbackGlobal](const event_t *e) {
            Master::runWithAttached<int>([&](JNIEnv *env) -> int {
                Master::runWithContext<void>(env, [&](Master::Context *context) {
                    context->tunCallbackNewSocket(callbackGlobal, static_cast<int>(strtol(e->payload, nullptr, 10)));
                });

                return 0;
            });
        });

        EventQueue::getInstance()->registerHandler(TUN_STOP, token, [callbackGlobal](const event_t *e) {
            auto queue = EventQueue::getInstance();

            queue->unregisterHandler(NEW_SOCKET, e->token);
            queue->unregisterHandler(TUN_STOP, e->token);

            Master::runWithAttached<int>([&](JNIEnv *env) -> int {
                Master::runWithContext<void>(env, [&](Master::Context *context) {
                    context->tunCallbackStop(callbackGlobal);
                    context->removeGlobalReference(callbackGlobal);
                });

                return 0;
            });
        });

        char *exception = startTunDevice(fd, mtu, gatewayString, mirrorString, dnsString, token);

        context->releaseString(gateway, gatewayString);
        context->releaseString(mirror, mirrorString);
        context->releaseString(dns, dnsString);

        if ( exception != nullptr ) {
            context->throwThrowable(context->newClashException(exception));
            context->removeGlobalReference(callbackGlobal);
            free(exception);
        }
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_stopTunDevice(JNIEnv *env, jclass clazz) {
    stopTunDevice();
}