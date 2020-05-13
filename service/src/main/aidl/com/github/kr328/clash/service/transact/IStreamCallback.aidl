package com.github.kr328.clash.service.transact;

import com.github.kr328.clash.service.transact.ParcelableContainer;

interface IStreamCallback {
    void send(in ParcelableContainer data);
    void complete();
    void completeExceptionally(String reason);
}
