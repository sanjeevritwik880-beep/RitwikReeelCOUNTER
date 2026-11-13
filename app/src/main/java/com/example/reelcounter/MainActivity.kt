package com.example.reelcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate

class MainActivity : Activity() {
    private lateinit var s: Store
    private var taps = 0
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun full() = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); s = Store(this)
        if (!s.onboarded) { startActivity(Intent(this, OnboardingActivity::class.java)); finish() }
    }

    override fun onResume() { super.onResume(); if (s.onboarded) render() }

    private fun serviceOn(): Boolean {
        val en = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return en.split(':').any { it.contains("$packageName/") && it.endsWith("CounterService") }
    }

    private fun tv(t: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(Color.parseColor("#222222"))
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() }; layoutParams = full() }

    private fun render() {
        val n = s.count(); val lim = s.limit; val over = n >= lim
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(32), dp(16), dp(24))
        }
        if (!serviceOn()) col.addView(tv("⚠ Counting is OFF. Tap here to turn Reel Counter back on in Accessibility settings.", 15f, true).apply {
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#C62828")); setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = full(); setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        col.addView(ImageView(this).apply {
            setImageResource(if (over) R.drawable.brain_tired else R.drawable.brain_happy)
            layoutParams = LinearLayout.LayoutParams(dp(140), dp(140)).apply { topMargin = dp(8) }
        })
        col.addView(tv(n.toString(), 64f, true))
        col.addView(tv("Reels today  ·  limit $lim"))
        col.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = lim; progress = minOf(n, lim); layoutParams = full()
        })

        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        col.addView(BarChart(this, days.map { it.dayOfWeek.name.take(3) to s.count(it.toString()) }, lim)
            .apply { layoutParams = LinearLayout.LayoutParams(-1, dp(180)).apply { topMargin = dp(16) } })

        val all = s.all()
        val base = all.filterKeys { it != Store.today() }.ifEmpty { all }
        col.addView(tv(if (base.isEmpty()) "No history yet." else {
            val best = base.minByOrNull { it.value }!!; val worst = base.maxByOrNull { it.value }!!
            "Average ${base.values.average().toInt()}/day  ·  Best ${best.value} (${best.key})  ·  Worst ${worst.value} (${worst.key})"
        }, 14f).apply { layoutParams = full() })

        col.addView(tv("History", 18f, true).apply { layoutParams = full() })
        for ((d, v) in all.entries.reversed()) {
            col.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutParams = full()
                addView(tv("$d    $v").apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                addView(Button(this@MainActivity).apply {
                    text = "Delete"
                    setOnClickListener {
                        AlertDialog.Builder(this@MainActivity).setMessage("Delete $d?")
                            .setPositiveButton("Delete") { _, _ -> s.delete(d); CounterWidget.update(this@MainActivity); render() }
                            .setNegativeButton("Cancel", null).show()
                    }
                })
            })
        }
        col.addView(btn("Settings") { startActivity(Intent(this, SettingsActivity::class.java)) })
        col.addView(btn("Privacy") { AlertDialog.Builder(this).setMessage(Texts.PRIVACY).setPositiveButton("OK", null).show() })
        col.addView(btn("Battery tips") {
            AlertDialog.Builder(this).setMessage(Texts.BATTERY).setPositiveButton("Open battery settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }.setNegativeButton("Close", null).show()
        })
        col.addView(tv("v1.0" + if (s.debug) "  (debug ON)" else "", 12f).apply {
            setPadding(0, dp(16), 0, 0)
            setOnClickListener { // hidden debug mode: tap 7 times
                if (++taps >= 7) { taps = 0; s.debug = !s.debug; Toast.makeText(context, "Debug ${if (s.debug) "ON" else "OFF"}", Toast.LENGTH_SHORT).show(); render() }
            }
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private class BarChart(c: Context, val vals: List<Pair<String, Int>>, val limit: Int) : View(c) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(cv: Canvas) {
            val d = resources.displayMetrics.density
            val top = 22 * d; val bot = 20 * d
            val mx = maxOf(limit, vals.maxOfOrNull { it.second } ?: 0, 1).toFloat()
            val slot = width / vals.size.toFloat(); p.textAlign = Paint.Align.CENTER; p.textSize = 11 * d
            vals.forEachIndexed { i, (l, v) ->
                val hgt = (height - top - bot) * v / mx
                p.color = if (v >= limit) Color.parseColor("#E53935") else Color.parseColor("#5C6BC0")
                val x = i * slot
                cv.drawRect(x + slot * 0.2f, height - bot - hgt, x + slot * 0.8f, height - bot, p)
                p.color = Color.DKGRAY
                cv.drawText(v.toString(), x + slot / 2, height - bot - hgt - 4 * d, p)
                cv.drawText(l, x + slot / 2, height - 4 * d, p)
            }
        }
    }
}
