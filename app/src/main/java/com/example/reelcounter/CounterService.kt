package com.example.reelcounter

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDateTime
import java.time.ZoneId

class CounterService : AccessibilityService() {
    companion object {
        const val IG = "com.instagram.android"
        const val YT = "com.google.android.youtube"
        const val DEBOUNCE_MS = 700L
        // ---- TUNE HERE if Instagram changes its layout (see isReelSwipe). These are best-guess view IDs of the Reels pager. ----
        val IG_REEL_IDS = listOf("com.instagram.android:id/clips_viewer_view_pager", "com.instagram.android:id/clips_viewer_clip_container")
        val YT_SHORT_IDS = listOf("com.google.android.youtube:id/reel_recycler", "com.google.android.youtube:id/reel_player_page_container")
        const val TAG = "ReelCounter"
    }

    private lateinit var store: Store
    private val h = Handler(Looper.getMainLooper())
    private var overlay: LinearLayout? = null
    private var img: ImageView? = null
    private var txt: TextView? = null
    private var lastCount = 0L
    private var widgetPending = false
    private var inTracked = false
    private var dbg = ""
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> try { applyPrefs() } catch (_: Throwable) {} }
    private val dateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { onNewDay() }
    }
    private val widgetRun = Runnable { widgetPending = false; CounterWidget.update(this) }
    private val midnightRun = Runnable { onNewDay() }

    override fun onServiceConnected() {
        try {
            store = Store(this)
            store.p.registerOnSharedPreferenceChangeListener(prefListener)
            val f = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED); addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            registerReceiver(dateReceiver, f)
            scheduleMidnight()
            CounterWidget.update(this)
        } catch (t: Throwable) { Log.w(TAG, "connect: $t") }
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        try {
            if (e == null || !::store.isInitialized) return
            val pkg = e.packageName?.toString() ?: return
            val tracked = pkg == IG || (store.shorts && pkg == YT)

            if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (tracked) {
                    inTracked = true
                    if (store.overlayOn) showOverlay()
                } else if (!isTransient(pkg)) { // user went to another app / home
                    inTracked = false; hideOverlay()
                }
                return
            }
            if (!tracked || e.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastCount < DEBOUNCE_MS) return        // debounce: one swipe = one count
            if (!isReelSwipe(e, pkg)) return
            lastCount = now
            onCounted()
        } catch (t: Throwable) { Log.w(TAG, "event: $t") } // never crash the service
    }

    /**
     * >>> THE ONE FUNCTION TO ADJUST <<<
     * Decides "did the user just swipe to a new Reel?".
     * Rule: a scroll event happened AND the Reels viewer is on screen (one of the known view IDs exists).
     * Only view-ID names are checked; no text is ever read.
     * canRetrieveWindowContent is needed ONLY for this ID lookup; without it we can't tell Reels from the feed.
     * To tune: turn on Debug mode (tap the version text on the main screen 7 times), swipe in Instagram,
     * and edit IG_REEL_IDS above. In debug mode the pill shows the event class + source view ID.
     */
    private fun isReelSwipe(e: AccessibilityEvent, pkg: String): Boolean {
        val ids = if (pkg == IG) IG_REEL_IDS else YT_SHORT_IDS
        val root = rootInActiveWindow ?: return false
        try {
            if (store.debug) {
                val sid = try { e.source?.viewIdResourceName } catch (_: Throwable) { null }
                dbg = "${e.className?.toString()?.substringAfterLast('.')} ${sid?.substringAfter(":id/")}"
                Log.d(TAG, "scroll class=${e.className} srcId=$sid")
            }
            for (id in ids) {
                val l = root.findAccessibilityNodeInfosByViewId(id)
                if (!l.isNullOrEmpty()) return true
            }
            return false
        } finally {
            if (Build.VERSION.SDK_INT < 33) root.recycle()
        }
    }

    /** Keyboards, system UI, etc. pop up on top of Instagram; don't treat them as "left Instagram". */
    private fun isTransient(pkg: String) =
        pkg == "com.android.systemui" || pkg.contains("inputmethod") || pkg.contains("keyboard") || pkg == packageName

    private fun onCounted() {
        val n = store.increment()
        refresh()
        if (n == store.limit) CounterWidget.update(this) else scheduleWidget()
        if (n >= store.limit) notifyLimit()
    }

    private fun onNewDay() {
        try { refresh(); CounterWidget.update(this); scheduleMidnight() } catch (_: Throwable) {}
    }

    private fun scheduleMidnight() {
        h.removeCallbacks(midnightRun)
        val now = LocalDateTime.now()
        val next = now.toLocalDate().plusDays(1).atStartOfDay()
        val ms = java.time.Duration.between(now, next).toMillis() + 1000
        h.postDelayed(midnightRun, ms.coerceAtLeast(1000))
    }

    private fun scheduleWidget() { if (!widgetPending) { widgetPending = true; h.postDelayed(widgetRun, 5000) } } // throttle: max 1 update / 5 s

    private fun notifyLimit() {
        if (store.notifiedDate == Store.today()) return // only once per day
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("limit", "Daily limit", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(1, Notification.Builder(this, "limit").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reel limit reached").setContentText("You've hit your limit. Maybe take a break?")
            .setContentIntent(pi).setAutoCancel(true).build())
        store.notifiedDate = Store.today()
    }

    // ---------------- overlay (TYPE_ACCESSIBILITY_OVERLAY: needs no extra permission) ----------------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun applyPrefs() {
        hideOverlay()
        if (inTracked && store.overlayOn) showOverlay()
        // YouTube toggle turned off while in YouTube: stop counting/showing there
        if (!store.shorts && inTracked && rootInActiveWindow?.packageName == YT) { inTracked = false; hideOverlay() }
    }

    private fun showOverlay() {
        if (overlay != null) { refresh(); return }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(14), dp(4))
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0x99000000.toInt()) }
        }
        img = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)) }
        txt = TextView(this).apply { setTextColor(Color.WHITE); textSize = 14f; setPadding(dp(6), 0, 0, 0) }
        row.addView(img); row.addView(txt)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or when (store.overlayPos) { 1 -> Gravity.START; 2 -> Gravity.END; else -> Gravity.CENTER_HORIZONTAL }
            x = dp(8); y = dp(32)
        }
        try { wm.addView(row, lp); overlay = row; refresh() } catch (t: Throwable) { Log.w(TAG, "overlay: $t"); overlay = null }
    }

    private fun hideOverlay() {
        try { overlay?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } } catch (_: Throwable) {}
        overlay = null; img = null; txt = null
    }

    private fun refresh() {
        val n = store.count()
        txt?.text = "Reels: $n" + if (store.debug && dbg.isNotEmpty()) "  [$dbg]" else ""
        img?.setImageResource(if (n >= store.limit) R.drawable.brain_tired else R.drawable.brain_happy)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        try {
            h.removeCallbacksAndMessages(null); hideOverlay()
            if (::store.isInitialized) store.p.unregisterOnSharedPreferenceChangeListener(prefListener)
            unregisterReceiver(dateReceiver)
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}
