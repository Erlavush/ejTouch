package com.dev.ejtouch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // Launcher for the Overlay Permission result
    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (hasOverlayPermission()) {
            Log.d(TAG, "Overlay permission granted. Now checking Accessibility permission.")
            checkAccessibilityPermission() // The next step in our sequence
        } else {
            handlePermissionDenied("Overlay Permission")
        }
    }

    // Launcher for the Accessibility Permission result
    private val accessibilityPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isAccessibilityServiceEnabled()) {
            Log.d(TAG, "Accessibility Service has been enabled by the user.")
            Toast.makeText(this, "All permissions granted! Service starting.", Toast.LENGTH_SHORT).show()
            startService(Intent(this, FloatingService::class.java))
        } else {
            handlePermissionDenied("Accessibility Service")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.d(TAG, "Switch ON. Starting permission checks.")
                checkOverlayPermission()
            } else {
                Log.d(TAG, "Switch OFF. Stopping service.")
                stopService(Intent(this, FloatingService::class.java))
            }
        }
    }

    override fun onResume() {

        super.onResume()
        // When the user returns to the app, sync the switch state with the actual permissions
        val masterSwitch = findViewById<MaterialSwitch>(R.id.master_switch)
        masterSwitch.isChecked = false
        masterSwitch.isChecked = hasOverlayPermission() && isAccessibilityServiceEnabled()
    }

    private fun checkOverlayPermission() {
        if (hasOverlayPermission()) {
            Log.d(TAG, "Overlay permission is already granted. Checking Accessibility next.")
            checkAccessibilityPermission()
        } else {
            Log.d(TAG, "Overlay permission not granted. Requesting it.")
            requestOverlayPermission()
        }
    }

    private fun checkAccessibilityPermission() {
        if (isAccessibilityServiceEnabled()) {
            Log.d(TAG, "Accessibility Service is already enabled.")
            Toast.makeText(this, "All permissions present. Service starting.", Toast.LENGTH_SHORT).show()
            startService(Intent(this, FloatingService::class.java))
        } else {
            Log.d(TAG, "Accessibility Service not enabled. Requesting it.")
            requestAccessibilityPermission()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceId = "$packageName/${ejTouchAccessibilityService::class.java.canonicalName}"
        val settingValue = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return settingValue?.let {
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(it)
            while (splitter.hasNext()) {
                if (splitter.next().equals(serviceId, ignoreCase = true)) {
                    return@let true
                }
            }
            false
        } ?: false
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        Toast.makeText(this, "Please find 'ejTouch Service' and enable it.", Toast.LENGTH_LONG).show()
        accessibilityPermissionLauncher.launch(intent)
    }

    private fun handlePermissionDenied(permissionName: String) {
        Log.d(TAG, "$permissionName was NOT granted by the user.")
        Toast.makeText(this, "$permissionName Denied. The service cannot start.", Toast.LENGTH_SHORT).show()
        findViewById<MaterialSwitch>(R.id.master_switch).isChecked = false
    }
}