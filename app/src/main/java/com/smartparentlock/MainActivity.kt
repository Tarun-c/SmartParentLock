package com.smartparentlock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.widget.RadioButton
import android.widget.Toast
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.smartparentlock.databinding.ActivityMainBinding
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback

/**
 * MainActivity - Parent Dashboard
 *
 * The main configuration screen for parents to manage lock settings, learning challenges,
 * and app behavior. This activity is protected by PIN authentication and session timeout.
 *
 * ## Key Features:
 * - Lock enable/disable toggle
 * - Learning mode configuration (enable/disable educational challenges)
 * - Multi-select challenge types (Math, Vocabulary, Patterns, GK, Translation)
 * - Age-based difficulty adjustment (5-18 years)
 * - Session duration control (1-60 minutes)
 * - Device admin management (uninstall protection)
 * - AdMob integration (Banner + Interstitial ads)
 *
 * ## Security:
 * - Requires valid PIN authentication to access
 * - 5-minute session timeout (auto-logout)
 * - Session state managed via companion object
 *
 * ## Monetization:
 * - Banner ad displayed at bottom of screen
 * - Interstitial ad shown on lock toggle and app exit
 *
 * @see PinActivity for authentication flow
 * @see LockService for lock screen implementation
 * @see SettingsRepository for data persistence
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var deviceAdmin: ComponentName
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var mInterstitialAd: InterstitialAd? = null
    
    companion object {
        const val PERM_REQ_CODE = 123
        const val ADMIN_REQ_CODE = 456

        const val VERIFY_PIN_REQ_CODE = 789
        const val VERIFY_ADMIN_REQ_CODE = 1010
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        
        // Static session management
        private var lastAuthTime: Long = 0
        
        fun isSessionValid(): Boolean {
            return System.currentTimeMillis() - lastAuthTime < SESSION_TIMEOUT_MS
        }
        
        fun markAuthenticated() {
            lastAuthTime = System.currentTimeMillis()
        }
        
        fun clearSession() {
            lastAuthTime = 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        
        // Check if coming from PinActivity with fresh auth
        val justAuthenticated = intent.getBooleanExtra("authenticated", false)
        if (justAuthenticated) {
            markAuthenticated()
        }
        
        // Security check - verify session is valid
        if (!isSessionValid()) {
            redirectToPinScreen()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = SettingsRepository(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        deviceAdmin = ComponentName(this, SmartParentAdminReceiver::class.java)

        setupUI()
    
    // Load Ad
    binding.adView.loadAd(AdRequest.Builder().build())
    loadInterstitial()
        
        // Exit Handler
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val learningEnabled = settingsRepository.isLearningEnabled()
                val chipsSelected = binding.chipGroupInfo.checkedChipIds.isNotEmpty()
                
                if (learningEnabled && !chipsSelected) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.dialog_select_challenge_title)
                        .setMessage(R.string.dialog_select_challenge_msg)
                        .setPositiveButton(R.string.ok, null)
                        .setCancelable(false)
                        .show()
                } else {
                    if (mInterstitialAd != null) {
                        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                             override fun onAdDismissedFullScreenContent() {
                                 isEnabled = false
                                 onBackPressedDispatcher.onBackPressed()
                             }
                        }
                        mInterstitialAd?.show(this@MainActivity)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        
        // Safety check on exit (minimize/home)
        if (settingsRepository.isLearningEnabled()) {
             val enabledTypes = settingsRepository.getEnabledChallengeTypes()
             // If repo returns default (Math) because set was empty, we are fine?
             // No, getEnabledChallengeTypes returns Math fallback if empty.
             // But valid empty is different from null/missing.
             // We need to check if we need to force something.
             
             // Wait, the repo's getEnabledChallengeTypes() fallback logic means if we saved emptySet(),
             // it returns emptySet() ?? No, "if (typesSet.isNullOrEmpty()) return setOf(Math)"
             // So it's impossible for getEnabledChallengeTypes to return empty set with current repo logic.
             // Checking repo is NOT reliable for empty set detection if fallback exists.
             
             // However, `binding.chipGroupInfo.checkedChipIds` is reliable for UI state.
             // But onPause might happen when UI is not visible? No, onPause is part of lifecycle.
             
             if (binding.chipGroupInfo.checkedChipIds.isEmpty()) {
                 // Force Math and Toast
                 settingsRepository.setEnabledChallengeTypes(setOf(ChallengeType.MATH))
                 Toast.makeText(this, getString(R.string.default_math_toast), Toast.LENGTH_SHORT).show()
             }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Re-check session on every resume (in case user came back after timeout)
        if (!isSessionValid()) {
            redirectToPinScreen()
            return
        }
        
        checkPermissions()
    }
    
    private fun redirectToPinScreen() {
        val intent = Intent(this, PinActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupUI() {
        // Load initial state
        // Load initial state
        binding.switchLock.isChecked = settingsRepository.isLockEnabled()
        
        binding.switchLock.setOnClickListener {
            performLockToggle()
        }
        
        // Learning Mode Switch
        binding.switchLearning.isChecked = settingsRepository.isLearningEnabled()
        val updateLearningUI = { enabled: Boolean ->
             binding.chipGroupInfo.isEnabled = enabled
             binding.sliderAge.isEnabled = enabled
             for (i in 0 until binding.chipGroupInfo.childCount) {
                 binding.chipGroupInfo.getChildAt(i).isEnabled = enabled
             }
             // Hide specific layouts if disabled?
             if (!enabled) {
                 binding.layoutMathSkills.visibility = View.GONE
                 binding.layoutLanguages.visibility = View.GONE
             } else {
                 val types = settingsRepository.getEnabledChallengeTypes()
                 binding.layoutMathSkills.visibility = if (types.contains(ChallengeType.MATH)) View.VISIBLE else View.GONE
                 binding.layoutLanguages.visibility = if (types.contains(ChallengeType.TRANSLATION)) View.VISIBLE else View.GONE
             }
        }
        updateLearningUI(binding.switchLearning.isChecked)
        
        binding.switchLearning.setOnCheckedChangeListener { _, isChecked ->
            settingsRepository.setLearningEnabled(isChecked)
            updateLearningUI(isChecked)
        }
        
        // Initialize Challenge Type Selection (Multi)
        // We set listener AFTER checking to avoid triggering it during init
        val enabledTypes = settingsRepository.getEnabledChallengeTypes()
        if (enabledTypes.contains(ChallengeType.MATH)) binding.chipGroupInfo.check(R.id.chipMath)
        if (enabledTypes.contains(ChallengeType.VOCABULARY)) binding.chipGroupInfo.check(R.id.chipVocabulary)
        if (enabledTypes.contains(ChallengeType.PATTERNS)) binding.chipGroupInfo.check(R.id.chipPatterns)
        if (enabledTypes.contains(ChallengeType.GK)) binding.chipGroupInfo.check(R.id.chipGK)
        if (enabledTypes.contains(ChallengeType.TRANSLATION)) binding.chipGroupInfo.check(R.id.chipTranslation)
        
        // Initial visibility based on Repo state (reliable)
        binding.layoutMathSkills.visibility = if (enabledTypes.contains(ChallengeType.MATH)) View.VISIBLE else View.GONE
        binding.layoutLanguages.visibility = if (enabledTypes.contains(ChallengeType.TRANSLATION)) View.VISIBLE else View.GONE

        // Listener
        binding.chipGroupInfo.setOnCheckedStateChangeListener { group, checkedIds ->
             val newSet = mutableSetOf<ChallengeType>()
             checkedIds.forEach { id ->
                 when(id) {
                     R.id.chipMath -> newSet.add(ChallengeType.MATH)
                     R.id.chipVocabulary -> newSet.add(ChallengeType.VOCABULARY)
                     R.id.chipPatterns -> newSet.add(ChallengeType.PATTERNS)
                     R.id.chipGK -> newSet.add(ChallengeType.GK)
                     R.id.chipTranslation -> newSet.add(ChallengeType.TRANSLATION)
                 }
             }
             
             // Ensure at least one is selected (Default to Math if empty)
             if (newSet.isEmpty()) {
                 // Allow saving empty set so we can detect it on exit
                 settingsRepository.setEnabledChallengeTypes(emptySet())
             } else {
                 settingsRepository.setEnabledChallengeTypes(newSet)
                 // Sync Legacy Key just in case
                 settingsRepository.setChallengeType(newSet.first())
             }

             // Toggle Visibility
             binding.layoutMathSkills.visibility = if (newSet.contains(ChallengeType.MATH) || newSet.isEmpty()) View.VISIBLE else View.GONE
             binding.layoutLanguages.visibility = if (newSet.contains(ChallengeType.TRANSLATION)) View.VISIBLE else View.GONE
        }

        binding.sliderAge.addOnChangeListener { _, value, _ ->
             val age = value.toInt()
             binding.tvAgeValue.text = "$age yrs"
             settingsRepository.setChildAge(age)
        }

        binding.sliderDuration.addOnChangeListener { _, value, _ ->
            val minutes = value.toInt()
            binding.tvDurationValue.text = "${minutes}m"
            settingsRepository.setSessionDurationMinutes(minutes)
            
            // Notify Service to update active timer immediately if possible
            val intent = Intent(this, LockService::class.java)
            intent.action = "UPDATE_TIMER"
            startService(intent)
        }
        
        binding.btnSetPin.setOnClickListener {
            // Clear PIN and restart to set new one
            settingsRepository.clearPin()
            Toast.makeText(this, getString(R.string.set_new_pin_toast), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PinActivity::class.java)
            intent.putExtra("FROM_WELCOME", true) // Skip onboarding
            startActivity(intent)
            finish()
        }

        binding.switchAdmin.isChecked = devicePolicyManager.isAdminActive(deviceAdmin)
        binding.switchAdmin.setOnClickListener {
             val isChecked = binding.switchAdmin.isChecked
             
             if (isChecked) {
                 // Enabling Admin
                  if (!devicePolicyManager.isAdminActive(deviceAdmin)) {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin)
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_description))
                    startActivityForResult(intent, ADMIN_REQ_CODE)
                }
             } else {
                 // Turning OFF Admin - Require Verification
                 // We revert the switch first
                 binding.switchAdmin.isChecked = true
                 
                 if (devicePolicyManager.isAdminActive(deviceAdmin)) {
                     val intent = Intent(this, PinActivity::class.java)
                     intent.putExtra("VERIFY_MODE", true)
                     intent.putExtra("TITLE", getString(R.string.verify_admin_remove_title))
                     intent.putExtra("SUBTITLE", getString(R.string.verify_admin_remove_subtitle))
                     startActivityForResult(intent, VERIFY_ADMIN_REQ_CODE)
                 }

             }
             }

        
        binding.cardFeedback.setOnClickListener {
            sendFeedback()
        }
        
        binding.tvPrivacy.setOnClickListener {
            // Placeholder URL
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"))
            startActivity(browserIntent)
        }
    }

    private fun sendFeedback() {
        startActivity(Intent(this, FeedbackActivity::class.java))
    }

    private fun checkPermissions(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, PERM_REQ_CODE)
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VERIFY_PIN_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                // PIN Verified, now allowed to turn off lock
                binding.switchLock.isChecked = false
                settingsRepository.setLockEnabled(false)
                stopLockService()
                Toast.makeText(this, getString(R.string.lock_disabled_toast), Toast.LENGTH_SHORT).show()
            } else {
                // Canceled
                Toast.makeText(this, getString(R.string.verification_canceled), Toast.LENGTH_SHORT).show()
                binding.switchLock.isChecked = true // Reset
            }
        } else if (requestCode == VERIFY_ADMIN_REQ_CODE) {
             if (resultCode == RESULT_OK) {
                // PIN Verified, now allowed to remove admin
                devicePolicyManager.removeActiveAdmin(deviceAdmin)
                binding.switchAdmin.isChecked = false
                Toast.makeText(this, getString(R.string.admin_disabled_toast), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.verification_canceled), Toast.LENGTH_SHORT).show()
                binding.switchAdmin.isChecked = true // Reset
            }
        }
    }

    private fun startLockService(startHidden: Boolean = false) {
        val intent = Intent(this, LockService::class.java)
        intent.putExtra("START_HIDDEN", startHidden)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopLockService() {
        val intent = Intent(this, LockService::class.java)
        stopService(intent)
    }
    private fun performLockToggle() {
        val isChecked = binding.switchLock.isChecked
        if (isChecked) {
            settingsRepository.setLockEnabled(true)
            startLockService(startHidden = true) 
            Toast.makeText(this, getString(R.string.lock_enabled_toast), Toast.LENGTH_SHORT).show()
        } else {
            // Require PIN to disable
            binding.switchLock.isChecked = true 
            val intent = Intent(this, PinActivity::class.java)
            intent.putExtra("VERIFY_MODE", true)
            intent.putExtra("TITLE", getString(R.string.verify_lock_disable_title))
            intent.putExtra("SUBTITLE", getString(R.string.verify_lock_disable_subtitle))
            startActivityForResult(intent, VERIFY_PIN_REQ_CODE)
        }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }
}
