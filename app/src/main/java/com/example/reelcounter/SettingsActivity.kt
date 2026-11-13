package com.example.reelcounter

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*

@Suppress("DEPRECATION")
class SettingsActivity : Activity() {
    private lateinit var s: Store
    private lateinit var limitEt: EditText
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); s = Store(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(40), dp(20), dp(20)) }
        col.addView(TextView(this).apply { text = "Settings"; textSize = 24f })
        col.addView(TextView(this).apply { text = "Daily limit"; setPadding(0, dp(16), 0, 0) })
        limitEt = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(s.limit.toString()) }
        col.addView(limitEt)
        col.addView(Switch(this).apply { text = "Show counter pill over Instagram"; isChecked = s.overlayOn
            setOnCheckedChangeListener { _, v -> s.overlayOn = v } })
        col.addView(TextView(this).apply { text = "Pill position"; setPadding(0, dp(12), 0, 0) })
        col.addView(RadioGroup(this).apply {
            listOf("Top centre", "Top left", "Top right").forEachIndexed { i, t -> addView(RadioButton(context).apply { text = t; id = 100 + i }) }
            check(100 + s.overlayPos); setOnCheckedChangeListener { _, id -> s.overlayPos = id - 100 }
        })
        col.addView(Switch(this).apply { text = "Also count YouTube Shorts"; isChecked = s.shorts
            setOnCheckedChangeListener { _, v -> s.shorts = v } })
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }
        col.addView(btn("Export data (JSON)") {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json").putExtra(Intent.EXTRA_TITLE, "reel-counts.json"), 1)
        })
        col.addView(btn("Import data (JSON)") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), 2)
        })
        col.addView(btn("Reset all data") {
            AlertDialog.Builder(this).setMessage("Delete ALL saved daily counts? This cannot be undone.")
                .setPositiveButton("Delete everything") { _, _ -> s.resetCounts(); CounterWidget.update(this); toast("Data reset") }
                .setNegativeButton("Cancel", null).show()
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    override fun onPause() {
        super.onPause()
        limitEt.text.toString().toIntOrNull()?.let { if (it >= 1) s.limit = it }
        CounterWidget.update(this)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (res != RESULT_OK) return
        try {
            if (req == 1) {
                contentResolver.openOutputStream(uri)?.use { it.write(s.export().toByteArray()) }; toast("Exported")
            } else if (req == 2) {
                val txt = contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: ""
                toast("Imported ${s.import(txt)} days"); CounterWidget.update(this)
            }
        } catch (t: Throwable) { toast("Failed: file not valid") }
    }
}
