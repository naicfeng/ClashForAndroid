#include "main.h"

#include <cstring>

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_setProxyMode(JNIEnv *env, jclass clazz,
                                                            jstring proxy_mode) {
    Master::runWithContext<void>(env, [&](Master::Context *context) {
        const char *m = context->getString(proxy_mode);
        int mode;

        if ( strcmp(m, "Direct") == 0 )
            mode = MODE_DIRECT;
        else if ( strcmp(m, "Global") == 0 )
            mode = MODE_GLOBAL;
        else if ( strcmp(m, "Rule") == 0 )
            mode = MODE_RULE;
        else if ( strcmp(m, "Script") == 0 )
            mode = MODE_SCRIPT;
        else
            mode = MODE_UNKNOWN;

        setProxyMode(mode);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_setDnsOverride(JNIEnv *env, jclass clazz,
                                                              jboolean override_dns,
                                                              jstring append_dns) {
    Master::runWithContext<void>(env, [&](Master::Context *context) {
        const char *appendDns = context->getString(append_dns);
        int override = 1;

        if ( override_dns )
            override = 1;
        else
            override = 0;

        dns_override_t dns = {
                .override_dns = override,
                .append_dns = appendDns
        };

        setDnsOverride(&dns);

        context->releaseString(append_dns, appendDns);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_github_kr328_clash_core_bridge_Bridge_setSelector(JNIEnv *env, jclass clazz, jstring group,
                                                           jstring selected) {
    UNUSED(clazz);

    return Master::runWithContext<bool>(env, [&](Master::Context *context) -> bool {
        const char *g = context->getString(group);
        const char *s = context->getString(selected);

        int r = setSelector(g, s);

        context->releaseString(group, g);
        context->releaseString(selected, s);

        return r == 0;
    });
}