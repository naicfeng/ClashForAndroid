package com.github.kr328.clash.core.bridge;

import androidx.annotation.Keep;

import com.github.kr328.clash.core.event.LogEvent;

@Keep
@SuppressWarnings("unused")
public interface LogCallback {
    void onMessage(LogEvent event);
}
