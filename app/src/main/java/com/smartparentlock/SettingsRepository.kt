package com.smartparentlock

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val prefFileName = "secret_shared_prefs"

        sharedPreferences = try {
            EncryptedSharedPreferences.create(
                prefFileName,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load EncryptedSharedPreferences. Resetting.", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            // If the Tink keyset is corrupted, delete the preferences and try again
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(prefFileName)
            } else {
                context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE).edit().clear().commit()
            }
            EncryptedSharedPreferences.create(
                prefFileName,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun isLockEnabled(): Boolean {
        return try { sharedPreferences.getBoolean(KEY_LOCK_ENABLED, false) } catch (e: Exception) { false }
    }

    fun setLockEnabled(enabled: Boolean) {
        try { sharedPreferences.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply() } catch (e: Exception) {}
    }

    fun getPin(): String? {
        return try { sharedPreferences.getString(KEY_PIN, null) } catch (e: Exception) { null }
    }

    fun setPin(pin: String) {
        try { sharedPreferences.edit().putString(KEY_PIN, pin).apply() } catch (e: Exception) {}
    }
    
    fun clearPin() {
        try { sharedPreferences.edit().remove(KEY_PIN).apply() } catch (e: Exception) {}
    }
    
    fun getSecurityQuestion(): String? {
        return try { sharedPreferences.getString(KEY_SECURITY_QUESTION, null) } catch (e: Exception) { null }
    }
    
    fun getSecurityAnswer(): String? {
        return try { sharedPreferences.getString(KEY_SECURITY_ANSWER, null) } catch (e: Exception) { null }
    }
    
    fun setSecurityQuestionAnswer(question: String, answer: String) {
        try {
            sharedPreferences.edit()
                .putString(KEY_SECURITY_QUESTION, question)
                .putString(KEY_SECURITY_ANSWER, answer.lowercase().trim())
                .apply()
        } catch (e: Exception) {}
    }

    fun getEnabledChallengeTypes(): Set<ChallengeType> {
        val typesSet = try { sharedPreferences.getStringSet(KEY_CHALLENGE_TYPES_SET, null) } catch (e: Exception) { null }
        if (typesSet.isNullOrEmpty()) {
            return setOf(getChallengeType())
        }
        return typesSet.mapNotNull {
            try { ChallengeType.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
    }

    fun setEnabledChallengeTypes(types: Set<ChallengeType>) {
        try {
            val strSet = types.map { it.name }.toSet()
            sharedPreferences.edit().putStringSet(KEY_CHALLENGE_TYPES_SET, strSet).apply()
        } catch (e: Exception) {}
    }

    private fun getChallengeType(): ChallengeType {
        val typeStr = try { sharedPreferences.getString(KEY_CHALLENGE_TYPE, ChallengeType.MATH.name) } catch (e: Exception) { ChallengeType.MATH.name }
        return try {
            ChallengeType.valueOf(typeStr!!)
        } catch (e: Exception) {
            ChallengeType.MATH
        }
    }

    fun setChallengeType(type: ChallengeType) {
        try { sharedPreferences.edit().putString(KEY_CHALLENGE_TYPE, type.name).apply() } catch (e: Exception) {}
    }

    fun getDifficulty(): Difficulty {
        val diffStr = try { sharedPreferences.getString(KEY_DIFFICULTY, Difficulty.EASY.name) } catch (e: Exception) { Difficulty.EASY.name }
        return try {
            Difficulty.valueOf(diffStr!!)
        } catch (e: Exception) {
            Difficulty.EASY
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        try { sharedPreferences.edit().putString(KEY_DIFFICULTY, difficulty.name).apply() } catch (e: Exception) {}
    }

    fun getSessionDurationMinutes(): Int {
        return try { sharedPreferences.getInt(KEY_SESSION_DURATION, 15) } catch (e: Exception) { 15 }
    }

    fun setSessionDurationMinutes(minutes: Int) {
        try { sharedPreferences.edit().putInt(KEY_SESSION_DURATION, minutes).apply() } catch (e: Exception) {}
    }

    fun getChildAge(): Int {
        return try { sharedPreferences.getInt(KEY_CHILD_AGE, 7) } catch (e: Exception) { 7 }
    }
    
    fun setChildAge(age: Int) {
        try { sharedPreferences.edit().putInt(KEY_CHILD_AGE, age).apply() } catch (e: Exception) {}
    }

    fun getMathSkill(op: MathOp): Boolean {
        val default = (op == MathOp.ADD || op == MathOp.SUB)
        return try { sharedPreferences.getBoolean("math_skill_${op.name}", default) } catch (e: Exception) { default }
    }

    fun setMathSkill(op: MathOp, enabled: Boolean) {
        try { sharedPreferences.edit().putBoolean("math_skill_${op.name}", enabled).apply() } catch (e: Exception) {}
    }

    fun getLanguageEnabled(lang: SupportedLanguage): Boolean {
        val default = (lang == SupportedLanguage.HINDI)
        return try { sharedPreferences.getBoolean("lang_enabled_${lang.name}", default) } catch (e: Exception) { default }
    }

    fun setLanguageEnabled(lang: SupportedLanguage, enabled: Boolean) {
        try { sharedPreferences.edit().putBoolean("lang_enabled_${lang.name}", enabled).apply() } catch (e: Exception) {}
    }

    companion object {
        private const val TAG = "SettingsRepoLogs"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_PIN = "parent_pin"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"
        private const val KEY_CHALLENGE_TYPE = "challenge_type"
        private const val KEY_CHALLENGE_TYPES_SET = "challenge_types_set"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_SESSION_DURATION = "session_duration"
        private const val KEY_CHILD_AGE = "child_age"
        private const val KEY_LEARNING_ENABLED = "learning_enabled"
        private const val KEY_SERVICE_DISCLOSURE_ACCEPTED = "service_disclosure_accepted"
    }
    
    fun hasAcceptedServiceDisclosure(): Boolean {
        return try { sharedPreferences.getBoolean(KEY_SERVICE_DISCLOSURE_ACCEPTED, false) } catch (e: Exception) { false }
    }
    
    fun setServiceDisclosureAccepted(accepted: Boolean) {
        try { sharedPreferences.edit().putBoolean(KEY_SERVICE_DISCLOSURE_ACCEPTED, accepted).apply() } catch (e: Exception) {}
    }

    fun isLearningEnabled(): Boolean {
        return try { sharedPreferences.getBoolean(KEY_LEARNING_ENABLED, true) } catch (e: Exception) { true }
    }

    fun setLearningEnabled(enabled: Boolean) {
        try { sharedPreferences.edit().putBoolean(KEY_LEARNING_ENABLED, enabled).apply() } catch (e: Exception) {}
    }
}
enum class MathOp { ADD, SUB, MUL, DIV }
enum class SupportedLanguage { SPANISH, FRENCH, GERMAN, HINDI }

enum class ChallengeType {
    MATH, LOGIC, VOCABULARY, PATTERNS, GK, TRANSLATION, TRICKY
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}
