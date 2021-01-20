#include "main.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_initialize(JNIEnv *env, jclass clazz,
                                                          jbyteArray database, jstring home,
                                                          jstring version) {
    UNUSED(clazz);

    Master::runWithContext<void>(env, [&](Master::Context *context) {
        const_buffer_t databaseBuffer = context->createConstBufferFromByteArray(database);
        const char *homeString = context->getString(home);
        const char *versionString = context->getString(version);

        initialize(&databaseBuffer, homeString, versionString);

        context->releaseConstBufferFromByteArray(database, databaseBuffer);
        context->releaseString(home, homeString);
        context->releaseString(version, versionString);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_reset(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    reset();
}