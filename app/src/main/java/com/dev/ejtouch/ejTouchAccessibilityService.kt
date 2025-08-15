package com.dev.ejtouch

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ejTouchAccessibilityService : AccessibilityService() {

    companion object {
        // We create a companion object to hold action constants
        const val ACTION_LOCK_SCREEN = "ACTION_LOCK_SCREEN"
        const val ACTION_POWER_DIALOG = "ACTION_POWER_DIALOG"
        const val ACTION_RESTART = "ACTION_RESTART" // Note: Restart can be unreliable
    }

    private val TAG = "ejTouchService"

    // This function is now called when we send an Intent to the service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_LOCK_SCREEN -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            ACTION_POWER_DIALOG -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)

            // We will add more actions here later
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for our use case
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted.")
    }
}