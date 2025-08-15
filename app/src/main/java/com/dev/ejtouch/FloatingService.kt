package com.dev.ejtouch

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var mainMenuView: View

    private lateinit var params: WindowManager.LayoutParams
    private lateinit var menuParams: WindowManager.LayoutParams

    private var isMenuVisible = false

    // THIS IS THE MISSING FUNCTION THAT WE ARE ADDING BACK
    override fun onBind(intent: Intent?): IBinder? {
        // We don't need to bind to this service, so we return null
        return null
    }

    // Function to convert DP to Pixels
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

        // --- Setup for the main floating icon (Bubble) ---
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

        // --- Setup for the Main Menu ---
        mainMenuView = LayoutInflater.from(this).inflate(R.layout.main_menu_layout, null)
        menuParams = WindowManager.LayoutParams(
            dpToPx(200),
            dpToPx(200),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // --- Touch Listener for the Bubble ---
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
                            toggleMainMenu()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isMenuVisible) {
                            hideMainMenu()
                        }
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        mainMenuView.findViewById<View>(R.id.main_menu_container).setOnClickListener {
            val buttonLock = mainMenuView.findViewById<ImageView>(R.id.button_lock)
            val buttonPowerOff = mainMenuView.findViewById<ImageView>(R.id.button_power_off)
            val buttonRestart = mainMenuView.findViewById<ImageView>(R.id.button_restart)

            buttonLock.setOnClickListener {
                sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_LOCK_SCREEN)
                hideMainMenu()
            }

            buttonPowerOff.setOnClickListener {
                sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_POWER_DIALOG)
                hideMainMenu()
            }

            buttonRestart.setOnClickListener {
                // Change the action to open the power dialog instead
                sendActionToAccessibilityService(ejTouchAccessibilityService.ACTION_POWER_DIALOG)
                hideMainMenu()
            }
        }
    }

    private fun toggleMainMenu() {
        if (isMenuVisible) {
            hideMainMenu()
        } else {
            showMainMenu()
        }
    }

    private fun showMainMenu() {
        try {
            val bubbleWidth = floatingView.width
            val bubbleHeight = floatingView.height

            menuParams.x = params.x - (dpToPx(200) / 2) + (bubbleWidth / 2)
            menuParams.y = params.y - (dpToPx(200) / 2) + (bubbleHeight / 2)

            windowManager.addView(mainMenuView, menuParams)
            isMenuVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideMainMenu() {
        try {
            if (isMenuVisible) {
                windowManager.removeView(mainMenuView)
                isMenuVisible = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideMainMenu()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    private fun sendActionToAccessibilityService(action: String) {
        val intent = Intent(this, ejTouchAccessibilityService::class.java)
        intent.action = action
        startService(intent)
    }
}