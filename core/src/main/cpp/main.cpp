#include "main.h"

#include <android/log.h>

Master *Master::master = nullptr;

template <class T>
inline T g(JNIEnv *env, T object) {
    return reinterpret_cast<T>(env->NewGlobalRef(object));
}

Master::Master(JavaVM *vm, JNIEnv *env): vm(vm) {
    master = this;

    cClashException = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/bridge/ClashException"));
    cTraffic = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/model/Traffic"));
    cGeneral = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/model/General"));
    cCompletableFuture = g<jclass>(env, env->FindClass("java/util/concurrent/CompletableFuture"));
    cProxyGroup = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/model/ProxyGroup"));
    cProxy = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/model/Proxy"));
    cLogEvent = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/event/LogEvent"));
    iTunCallback = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/bridge/TunCallback"));
    iLogCallback = g<jclass>(env, env->FindClass("com/github/kr328/clash/core/bridge/LogCallback"));
    cClashExceptionConstructor = env->GetMethodID(cClashException, "<init>",
                                                  "(Ljava/lang/String;)V");
    cTrafficConstructor = env->GetMethodID(cTraffic, "<init>", "(JJ)V");
    cGeneralConstructor = env->GetMethodID(cGeneral, "<init>", "(Ljava/lang/String;IIII)V");
    cCompletableFutureConstructor = env->GetMethodID(cCompletableFuture, "<init>", "()V");
    cProxyGroupConstructor = env->GetMethodID(cProxyGroup, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lcom/github/kr328/clash/core/model/Proxy;)V");
    cProxyConstructor = env->GetMethodID(cProxy, "<init>", "(Ljava/lang/String;Ljava/lang/String;J)V");
    cLogEventConstructor = env->GetMethodID(cLogEvent, "<init>", "(Ljava/lang/String;)V");
    mCompletableFutureComplete = env->GetMethodID(cCompletableFuture, "complete",
                                                  "(Ljava/lang/Object;)Z");
    mCompletableFutureCompleteExceptionally = env->GetMethodID(cCompletableFuture,
                                                               "completeExceptionally",
                                                               "(Ljava/lang/Throwable;)Z");
    mTunCallbackOnNewSocket = env->GetMethodID(iTunCallback, "onNewSocket", "(I)V");
    mTunCallbackOnStop = env->GetMethodID(iTunCallback, "onStop", "()V");
    mLogCallbackOnMessage = env->GetMethodID(iLogCallback, "onMessage", "(Lcom/github/kr328/clash/core/event/LogEvent;)V");

    sDirect = g<jstring>(env, env->NewStringUTF("Direct"));
    sReject = g<jstring>(env, env->NewStringUTF("Reject"));
    sShadowsocks = g<jstring>(env, env->NewStringUTF("Shadowsocks"));
    sSnell = g<jstring>(env, env->NewStringUTF("Snell"));
    sSocks5 = g<jstring>(env, env->NewStringUTF("Socks5"));
    sHttp = g<jstring>(env, env->NewStringUTF("Http"));
    sVmess = g<jstring>(env, env->NewStringUTF("Vmess"));
    sTrojan = g<jstring>(env, env->NewStringUTF("Trojan"));
    sRelay = g<jstring>(env, env->NewStringUTF("Relay"));
    sSelector = g<jstring>(env, env->NewStringUTF("Selector"));
    sFallback = g<jstring>(env, env->NewStringUTF("Fallback"));
    sURLTest = g<jstring>(env, env->NewStringUTF("URLTest"));
    sLoadBalance = g<jstring>(env, env->NewStringUTF("LoadBalance"));
    sUnknown = g<jstring>(env, env->NewStringUTF("Unknown"));
}

Master::Context::Context(JNIEnv *env) {
    this->env = env;
}

jthrowable Master::Context::newClashException(const char *reason) {
    return reinterpret_cast<jthrowable>(env->NewObject(master->cClashException,
                                                       master->cClashExceptionConstructor,
                                                       env->NewStringUTF(reason)));
}

void Master::Context::throwThrowable(jthrowable throwable) {
    env->Throw(throwable);
}

jobject Master::Context::newTraffic(jlong upload, jlong download) {
    return env->NewObject(master->cTraffic, master->cTrafficConstructor, upload, download);
}

jobject Master::Context::newGeneral(char const *mode, jint http, jint socks, jint redirect,
                                    jint mixed) {
    return env->NewObject(master->cGeneral, master->cGeneralConstructor,
                          env->NewStringUTF(mode), http, socks, redirect, mixed);
}

jobject Master::Context::newCompletableFuture() {
    return env->NewObject(master->cCompletableFuture, master->cCompletableFutureConstructor);
}

jobject Master::Context::newGlobalReference(jobject obj) {
    return env->NewGlobalRef(obj);
}

jobject Master::Context::removeGlobalReference(jobject obj) {
    env->DeleteGlobalRef(obj);

    return obj;
}

bool Master::Context::completeCompletableFuture(jobject completable, jobject object) {
    return env->CallBooleanMethod(completable, master->mCompletableFutureComplete, object);
}

bool
Master::Context::completeExceptionallyCompletableFuture(jobject completable, jthrowable throwable) {
    return env->CallBooleanMethod(completable, master->mCompletableFutureCompleteExceptionally,
                                  throwable);
}

const_buffer_t Master::Context::createConstBufferFromByteArray(jbyteArray array) {
    return {
        .buffer = env->GetByteArrayElements(array, nullptr),
        .length = env->GetArrayLength(array)
    };
}

void Master::Context::releaseConstBufferFromByteArray(jbyteArray array, const_buffer_t &buffer) {
    env->ReleaseByteArrayElements(array, const_cast<jbyte*>(reinterpret_cast<const jbyte*>(buffer.buffer)), JNI_ABORT);
}

const char *Master::Context::getString(jstring str) {
    return env->GetStringUTFChars(str, nullptr);
}

void Master::Context::releaseString(jstring str, const char *c) {
    env->ReleaseStringUTFChars(str, c);
}

void Master::Context::tunCallbackNewSocket(jobject callback, jint fd) {
    env->CallVoidMethod(callback, master->mTunCallbackOnNewSocket, fd);
}

void Master::Context::tunCallbackStop(jobject callback) {
    env->CallVoidMethod(callback, master->mTunCallbackOnStop);
}

jobjectArray Master::Context::createProxyGroupArray(int size, jobject elements[]) {
    jobjectArray result = env->NewObjectArray(size, master->cProxyGroup, nullptr);

    for ( int i = 0 ; i < size ; i++ )
        env->SetObjectArrayElement(result, i, elements[i]);

    return result;
}

jobjectArray Master::Context::createProxyArray(int size, jobject elements[]) {
    jobjectArray result = env->NewObjectArray(size, master->cProxy, nullptr);

    for ( int i = 0 ; i < size ; i++ )
        env->SetObjectArrayElement(result, i, elements[i]);

    return result;
}

jobject Master::Context::createProxy(char const *name, proxy_type_t type, jlong delay) {
    jstring ts = nullptr;

    switch (type) {
        case Direct:
            ts = master->sDirect;
            break;
        case Reject:
            ts = master->sReject;
            break;
        case Socks5:
            ts = master->sSocks5;
            break;
        case Http:
            ts = master->sHttp;
            break;
        case Shadowsocks:
            ts = master->sShadowsocks;
            break;
        case Vmess:
            ts = master->sVmess;
            break;
        case Snell:
            ts = master->sSnell;
            break;
        case Trojan:
            ts = master->sTrojan;
            break;
        case Selector:
            ts = master->sSelector;
            break;
        case Fallback:
            ts = master->sFallback;
            break;
        case LoadBalance:
            ts = master->sLoadBalance;
            break;
        case URLTest:
            ts = master->sURLTest;
            break;
        case Relay:
            ts = master->sRelay;
            break;
        case Unknown:
            ts = master->sUnknown;
            break;
        default:
            ts = master->sUnknown;
    }

    return env->NewObject(master->cProxy, master->cProxyConstructor, env->NewStringUTF(name), ts, delay);
}

jobject Master::Context::createProxyGroup(char const *name, proxy_type_t type,
                                          char const *current, jobjectArray proxies) {
    jstring ts = nullptr;

    switch (type) {
        case Selector:
            ts = master->sSelector;
            break;
        case Fallback:
            ts = master->sFallback;
            break;
        case LoadBalance:
            ts = master->sLoadBalance;
            break;
        case URLTest:
            ts = master->sURLTest;
            break;
        case Relay:
            ts = master->sRelay;
            break;
        case Unknown:
            ts = master->sUnknown;
            break;
        default:
            ts = master->sUnknown;
    }

    return env->NewObject(master->cProxyGroup, master->cProxyGroupConstructor, env->NewStringUTF(name), ts, env->NewStringUTF(current), proxies);
}

void Master::Context::logCallbackMessage(jobject callback, const char *data) {
    jobject event = env->NewObject(master->cLogEvent, master->cLogEventConstructor, env->NewStringUTF(data));

    env->CallVoidMethod(callback, master->mLogCallbackOnMessage, event);
}

static void enqueue_event(const event_t *e) {
    EventQueue::getInstance()->enqueueEvent(e);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *unused) {
    UNUSED(unused);

    JNIEnv *env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return -1;

    new Master(vm, env);
    new EventQueue();

    set_event_handler(&enqueue_event);

    return JNI_VERSION_1_6;
}