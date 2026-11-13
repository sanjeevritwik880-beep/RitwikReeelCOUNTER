package com.example.reelcounter

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate

/** Local-only storage: one int per date ("d_2026-11-13") plus a few settings. Nothing else is ever saved. */
class Store(ctx: Context) {
    val p: SharedPreferences = ctx.applicationContext.getSharedPreferences("reelcount", Context.MODE_PRIVATE)

    var limit: Int
        get() = p.getInt("limit", 560)
        set(v) = p.edit().putInt("limit", v.coerceIn(1, 1_000_000)).apply()
    var overlayOn: Boolean
        get() = p.getBoolean("overlay", true)
        set(v) = p.edit().putBoolean("overlay", v).apply()
    var overlayPos: Int // 0 top-center, 1 top-left, 2 top-right
        get() = p.getInt("pos", 0)
        set(v) = p.edit().putInt("pos", v).apply()
    var shorts: Boolean
        get() = p.getBoolean("shorts", false)
        set(v) = p.edit().putBoolean("shorts", v).apply()
    var debug: Boolean
        get() = p.getBoolean("debug", false)
        set(v) = p.edit().putBoolean("debug", v).apply()
    var onboarded: Boolean
        get() = p.getBoolean("onboarded", false)
        set(v) = p.edit().putBoolean("onboarded", v).apply()
    var notifiedDate: String
        get() = p.getString("notified", "") ?: ""
        set(v) = p.edit().putString("notified", v).apply()

    companion object { fun today(): String = LocalDate.now().toString() } // device-local date

    fun count(date: String = today()): Int = p.getInt("d_$date", 0)

    @Synchronized
    fun increment(): Int {
        val d = today() // read on every call => midnight rollover is automatic
        val n = minOf(count(d), Int.MAX_VALUE - 1) + 1
        p.edit().putInt("d_$d", n).apply()
        return n
    }

    fun all(): Map<String, Int> =
        p.all.filterKeys { it.startsWith("d_") }
            .mapKeys { it.key.substring(2) }
            .mapValues { (it.value as? Int) ?: 0 }
            .toSortedMap()

    fun delete(date: String) = p.edit().remove("d_$date").apply()

    fun resetCounts() {
        val e = p.edit()
        p.all.keys.filter { it.startsWith("d_") }.forEach { e.remove(it) }
        e.remove("notified").apply()
    }

    fun export(): String {
        val days = JSONObject(); all().forEach { (k, v) -> days.put(k, v) }
        return JSONObject().put("version", 1).put("days", days).toString(2)
    }

    /** Returns number of days imported. Invalid entries are skipped. */
    fun import(text: String): Int {
        val days = JSONObject(text).getJSONObject("days")
        var n = 0; val e = p.edit()
        for (k in days.keys()) {
            val d = try { LocalDate.parse(k).toString() } catch (_: Exception) { continue }
            val v = days.optInt(k, -1)
            if (v >= 0) { e.putInt("d_$d", v); n++ }
        }
        e.apply(); return n
    }
}
