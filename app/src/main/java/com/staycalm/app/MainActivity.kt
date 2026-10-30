package com.staycalm.app

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var wordsView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(24))
        }

        col.addView(label("StayCalm", 28f, true))

        status = label("", 16f)
        col.addView(status)

        col.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        col.addView(label("Find StayCalm in the list and turn it on.", 14f))

        stats = label("", 16f)
        col.addView(stats)

        col.addView(Button(this).apply {
            text = "Preview calm screen"
            setOnClickListener { preview() }
        })

        col.addView(label("Blocked words", 18f, true))
        wordsView = label("", 14f)
        col.addView(wordsView)

        val input = EditText(this).apply {
            hint = "Add a word (for example: hamster)"
            setSingleLine()
        }
        col.addView(input)
        col.addView(Button(this).apply {
            text = "Add word"
            setOnClickListener {
                if (Prefs.addWord(this@MainActivity, input.text.toString())) {
                    input.text.clear()
                    refresh()
                } else {
                    Toast.makeText(this@MainActivity, "Type at least 3 letters", Toast.LENGTH_SHORT).show()
                }
            }
        })
        col.addView(label("Words can only be added, not removed.", 13f))

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        }

    private fun serviceOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName) && enabled.contains("BlockerService")
    }

    private fun refresh() {
        status.text = if (serviceOn()) "Blocker is ON" else "Blocker is OFF. Turn it on in Accessibility settings."

        val last = Prefs.lastBlock(this)
        stats.text = if (last == 0L) {
            "No blocks yet."
        } else {
            "Time since last block: ${fmt(System.currentTimeMillis() - last)}\n" +
                "Total blocks: ${Prefs.totalBlocks(this)}"
        }

        val extra = Prefs.extraWords(this)
        val mine = if (extra.isNotEmpty()) " + yours: ${extra.joinToString(", ")}" else ""
        wordsView.text = "${Config.DEFAULT_WORDS.size} built-in words$mine"
    }

    private fun preview() {
        val s = BlockerService.instance
        if (s == null) {
            Toast.makeText(this, "Turn the blocker on first", Toast.LENGTH_SHORT).show()
        } else {
            s.showCalmOverlay()
        }
    }

    private fun fmt(ms: Long): String {
        val totalMin = ms / 60000
        val d = totalMin / 1440
        val h = (totalMin % 1440) / 60
        val m = totalMin % 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }
}
