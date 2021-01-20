#include <jni.h>
#include <string>
#include <functional>
#include <cstdint>

#include "libclash.h"
#include "event_queue.h"

#define UNUSED(v) ((void)v)

class Master {
public:
    class Context;

public:
    Master(JavaVM *vm, JNIEnv *env);

public:
    template <class R> static R runWithContext(JNIEnv *env, const std::function<R (Context *)>& func);
    template <class R> static R runWithAttached(const std::function<R (JNIEnv *)> &func);

private:
    jclass cClashException;
    jclass cTraffic;
    jclass cGeneral;
    jclass cCompletableFuture;
    jclass cProxyGroup;
    jclass cProxy;
    jclass cLogEvent;
    jclass iTunCallback;
    jclass iLogCallback;
    jmethodID cClashExceptionConstructor;
    jmethodID cTrafficConstructor;
    jmethodID cGeneralConstructor;
    jmethodID cCompletableFutureConstructor;
    jmethodID cProxyGroupConstructor;
    jmethodID cProxyConstructor;
    jmethodID cLogEventConstructor;
    jmethodID mCompletableFutureComplete;
    jmethodID mCompletableFutureCompleteExceptionally;
    jmethodID mTunCallbackOnNewSocket;
    jmethodID mTunCallbackOnStop;
    jmethodID mLogCallbackOnMessage;

private:
    jstring sDirect;
    jstring sReject;
    jstring sShadowsocks;
    jstring sSnell;
    jstring sSocks5;
    jstring sHttp;
    jstring sVmess;
    jstring sTrojan;
    jstring sRelay;
    jstring sSelector;
    jstring sFallback;
    jstring sURLTest;
    jstring sLoadBalance;
    jstring sUnknown;

private:
    JavaVM *vm;

private:
    static Master *master;

private:
    friend class Context;
};

class Master::Context {
public:
public:
    Context(JNIEnv *env);

public:
    jthrowable newClashException(const char *message);
    jobject newTraffic(jlong upload, jlong download);
    jobject newGeneral(char const *mode, jint http, jint socks, jint redirect, jint mixed);
    jobject newCompletableFuture();

public:
    void throwThrowable(jthrowable throwable);

public:
    jobject newGlobalReference(jobject obj);
    jobject removeGlobalReference(jobject obj);

public:
    bool completeCompletableFuture(jobject completable, jobject object);
    bool completeExceptionallyCompletableFuture(jobject completable, jthrowable throwable);
    void tunCallbackNewSocket(jobject callback, jint fd);
    void tunCallbackStop(jobject callback);
    void logCallbackMessage(jobject callback, const char *data);

public:
    jobject createProxy(char const *name, proxy_type_t type, jlong delay);
    jobject createProxyGroup(char const *name, proxy_type_t type, char const *current, jobjectArray proxies);
    jobjectArray createProxyArray(int size, jobject elements[]);
    jobjectArray createProxyGroupArray(int size, jobject elements[]);
    const_buffer_t createConstBufferFromByteArray(jbyteArray array);
    void releaseConstBufferFromByteArray(jbyteArray array, const_buffer_t &buffer);
    const char *getString(jstring str);
    void releaseString(jstring str, const char *c);

private:
    JNIEnv *env;
};

template <class R>
R Master::runWithContext(JNIEnv *env, const std::function<R (Master::Context *)>& func) {
    Master::Context context(env);

    return func(&context);
}

template <class R> R Master::runWithAttached(const std::function<R (JNIEnv *)> &func) {
    Master *m = Master::master;

    JNIEnv *env;

    m->vm->AttachCurrentThread(&env, nullptr);

    R result = func(env);

    m->vm->DetachCurrentThread();

    return result;
}