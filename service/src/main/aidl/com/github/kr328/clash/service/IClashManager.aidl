package com.github.kr328.clash.service;

import com.github.kr328.clash.service.transact.IStreamCallback;
import com.github.kr328.clash.core.model.Packet;

interface IClashManager {
    // Control
    void setSelector(String group, String selected);
    void performHealthCheck(String group, IStreamCallback callback);
    void setProxyMode(String mode);

    // Query
    ProxyGroupWrapper queryProxyGroups();
    General queryGeneral();
    long queryBandwidth();

    // Events
    void registerLogListener(String key, IStreamCallback callback);
    void unregisterLogListener(String key);
}
