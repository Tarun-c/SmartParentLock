package com.smartparentlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.animation.ObjectAnimator
import android.view.HapticFeedbackConstants

class LockService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var challengeManager: ChallengeManager
    
    private val handler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable { 
        sessionExpiryTime = 0 // Expire strictly
        screenWentOff = false // Reset so overlay shows immediately
        showOverlay(false) 
    }
    
    private var sessionExpiryTime: Long = 0
    private var screenWentOff: Boolean = true // Start as true so first unlock shows overlay
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen turned off - next unlock should show the overlay
                    screenWentOff = true
                    // Cancel the timer since screen is off
                    handler.removeCallbacks(lockRunnable)
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Screen turned on - check if we need to show overlay
                    // For devices without a secure lock screen
                    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    if (!keyguardManager.isKeyguardSecure) {
                        // No lock screen - show overlay immediately when screen turns on
                        if (screenWentOff || System.currentTimeMillis() > sessionExpiryTime) {
                            showOverlay(false)
                        }
                    }
                    // If device has lock screen, wait for ACTION_USER_PRESENT
                }
                Intent.ACTION_USER_PRESENT -> {
                    // User unlocked their device (pattern/PIN/fingerprint)
                    // ALWAYS show overlay after unlocking (if screen was off)
                    // OR if timer expired during active use
                    if (screenWentOff || System.currentTimeMillis() > sessionExpiryTime) {
                        showOverlay(false)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(this)
        challengeManager = ChallengeManager(settingsRepository)
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Listen for screen state changes and user unlock
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_TIMER") {
            if (overlayView == null) {
                 // Only update if currently unlocked (session active)
                 startSessionTimer()
                 Toast.makeText(this, getString(R.string.timer_updated), Toast.LENGTH_SHORT).show()
            }
            return START_STICKY
        }
        
        val forcePin = intent?.getBooleanExtra("FORCE_PIN_CHECK", false) ?: false
        val startHidden = intent?.getBooleanExtra("START_HIDDEN", false) ?: false
        
        if (startHidden) {
            startSessionTimer()
        } else {
            showOverlay(forcePin)
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "lock_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Smart Parent Lock Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showOverlay(isPinMode: Boolean) {
        if (overlayView != null) {
            if (isPinMode) {
                 setupPinChallenge(overlayView!!)
            } else {
                 setupChallenge(overlayView!!)
            }
            return
        }

        try {
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            )
            layoutParams.flags = layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.overlay_view, null)

            if (isPinMode) {
                setupPinChallenge(overlayView!!)
            } else {
                setupChallenge(overlayView!!)
            }

            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun setupChallenge(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvQuestion = view.findViewById<TextView>(R.id.tvQuestion)
        val optionsContainer = view.findViewById<View>(R.id.optionsContainer)
        val pinContainer = view.findViewById<View>(R.id.pinContainer)
        val btnOption1 = view.findViewById<Button>(R.id.btnOption1)
        val btnOption2 = view.findViewById<Button>(R.id.btnOption2)
        val btnOption3 = view.findViewById<Button>(R.id.btnOption3)
        val btnOption4 = view.findViewById<Button>(R.id.btnOption4)
        val btnEmergency = view.findViewById<Button>(R.id.btnEmergency)

        // Reset visibility state from PIN mode
        optionsContainer.visibility = View.VISIBLE
        pinContainer.visibility = View.GONE
        btnEmergency.visibility = View.VISIBLE
        tvTitle.text = "🧠 Brain Time!"

        if (!settingsRepository.isLearningEnabled()) {
             // Simple Unlock Mode
             tvQuestion.text = getString(R.string.unlock_swipe_instruction)
             val btnUnlock = btnOption1
             btnUnlock.text = getString(R.string.unlock_button_text)
             btnUnlock.visibility = View.VISIBLE
             btnUnlock.isEnabled = true
             btnUnlock.alpha = 1f
             btnUnlock.setBackgroundResource(R.drawable.bg_option_button)
             btnUnlock.setOnClickListener {
                 removeOverlay()
                 startSessionTimer()
             }
             
             // Hide others
             btnOption2.visibility = View.GONE
             btnOption3.visibility = View.GONE
             btnOption4.visibility = View.GONE
             return
        }

        // Pick a random challenge type from enabled ones
        val enabledTypes = settingsRepository.getEnabledChallengeTypes()
        // Safety check, though Repo ensures non-empty fallback
        val typeToUse = if (enabledTypes.isNotEmpty()) enabledTypes.random() else ChallengeType.MATH
        
        val challenge = challengeManager.generateChallenge(
            typeToUse,
            settingsRepository.getChildAge()
        )

        tvQuestion.text = challenge.question
        val buttons = listOf(btnOption1, btnOption2, btnOption3, btnOption4)
        
        buttons.forEachIndexed { index, button ->
            if (index < challenge.options.size) {
                button.text = challenge.options[index]
                button.visibility = View.VISIBLE
                
                // Reset visual state
                button.alpha = 1f
                button.isEnabled = true
                button.setBackgroundResource(R.drawable.bg_option_button)

                button.setOnClickListener {
                    if (index == challenge.correctIndex) {
                        handleCorrectAnswer(button)
                    } else {
                        handleWrongAnswer(view, button, buttons)
                    }
                }
            } else {
                button.visibility = View.GONE
            }
        }

        btnEmergency.setOnClickListener {
            // Switch to PIN mode for parent unlock
            setupPinChallenge(view)
        }
    }
    
    private fun handleCorrectAnswer(selectedButton: Button) {
        // Green state
        selectedButton.setBackgroundResource(R.drawable.bg_option_button_correct)
        
        // Success Haptic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            selectedButton.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
             selectedButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }

        // Delay for visual confirmation
        handler.postDelayed({
             // No Toast
             removeOverlay()
             startSessionTimer()
        }, 500)
    }

    private fun handleWrongAnswer(view: View, selectedButton: Button, allButtons: List<Button>) {
         // Red state
         selectedButton.setBackgroundResource(R.drawable.bg_option_button_error)
         
         // Error Haptic
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             selectedButton.performHapticFeedback(HapticFeedbackConstants.REJECT)
         } else {
             selectedButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
         }
         
         // Shake Animation (Shake the specific button or the whole question area)
         // Shaking the button is clearer
         ObjectAnimator.ofFloat(selectedButton, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
             duration = 500
             start()
         }
         
         // Disable all buttons momentarily to prevent spamming
         allButtons.forEach { it.isEnabled = false }
         
         // Delay refresh
         handler.postDelayed({
             // Reset state is handled by setupChallenge generating new buttons
             // No Toast
             setupChallenge(view)
         }, 800)
    }
    
    private fun setupPinChallenge(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvQuestion = view.findViewById<TextView>(R.id.tvQuestion)
        val optionsContainer = view.findViewById<View>(R.id.optionsContainer)
        val pinContainer = view.findViewById<View>(R.id.pinContainer)
        val btnEmergency = view.findViewById<Button>(R.id.btnEmergency)
        
        tvTitle.text = getString(R.string.parent_auth_title)
        tvQuestion.text = getString(R.string.parent_auth_instruction)
        optionsContainer.visibility = View.GONE
        btnEmergency.visibility = View.GONE
        pinContainer.visibility = View.VISIBLE
        
        var enteredPin = ""
        val dots = listOf(
            view.findViewById<View>(R.id.dotS1),
            view.findViewById<View>(R.id.dotS2),
            view.findViewById<View>(R.id.dotS3),
            view.findViewById<View>(R.id.dotS4)
        )
        
        val buttons = listOf(
             view.findViewById<Button>(R.id.btnS0),
             view.findViewById<Button>(R.id.btnS1),
             view.findViewById<Button>(R.id.btnS2),
             view.findViewById<Button>(R.id.btnS3),
             view.findViewById<Button>(R.id.btnS4),
             view.findViewById<Button>(R.id.btnS5),
             view.findViewById<Button>(R.id.btnS6),
             view.findViewById<Button>(R.id.btnS7),
             view.findViewById<Button>(R.id.btnS8),
             view.findViewById<Button>(R.id.btnS9)
        )
        
        fun updateDots() {
            dots.forEachIndexed { index, dot ->
                dot.setBackgroundResource(
                    if (index < enteredPin.length) R.drawable.pin_dot_filled
                    else R.drawable.pin_dot_empty
                )
            }
        }
        
        fun onDigitPressed(digit: String) {
            if (enteredPin.length < 4) {
                enteredPin += digit
                updateDots()
                
                if (enteredPin.length == 4) {
                    val actualPin = settingsRepository.getPin()
                    if (enteredPin == actualPin) {
                        Toast.makeText(this, getString(R.string.pin_authenticated), Toast.LENGTH_SHORT).show()
                        removeOverlay()
                        startSessionTimer() 
                    } else {
                        Toast.makeText(this, getString(R.string.pin_incorrect), Toast.LENGTH_SHORT).show()
                        enteredPin = ""
                        updateDots()
                    }
                }
            }
        }
        
        buttons.forEachIndexed { index, button ->
             button.setOnClickListener { onDigitPressed(button.text.toString()) }
        }
        
        view.findViewById<Button>(R.id.btnSDelete).setOnClickListener {
             if (enteredPin.isNotEmpty()) {
                 enteredPin = enteredPin.dropLast(1)
                 updateDots()
             }
        }
        
        // Back to Quiz Logic
        view.findViewById<Button>(R.id.btnBackToQuiz).setOnClickListener {
            // Re-setup challenge (generates a new one)
            setupChallenge(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        handler.removeCallbacks(lockRunnable)
        unregisterReceiver(screenReceiver)
    }
    
    private fun startSessionTimer() {
        val minutes = settingsRepository.getSessionDurationMinutes()
        handler.removeCallbacks(lockRunnable)
        val delay = minutes * 60 * 1000L
        sessionExpiryTime = System.currentTimeMillis() + delay
        screenWentOff = false // Reset - user solved challenge, allow free use until screen goes off or timer expires
        handler.postDelayed(lockRunnable, delay)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
