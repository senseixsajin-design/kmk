package com.staycalm.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class BlockerService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: BlockerService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastTrigger = 0L
    private var lastScan = 0L
    private var overlay: View? = null
    private var banner: View? = null
    private var receiverRegistered = false

    // After the phone is unlocked, show the calm screen if a block just happened.
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Prefs.pendingCalm(context)) {
                Prefs.setPendingCalm(context, false)
                handler.postDelayed({ showCalmOverlay() }, 700)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun onDestroy() {
        instance = null
        if (receiverRegistered) {
            try {
                unregisterReceiver(unlockReceiver)
            } catch (e: Exception) {
            }
            receiverRegistered = false
        }
        removeOverlay()
        removeBanner()
        super.onDestroy()
    }

    override fun onInterrupt() {}

    // ------------------------------------------------------------------ events

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return
        if (overlay != null) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTrigger < Config.COOLDOWN_MS) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (event.isPassword) return
                val typed = event.text.joinToString(" ")
                if (Matcher.hit(typed, Prefs.words(this))) trigger()
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (pkg in Config.BROWSERS && now - lastScan > 500) {
                    lastScan = now
                    if (scanBrowser()) trigger()
                }
            }
        }
    }

    /** Looks only at the address bar, text fields and the page title, not the whole page. */
    private fun scanBrowser(): Boolean {
        val root = rootInActiveWindow ?: return false
        val words = Prefs.words(this)
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < 600) {
            val node = stack.removeLast()
            visited++
            val id = node.viewIdResourceName?.lowercase() ?: ""
            val cls = node.className?.toString() ?: ""
            val interesting = id.contains("url") || id.contains("location_bar") ||
                id.contains("omnibox") || cls.endsWith("EditText") ||
                cls == "android.webkit.WebView"
            if (interesting) {
                val t = "${node.text ?: ""} ${node.contentDescription ?: ""}"
                if (Matcher.hit(t, words)) return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.add(it) }
            }
        }
        return false
    }

    // ----------------------------------------------------------------- blocking

    private fun trigger() {
        lastTrigger = SystemClock.elapsedRealtime()
        Prefs.recordBlock(this)
        Prefs.setPendingCalm(this, true)
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }, 400)
    }

    // -------------------------------------------------------------- calm screen

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun styledButton(label: String, color: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 18f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(color))
                cornerRadius = dp(16).toFloat()
            }
            setOnClickListener { onClick() }
        }

    fun showCalmOverlay() {
        if (overlay != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F20B1020"))
            setPadding(dp(28), dp(48), dp(28), dp(48))
        }

        root.addView(TextView(this).apply {
            text = Config.TITLE
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }, lp())

        root.addView(TextView(this).apply {
            text = Config.SUBTITLE
            textSize = 18f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER
        }, lp(16))

        for (choice in Config.CHOICES) {
            root.addView(styledButton(choice.label, choice.color) { choose(choice) }, lp(14))
        }

        val close = styledButton("Close", "#334155") { removeOverlay() }
        close.isEnabled = false
        root.addView(close, lp(36))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(root, params)
        overlay = root

        // Close button unlocks after a short forced pause.
        var left = Config.CLOSE_DELAY_SEC
        val tick = object : Runnable {
            override fun run() {
                if (overlay !== root) return
                if (left > 0) {
                    close.text = "Close (wait ${left}s)"
                    left--
                    handler.postDelayed(this, 1000)
                } else {
                    close.text = "Close"
                    close.isEnabled = true
                }
            }
        }
        handler.post(tick)
    }

    private fun choose(choice: Config.Choice) {
        val launch = packageManager.getLaunchIntentForPackage(choice.pkg)
        if (launch == null) {
            Toast.makeText(this, "${choice.label} is not installed", Toast.LENGTH_LONG).show()
            return
        }
        removeOverlay()
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
        handler.postDelayed({ showBanner(Config.AFTER_PICK) }, 800)
    }

    private fun showBanner(message: String) {
        removeBanner()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val tv = TextView(this).apply {
            text = message
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E61E293B"))
                cornerRadius = dp(18).toFloat()
            }
        }
        val params = WindowManager.LayoutParams(
            resources.displayMetrics.widthPixels - dp(32),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(64)
        }
        wm.addView(tv, params)
        banner = tv
        handler.postDelayed({ if (banner === tv) removeBanner() }, 7000)
    }

    private fun removeOverlay() {
        overlay?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (e: Exception) {
            }
        }
        overlay = null
    }

    private fun removeBanner() {
        banner?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (e: Exception) {
            }
        }
        banner = null
    }
}
