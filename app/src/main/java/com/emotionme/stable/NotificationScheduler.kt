package com.emotionme.stable

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object NotificationScheduler {

    private const val REQ_NOON = 100
    private const val REQ_EVENING = 101

    fun scheduleDaily(context: Context, baseHour: Int) {
        schedule(context, baseHour, REQ_NOON)
        schedule(context, (baseHour + 6) % 24, REQ_EVENING)
    }

    fun schedule(context: Context, hour: Int, req: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            req,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancelAll(context: Context) {
        cancel(context, REQ_NOON)
        cancel(context, REQ_EVENING)
    }

    private fun cancel(context: Context, req: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            req,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }

    fun rescheduleFromSettings(context: Context){
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("notify_enabled", false)
        val hour = prefs.getInt("notify_hour", 20)

        cancelAll(context)

        if (enabled){
            scheduleDaily(context, hour)
        }
    }
}