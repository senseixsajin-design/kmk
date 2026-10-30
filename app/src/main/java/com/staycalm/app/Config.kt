package com.staycalm.app

/** Everything you might want to tweak lives here. */
object Config {

    // ---- Messages ----------------------------------------------------------
    const val TITLE = "You got out of control again??!!"
    const val SUBTITLE = "Stay calm.\nWatch Crunchyroll, chat on Discord, or watch Reels."
    const val AFTER_PICK = "Stay calm sajin. Just make sure you don't get out of control again."

    // ---- Behaviour ---------------------------------------------------------
    /** The calm screen can only be closed after this many seconds. */
    const val CLOSE_DELAY_SEC = 10

    /** After a block, ignore new triggers for this long. */
    const val COOLDOWN_MS = 8000L

    // ---- Apps shown on the calm screen -------------------------------------
    data class Choice(val label: String, val pkg: String, val color: String)

    val CHOICES = listOf(
        Choice("Crunchyroll", "com.crunchyroll.crunchyroid", "#F47521"),
        Choice("Discord", "com.discord", "#5865F2"),
        Choice("Reels (Instagram)", "com.instagram.android", "#E1306C")
    )

    // ---- Browsers whose address bar / page title is scanned ----------------
    val BROWSERS = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
        "org.chromium.chrome", "org.mozilla.firefox", "org.mozilla.firefox_beta",
        "org.mozilla.fenix", "org.mozilla.focus", "com.sec.android.app.sbrowser",
        "com.microsoft.emmx", "com.brave.browser", "com.opera.browser",
        "com.opera.mini.native", "com.duckduckgo.mobile.android", "com.vivaldi.browser",
        "com.kiwibrowser.browser", "com.UCMobile.intl", "com.mi.globalbrowser",
        "com.android.browser", "com.google.android.googlequicksearchbox"
    )

    // ---- Built-in blocked words --------------------------------------------
    // Add more inside the app (Blocked words section). Note: plain "hamster" is
    // not built in because it would also trigger when you type about pets; add
    // it yourself in the app if you want it.
    val DEFAULT_WORDS = listOf(
        "porn", "hentai", "xhamster", "xvideos", "xnxx", "redtube", "spankbang",
        "hanime", "rule34", "brazzers", "fapello", "chaturbate", "stripchat",
        "bongacams", "eporner", "tnaflix"
    )
}
