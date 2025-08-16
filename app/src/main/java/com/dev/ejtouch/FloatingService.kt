package com.dev.ejtouch

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var mainMenuView: View
    private lateinit var quickActionsView: View

    private lateinit var params: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var isMainMenuVisible = false
    private var isQuickActionsVisible = false


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100
        windowManager.addView(floatingView, params)

        mainMenuView = LayoutInflater.from(this).inflate(R.layout.main_menu_layout, null)
        quickActionsView = LayoutInflater.from(this).inflate(R.layout.quick_actions_layout, null)

        menuParams = WindowManager.LayoutParams(
            dpToPx(200),
            dpToPx(200),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        menuParams.gravity = Gravity.CENTER

        setupTouchListener()
        setupMainMenuListeners()
        setupQuickActionsListeners()
    }

    private fun setupTouchListener() {
        val floatingIcon = floatingView.findViewById<ImageView>(R.id.floating_icon_image)
        floatingIcon.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private val CLICK_DRAG_TOLERANCE = 10f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val screenWidth = windowManager.defaultDisplay.width
                        if (params.x > screenWidth / 2) {
                            params.x = screenWidth
                        } else {
                            params.x = 0
                        }
                        windowManager.updateViewLayout(floatingView, params)

                        val xDiff = event.rawX - initialTouchX
                        val yDiff = event.rawY - initialTouchY
                        if (abs(xDiff) < CLICK_DRAG_TOLERANCE && abs(yDiff) < CLICK_DRAG_TOLERANCE) {
                            if (isMainMenuVisible || isQuickActionsVisible) {
                                hideAllMenus()
                            } else {
                                showMainMenu()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        hideAllMenus()
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupMainMenuListeners() {
        val buttonLock = mainMenuView.findViewById<ImageView>(R.id.button_lock)
        val buttonPowerOff = mainMenuView.findViewById<ImageView>(R.id.button_power_off)
        val buttonRestart = mainMenuView.findViewById<ImageView>(R.id.button_restart)
        val buttonVolumeUp = mainMenuView.findViewById<ImageView>(R.id.button_volume_up)
        val buttonVolumeDown = mainMenuView.findViewById<ImageView>(R.id.button_volume_down)
        val buttonQuickActions = mainMenuView.findViewById<ImageView>(R.id.button_quick_actions)

        buttonLock.setOnClickListener { sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_LOCK_SCREEN); hideAllMenus() }
        buttonPowerOff.setOnClickListener { sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_POWER_DIALOG); hideAllMenus() }
        buttonRestart.setOnClickListener { sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_POWER_DIALOG); hideAllMenus() }
        buttonVolumeUp.setOnClickListener { volumeUp() }
        buttonVolumeDown.setOnClickListener { volumeDown() }
        buttonQuickActions.setOnClickListener {
            hideMainMenu()
            showQuickActionsMenu()
        }
    }

    private fun setupQuickActionsListeners() {
        val buttonBack = quickActionsView.findViewById<ImageView>(R.id.button_back)
        val buttonScreenshot = quickActionsView.findViewById<ImageView>(R.id.button_screenshot)
        val buttonFlashlight = quickActionsView.findViewById<ImageView>(R.id.button_flashlight)
        val buttonWifi = quickActionsView.findViewById<ImageView>(R.id.button_wifi)
        val buttonBluetooth = quickActionsView.findViewById<ImageView>(R.id.button_bluetooth)
        val buttonBrightness = quickActionsView.findViewById<ImageView>(R.id.button_brightness)

        buttonBack.setOnClickListener {
            hideQuickActionsMenu()
            showMainMenu()
        }
        buttonScreenshot.setOnClickListener {
            sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_SCREENSHOT)
            hideAllMenus()
        }
        buttonFlashlight.setOnClickListener {
            toggleFlashlight()
            hideAllMenus()
        }
        buttonWifi.setOnClickListener {
            openWifiSettingsPanel()
            hideAllMenus()
        }
        buttonBluetooth.setOnClickListener {
            openBluetoothSettings()
            hideAllMenus()
        }
        buttonBrightness.setOnClickListener {
            cycleBrightness()
        }
    }

    private fun showMainMenu() {
        try {
            if (!isMainMenuVisible) {
                menuParams.x = 0
                menuParams.y = 0
                windowManager.addView(mainMenuView, menuParams)
                isMainMenuVisible = true
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideMainMenu() {
        try {
            if (isMainMenuVisible) {
                windowManager.removeView(mainMenuView)
                isMainMenuVisible = false
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showQuickActionsMenu() {
        try {
            if (!isQuickActionsVisible) {
                menuParams.x = 0
                menuParams.y = 0
                windowManager.addView(quickActionsView, menuParams)
                isQuickActionsVisible = true
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideQuickActionsMenu() {
        try {
            if (isQuickActionsVisible) {
                windowManager.removeView(quickActionsView)
                isQuickActionsVisible = false
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun hideAllMenus() {
        hideMainMenu()
        hideQuickActionsMenu()
    }

    private fun sendActionToAccessibilityService(action: String) {
        val intent = Intent(this, ejTouchAccessibilityService::class.java)
        intent.action = action
        startService(intent)
    }

    private fun volumeUp() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, 0)
    }

    private fun volumeDown() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER, 0)
    }

    private fun toggleFlashlight() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val isFlashlightOn = floatingView.tag as? Boolean ?: false
            cameraManager.setTorchMode(cameraId, !isFlashlightOn)
            floatingView.tag = !isFlashlightOn
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openWifiSettingsPanel() {
        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun cycleBrightness() {
        if (!Settings.System.canWrite(applicationContext)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please grant permission to modify settings", Toast.LENGTH_LONG).show()
            hideAllMenus()
            return
        }

        val contentResolver = contentResolver
        try {
            val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            val newBrightness = when {
                currentBrightness < 60 -> 150
                currentBrightness < 200 -> 255
                else -> 50
            }
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideAllMenus()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}