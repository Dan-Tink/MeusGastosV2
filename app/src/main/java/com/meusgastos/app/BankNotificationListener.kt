package com.meusgastos.app

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Lê as notificações dos apps de banco e transforma compras, pagamentos e recebimentos em
 * lançamentos pendentes (ou lançados direto, se o modo automático estiver ligado).
 *
 * Só olha para os apps da lista [BankApps], e só se estiverem ligados. Nada sai do aparelho
 * e o texto original da notificação não é guardado.
 */
class BankNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        val bank = BankApps.packages[n.packageName] ?: return

        val settings = Settings(this)
        if (!settings.captureEnabled || n.packageName in settings.captureDisabled) return

        val notification = n.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()

        val parsed = parseBankNotification(title, text) ?: return
        val postTime = n.postTime
        val appContext = applicationContext
        scope.launch {
            try {
                CaptureProcessor.handle(appContext, bank, parsed, postTime)
            } catch (e: Exception) {
                // Uma notificação com problema não pode derrubar o serviço.
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

object CaptureProcessor {
    /** Mesma compra notificada de novo (o Android reenvia ao atualizar) dentro desta janela é ignorada. */
    private const val DEDUPE_WINDOW_MS = 2 * 60 * 1000L
    private const val KEEP_DAYS_MS = 7 * 24 * 60 * 60 * 1000L

    suspend fun handle(context: Context, bank: String, p: ParsedNotification, postTime: Long) {
        val dao = AppDatabase.get(context).dao()
        val settings = Settings(context)
        val now = System.currentTimeMillis()

        val repeated = dao.countRecentPending(
            bank, p.amountCents, p.isIncome, p.merchant, now - DEDUPE_WINDOW_MS
        )
        if (repeated > 0) return
        dao.purgePending(now - KEEP_DAYS_MS)

        val day = Instant.ofEpochMilli(postTime).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        // Tipo e categoria: o que você já usou com o mesmo nome, senão um palpite, senão "Outros".
        val types = dao.typesOnce()
        val categories = dao.categoriesOnce()
        val known = if (p.isIncome) null else dao.lastForDescription(p.merchant)
        val type = known?.type?.takeIf { t -> types.any { it.name == t } }
            ?: settings.lastType.takeIf { t -> types.any { it.name == t } }
            ?: types.firstOrNull { !it.fixed }?.name
            ?: types.firstOrNull()?.name
            ?: "Variável"
        val category = known?.category?.takeIf { c -> categories.any { it.name == c } }
            ?: guessCategory(p.merchant)?.takeIf { c -> categories.any { it.name == c } }
            ?: "Outros"

        val auto = settings.captureAuto
        val row = PendingTx(
            source = bank,
            amountCents = p.amountCents,
            isIncome = p.isIncome,
            description = p.merchant,
            method = p.method,
            epochDay = day,
            createdAt = now,
            type = type,
            category = category,
            status = if (auto) 1 else 0
        )
        dao.insertPending(row)

        if (auto) {
            if (p.isIncome) {
                dao.insertIncome(Income(description = p.merchant, amountCents = p.amountCents, epochDay = day))
            } else {
                dao.insert(
                    Expense(
                        type = type,
                        description = p.merchant,
                        amountCents = p.amountCents,
                        epochDay = day,
                        category = category
                    )
                )
            }
        }
    }
}
