package com.staycalm.app

import android.content.Context

/** Tiny local storage: extra words, block counter, and the "show calm screen" flag. */
object Prefs {

    private fun sp(c: Context) = c.getSharedPreferences("staycalm", Context.MODE_PRIVATE)

    fun words(c: Context): List<String> =
        (Config.DEFAULT_WORDS + extraWords(c))
            .map { it.lowercase().trim() }
            .filter { it.length >= 3 }
            .distinct()

    fun extraWords(c: Context): List<String> =
        (sp(c).getStringSet("extra_words", emptySet()) ?: emptySet()).sorted()

    /** Words can only be added, never removed, so an urge can't talk you out of it. */
    fun addWord(c: Context, word: String): Boolean {
        val w = word.lowercase().trim()
        if (w.length < 3) return false
        val set = HashSet<String>(sp(c).getStringSet("extra_words", emptySet()) ?: emptySet())
        set.add(w)
        sp(c).edit().putStringSet("extra_words", set).apply()
        return true
    }

    fun recordBlock(c: Context) {
        val p = sp(c)
        p.edit()
            .putLong("last_block", System.currentTimeMillis())
            .putInt("total_blocks", p.getInt("total_blocks", 0) + 1)
            .apply()
    }

    fun lastBlock(c: Context): Long = sp(c).getLong("last_block", 0L)

    fun totalBlocks(c: Context): Int = sp(c).getInt("total_blocks", 0)

    fun pendingCalm(c: Context): Boolean = sp(c).getBoolean("pending_calm", false)

    fun setPendingCalm(c: Context, value: Boolean) {
        sp(c).edit().putBoolean("pending_calm", value).apply()
    }
}
