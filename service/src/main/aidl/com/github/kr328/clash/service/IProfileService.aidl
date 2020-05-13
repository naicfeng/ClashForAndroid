package com.github.kr328.clash.service;

import com.github.kr328.clash.service.transact.IStreamCallback;
import com.github.kr328.clash.service.model.Profile;

interface IProfileService {
    long acquireUnused(String type, String source);
    long acquireCloned(long id);
    String acquireTempUri(long id);
    void release(long id);
    void update(long id, in Profile metadata);
    void commit(long id, in IStreamCallback callback);
    void delete(long id);
    void clear(long id);

    Profile queryById(long id);
    Profile[] queryAll();
    Profile queryActive();

    void setActive(long id);
}
