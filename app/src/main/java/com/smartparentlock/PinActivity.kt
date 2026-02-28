package com.smartparentlock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository
    
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var dots: List<View>
    
    private var enteredPin = ""
    private var isSetupMode = false
    private var isVerifyMode = false
    private var confirmPin = ""
    private var isConfirmStep = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        
        settingsRepository = SettingsRepository(this)
        
        tvTitle = findViewById(R.id.tvPinTitle)
        tvSubtitle = findViewById(R.id.tvPinSubtitle)
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )
        
        // Check if this is first time setup, verification, or PIN change mode
        val isChangeMode = intent.getBooleanExtra("CHANGE_PIN_MODE", false)
        isSetupMode = settingsRepository.getPin() == null || isChangeMode
        isVerifyMode = intent.getBooleanExtra("VERIFY_MODE", false)
        
        // If first run and not coming from Welcome screen, show Welcome screen
        if (isSetupMode && !intent.getBooleanExtra("FROM_WELCOME", false)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        
        updateUI()
        
        setupNumberPad()
    }

    private fun updateUI() {
        if (isSetupMode) {
            if (isConfirmStep) {
                tvTitle.text = getString(R.string.pin_confirm_title)
                tvSubtitle.text = getString(R.string.pin_confirm_subtitle)
            } else {
                tvTitle.text = getString(R.string.pin_create_title)
                tvSubtitle.text = getString(R.string.pin_create_subtitle)
            }
        } else {
            // Check for custom title passed via intent
            val customTitle = intent.getStringExtra("TITLE")
            val customSubtitle = intent.getStringExtra("SUBTITLE")
            
            if (!customTitle.isNullOrEmpty()) {
                tvTitle.text = customTitle
            } else {
                tvTitle.text = getString(R.string.pin_enter_title)
            }
            
            if (!customSubtitle.isNullOrEmpty()) {
                tvSubtitle.text = customSubtitle
            } else {
                if (isVerifyMode) {
                    tvSubtitle.text = getString(R.string.pin_verify_subtitle)
                } else {
                    tvSubtitle.text = getString(R.string.pin_access_subtitle)
                }
            }
        }
    }

    private fun setupNumberPad() {
        val buttons = listOf(
            findViewById<Button>(R.id.btn0),
            findViewById<Button>(R.id.btn1),
            findViewById<Button>(R.id.btn2),
            findViewById<Button>(R.id.btn3),
            findViewById<Button>(R.id.btn4),
            findViewById<Button>(R.id.btn5),
            findViewById<Button>(R.id.btn6),
            findViewById<Button>(R.id.btn7),
            findViewById<Button>(R.id.btn8),
            findViewById<Button>(R.id.btn9)
        )
        
        buttons.forEachIndexed { index, button ->
            button.setOnClickListener { onDigitPressed(index.toString()) }
        }
        
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { onBackspacePressed() }
    }

    private fun onDigitPressed(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin += digit
            updateDots()
            
            if (enteredPin.length == 4) {
                onPinComplete()
            }
        }
    }

    private fun onBackspacePressed() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            updateDots()
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < enteredPin.length) R.drawable.pin_dot_filled
                else R.drawable.pin_dot_empty
            )
        }
    }

    private fun onPinComplete() {
        if (isSetupMode) {
            if (!isConfirmStep) {
                // First entry - save and ask for confirmation
                confirmPin = enteredPin
                enteredPin = ""
                isConfirmStep = true
                updateUI()
                updateDots()
            } else {
                // Confirm step
                if (enteredPin == confirmPin) {
                    settingsRepository.setPin(enteredPin)
                    Toast.makeText(this, getString(R.string.pin_success), Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    Toast.makeText(this, getString(R.string.pin_mismatch), Toast.LENGTH_SHORT).show()
                    enteredPin = ""
                    confirmPin = ""
                    isConfirmStep = false
                    updateUI()
                    updateDots()
                }
            }
        } else {
            // Verification mode
            if (enteredPin == settingsRepository.getPin()) {
                if (isVerifyMode) {
                    setResult(RESULT_OK)
                    finish()
                } else {
                    goToMainActivity()
                }
            } else {
                Toast.makeText(this, getString(R.string.pin_incorrect), Toast.LENGTH_SHORT).show()
                enteredPin = ""
                updateDots()
            }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("authenticated", true)
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        if (isVerifyMode) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        
        val isChangeMode = intent.getBooleanExtra("CHANGE_PIN_MODE", false)
        if (isChangeMode) {
            goToMainActivity()
            return
        }
        
        // Prevent back navigation without PIN
        if (!isSetupMode) {
            Toast.makeText(this, getString(R.string.pin_required_back), Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }
}
