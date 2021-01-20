#include "libclash.h"
#include "main.h"

static jobject logCallback;

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_setLogCallback(JNIEnv *env, jclass clazz,
                                                              jobject callback) {
    UNUSED(clazz);

    Master::runWithContext<void>(env, [&](Master::Context *context) {
        if ( logCallback != nullptr )
            context->removeGlobalReference(logCallback);

        if ( callback == nullptr ) {
            logCallback = nullptr;
            return;
        }

        logCallback = context->newGlobalReference(callback);

        EventQueue::getInstance()->registerHandler(LOG_RECEIVED, 0, [](const event_t *event) {
            Master::runWithAttached<int>([&](JNIEnv *env) {
                Master::runWithContext<void>(env, [&](Master::Context *context) {
                    context->logCallbackMessage(logCallback, event->payload);
                });

                return 0;
            });
        });
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_enableLogReport(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    enableLogReport();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_disableLogReport(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    disableLogReport();
}