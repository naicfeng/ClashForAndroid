package com.github.kr328.clash.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle

class ApplicationObserver(val stateChanged: (Boolean) -> Unit) {
    private var applicationRunning = false
        private set(value) {
            if ( field != value )
                stateChanged(value)

            field = value
        }
    private var activityCount: Int = 0

    private val activityObserver = object: Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {
            synchronized(this) {
                activityCount--
                applicationRunning = activityCount > 0
            }
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            synchronized(this) {
                activityCount++
                applicationRunning = activityCount > 0
            }
        }
    }

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(activityObserver)
    }
}