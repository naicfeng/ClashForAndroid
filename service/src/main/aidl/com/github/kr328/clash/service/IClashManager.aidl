package com.github.kr328.clash.service;

import com.github.kr328.clash.service.transact.IStreamCallback;
import com.github.kr328.clash.core.model.Packet;

interface IClashManager {
    // Control
    boolean setSelectProxy(String proxy, String selected);
    void startHealthCheck(String group, IStreamCallback callback);
    void setProxyMode(String mode);

    // Query
    ProxyGroupList queryAllProxies();
    General queryGeneral();
    long queryBandwidth();

    // Events
    void registerLogListener(String key, IStreamCallback callback);
    void unregisterLogListener(String key);
}
