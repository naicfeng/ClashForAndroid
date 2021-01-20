package com.github.kr328.clash.core.model

import androidx.annotation.Keep

data class Traffic @Keep constructor(val upload: Long, val download: Long)