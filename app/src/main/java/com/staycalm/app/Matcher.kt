package com.staycalm.app

object Matcher {

    /**
     * True if [text] contains any blocked word. Long words are also matched with
     * spaces and punctuation stripped, so "porn hub" or "e-hentai" are caught.
     */
    fun hit(text: String, words: List<String>): Boolean {
        if (text.isBlank()) return false
        val raw = text.lowercase()
        val compact = raw.filter { it.isLetterOrDigit() }
        for (w in words) {
            if (raw.contains(w)) return true
            val cw = w.filter { it.isLetterOrDigit() }
            if (cw.length >= 6 && compact.contains(cw)) return true
        }
        return false
    }
}
