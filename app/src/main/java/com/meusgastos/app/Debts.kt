@file:OptIn(ExperimentalMaterial3Api::class)

package com.meusgastos.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// ---------------------------------------------------------------------------------------
// Dívidas: dinheiro que devo e ainda não tem data para pagar. Ficam separadas das despesas:
// pagar uma dívida não cria despesa, então nada é contado duas vezes nos relatórios.
// ---------------------------------------------------------------------------------------

/** Texto de erro de um pagamento recusado. */
fun paymentMessage(result: PaymentCheck, balanceCents: Long): String = when (result) {
    PaymentCheck.OK -> ""
    PaymentCheck.INVALID_AMOUNT -> "Informe um valor maior que zero."
    PaymentCheck.EXCEEDS_BALANCE ->
        "O valor é maior que o saldo devedor (${balanceCents.toMoney()}). Informe no máximo esse valor."
    PaymentCheck.ALREADY_SETTLED -> "Esta dívida já está quitada."
    PaymentCheck.DEBT_NOT_FOUND -> "Esta dívida não existe mais."
}

// ------------------------------------------------------------------ aba

/** Conteúdo da aba Dívidas: resumo, filtro (em aberto ou quitadas) e a lista. */
fun LazyListScope.debtsTab(
    debts: List<DebtView>,
    showOpen: Boolean,
    onShowOpen: (Boolean) -> Unit,
    onOpen: (Long) -> Unit
) {
    val totals = summarizeDebts(debts.map { it.status })

    item(key = "debt-summary") { DebtSummaryCard(totals) }

    item(key = "debt-filter") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip(showOpen, { onShowOpen(true) }, "Em aberto (${totals.openCount})")
            PillChip(!showOpen, { onShowOpen(false) }, "Quitadas (${totals.settledCount})")
        }
    }

    val shown = debts.filter { it.status.settled != showOpen }
    if (shown.isEmpty()) {
        item(key = "debt-empty") {
            EmptyState(
                androidx.compose.material.icons.Icons.Default.AccountBalanceWallet,
                when {
                    debts.isEmpty() -> "Nenhuma dívida cadastrada"
                    showOpen -> "Nenhuma dívida em aberto"
                    else -> "Nenhuma dívida quitada ainda"
                },
                if (debts.isEmpty()) "Toque no + para registrar uma. Não precisa de data de vencimento."
                else "Tudo certo por aqui."
            )
        }
    }
    items(shown, key = { "debt-${it.debt.id}" }) { v ->
        DebtCard(v, onClick = { onOpen(v.debt.id) })
    }
}

@Composable
private fun DebtSummaryCard(t: DebtTotals) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "AINDA DEVO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                t.openBalanceCents.toMoney(),
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = MoneyFont,
                fontWeight = FontWeight.Bold,
                color = if (t.openBalanceCents > 0) MaterialTheme.colorScheme.error else PositiveColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Soma do que ainda falta pagar. É dinheiro que devo, não são despesas já pagas.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Total já pago",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        t.totalPaidCents.toMoney(),
                        fontFamily = MoneyFont,
                        fontWeight = FontWeight.Bold,
                        color = PositiveColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Pendentes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (t.openCount == 1) "1 dívida" else "${t.openCount} dívidas",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(settled: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (settled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            if (settled) "Quitada" else "Em aberto",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (settled) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun DebtCard(v: DebtView, onClick: () -> Unit) {
    val st = v.status
    val tint = typeColor(v.debt.creditor)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        v.debt.creditor.trim().firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        v.debt.creditor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        v.debt.description.ifBlank {
                            "Cadastrada em ${LocalDate.ofEpochDay(v.debt.createdDay).fmt()}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (st.settled) {
                    StatusChip(settled = true)
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            st.balanceCents.toMoney(),
                            fontFamily = MoneyFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "falta pagar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { st.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PositiveColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                "Pago ${st.paidCents.toMoney()} de ${st.originalCents.toMoney()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------------------ telas e diálogos

/**
 * Tudo que abre por cima da aba: nova dívida, detalhe com histórico, pagamento, edição e exclusões.
 * Fica fora da lista para continuar aberto quando a lista muda.
 */
@Composable
fun DebtFlows(
    vm: MainViewModel,
    debts: List<DebtView>,
    showNew: Boolean,
    onNewDismiss: () -> Unit,
    openId: Long?,
    onOpenDismiss: () -> Unit
) {
    val context = LocalContext.current
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    var editing by remember { mutableStateOf<Debt?>(null) }
    var payingId by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<Debt?>(null) }
    var deletingPayment by remember { mutableStateOf<DebtPayment?>(null) }

    val open = debts.firstOrNull { it.debt.id == openId }
    val paying = debts.firstOrNull { it.debt.id == payingId }

    // A dívida pode sumir (foi excluída): fecha o que dependia dela.
    LaunchedEffect(openId, open) { if (openId != null && open == null) onOpenDismiss() }
    LaunchedEffect(payingId, paying) { if (payingId != null && paying == null) payingId = null }

    if (showNew) {
        DebtFormSheet(
            initial = null,
            paidCents = 0L,
            onSave = {
                vm.saveDebt(it)
                toast("Dívida cadastrada.")
                onNewDismiss()
            },
            onDismiss = onNewDismiss
        )
    }

    editing?.let { d ->
        val paid = debts.firstOrNull { it.debt.id == d.id }?.paidCents ?: 0L
        DebtFormSheet(
            initial = d,
            paidCents = paid,
            onSave = {
                vm.saveDebt(it)
                toast("Dívida atualizada.")
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    if (open != null) {
        DebtDetailSheet(
            view = open,
            onPay = { payingId = open.debt.id },
            onEdit = { editing = open.debt },
            onDelete = { deleting = open.debt },
            onDeletePayment = { deletingPayment = it },
            onDismiss = onOpenDismiss
        )
    }

    if (paying != null) {
        PaymentDialog(
            view = paying,
            onConfirm = { cents, day, note, onRefused ->
                val settlesAll = cents == paying.status.balanceCents
                vm.payDebt(paying.debt.id, cents, day, note) { result ->
                    if (result == PaymentCheck.OK) {
                        toast(if (settlesAll) "Pagamento registrado. Dívida quitada!" else "Pagamento registrado.")
                        payingId = null
                    } else {
                        onRefused(paymentMessage(result, paying.status.balanceCents))
                    }
                }
            },
            onDismiss = { payingId = null }
        )
    }

    deleting?.let { d ->
        val count = debts.firstOrNull { it.debt.id == d.id }?.payments?.size ?: 0
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir dívida?") },
            text = {
                Text(
                    "\"${d.creditor}\" será excluída" +
                        (if (count > 0) " junto com $count pagamento(s) do histórico" else "") +
                        ". Isso não altera suas despesas nem seus ganhos."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDebt(d)
                    deleting = null
                    toast("Dívida excluída.")
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }

    deletingPayment?.let { p ->
        AlertDialog(
            onDismissRequest = { deletingPayment = null },
            title = { Text("Excluir pagamento?") },
            text = {
                Text(
                    "O pagamento de ${p.amountCents.toMoney()} (${LocalDate.ofEpochDay(p.epochDay).fmt()}) " +
                        "será removido e o saldo da dívida volta a subir."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDebtPayment(p)
                    deletingPayment = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingPayment = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DebtDetailSheet(
    view: DebtView,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeletePayment: (DebtPayment) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val st = view.status
    val d = view.debt

    // Saldo depois de cada pagamento, contado na ordem em que aconteceram.
    val chrono = view.payments.sortedWith(compareBy({ it.epochDay }, { it.id }))
    val after = balancesAfterPayments(d.originalCents, chrono.map { it.amountCents })
    val history = chrono.zip(after).reversed()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        d.creditor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (d.description.isNotBlank()) {
                        Text(
                            d.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Cadastrada em ${LocalDate.ofEpochDay(d.createdDay).fmt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(settled = st.settled)
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                border = CardBorder,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Valor original", st.originalCents.toMoney(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                        Stat("Já pago", st.paidCents.toMoney(), PositiveColor, Modifier.weight(1f))
                        Stat(
                            "Saldo devedor",
                            st.balanceCents.toMoney(),
                            if (st.settled) PositiveColor else MaterialTheme.colorScheme.error,
                            Modifier.weight(1f)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { st.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PositiveColor,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    if (st.overpaidCents > 0) {
                        Text(
                            "Pago a mais: ${st.overpaidCents.toMoney()} (o valor original foi reduzido depois dos pagamentos).",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningColor
                        )
                    }
                }
            }

            Button(
                onClick = onPay,
                enabled = !st.settled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (st.settled) "Dívida quitada" else "Registrar pagamento") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Editar") }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                "Histórico de pagamentos (${history.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (history.isEmpty()) {
                Text(
                    "Nenhum pagamento ainda. Você pode pagar aos poucos: o saldo desce a cada pagamento.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            history.forEach { (p, balanceAfter) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            LocalDate.ofEpochDay(p.epochDay).fmt(),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (p.note.isNotBlank()) {
                            Text(
                                p.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (balanceAfter == 0L) "Quitou a dívida"
                            else "Faltava pagar ${balanceAfter.toMoney()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        p.amountCents.toMoney(),
                        fontFamily = MoneyFont,
                        fontWeight = FontWeight.Bold,
                        color = PositiveColor
                    )
                    IconButton(onClick = { onDeletePayment(p) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir pagamento",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontFamily = MoneyFont,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Registrar pagamento. A conferência acontece aqui (para avisar na hora) e de novo dentro da
 * transação do banco (para nunca passar do saldo). Se o valor for recusado, nada é gravado.
 */
@Composable
private fun PaymentDialog(
    view: DebtView,
    onConfirm: (Long, LocalDate, String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val st = view.status
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar pagamento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${view.debt.creditor} · saldo devedor ${st.balanceCents.toMoney()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Valor pago") },
                    placeholder = { Text("50,00") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
                TextButton(onClick = {
                    amount = st.balanceCents.toPlain()
                    error = null
                }) { Text("Pagar tudo (${st.balanceCents.toMoney()})") }
                OutlinedButton(
                    onClick = { pickDate = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Data: ${date.fmt()}")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Observação (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseCents(amount)
                val result = if (cents == null) PaymentCheck.INVALID_AMOUNT
                else checkPayment(view.debt.originalCents, view.paidCents, cents)
                if (cents == null || result != PaymentCheck.OK) {
                    error = paymentMessage(result, st.balanceCents)
                } else {
                    onConfirm(cents, date, note) { msg -> error = msg }
                }
            }) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (pickDate) {
        PickDate(initial = date, onPick = { date = it }, onDismiss = { pickDate = false })
    }
}

/**
 * Nova dívida ou edição. O "já pago" e a data de cadastro são automáticos. Se o novo valor ficar
 * menor que o que já foi pago, pede confirmação e mantém o histórico intacto.
 */
@Composable
private fun DebtFormSheet(
    initial: Debt?,
    paidCents: Long,
    onSave: (Debt) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var creditor by remember { mutableStateOf(initial?.creditor ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.originalCents?.toPlain() ?: "") }
    var confirmLower by remember { mutableStateOf(false) }

    val cents = parseCents(amount)
    val valid = creditor.isNotBlank() && cents != null
    val preview = if (cents != null) DebtStatus(cents, paidCents) else null

    fun build() = Debt(
        id = initial?.id ?: 0L,
        creditor = creditor.trim(),
        description = description.trim(),
        originalCents = cents ?: 0L,
        createdDay = initial?.createdDay ?: LocalDate.now().toEpochDay()
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                label = { Text("Credor") },
                placeholder = { Text("ex.: Minha irmã") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição (opcional)") },
                placeholder = { Text("ex.: Dinheiro emprestado") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Valor original") },
                placeholder = { Text("300,00") },
                prefix = { Text("R$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp)
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                border = CardBorder,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Já pago: ${paidCents.toMoney()}")
                    Text(
                        "Saldo devedor: " + (preview?.balanceCents?.toMoney() ?: "—"),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Cadastro: " + LocalDate.ofEpochDay(
                            initial?.createdDay ?: LocalDate.now().toEpochDay()
                        ).fmt() + " · sem data de vencimento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (initial != null && preview != null) {
                        val wasSettled = DebtStatus(initial.originalCents, paidCents).settled
                        if (wasSettled && !preview.settled) {
                            Text(
                                "Com esse valor a dívida volta para \"Em aberto\".",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningColor
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (initial != null && cents != null && cents < paidCents) {
                        confirmLower = true
                    } else {
                        onSave(build())
                    }
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (initial == null) "Cadastrar dívida" else "Salvar alteração") }
        }
    }

    if (confirmLower && cents != null) {
        AlertDialog(
            onDismissRequest = { confirmLower = false },
            title = { Text("Valor menor que o já pago") },
            text = {
                Text(
                    "Você já pagou ${paidCents.toMoney()}, mais que o novo valor (${cents.toMoney()}). " +
                        "O histórico de pagamentos será mantido sem alterações. A dívida ficará quitada " +
                        "e ${(paidCents - cents).toMoney()} serão mostrados como pago a mais."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLower = false
                    onSave(build())
                }) { Text("Salvar mesmo assim") }
            },
            dismissButton = { TextButton(onClick = { confirmLower = false }) { Text("Voltar") } }
        )
    }
}
