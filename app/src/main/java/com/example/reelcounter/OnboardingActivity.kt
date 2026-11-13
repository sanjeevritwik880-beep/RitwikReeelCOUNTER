package com.example.reelcounter

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*

class OnboardingActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(40), dp(20), dp(20)) }
        col.addView(ImageView(this).apply { setImageResource(R.drawable.brain_happy); layoutParams = LinearLayout.LayoutParams(dp(120), dp(120)) })
        col.addView(TextView(this).apply { text = "Welcome to Reel Counter"; textSize = 24f; setPadding(0, dp(12), 0, dp(8)) })
        col.addView(TextView(this).apply { text = Texts.ABOUT; textSize = 15f })
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }
        col.addView(btn("1. Open Accessibility settings") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        col.addView(btn("2. Allow notifications") {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            else Toast.makeText(this, "Nothing to allow on this Android version", Toast.LENGTH_SHORT).show()
        })
        col.addView(btn("3. Done, take me to the app") { Store(this).onboarded = true; startActivity(Intent(this, MainActivity::class.java)); finish() })
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
