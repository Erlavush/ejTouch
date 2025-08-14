package com.dev.ejtouch

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ejTouchAccessibilityService : AccessibilityService() {

    private val TAG = "ejTouchService"

    // This function is called when the system first connects to our service.
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected.")
    }

    // This function is called for every event that happens on the screen.
    // We don't need to do anything here for our app, but we must have the function.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Log.d(TAG, "onAccessibilityEvent: ${event?.eventType}")
    }

    // This function is called when the system wants to interrupt our service.
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted.")
    }
}