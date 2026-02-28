package com.smartparentlock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RadioButton
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.view.HapticFeedbackConstants
import android.os.Build
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.appcompat.app.AlertDialog
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
        
        val btnForgotPin = findViewById<Button>(R.id.btnForgotPin)
        if (!isSetupMode && settingsRepository.getSecurityQuestion() != null) {
            btnForgotPin.visibility = View.VISIBLE
            btnForgotPin.setOnClickListener {
                showForgotPinDialog()
            }
        } else {
            btnForgotPin.visibility = View.GONE
        }
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
                    showSecurityQuestionSetupDialog()
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
    
    private fun showSecurityQuestionSetupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_security_question, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        // Transparent background so the CardView corners show
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val rgQuestions = dialogView.findViewById<RadioGroup>(R.id.rgQuestions)
        val layoutCustomQuestion = dialogView.findViewById<TextInputLayout>(R.id.layoutCustomQuestion)
        val etCustomQuestion = dialogView.findViewById<TextInputEditText>(R.id.etCustomQuestion)
        val etAnswer = dialogView.findViewById<TextInputEditText>(R.id.etAnswer)
        val btnSaveQuestion = dialogView.findViewById<Button>(R.id.btnSaveQuestion)
        
        // Select first by default
        rgQuestions.check(R.id.rbQuestion1)
        
        rgQuestions.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbCustom) {
                layoutCustomQuestion.visibility = View.VISIBLE
            } else {
                layoutCustomQuestion.visibility = View.GONE
            }
        }
        
        btnSaveQuestion.setOnClickListener {
            val selectedId = rgQuestions.checkedRadioButtonId
            
            val q = if (selectedId == R.id.rbCustom) {
                etCustomQuestion.text.toString().trim()
            } else {
                dialogView.findViewById<RadioButton>(selectedId).text.toString().trim()
            }
            
            val a = etAnswer.text.toString().trim()
            
            if (q.isEmpty()) {
                Toast.makeText(this, "Please select or enter a question", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (a.isEmpty()) {
                Toast.makeText(this, "Please enter an answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            settingsRepository.setSecurityQuestionAnswer(q, a)
            Toast.makeText(this, "Security question saved", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            goToMainActivity()
        }
        
        dialog.show()
    }

    private fun showForgotPinDialog() {
        val question = settingsRepository.getSecurityQuestion()
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_pin, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val tvSecurityQuestion = dialogView.findViewById<TextView>(R.id.tvSecurityQuestion)
        val layoutAnswerRecovery = dialogView.findViewById<TextInputLayout>(R.id.layoutAnswerRecovery)
        val etAnswerRecovery = dialogView.findViewById<TextInputEditText>(R.id.etAnswerRecovery)
        val btnCancelRecovery = dialogView.findViewById<Button>(R.id.btnCancelRecovery)
        val btnResetPin = dialogView.findViewById<Button>(R.id.btnResetPin)
        
        tvSecurityQuestion.text = question
        
        etAnswerRecovery.setOnFocusChangeListener { _, hasFocus -> 
            if (hasFocus) layoutAnswerRecovery.error = null 
        }
        
        btnCancelRecovery.setOnClickListener {
            dialog.dismiss()
        }
        
        btnResetPin.setOnClickListener {
            val a = etAnswerRecovery.text.toString().trim().lowercase()
            val actualAnswer = settingsRepository.getSecurityAnswer()?.lowercase()
            
            if (a.isEmpty()) {
                Toast.makeText(this, "Please enter an answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (a == actualAnswer) {
                layoutAnswerRecovery.error = null
                // Answer matches! Clear PIN and restart activity in Setup Mode
                settingsRepository.clearPin()
                Toast.makeText(this, "Correct! Please set a new PIN.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                val intent = Intent(this, PinActivity::class.java)
                intent.putExtra("FROM_WELCOME", true)
                intent.putExtra("CHANGE_PIN_MODE", true)
                startActivity(intent)
                finish()
            } else {
                layoutAnswerRecovery.error = "Incorrect answer"
                
                // Shake Animation
                ObjectAnimator.ofFloat(layoutAnswerRecovery, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
                    duration = 500
                    start()
                }
                
                // Vibrate/Haptic feedback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    layoutAnswerRecovery.performHapticFeedback(HapticFeedbackConstants.REJECT)
                } else {
                    layoutAnswerRecovery.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
        }
        
        dialog.show()
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
