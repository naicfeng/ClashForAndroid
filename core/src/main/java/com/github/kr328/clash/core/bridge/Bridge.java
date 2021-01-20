package com.github.kr328.clash.core.bridge;

import androidx.annotation.Keep;

import com.github.kr328.clash.core.model.General;
import com.github.kr328.clash.core.model.ProxyGroup;
import com.github.kr328.clash.core.model.Traffic;

import java.util.concurrent.CompletableFuture;

@Keep
public final class Bridge {
    static {
        System.loadLibrary("bridge");
    }

    private Bridge() {
    }

    public static native void initialize(byte[] database, String home, String version);
    public static native void reset();

    public static native General queryGeneral();
    public static native Traffic querySpeed();
    public static native Traffic queryBandwidth();
    public static native ProxyGroup[] queryProxyGroups();

    public static native void startTunDevice(int fd, int mtu, String gateway, String mirror, String dns, TunCallback callback) throws ClashException;
    public static native void stopTunDevice();

    public static native void setDnsOverride(boolean overrideDns, String appendNameservers);
    public static native void setProxyMode(String mode);
    public static native boolean setSelector(String group, String selected);

    public static native CompletableFuture<Object> downloadProfile(String url, String base, String output);
    public static native CompletableFuture<Object> downloadProfile(int fd, String base, String output);

    public static native CompletableFuture<Object> loadProfile(String path, String base);
    public static native CompletableFuture<Object> performHealthCheck(String group);

    public static native void setLogCallback(LogCallback callback);
    public static native void enableLogReport();
    public static native void disableLogReport();
}
