@file:OptIn(ExperimentalMaterial3Api::class)

package com.meusgastos.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDate

/** O Android permite que o app leia notificações? (A permissão é dada nos ajustes do sistema.) */
fun isListenerEnabled(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Estado da permissão, atualizado ao voltar dos ajustes do sistema. */
@Composable
fun rememberListenerEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isListenerEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = isListenerEnabled(context) }
    return enabled
}

private fun openListenerSettings(context: Context) {
    try {
        context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    } catch (e: Exception) {
        // Aparelho sem essa tela de ajustes: nada a fazer.
    }
}

/** Cartão de convite, mostrado enquanto a leitura de notificações não foi ativada. */
fun LazyListScope.captureTip(show: Boolean, onEnable: () -> Unit, onDismiss: () -> Unit) {
    if (!show) return
    item(key = "capture-tip") {
        Card(
            shape = RoundedCornerShape(16.dp),
            border = CardBorder,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Lance gastos sozinho", fontWeight = FontWeight.Bold)
                Text(
                    "Deixe o app ler as notificações do seu banco. Cada compra, Pix e pagamento " +
                        "aparece aqui para você confirmar com um toque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Agora não") }
                    Button(onClick = onEnable, shape = RoundedCornerShape(12.dp)) { Text("Ativar") }
                }
            }
        }
    }
}

/** Lista "Pendentes": o que foi detectado nas notificações e ainda não virou lançamento. */
fun LazyListScope.pendingItems(
    pending: List<PendingTx>,
    history: List<Expense>,
    onConfirm: (PendingTx) -> Unit,
    onConfirmAll: () -> Unit,
    onEdit: (PendingTx) -> Unit,
    onDismiss: (PendingTx) -> Unit
) {
    if (pending.isEmpty()) return
    item(key = "pending-header") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Pendentes (${pending.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (pending.size > 1) {
                TextButton(onClick = onConfirmAll) { Text("Confirmar todos") }
            }
        }
    }
    items(pending, key = { "pending-${it.id}" }) { p ->
        // Avisa se parece repetido: já existe um gasto do mesmo valor no mesmo dia.
        val duplicate = !p.isIncome &&
            history.any { it.amountCents == p.amountCents && it.epochDay == p.epochDay }
        PendingCard(
            p = p,
            duplicate = duplicate,
            onConfirm = { onConfirm(p) },
            onEdit = { onEdit(p) },
            onDismiss = { onDismiss(p) }
        )
    }
}

@Composable
private fun PendingCard(
    p: PendingTx,
    duplicate: Boolean,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val color = if (p.isIncome) PositiveColor else MaterialTheme.colorScheme.error
    val today = LocalDate.now().toEpochDay()
    val whenText = when (p.epochDay) {
        today -> "hoje"
        today - 1 -> "ontem"
        else -> LocalDate.ofEpochDay(p.epochDay).fmt()
    }
    val details = buildString {
        append(p.source).append(" · ").append(p.method).append(" · ").append(whenText)
        if (!p.isIncome) append(" · ").append(p.category)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (p.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = if (p.isIncome) "Entrada" else "Saída",
                        tint = color
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        p.description,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (duplicate) {
                        Text(
                            "Parece que você já lançou um gasto igual",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningColor
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    p.amountCents.toMoney(),
                    fontFamily = MoneyFont,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Ignorar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!p.isIncome) {
                    TextButton(onClick = onEdit) { Text("Editar") }
                }
                Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Confirmar")
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun CaptureDialog(
    captureEnabled: Boolean,
    auto: Boolean,
    disabled: Set<String>,
    onEnabled: (Boolean) -> Unit,
    onAuto: (Boolean) -> Unit,
    onToggleBank: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listenerOn = rememberListenerEnabled()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Captura por notificação") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (listenerOn) "Permissão do Android: ativada" else "Permissão do Android: desativada",
                    fontWeight = FontWeight.SemiBold,
                    color = if (listenerOn) PositiveColor else WarningColor
                )
                if (!listenerOn) {
                    Text(
                        "Abra os ajustes e permita o Meus Gastos em \"Acesso às notificações\". " +
                            "Se o Android avisar de \"configuração restrita\", vá em Informações do app, " +
                            "toque nos três pontinhos e escolha \"Permitir configurações restritas\"; " +
                            "depois tente de novo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { openListenerSettings(context) },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (listenerOn) "Abrir ajustes" else "Ativar permissão") }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                SwitchRow(
                    "Ler notificações",
                    "Liga ou desliga a captura sem mexer na permissão.",
                    captureEnabled,
                    onEnabled
                )
                SwitchRow(
                    "Lançar sozinho",
                    "Sem revisar. Desligado, cada item vai para \"Pendentes\" e você confirma com um toque. " +
                        "Recomendado até você confiar nas leituras.",
                    auto,
                    onAuto
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Text(
                    "Apps monitorados",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BankApps.packages.forEach { (pkg, name) ->
                    SwitchRow(name, null, pkg !in disabled) { on -> onToggleBank(pkg, on) }
                }

                Text(
                    "Privacidade: só as notificações desses apps são lidas, tudo fica no aparelho " +
                        "e o texto original não é guardado, só o valor e o nome do estabelecimento.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
