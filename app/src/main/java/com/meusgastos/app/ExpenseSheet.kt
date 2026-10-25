@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meusgastos.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

private val InstallmentPresets = listOf(2, 3, 6, 10, 12, 24, 36, 48)

private enum class ScopeAction { SAVE, DELETE }

@Composable
fun ExpenseSheet(
    initial: Expense?,
    types: List<ExpenseType>,
    categories: List<Category>,
    history: List<Expense>,
    lastType: String,
    lastCategory: String,
    onDismiss: () -> Unit,
    /** (gasto, nº de parcelas, valor é o total?, continuar lançando?) */
    onSave: (Expense, Int, Boolean, Boolean) -> Unit,
    onSaveScoped: (Expense, GroupScope) -> Unit,
    onDelete: (Expense) -> Unit,
    onDeleteScoped: (Expense, GroupScope) -> Unit,
    onManageTypes: () -> Unit,
    onManageCategories: () -> Unit
) {
    val context = LocalContext.current
    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember {
        mutableStateOf(
            initial?.type
                ?: lastType.takeIf { lt -> types.any { it.name == lt } }
                ?: types.firstOrNull()?.name
                ?: ""
        )
    }
    var category by remember {
        mutableStateOf(
            initial?.category
                ?: lastCategory.takeIf { lc -> categories.any { it.name == lc } }
                ?: "Outros"
        )
    }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.amountCents?.toPlain() ?: "") }
    var date by remember {
        mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.epochDay) } ?: LocalDate.now())
    }
    var instText by remember { mutableStateOf("1") }
    var showCustom by remember { mutableStateOf(false) }
    var amountIsTotal by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var scopeAction by remember { mutableStateOf<ScopeAction?>(null) }

    // Situação (pago/pendente) e comprovante.
    var paid by remember { mutableStateOf(initial?.paid ?: true) }
    var paidTouched by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf(initial?.receipt) }
    val addedReceipts = remember { mutableListOf<String>() }
    var cameraName by remember { mutableStateOf<String?>(null) }

    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = Receipts.saveCopy(context, uri)
            if (name != null) {
                addedReceipts.add(name)
                receipt = name
            } else {
                toast("Escolha uma foto ou um PDF.")
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val name = cameraName
        if (name != null) {
            if (ok) {
                addedReceipts.add(name)
                receipt = name
            } else {
                Receipts.delete(context, name)
            }
        }
        cameraName = null
    }

    LaunchedEffect(types) {
        if (type.isBlank()) type = types.firstOrNull()?.name ?: ""
    }

    val isGrouped = initial != null && initial.groupId != null && initial.installmentCount > 1
    val cents = parseCents(amount)
    val installments = if (initial == null) (instText.toIntOrNull() ?: 1).coerceIn(1, 120) else 1
    val valid = type.isNotBlank() && desc.isNotBlank() && cents != null &&
        (installments == 1 || !amountIsTotal || cents >= installments)

    // Gasto fixo e parcelado nascem pendentes; o avulso nasce pago. Vale até você mexer na chave.
    val typeIsFixed = types.firstOrNull { it.name == type }?.fixed == true
    LaunchedEffect(type, installments, typeIsFixed) {
        if (initial == null && !paidTouched) paid = !(typeIsFixed || installments > 1)
    }

    // Quanto cada parcela custa e quanto fica o total, conforme o modo escolhido.
    val perInstallment: Long? = cents?.let { if (amountIsTotal) it / installments else it }
    val totalCents: Long? = cents?.let { if (amountIsTotal) it else it * installments }
    val firstExtra: Long =
        if (amountIsTotal && cents != null) cents - (cents / installments) * installments else 0L

    // Sugestões: descrições já usadas antes, da mais recente para a mais antiga.
    val suggestions = remember(desc, history) {
        val q = desc.trim().lowercase()
        if (initial != null || q.isEmpty()) {
            emptyList()
        } else {
            history.asSequence()
                .filter { d ->
                    d.description.lowercase().contains(q) &&
                        !d.description.equals(desc.trim(), ignoreCase = true)
                }
                .distinctBy { it.description.lowercase() }
                .take(4)
                .toList()
        }
    }

    fun buildExpense() = Expense(
        id = initial?.id ?: 0L,
        type = type,
        description = desc.trim(),
        amountCents = cents ?: 0L,
        epochDay = date.toEpochDay(),
        category = category,
        groupId = initial?.groupId,
        installmentNo = initial?.installmentNo ?: 0,
        installmentCount = initial?.installmentCount ?: 0,
        paid = paid,
        receipt = receipt
    )

    /** Apaga arquivos de comprovante que ficaram sem uso (anexados e descartados, ou trocados). */
    fun finalizeReceipts(saved: Boolean) {
        val keep = if (saved) receipt else initial?.receipt
        addedReceipts.filter { it != keep }.forEach { Receipts.delete(context, it) }
        if (saved) {
            val old = initial?.receipt
            if (old != null && old != receipt) Receipts.delete(context, old)
        }
        addedReceipts.clear()
    }

    ModalBottomSheet(
        onDismissRequest = {
            finalizeReceipts(false)
            onDismiss()
        },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (initial == null) "Novo gasto" else "Editar gasto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (isGrouped && initial != null) {
                Text(
                    "Parcela ${initial.installmentNo} de ${initial.installmentCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SheetLabel("Tipo")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                types.forEach { t ->
                    FilterChip(
                        selected = type == t.name,
                        onClick = { type = t.name },
                        label = { Text(t.name) }
                    )
                }
                AssistChip(onClick = onManageTypes, label = { Text("+ Tipo") })
            }

            SheetLabel("Categoria")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { c ->
                    FilterChip(
                        selected = category == c.name,
                        onClick = { category = c.name },
                        label = { Text(c.name) }
                    )
                }
                AssistChip(onClick = onManageCategories, label = { Text("+ Categoria") })
            }

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição") },
                placeholder = { Text("ex.: comprei uma coxinha") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            if (suggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.forEach { s ->
                        AssistChip(
                            onClick = {
                                desc = s.description
                                if (types.any { it.name == s.type }) type = s.type
                                if (categories.any { it.name == s.category }) category = s.category
                                if (amount.isBlank()) amount = s.amountCents.toPlain()
                            },
                            label = { Text(s.description) }
                        )
                    }
                }
            }

            // ---- Pagamento: à vista ou parcelado (só em gasto novo) ----
            if (initial == null) {
                SheetLabel("Pagamento")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = installments == 1 && !showCustom,
                        onClick = { instText = "1"; showCustom = false },
                        label = { Text("À vista") }
                    )
                    InstallmentPresets.forEach { n ->
                        FilterChip(
                            selected = installments == n && !showCustom,
                            onClick = { instText = n.toString(); showCustom = false },
                            label = { Text("${n}x") }
                        )
                    }
                    FilterChip(
                        selected = showCustom,
                        onClick = { showCustom = true },
                        label = { Text("Outro") }
                    )
                }
                if (showCustom) {
                    OutlinedTextField(
                        value = instText,
                        onValueChange = { instText = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Número de parcelas") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                if (installments > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !amountIsTotal,
                            onClick = { amountIsTotal = false },
                            label = { Text("Valor da parcela") }
                        )
                        FilterChip(
                            selected = amountIsTotal,
                            onClick = { amountIsTotal = true },
                            label = { Text("Valor total") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        when {
                            isGrouped -> "Valor da parcela"
                            initial == null && installments > 1 && amountIsTotal -> "Valor total"
                            initial == null && installments > 1 -> "Valor da parcela"
                            else -> "Valor"
                        }
                    )
                },
                placeholder = { Text("4,50") },
                prefix = { Text("R$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp)
            )

            // ---- Resumo ao vivo do parcelamento ----
            if (initial == null && installments > 1 && perInstallment != null && totalCents != null) {
                val last = YearMonth.from(date).plusMonths((installments - 1).toLong())
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "${installments}x de ${perInstallment.toMoney()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Total: ${totalCents.toMoney()}")
                        Text(
                            "1ª em ${date.fmt()} • última em ${last.monthName()} de ${last.year}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (firstExtra > 0) {
                            Text(
                                "A 1ª parcela leva ${firstExtra.toMoney()} a mais (centavos da divisão).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { pickDate = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (initial == null && installments > 1) "1ª parcela em ${date.fmt()}" else date.fmt())
            }

            // ---- Situação: pago ou pendente ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (initial == null && installments > 1) "1ª parcela já foi paga?" else "Já foi pago?",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (paid) "Marcado como pago."
                        else "Fica como pendente até você confirmar na lista.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = paid,
                    onCheckedChange = {
                        paid = it
                        paidTouched = true
                    }
                )
            }

            // ---- Comprovante ----
            SheetLabel("Comprovante")
            val receiptName = receipt
            if (receiptName != null) {
                val fileOk = Receipts.exists(context, receiptName)
                Text(
                    if (fileOk) "Comprovante anexado."
                    else "O arquivo deste comprovante não está neste aparelho.",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (fileOk) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fileOk) {
                        OutlinedButton(
                            onClick = {
                                if (!Receipts.open(context, receiptName)) {
                                    toast("Não há app para abrir este arquivo.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ver")
                        }
                    }
                    OutlinedButton(
                        onClick = { receipt = null },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Remover") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { attachLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Anexar")
                    }
                    OutlinedButton(
                        onClick = {
                            val (name, uri) = Receipts.newPhoto(context)
                            cameraName = name
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Tirar foto")
                    }
                }
            }

            if (isGrouped) {
                Text(
                    "Ao salvar, você escolhe se vale só para esta parcela, para as próximas ou para todas. " +
                        "Situação, data e comprovante valem só para esta parcela.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (isGrouped) {
                        scopeAction = ScopeAction.SAVE
                    } else {
                        val e = buildExpense()
                        finalizeReceipts(true)
                        onSave(e, installments, amountIsTotal, false)
                    }
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (initial == null && installments > 1) "Lançar $installments parcelas" else "Salvar")
            }

            if (initial == null) {
                OutlinedButton(
                    onClick = {
                        val e = buildExpense()
                        finalizeReceipts(true)
                        onSave(e, installments, amountIsTotal, true)
                        desc = ""
                        amount = ""
                        instText = "1"
                        showCustom = false
                        receipt = null
                        paidTouched = false
                        paid = !typeIsFixed
                    },
                    enabled = valid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Salvar e adicionar outro") }
            }

            if (initial != null) {
                TextButton(
                    onClick = {
                        if (isGrouped) {
                            scopeAction = ScopeAction.DELETE
                        } else {
                            confirmDelete = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir gasto", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pickDate) {
        PickDate(initial = date, onPick = { date = it }, onDismiss = { pickDate = false })
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir gasto?") },
            text = { Text("\"${initial.title()}\" será removido.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    finalizeReceipts(false)
                    onDelete(initial)
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }

    val action = scopeAction
    if (action != null && initial != null) {
        ScopeDialog(
            deleting = action == ScopeAction.DELETE,
            expense = initial,
            history = history,
            onPick = { scope ->
                scopeAction = null
                if (action == ScopeAction.SAVE) {
                    val e = buildExpense()
                    finalizeReceipts(true)
                    onSaveScoped(e, scope)
                } else {
                    finalizeReceipts(false)
                    onDeleteScoped(initial, scope)
                }
            },
            onDismiss = { scopeAction = null }
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Pergunta se a mudança vale só para esta parcela, para as próximas ou para todas. */
@Composable
private fun ScopeDialog(
    deleting: Boolean,
    expense: Expense,
    history: List<Expense>,
    onPick: (GroupScope) -> Unit,
    onDismiss: () -> Unit
) {
    val group = history.filter { it.groupId == expense.groupId }
    val fromHere = group.count { it.installmentNo >= expense.installmentNo }
    val color = if (deleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deleting) "Excluir parcelas" else "Salvar alterações") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Esta é a parcela ${expense.installmentNo} de ${expense.installmentCount}." +
                        if (deleting) "" else " A data muda só nesta parcela.",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { onPick(GroupScope.ONE) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Só esta parcela", color = color) }
                TextButton(
                    onClick = { onPick(GroupScope.FROM_HERE) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Esta e as próximas ($fromHere)", color = color) }
                TextButton(
                    onClick = { onPick(GroupScope.ALL) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Todas as parcelas (${group.size})", color = color) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
