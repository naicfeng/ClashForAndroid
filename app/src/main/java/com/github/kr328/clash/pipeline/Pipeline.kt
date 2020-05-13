package com.github.kr328.clash.pipeline

import com.github.kr328.clash.common.settings.BaseSettings

data class Pipeline<T>(val input: T, val settings: BaseSettings)