package com.stepandemianenko.sdtfitness.profile

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar

class RoutineReminderScheduler(private val appContext: Context) {

    private val alarmManager: AlarmManager
        get() = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(accountId: String, routine: RoutineSettings) {
        cancelAll(accountId)
        if (!routine.reminderEnabled || routine.dayIds.isEmpty()) return
        routine.allReminderTimes.take(MAX_SLOTS).forEachIndexed { slot, time ->
            val (hour, minute) = parseReminderTime(time) ?: return@forEachIndexed
            armSlot(accountId, slot, hour, minute)
        }
    }

    fun rescheduleSlot(accountId: String, slot: Int, hour: Int, minute: Int) {
        armSlot(accountId, slot, hour, minute)
    }

    fun cancelAll(accountId: String) {
        for (slot in 0 until MAX_SLOTS) {
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                slot,
                reminderIntent(accountId, slot),
                PENDING_INTENT_FLAGS or PendingIntent.FLAG_NO_CREATE
            ) ?: continue
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun armSlot(accountId: String, slot: Int, hour: Int, minute: Int) {
        val triggerAtMillis = nextTriggerMillis(hour, minute)
        val intent = reminderIntent(accountId, slot).apply {
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(appContext, slot, intent, PENDING_INTENT_FLAGS)
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val trigger = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!trigger.after(now)) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }
        return trigger.timeInMillis
    }

    private fun reminderIntent(accountId: String, slot: Int): Intent {
        return Intent(appContext, RoutineReminderReceiver::class.java).apply {
            action = ACTION_ROUTINE_REMINDER
            data = Uri.parse("sdtfitness://routine-reminder/$accountId/$slot")
            putExtra(EXTRA_ACCOUNT_ID, accountId)
            putExtra(EXTRA_SLOT, slot)
        }
    }

    companion object {
        const val ACTION_ROUTINE_REMINDER = "com.stepandemianenko.sdtfitness.action.ROUTINE_REMINDER"
        const val EXTRA_ACCOUNT_ID = "extra_account_id"
        const val EXTRA_SLOT = "extra_slot"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"

        private const val MAX_SLOTS = 16
        private val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
