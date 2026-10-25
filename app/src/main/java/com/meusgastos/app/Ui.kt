@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meusgastos.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val types by vm.types.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val overview by vm.overview.collectAsStateWithLifecycle()
    val monthIncomes by vm.monthIncomes.collectAsStateWithLifecycle()
    val recurring by vm.recurring.collectAsStateWithLifecycle()
    val history by vm.allExpenses.collectAsStateWithLifecycle()
    val limit by vm.limitCents.collectAsStateWithLifecycle()
    val reminderOn by vm.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by vm.reminderHour.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<Expense?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showTypes by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showIncomes by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var pendingHour by remember { mutableStateOf(21) }

    val context = LocalContext.current
    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            vm.exportCsv(uri) { ok ->
                toast(if (ok) "Planilha salva!" else "Não foi possível salvar o arquivo.")
            }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.exportBackup(uri) { ok ->
                toast(if (ok) "Backup salvo!" else "Não foi possível salvar o backup.")
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestore = uri
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.setReminder(true, pendingHour)
        } else {
            toast("Sem permissão de notificação, o lembrete não pode ser ativado.")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Novo gasto") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.month.value = month.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            month.label(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val hint = when (state.scope) {
                            Scope.RANGE -> "Período personalizado"
                            Scope.SEARCH -> "Buscando em todos os meses"
                            Scope.MONTH -> null
                        }
                        if (hint != null) {
                            Text(
                                hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { vm.month.value = month.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Tipos de despesa") },
                                onClick = { menuOpen = false; showTypes = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Categorias") },
                                onClick = { menuOpen = false; showCategories = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Gastos recorrentes") },
                                onClick = { menuOpen = false; showRecurring = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Copiar fixos do mês anterior") },
                                onClick = {
                                    menuOpen = false
                                    vm.copyFixedFromPreviousMonth { n ->
                                        toast(
                                            if (n > 0) "$n lançamento(s) copiado(s)."
                                            else "Nada novo para copiar."
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Limite e lembrete") },
                                onClick = { menuOpen = false; showSettings = true }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Exportar planilha (CSV)") },
                                onClick = {
                                    menuOpen = false
                                    csvLauncher.launch("meus-gastos-${LocalDate.now()}.csv")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Salvar backup") },
                                onClick = {
                                    menuOpen = false
                                    backupLauncher.launch("meus-gastos-backup-${LocalDate.now()}.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restaurar backup") },
                                onClick = {
                                    menuOpen = false
                                    restoreLauncher.launch(arrayOf("*/*"))
                                }
                            )
                        }
                    }
                }
            }

            item {
                OverviewCard(
                    o = overview,
                    limit = limit.takeIf { it > 0 },
                    prevLabel = month.minusMonths(1).monthName(),
                    onIncomeClick = { showIncomes = true }
                )
            }

            item {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = { vm.setQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Buscar (ex.: coxinha)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filters.query.isNotEmpty()) {
                                IconButton(onClick = { vm.setQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                                }
                            }
                            IconButton(onClick = { showFilters = true }) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Filtros",
                                    tint = if (filters.activeCount > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }

            item { SummaryCard(state) }

            if (state.items.isEmpty()) {
                item {
                    Text(
                        "Nenhum gasto por aqui.\nToque em \"Novo gasto\" para lançar.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.items.isNotEmpty()) {
                item {
                    Text(
                        "Toque no quadrado à esquerda para marcar como pago ou pendente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.items, key = { it.id }) { e ->
                ExpenseRow(
                    e = e,
                    today = LocalDate.now(),
                    onClick = { editing = e },
                    onTogglePaid = { vm.setPaid(e, !e.paid) }
                )
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showNew) {
        ExpenseSheet(
            initial = null,
            types = types,
            categories = categories,
            history = history,
            lastType = vm.lastType,
            lastCategory = vm.lastCategory,
            onDismiss = { showNew = false },
            onSave = { e, installments, amountIsTotal, keepOpen ->
                vm.save(e, installments, amountIsTotal)
                if (keepOpen) {
                    toast("Gasto salvo!")
                } else {
                    showNew = false
                }
            },
            onSaveScoped = { _, _ -> },
            onDelete = {},
            onDeleteScoped = { _, _ -> },
            onManageTypes = { showTypes = true },
            onManageCategories = { showCategories = true }
        )
    }

    editing?.let { current ->
        ExpenseSheet(
            initial = current,
            types = types,
            categories = categories,
            history = history,
            lastType = vm.lastType,
            lastCategory = vm.lastCategory,
            onDismiss = { editing = null },
            onSave = { e, _, _, _ ->
                vm.save(e, 1)
                editing = null
            },
            onSaveScoped = { e, scope ->
                vm.saveScoped(e, scope)
                editing = null
            },
            onDelete = {
                vm.delete(it)
                editing = null
            },
            onDeleteScoped = { e, scope ->
                vm.deleteScoped(e, scope)
                editing = null
            },
            onManageTypes = { showTypes = true },
            onManageCategories = { showCategories = true }
        )
    }

    if (showFilters) {
        FiltersSheet(
            filters = filters,
            types = types,
            categories = categories,
            onApply = {
                vm.applyFilters(it)
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
    }

    if (showIncomes) {
        IncomesSheet(
            month = month,
            incomes = monthIncomes,
            onSave = { vm.saveIncome(it) },
            onDelete = { vm.deleteIncome(it) },
            onDismiss = { showIncomes = false }
        )
    }

    if (showRecurring) {
        RecurringSheet(
            recurring = recurring,
            types = types,
            categories = categories,
            onSave = { vm.saveRecurring(it) },
            onDelete = { vm.deleteRecurring(it) },
            onDismiss = { showRecurring = false }
        )
    }

    if (showTypes) {
        TypesDialog(
            types = types,
            onAdd = { vm.addType(it) },
            onRename = { old, new -> vm.renameType(old, new) },
            onToggleFixed = { name, fixed -> vm.setTypeFixed(name, fixed) },
            onDelete = { name ->
                vm.deleteType(name) { ok ->
                    if (!ok) toast("Esse tipo está em uso. Mude os gastos de tipo antes de excluir.")
                }
            },
            onDismiss = { showTypes = false }
        )
    }

    if (showCategories) {
        CategoriesDialog(
            categories = categories,
            onAdd = { vm.addCategory(it) },
            onRename = { old, new -> vm.renameCategory(old, new) },
            onDelete = { name ->
                vm.deleteCategory(name) { ok ->
                    if (!ok) toast("Essa categoria está em uso. Mude os gastos de categoria antes de excluir.")
                }
            },
            onDismiss = { showCategories = false }
        )
    }

    if (showSettings) {
        SettingsDialog(
            limitCents = limit,
            reminderEnabled = reminderOn,
            reminderHour = reminderHour,
            onSaveLimit = { vm.setLimit(it) },
            onSetReminder = { enabled, hour ->
                val needsPermission = enabled &&
                    Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    pendingHour = hour
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.setReminder(enabled, hour)
                }
            },
            onDismiss = { showSettings = false }
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar backup?") },
            text = { Text("Isso substitui todos os dados atuais pelos do arquivo escolhido.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    vm.restoreBackup(uri) { ok ->
                        toast(if (ok) "Backup restaurado!" else "Arquivo de backup inválido.")
                    }
                }) { Text("Restaurar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SummaryCard(state: UiState) {
    var byCategory by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Total da lista",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.total.toMoney(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${state.items.size} lançamento(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.total > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !byCategory,
                        onClick = { byCategory = false },
                        label = { Text("Por tipo") }
                    )
                    FilterChip(
                        selected = byCategory,
                        onClick = { byCategory = true },
                        label = { Text("Por categoria") }
                    )
                }
                if (byCategory) {
                    CategoryDonut(state.byCategory, state.total)
                } else {
                    state.byType.forEach { (name, value) ->
                        val color = typeColor(name)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name, modifier = Modifier.weight(1f))
                            Text(value.toMoney(), fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { value.toFloat() / state.total.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDonut(data: List<Pair<String, Long>>, total: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(Modifier.size(120.dp)) {
            val strokeWidth = size.minDimension * 0.2f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var start = -90f
            data.forEach { (name, value) ->
                val sweep = 360f * value.toFloat() / total.toFloat()
                drawArc(
                    color = categoryColor(name),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth)
                )
                start += sweep
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.forEach { (name, value) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(categoryColor(name))
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "$name · ${value * 100 / total}%",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            value.toMoney(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PickDate(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val st = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now())
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                st.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = st)
    }
}

@Composable
private fun FiltersSheet(
    filters: Filters,
    types: List<ExpenseType>,
    categories: List<Category>,
    onApply: (Filters) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selTypes by remember { mutableStateOf(filters.types) }
    var selCategories by remember { mutableStateOf(filters.categories) }
    var minText by remember { mutableStateOf(filters.minCents?.toPlain() ?: "") }
    var maxText by remember { mutableStateOf(filters.maxCents?.toPlain() ?: "") }
    var from by remember { mutableStateOf(filters.from) }
    var to by remember { mutableStateOf(filters.to) }
    var byValue by remember { mutableStateOf(filters.sortByValue) }
    var status by remember { mutableStateOf(filters.status) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Filtros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Tipo de despesa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                types.forEach { t ->
                    FilterChip(
                        selected = t.name in selTypes,
                        onClick = {
                            selTypes = if (t.name in selTypes) selTypes - t.name else selTypes + t.name
                        },
                        label = { Text(t.name) }
                    )
                }
            }

            Text(
                "Categoria",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { c ->
                    FilterChip(
                        selected = c.name in selCategories,
                        onClick = {
                            selCategories =
                                if (c.name in selCategories) selCategories - c.name
                                else selCategories + c.name
                        },
                        label = { Text(c.name) }
                    )
                }
            }

            Text(
                "Valor",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Mínimo") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Máximo") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Text(
                "Período (substitui o mês)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { pickFrom = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(from?.fmt() ?: "De") }
                OutlinedButton(
                    onClick = { pickTo = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(to?.fmt() ?: "Até") }
            }
            if (from != null || to != null) {
                TextButton(onClick = { from = null; to = null }) { Text("Limpar datas") }
            }

            Text(
                "Situação",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = status == PayStatus.ALL,
                    onClick = { status = PayStatus.ALL },
                    label = { Text("Todas") }
                )
                FilterChip(
                    selected = status == PayStatus.PENDING,
                    onClick = { status = PayStatus.PENDING },
                    label = { Text("Pendentes") }
                )
                FilterChip(
                    selected = status == PayStatus.PAID,
                    onClick = { status = PayStatus.PAID },
                    label = { Text("Pagas") }
                )
            }

            Text(
                "Ordenar por",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !byValue,
                    onClick = { byValue = false },
                    label = { Text("Data") }
                )
                FilterChip(
                    selected = byValue,
                    onClick = { byValue = true },
                    label = { Text("Maior valor") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onApply(Filters(query = filters.query)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Limpar") }
                Button(
                    onClick = {
                        onApply(
                            filters.copy(
                                types = selTypes,
                                categories = selCategories,
                                minCents = parseCents(minText),
                                maxCents = parseCents(maxText),
                                from = from,
                                to = to,
                                sortByValue = byValue,
                                status = status
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Aplicar") }
            }
        }
    }

    if (pickFrom) PickDate(from, onPick = { from = it }, onDismiss = { pickFrom = false })
    if (pickTo) PickDate(to, onPick = { to = it }, onDismiss = { pickTo = false })
}
