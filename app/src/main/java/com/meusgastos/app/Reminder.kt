package com.meusgastos.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminder {
    private const val WORK_NAME = "lembrete_diario"
    const val CHANNEL_ID = "lembrete"

    /** Agenda um lembrete por dia, por volta da hora escolhida. */
    fun schedule(context: Context, hour: Int, replace: Boolean) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext

        // Contas pendentes vencidas ou que vencem até amanhã; e se já lançou algo hoje.
        val dao = AppDatabase.get(ctx).dao()
        val today = LocalDate.now().toEpochDay()
        val due = dao.countPendingUntil(today + 1)
        val noLaunchToday = dao.countOnDay(today) == 0
        if (due == 0 && !noLaunchToday) return Result.success()

        val parts = mutableListOf<String>()
        if (due > 0) {
            parts.add(
                if (due == 1) "Você tem 1 conta pendente vencida ou a vencer até amanhã."
                else "Você tem $due contas pendentes vencidas ou a vencer até amanhã."
            )
        }
        if (noLaunchToday) parts.add("Lançou os gastos de hoje?")
        val message = parts.joinToString(" ")

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                Reminder.CHANNEL_ID,
                "Lembrete diário",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val intent = Intent(ctx, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, Reminder.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Meus Gastos")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
        return Result.success()
    }
}
