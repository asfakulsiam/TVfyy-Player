package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SupportPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tvfyy_support_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NEVER_SHOW = "support_prompt_never_show"
        private const val KEY_LAST_SHOWN_TIMESTAMP = "support_prompt_last_shown"
        private const val KEY_PROMPT_SHOWN_COUNT = "support_prompt_shown_count"
        private const val MIN_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours cooldown between contextual prompts
    }

    fun isNeverShowAgain(): Boolean {
        return prefs.getBoolean(KEY_NEVER_SHOW, false)
    }

    fun setNeverShowAgain(neverShow: Boolean) {
        prefs.edit().putBoolean(KEY_NEVER_SHOW, neverShow).apply()
    }

    fun recordPromptShown() {
        val count = prefs.getInt(KEY_PROMPT_SHOWN_COUNT, 0)
        prefs.edit()
            .putLong(KEY_LAST_SHOWN_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_PROMPT_SHOWN_COUNT, count + 1)
            .apply()
    }

    fun canShowContextualPrompt(sessionWatchSeconds: Long = 0): Boolean {
        if (isNeverShowAgain()) return false
        val lastShown = prefs.getLong(KEY_LAST_SHOWN_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (now - lastShown < MIN_COOLDOWN_MS) return false
        // Meaningful watch time required: at least 45 seconds of continuous playback
        return sessionWatchSeconds >= 45
    }
}
