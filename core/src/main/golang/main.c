#include <android/log.h>

#define TAG "ClashForAndroid"

void log_info(const char *msg) {
    __android_log_write(ANDROID_LOG_INFO, TAG, msg);
}

void log_error(const char *msg) {
    __android_log_write(ANDROID_LOG_ERROR, TAG, msg);
}

void log_warn(const char *msg) {
    __android_log_write(ANDROID_LOG_WARN, TAG, msg);
}

void log_debug(const char *msg) {
    __android_log_write(ANDROID_LOG_DEBUG, TAG, msg);
}

void log_verbose(const char *msg) {
    __android_log_write(ANDROID_LOG_VERBOSE, TAG, msg);
}

