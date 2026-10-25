@file:OptIn(ExperimentalMaterial3Api::class)

package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val PaidColor = Color(0xFF34D399)
private val RowPendingColor = Color(0xFFFBBF24)

/**
 * Linha da lista. O quadrado da esquerda é o botão de pago/pendente:
 * verde com ✓ = pago; amarelo com o dia = pendente; vermelho = atrasada.
 */
@Composable
fun ExpenseRow(
    e: Expense,
    today: LocalDate,
    onClick: () -> Unit,
    onTogglePaid: () -> Unit
) {
    val date = LocalDate.ofEpochDay(e.epochDay)
    val due = e.dueText(today)
    val overdue = !e.paid && date.isBefore(today)
    val statusColor = when {
        e.paid -> PaidColor
        overdue -> MaterialTheme.colorScheme.error
        else -> RowPendingColor
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .clickable(onClick = onTogglePaid),
                contentAlignment = Alignment.Center
            ) {
                if (e.paid) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Pago. Toque para marcar como pendente",
                        tint = statusColor
                    )
                } else {
                    Text(
                        date.dayOfMonth.toString().padStart(2, '0'),
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.title(),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (e.receipt != null) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Tem comprovante",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "${e.type} • ${e.category} • ${date.fmt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (due != null) {
                    Text(
                        due,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(e.amountCents.toMoney(), fontWeight = FontWeight.Bold)
        }
    }
}
