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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.material3.TextField
import java.time.YearMonth
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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

private const val TAB_EXPENSES = 0
private const val TAB_SUMMARY = 1
private const val TAB_INCOMES = 2
private const val TAB_DEBTS = 3

/** Folha de ganho aberta: [fixed] ou [variable] preenchido = editando; os dois nulos = novo ganho. */
private data class IncomeSheetState(val fixed: FixedIncome? = null, val variable: Income? = null)

@Composable
fun HomeScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val types by vm.types.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val overview by vm.overview.collectAsStateWithLifecycle()
    val monthIncomes by vm.monthIncomes.collectAsStateWithLifecycle()
    val monthFixed by vm.monthFixedIncomes.collectAsStateWithLifecycle()
    val summary by vm.monthSummary.collectAsStateWithLifecycle()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val recurring by vm.recurring.collectAsStateWithLifecycle()
    val history by vm.allExpenses.collectAsStateWithLifecycle()
    val limit by vm.limitCents.collectAsStateWithLifecycle()
    val reminderOn by vm.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by vm.reminderHour.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val captureEnabled by vm.captureEnabled.collectAsStateWithLifecycle()
    val captureAuto by vm.captureAuto.collectAsStateWithLifecycle()
    val captureDisabled by vm.captureDisabled.collectAsStateWithLifecycle()
    val captureTipDismissed by vm.captureTipDismissed.collectAsStateWithLifecycle()
    val listenerOn = rememberListenerEnabled()

    var editing by remember { mutableStateOf<Expense?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showTypes by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(TAB_SUMMARY) }
    var incomeSheet by remember { mutableStateOf<IncomeSheetState?>(null) }
    var showNewDebt by remember { mutableStateOf(false) }
    var openDebtId by remember { mutableStateOf<Long?>(null) }
    var debtsOpenFilter by rememberSaveable { mutableStateOf(true) }
    var showTheme by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCapture by remember { mutableStateOf(false) }
    var editingPending by remember { mutableStateOf<PendingTx?>(null) }
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

    val listState = rememberLazyListState()
    LaunchedEffect(tab) { listState.scrollToItem(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                items = listOf(
                    NavItem("Gastos", Icons.Outlined.Receipt, Icons.Filled.Receipt, badge = pending.size),
                    NavItem("Resumo", Icons.Outlined.Home, Icons.Filled.Home),
                    NavItem("Ganhos", Icons.Outlined.Payments, Icons.Filled.Payments),
                    NavItem("Dívidas", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet)
                ),
                selected = tab,
                onSelect = { tab = it },
                addLabel = when (tab) {
                    TAB_INCOMES -> "Novo ganho"
                    TAB_DEBTS -> "Nova dívida"
                    else -> "Novo gasto"
                },
                onAdd = {
                    when (tab) {
                        TAB_INCOMES -> incomeSheet = IncomeSheetState()
                        TAB_DEBTS -> showNewDebt = true
                        else -> showNew = true
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Cabeçalho verde: mês (ou título da aba Dívidas) e menu, fixos no topo.
            HeaderBar {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (tab != TAB_DEBTS) {
                        IconButton(onClick = { vm.month.value = month.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (tab == TAB_DEBTS) "Dívidas" else month.label(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val hint = if (tab != TAB_EXPENSES) null else when (state.scope) {
                            Scope.RANGE -> "Período personalizado"
                            Scope.SEARCH -> "Buscando em todos os meses"
                            Scope.MONTH -> null
                        }
                        if (hint != null) {
                            Text(
                                hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = OnHeaderMuted
                            )
                        }
                    }
                    if (tab != TAB_DEBTS) {
                        IconButton(onClick = { vm.month.value = month.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
                        }
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
                                text = { Text("Captura por notificação") },
                                onClick = { menuOpen = false; showCapture = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Tema (claro ou escuro)") },
                                onClick = { menuOpen = false; showTheme = true }
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

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (tab) {
                    // ---------------------------------------------- Gastos: só a lista
                    TAB_EXPENSES -> {
                        pendingItems(
                            pending = pending,
                            history = history,
                            onConfirm = { vm.confirmPending(it) },
                            onConfirmAll = { vm.confirmAllPending() },
                            onEdit = { editingPending = it },
                            onDismiss = { vm.dismissPending(it) }
                        )

            item {
                TextField(
                    value = filters.query,
                    onValueChange = { vm.setQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = softFieldColors(),
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

                        item { ListTotal(state) }

                        if (state.items.isEmpty()) {
                            item {
                                EmptyState(
                                    Icons.Default.Receipt,
                                    "Nenhum gasto por aqui",
                                    "Toque no + para lançar o primeiro."
                                )
                            }
                        }

                        if (filters.sortByValue) {
                            items(state.items, key = { it.id }) { e ->
                                ExpenseRow(e, showDate = true, onClick = { editing = e })
                            }
                        } else {
                            state.items.groupBy { it.epochDay }.forEach { (day, list) ->
                                item(key = "day-$day") { DayHeader(day, list.sumOf { it.amountCents }) }
                                items(list, key = { it.id }) { e ->
                                    ExpenseRow(e, showDate = false, onClick = { editing = e })
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }

                    // ---------------------------------------------- Resumo: o painel do mês
                    TAB_SUMMARY -> {
                        captureTip(
                            show = !listenerOn && !captureTipDismissed,
                            onEnable = { showCapture = true },
                            onDismiss = { vm.dismissCaptureTip() }
                        )
                        item {
                            OverviewCard(
                                o = overview,
                                limit = limit.takeIf { it > 0 },
                                prevLabel = month.minusMonths(1).monthName(),
                                onIncomeClick = { tab = TAB_INCOMES }
                            )
                        }
                        if (summary.total > 0) {
                            item { SummaryCard(summary) }
                        } else {
                            item {
                                EmptyState(
                                    Icons.Default.Receipt,
                                    "Nenhum gasto neste mês",
                                    "Quando você lançar gastos, o resumo aparece aqui."
                                )
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }

                    // ---------------------------------------------- Ganhos: fixos e variáveis
                    TAB_INCOMES -> {
                        incomesTab(
                            month = month,
                            fixed = monthFixed,
                            variable = monthIncomes,
                            onEditFixed = { incomeSheet = IncomeSheetState(fixed = it) },
                            onEditVariable = { incomeSheet = IncomeSheetState(variable = it) }
                        )
                        item { Spacer(Modifier.height(24.dp)) }
                    }

                    // ---------------------------------------------- Dívidas: sem data de vencimento
                    else -> {
                        debtsTab(
                            debts = debts,
                            showOpen = debtsOpenFilter,
                            onShowOpen = { debtsOpenFilter = it },
                            onOpen = { openDebtId = it }
                        )
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
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

    editingPending?.let { p ->
        ExpenseSheet(
            initial = Expense(
                type = p.type,
                description = p.description,
                amountCents = p.amountCents,
                epochDay = p.epochDay,
                category = p.category
            ),
            types = types,
            categories = categories,
            history = history,
            lastType = vm.lastType,
            lastCategory = vm.lastCategory,
            onDismiss = { editingPending = null },
            onSave = { e, _, _, _ ->
                vm.confirmPendingEdited(p, e)
                editingPending = null
            },
            onSaveScoped = { _, _ -> },
            onDelete = {},
            onDeleteScoped = { _, _ -> },
            onManageTypes = { showTypes = true },
            onManageCategories = { showCategories = true },
            isPrefill = true
        )
    }

    if (showCapture) {
        CaptureDialog(
            captureEnabled = captureEnabled,
            auto = captureAuto,
            disabled = captureDisabled,
            onEnabled = { vm.setCaptureEnabled(it) },
            onAuto = { vm.setCaptureAuto(it) },
            onToggleBank = { pkg, on -> vm.setBankEnabled(pkg, on) },
            onDismiss = { showCapture = false }
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

    if (showTheme) {
        ThemeDialog(
            current = themeMode,
            onSelect = { vm.setThemeMode(it) },
            onDismiss = { showTheme = false }
        )
    }

    DebtFlows(
        vm = vm,
        debts = debts,
        showNew = showNewDebt,
        onNewDismiss = { showNewDebt = false },
        openId = openDebtId,
        onOpenDismiss = { openDebtId = null }
    )

    incomeSheet?.let { st ->
        IncomeSheet(
            month = month,
            editingFixed = st.fixed,
            editingVariable = st.variable,
            onSaveVariable = { vm.saveIncome(it) },
            onDeleteVariable = { vm.deleteIncome(it) },
            onSaveFixed = { d, c, day, existing -> vm.saveFixedIncome(d, c, day, existing) },
            onEndFixed = { vm.endFixedIncome(it) },
            onDismiss = { incomeSheet = null }
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

/** Linha enxuta no topo da lista de gastos: quanto soma o que está na tela. */
@Composable
private fun ListTotal(state: UiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Total · ${state.items.size} lançamento(s)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            state.total.toMoney(),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = MoneyFont,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SummaryCard(state: MonthSummary) {
    var byCategory by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Gastos do mês",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.total.toMoney(),
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = MoneyFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${state.count} lançamento(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.total > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillChip(!byCategory, { byCategory = false }, "Por tipo")
                    PillChip(byCategory, { byCategory = true }, "Por categoria")
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
                            Text(value.toMoney(), fontFamily = MoneyFont, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { value.toFloat() / state.total.toFloat() },
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDonut(data: List<Pair<String, Long>>, total: Long) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(Modifier.size(120.dp)) {
            val strokeWidth = size.minDimension * 0.2f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth)
            )
            val gap = if (data.size > 1) 2f else 0f
            var start = -90f
            data.forEach { (name, value) ->
                val sweep = 360f * value.toFloat() / total.toFloat()
                drawArc(
                    color = categoryColor(name),
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
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
                            fontFamily = MoneyFont,
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
private fun DayHeader(epochDay: Long, totalCents: Long) {
    val d = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    val name = when (d) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> d.dayOfMonth.toString().padStart(2, '0') + " de " + d.monthValue.let { m ->
            YearMonth.of(d.year, m).monthName()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            totalCents.toMoney(),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = MoneyFont,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Mensagem de lista vazia: ícone num círculo suave, título e uma linha de ajuda. */
@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, hint: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            hint,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpenseRow(e: Expense, showDate: Boolean, onClick: () -> Unit) {
    val date = LocalDate.ofEpochDay(e.epochDay)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = CardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadge(e.category, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    e.title(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (showDate) "${e.category} • ${e.type} • ${date.fmt()}" else "${e.category} • ${e.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                e.amountCents.toMoney(),
                fontFamily = MoneyFont,
                fontWeight = FontWeight.Bold
            )
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
                                sortByValue = byValue
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
