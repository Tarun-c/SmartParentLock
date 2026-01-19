package com.smartparentlock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartparentlock.databinding.ActivityFeedbackBinding

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Rotation handles back icon direction visually, but click handles action
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSubmit.setOnClickListener {
            sendFeedback()
        }
    }

    private fun sendFeedback() {
        val subject = binding.etSubject.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()
        val userEmail = binding.etEmail.text.toString().trim()

        if (subject.isEmpty()) {
             binding.etSubject.error = getString(R.string.feedback_error_subject)
             binding.etSubject.requestFocus()
             return
        }

        if (message.isEmpty()) {
            binding.etMessage.error = getString(R.string.feedback_error_message)
            binding.etMessage.requestFocus()
            return
        }

        val fullBody = buildString {
            if (userEmail.isNotEmpty()) append("Contact Email: $userEmail\n\n")
            append(message)
            append("\n\n----------------")
            append("\nApp Version: 1.0")
            append("\nDevice: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
        }

        val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            selector = selectorIntent
            putExtra(Intent.EXTRA_EMAIL, arrayOf("devloppercom@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback: $subject")
            putExtra(Intent.EXTRA_TEXT, fullBody)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_email_chooser)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.feedback_no_email_app), Toast.LENGTH_SHORT).show()
        }
    }
}
