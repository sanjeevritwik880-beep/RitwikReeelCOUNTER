package com.example.reelcounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews

class CounterWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { update(c) }

    companion object {
        fun update(c: Context) {
            try {
                val s = Store(c); val n = s.count(); val lim = s.limit
                val rv = RemoteViews(c.packageName, R.layout.widget)
                rv.setImageViewResource(R.id.w_img, if (n >= lim) R.drawable.brain_tired else R.drawable.brain_happy)
                rv.setTextViewText(R.id.w_count, n.toString())
                rv.setTextViewText(R.id.w_sub, "$n / $lim")
                val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                rv.setOnClickPendingIntent(R.id.w_root, pi)
                AppWidgetManager.getInstance(c).updateAppWidget(ComponentName(c, CounterWidget::class.java), rv)
            } catch (t: Throwable) { Log.w("ReelCounter", "widget: $t") }
        }
    }
}
