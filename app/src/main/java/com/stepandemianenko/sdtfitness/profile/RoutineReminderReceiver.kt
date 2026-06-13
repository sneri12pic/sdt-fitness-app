package com.stepandemianenko.sdtfitness.profile

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stepandemianenko.sdtfitness.R
import com.stepandemianenko.sdtfitness.data.local.UserSettingsEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class RoutineReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RoutineReminderScheduler.ACTION_ROUTINE_REMINDER) return
        val accountId = intent.getStringExtra(RoutineReminderScheduler.EXTRA_ACCOUNT_ID) ?: return
        val slot = intent.getIntExtra(RoutineReminderScheduler.EXTRA_SLOT, -1)
        val hour = intent.getIntExtra(RoutineReminderScheduler.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(RoutineReminderScheduler.EXTRA_MINUTE, -1)
        if (slot < 0 || hour !in 0..23 || minute !in 0..59) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleAlarm(appContext, accountId, slot, hour, minute)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, accountId: String, slot: Int, hour: Int, minute: Int) {
        val settings = WorkoutDatabase.getInstance(context).userSettingsDao().getByAccountId(accountId)
        if (settings != null && shouldNotify(settings, slot)) {
            notify(context, slot)
        }
        RoutineReminderScheduler(context).rescheduleSlot(accountId, slot, hour, minute)
    }

    private fun shouldNotify(settings: UserSettingsEntity, slot: Int): Boolean {
        if (!settings.routineReminderEnabled) return false
        if (todayDayId() !in decodeCsvSet(settings.routineDayIdsCsv)) return false
        return slot < scheduledTimeCount(settings)
    }

    private fun scheduledTimeCount(settings: UserSettingsEntity): Int {
        val preset = decodeCsvSet(settings.routineReminderTimesCsv)
        val custom = decodeCsvSet(settings.routineCustomReminderTimesCsv)
        return (preset + custom).count { parseReminderTime(it) != null }
    }

    private fun decodeCsvSet(value: String): Set<String> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private fun todayDayId(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "mon"
            Calendar.TUESDAY -> "tue"
            Calendar.WEDNESDAY -> "wed"
            Calendar.THURSDAY -> "thu"
            Calendar.FRIDAY -> "fri"
            Calendar.SATURDAY -> "sat"
            else -> "sun"
        }
    }

    private fun notify(context: Context, slot: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(context, slot, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val body = context.getString(R.string.routine_reminder_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_routine_reminder)
            .setContentTitle(context.getString(R.string.routine_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + slot, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.routine_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.routine_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "routine_reminders"
        private const val NOTIFICATION_ID_BASE = 4200
    }
}
