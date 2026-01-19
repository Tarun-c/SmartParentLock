package com.smartparentlock

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        sharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isLockEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun getPin(): String? {
        return sharedPreferences.getString(KEY_PIN, null)
    }

    fun setPin(pin: String) {
        sharedPreferences.edit().putString(KEY_PIN, pin).apply()
    }
    
    fun clearPin() {
        sharedPreferences.edit().remove(KEY_PIN).apply()
    }

    fun getEnabledChallengeTypes(): Set<ChallengeType> {
        val typesSet = sharedPreferences.getStringSet(KEY_CHALLENGE_TYPES_SET, null)
        if (typesSet.isNullOrEmpty()) {
            // Fallback to legacy single type or default Math
            return setOf(getChallengeType())
        }
        return typesSet.mapNotNull {
            try { ChallengeType.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
    }

    fun setEnabledChallengeTypes(types: Set<ChallengeType>) {
        val strSet = types.map { it.name }.toSet()
        sharedPreferences.edit().putStringSet(KEY_CHALLENGE_TYPES_SET, strSet).apply()
    }

    // Deprecate single getters/setters but keep for migration/fallback
    private fun getChallengeType(): ChallengeType {
        val typeStr = sharedPreferences.getString(KEY_CHALLENGE_TYPE, ChallengeType.MATH.name)
        return try {
            ChallengeType.valueOf(typeStr!!)
        } catch (e: Exception) {
            ChallengeType.MATH
        }
    }

    fun setChallengeType(type: ChallengeType) {
        sharedPreferences.edit().putString(KEY_CHALLENGE_TYPE, type.name).apply()
    }

    fun getDifficulty(): Difficulty {
        val diffStr = sharedPreferences.getString(KEY_DIFFICULTY, Difficulty.EASY.name)
        return try {
            Difficulty.valueOf(diffStr!!)
        } catch (e: Exception) {
            Difficulty.EASY
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        sharedPreferences.edit().putString(KEY_DIFFICULTY, difficulty.name).apply()
    }

    fun getSessionDurationMinutes(): Int {
        return sharedPreferences.getInt(KEY_SESSION_DURATION, 15)
    }

    fun setSessionDurationMinutes(minutes: Int) {
        sharedPreferences.edit().putInt(KEY_SESSION_DURATION, minutes).apply()
    }

    fun getChildAge(): Int {
        return sharedPreferences.getInt(KEY_CHILD_AGE, 7) // Default 7 years old
    }
    
    fun setChildAge(age: Int) {
        sharedPreferences.edit().putInt(KEY_CHILD_AGE, age).apply()
    }

    fun getMathSkill(op: MathOp): Boolean {
        // Default: Add/Sub True. Mul/Div False.
        val default = (op == MathOp.ADD || op == MathOp.SUB)
        return sharedPreferences.getBoolean("math_skill_${op.name}", default)
    }

    fun setMathSkill(op: MathOp, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("math_skill_${op.name}", enabled).apply()
    }

    fun getLanguageEnabled(lang: SupportedLanguage): Boolean {
        // Default: Spanish True, others False
        val default = (lang == SupportedLanguage.SPANISH)
        return sharedPreferences.getBoolean("lang_enabled_${lang.name}", default)
    }

    fun setLanguageEnabled(lang: SupportedLanguage, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("lang_enabled_${lang.name}", enabled).apply()
    }

    companion object {
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_PIN = "parent_pin"
        private const val KEY_CHALLENGE_TYPE = "challenge_type"
        private const val KEY_CHALLENGE_TYPES_SET = "challenge_types_set"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_SESSION_DURATION = "session_duration"
        private const val KEY_CHILD_AGE = "child_age"
        private const val KEY_LEARNING_ENABLED = "learning_enabled"
    }

    fun isLearningEnabled(): Boolean {
        // Default True (Challenges ON)
        return sharedPreferences.getBoolean(KEY_LEARNING_ENABLED, true)
    }

    fun setLearningEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_LEARNING_ENABLED, enabled).apply()
    }
}
enum class MathOp { ADD, SUB, MUL, DIV }
enum class SupportedLanguage { SPANISH, FRENCH, GERMAN, HINDI }

enum class ChallengeType {
    MATH, LOGIC, VOCABULARY, PATTERNS, GK, TRANSLATION
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}
