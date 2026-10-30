@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meusgastos.app

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
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
    onManageCategories: () -> Unit,
    /** true = os dados vêm de uma notificação de banco: ao salvar, é um lançamento novo. */
    isPrefill: Boolean = false
) {
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

    LaunchedEffect(types) {
        if (type.isBlank()) type = types.firstOrNull()?.name ?: ""
    }

    val isGrouped = initial != null && initial.groupId != null && initial.installmentCount > 1
    val cents = parseCents(amount)
    val installments = if (initial == null) (instText.toIntOrNull() ?: 1).coerceIn(1, 120) else 1
    val valid = type.isNotBlank() && desc.isNotBlank() && cents != null &&
        (installments == 1 || !amountIsTotal || cents >= installments)

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
        installmentCount = initial?.installmentCount ?: 0
    )

    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (initial == null) {
            kotlinx.coroutines.delay(350)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }
    val parcelar = initial == null && (installments > 1 || showCustom)
    val amountLabel = when {
        isGrouped -> "Valor da parcela"
        initial == null && installments > 1 && amountIsTotal -> "Valor total"
        initial == null && installments > 1 -> "Valor da parcela"
        else -> "Valor"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---- Título e valor em destaque ----
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        if (initial == null) "Novo gasto" else if (isPrefill) "Confirmar gasto" else "Editar gasto",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isGrouped && initial != null) {
                        Text(
                            "Parcela ${initial.installmentNo} de ${initial.installmentCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = {
                            Text(
                                "0,00",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Text(
                        "$amountLabel (R$)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ---- Tipo: controle segmentado ----
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SheetLabel("Tipo", Modifier.weight(1f))
                        ManageLink(onManageTypes)
                    }
                    if (types.size in 1..3) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            types.forEachIndexed { i, t ->
                                SegmentedButton(
                                    selected = type == t.name,
                                    onClick = { type = t.name },
                                    shape = SegmentedButtonDefaults.itemShape(i, types.size),
                                    icon = {},
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primary,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                        activeBorderColor = Color.Transparent,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                                        inactiveBorderColor = Color.Transparent
                                    )
                                ) { Text(t.name, maxLines = 1) }
                            }
                        }
                    } else {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            types.forEach { t ->
                                PillChip(type == t.name, { type = t.name }, t.name)
                            }
                        }
                    }
                }

                // ---- Categoria: grade de ícones ----
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SheetLabel("Categoria", Modifier.weight(1f))
                        ManageLink(onManageCategories)
                    }
                    categories.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { c ->
                                val sel = category == c.name
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .selectable(
                                            selected = sel,
                                            role = Role.RadioButton,
                                            onClick = { category = c.name }
                                        )
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CategoryBadge(c.name, size = 44.dp, selected = sel)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        c.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // ---- Descrição e data ----
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = desc,
                        onValueChange = { desc = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Descrição (ex.: coxinha)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = softFieldColors()
                    )
                    if (suggestions.isNotEmpty()) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { s ->
                                PillChip(false, {
                                    desc = s.description
                                    if (types.any { it.name == s.type }) type = s.type
                                    if (categories.any { it.name == s.category }) category = s.category
                                    if (amount.isBlank()) amount = s.amountCents.toPlain()
                                }, s.description)
                            }
                        }
                    }
                    Surface(
                        onClick = { pickDate = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (initial == null && installments > 1) "1ª parcela em ${date.fmt()}"
                                else date.fmt(),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ---- Pagamento: à vista ou parcelado (só em gasto novo) ----
                if (initial == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SheetLabel("Pagamento", Modifier.weight(1f))
                            PillChip(!parcelar, { instText = "1"; showCustom = false }, "À vista")
                            PillChip(parcelar, {
                                if (!parcelar) instText = "2"
                            }, "Parcelar")
                        }
                        if (parcelar) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InstallmentPresets.forEach { n ->
                                    PillChip(installments == n && !showCustom, {
                                        instText = n.toString(); showCustom = false
                                    }, "${n}x")
                                }
                                PillChip(showCustom, { showCustom = true }, "Outro")
                            }
                            if (showCustom) {
                                TextField(
                                    value = instText,
                                    onValueChange = { instText = it.filter(Char::isDigit).take(3) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Número de parcelas") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = softFieldColors()
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillChip(!amountIsTotal, { amountIsTotal = false }, "Valor da parcela")
                                PillChip(amountIsTotal, { amountIsTotal = true }, "Valor total")
                            }
                        }
                    }
                }

                // ---- Resumo ao vivo do parcelamento ----
                if (initial == null && installments > 1 && perInstallment != null && totalCents != null) {
                    val last = YearMonth.from(date).plusMonths((installments - 1).toLong())
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                                fontFamily = MoneyFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Total: ${totalCents.toMoney()}", fontFamily = MoneyFont)
                            Text(
                                "1ª em ${date.fmt()} • última em ${last.monthName()} de ${last.year}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (firstExtra > 0) {
                                Text(
                                    "A 1ª parcela leva ${firstExtra.toMoney()} a mais (centavos da divisão).",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                if (isGrouped) {
                    Text(
                        "Ao salvar, você escolhe se vale só para esta parcela, para as próximas ou para todas.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (initial != null && !isPrefill) {
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
                Spacer(Modifier.height(4.dp))
            }

            // ---- Ações sempre visíveis no rodapé ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (isGrouped) {
                            scopeAction = ScopeAction.SAVE
                        } else {
                            onSave(buildExpense(), installments, amountIsTotal, false)
                        }
                    },
                    enabled = valid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (initial == null && installments > 1) "Lançar $installments parcelas" else "Salvar",
                        fontWeight = FontWeight.Bold
                    )
                }
                if (initial == null) {
                    TextButton(
                        onClick = {
                            onSave(buildExpense(), installments, amountIsTotal, true)
                            desc = ""
                            amount = ""
                            instText = "1"
                            showCustom = false
                        },
                        enabled = valid
                    ) { Text("Salvar e adicionar outro") }
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
                    onSaveScoped(buildExpense(), scope)
                } else {
                    onDeleteScoped(initial, scope)
                }
            },
            onDismiss = { scopeAction = null }
        )
    }
}

@Composable
private fun SheetLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
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

/** Link discreto "Gerenciar" ao lado do título de uma seção. */
@Composable
private fun ManageLink(onClick: () -> Unit) {
    Text(
        "Gerenciar",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}
