#include "main.h"

#include <android/log.h>

static std::pair<jobject, uint64_t> completableFutureWithToken(Master::Context *context) {
    uint64_t token = EventQueue::getInstance()->obtainToken();
    jobject completableFuture = context->newGlobalReference(context->newCompletableFuture());

    EventQueue::getInstance()->registerHandler(COMPLETE, token, [completableFuture](const event_t *event) {
        EventQueue::getInstance()->unregisterHandler(COMPLETE, event->token);

        Master::runWithAttached<int>([&](JNIEnv *env) -> int {
            Master::runWithContext<void>(env, [&](Master::Context *context) {
                if ( strlen(event->payload) == 0 ) {
                    context->completeCompletableFuture(completableFuture, nullptr);
                } else {
                    context->completeExceptionallyCompletableFuture(completableFuture, context->newClashException(event->payload));
                }

                context->removeGlobalReference(completableFuture);
            });

            return 0;
        });
    });

    return {completableFuture, token};
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_downloadProfile__ILjava_lang_String_2Ljava_lang_String_2(
        JNIEnv *env, jclass clazz, jint fd, jstring base, jstring output) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *b = context->getString(base);
        const char *o = context->getString(output);

        auto completableFuture = completableFutureWithToken(context);

        downloadProfileFromFd(fd, b, o, completableFuture.second);

        context->releaseString(base, b);
        context->releaseString(output, o);

        return completableFuture.first;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_downloadProfile__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_String_2(
        JNIEnv *env, jclass clazz, jstring url, jstring base, jstring output) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *u = context->getString(url);
        const char *b = context->getString(base);
        const char *o = context->getString(output);

        auto completableFuture = completableFutureWithToken(context);

        downloadProfileFromUrl(u, b, o, completableFuture.second);

        context->releaseString(url, u);
        context->releaseString(base, b);
        context->releaseString(output, o);

        return completableFuture.first;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_loadProfile(JNIEnv *env, jclass clazz, jstring path,
                                                           jstring base) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *p = context->getString(path);
        const char *b = context->getString(base);

        auto completableFuture = completableFutureWithToken(context);

        loadProfile(p, b, completableFuture.second);

        context->releaseString(path, p);
        context->releaseString(base, b);

        return completableFuture.first;
    });
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_performHealthCheck(JNIEnv *env, jclass clazz,
                                                                  jstring group) {
    UNUSED(clazz);

    return Master::runWithContext<jobject>(env, [&](Master::Context *context) -> jobject {
        const char *g = context->getString(group);

        auto completableFuture = completableFutureWithToken(context);

        performHealthCheck(g, completableFuture.second);

        context->releaseString(group, g);

        return completableFuture.first;
    });
}