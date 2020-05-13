package com.github.kr328.clash.common.ids

object NotificationIds {
    const val CLASH_STATUS = 1
    const val PROFILE_STATUS = 2
    private val PROFILE_RESULT = 10000..20000

    fun generateProfileResultId(profileId: Long): Int {
        val bound = PROFILE_RESULT.last - PROFILE_RESULT.first
        return (profileId % bound + PROFILE_RESULT.first).toInt()
    }
}