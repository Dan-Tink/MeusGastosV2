@file:OptIn(ExperimentalMaterial3Api::class)

package com.meusgastos.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

private val DebtColor = Color(0xFFFB923C)
private val DoneColor = Color(0xFF34D399)

/** Aba de dívidas: só um lembrete do que você deve, sem prazo e sem mexer no saldo do mês. */
@Composable
fun DebtsScreen(vm: MainViewModel) {
    val debts by vm.debts.collectAsStateWithLifecycle()

    var showNew by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Debt?>(null) }
    var paying by remember { mutableStateOf<Debt?>(null) }
    var editing by remember { mutableStateOf<Debt?>(null) }
    var deleting by remember { mutableStateOf<Debt?>(null) }

    val openDebts = debts.filter { !it.settled }
    val done = debts.filter { it.settled }
    val totalOpen = openDebts.sumOf { it.remainingCents }

    // Mantém a dívida selecionada atualizada depois de abater um valor.
    val current = selected?.let { s -> debts.firstOrNull { it.id == s.id } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nova dívida") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Dívidas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "Total que você deve",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            totalOpen.toMoney(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalOpen > 0) DebtColor else DoneColor
                        )
                        Text(
                            if (openDebts.isEmpty()) "Nenhuma dívida em aberto."
                            else "${openDebts.size} em aberto • não altera o saldo do mês.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (debts.isEmpty()) {
                item {
                    Text(
                        "Toque em \"Nova dívida\" para anotar, por exemplo, um valor que alguém te emprestou.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            items(openDebts, key = { it.id }) { d -> DebtCard(d) { selected = d } }

            if (done.isNotEmpty()) {
                item {
                    Text(
                        "Quitadas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(done, key = { it.id }) { d -> DebtCard(d) { selected = d } }
            }
        }
    }

    if (showNew) {
        DebtSheet(
            initial = null,
            onDismiss = { showNew = false },
            onSave = {
                vm.saveDebt(it)
                showNew = false
            }
        )
    }

    editing?.let { d ->
        DebtSheet(
            initial = d,
            onDismiss = { editing = null },
            onSave = { edited ->
                // Se o novo valor já está coberto pelo que foi pago, a dívida fica quitada.
                val paid = d.paidCents.coerceAtMost(edited.amountCents)
                val settledDay = if (paid >= edited.amountCents) {
                    d.settledDay ?: LocalDate.now().toEpochDay()
                } else null
                vm.saveDebt(edited.copy(paidCents = paid, settledDay = settledDay))
                editing = null
                selected = null
            }
        )
    }

    if (current != null && paying == null && editing == null && deleting == null) {
        DebtActions(
            d = current,
            onDismiss = { selected = null },
            onPay = { paying = current },
            onSettle = {
                vm.settleDebt(current)
                selected = null
            },
            onReopen = {
                vm.reopenDebt(current)
                selected = null
            },
            onEdit = { editing = current },
            onDelete = { deleting = current }
        )
    }

    paying?.let { d ->
        PayDialog(
            d = d,
            onDismiss = { paying = null },
            onPay = { cents ->
                vm.payDebt(d, cents)
                paying = null
                selected = null
            },
            onAll = {
                vm.settleDebt(d)
                paying = null
                selected = null
            }
        )
    }

    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir dívida?") },
            text = { Text("\"${d.creditor}\" será removida da lista.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDebt(d)
                    deleting = null
                    selected = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DebtCard(d: Debt, onClick: () -> Unit) {
    val color = if (d.settled) DoneColor else DebtColor
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(d.creditor, fontWeight = FontWeight.SemiBold)
                    if (d.note.isNotBlank()) {
                        Text(
                            d.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (d.settled) "Quitada" else d.remainingCents.toMoney(),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            if (!d.settled) {
                LinearProgressIndicator(
                    progress = { (d.paidCents.toFloat() / d.amountCents.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = DoneColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Text(
                if (d.settled) {
                    "${d.amountCents.toMoney()} • quitada em ${LocalDate.ofEpochDay(d.settledDay ?: d.epochDay).fmt()}"
                } else if (d.paidCents > 0) {
                    "Total ${d.amountCents.toMoney()} • já pago ${d.paidCents.toMoney()} • desde ${LocalDate.ofEpochDay(d.epochDay).fmt()}"
                } else {
                    "Total ${d.amountCents.toMoney()} • desde ${LocalDate.ofEpochDay(d.epochDay).fmt()}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Ações ao tocar numa dívida. */
@Composable
private fun DebtActions(
    d: Debt,
    onDismiss: () -> Unit,
    onPay: () -> Unit,
    onSettle: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(d.creditor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (d.note.isNotBlank()) Text(d.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (d.settled) "Quitada • total ${d.amountCents.toMoney()}"
                else "Falta ${d.remainingCents.toMoney()} de ${d.amountCents.toMoney()}",
                color = if (d.settled) DoneColor else DebtColor,
                fontWeight = FontWeight.SemiBold
            )

            if (d.settled) {
                OutlinedButton(
                    onClick = onReopen,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Reabrir (zera o valor pago)") }
            } else {
                Button(
                    onClick = onPay,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Pagar / abater valor") }
                OutlinedButton(
                    onClick = onSettle,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Paguei tudo (${d.remainingCents.toMoney()})") }
            }
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Editar") }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Excluir dívida", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Pede o valor pago. Se for menos que o restante, só abate; há um botão para o valor todo. */
@Composable
private fun PayDialog(d: Debt, onDismiss: () -> Unit, onPay: (Long) -> Unit, onAll: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val cents = parseCents(text)
    val valid = cents != null && cents > 0
    val after = if (cents != null) (d.remainingCents - cents).coerceAtLeast(0) else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quanto você pagou?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${d.creditor} • falta ${d.remainingCents.toMoney()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Valor pago") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
                if (valid && after != null) {
                    Text(
                        if (after == 0L) "Isso quita a dívida."
                        else "Depois disso, ainda faltam ${after.toMoney()}.",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (after == 0L) DoneColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Foi o valor todo (${d.remainingCents.toMoney()})") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPay(cents ?: 0L) }, enabled = valid) { Text("Abater") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Criar ou editar uma dívida. */
@Composable
private fun DebtSheet(initial: Debt?, onDismiss: () -> Unit, onSave: (Debt) -> Unit) {
    var creditor by remember { mutableStateOf(initial?.creditor ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var amount by remember { mutableStateOf(initial?.amountCents?.toPlain() ?: "") }
    var date by remember {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.epochDay) } ?: LocalDate.now())
    }
    var pickDate by remember { mutableStateOf(false) }

    val cents = parseCents(amount)
    val valid = creditor.isNotBlank() && cents != null && cents > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial == null) "Nova dívida" else "Editar dívida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = creditor,
                onValueChange = { creditor = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("A quem você deve") },
                placeholder = { Text("ex.: minha irmã") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observação (opcional)") },
                placeholder = { Text("ex.: empréstimo") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Valor da dívida") },
                placeholder = { Text("300,00") },
                prefix = { Text("R$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedButton(
                onClick = { pickDate = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Desde ${date.fmt()}")
            }
            Button(
                onClick = {
                    onSave(
                        Debt(
                            id = initial?.id ?: 0L,
                            creditor = creditor.trim(),
                            note = note.trim(),
                            amountCents = cents ?: 0L,
                            paidCents = initial?.paidCents ?: 0L,
                            epochDay = date.toEpochDay(),
                            settledDay = initial?.settledDay
                        )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Salvar") }
        }
    }

    if (pickDate) {
        PickDate(initial = date, onPick = { date = it }, onDismiss = { pickDate = false })
    }
}
