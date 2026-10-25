package com.meusgastos.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

data class Filters(
    val query: String = "",
    val types: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val minCents: Long? = null,
    val maxCents: Long? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val sortByValue: Boolean = false
) {
    val dateActive: Boolean get() = from != null || to != null
    val activeCount: Int
        get() = listOf(
            types.isNotEmpty(),
            categories.isNotEmpty(),
            minCents != null || maxCents != null,
            dateActive,
            sortByValue
        ).count { it }
}

enum class Scope { MONTH, RANGE, SEARCH }

data class UiState(
    val items: List<Expense> = emptyList(),
    val total: Long = 0,
    val byType: List<Pair<String, Long>> = emptyList(),
    val byCategory: List<Pair<String, Long>> = emptyList(),
    val scope: Scope = Scope.MONTH
)

/** Resumo do mês exibido no topo: não depende de busca nem de filtros. */
data class Overview(
    val income: Long = 0,
    val fixed: Long = 0,
    val variable: Long = 0,
    /** Total gasto no mês anterior, para comparação. */
    val prevSpent: Long = 0,
    /** Se não for null, [prevSpent] só conta até este dia do mês anterior (comparação justa). */
    val prevUpToDay: Int? = null
) {
    val spent: Long get() = fixed + variable
    val balance: Long get() = income - fixed - variable
}

private fun inMonth(epochDay: Long, m: YearMonth): Boolean =
    YearMonth.from(LocalDate.ofEpochDay(epochDay)) == m

private fun dayIn(m: YearMonth, day: Int): LocalDate = m.atDay(minOf(day, m.lengthOfMonth()))

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val settings = Settings(app)

    val month = MutableStateFlow(YearMonth.now())
    val filters = MutableStateFlow(Filters())

    val types: StateFlow<List<ExpenseType>> = dao.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = dao.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurring: StateFlow<List<Recurring>> = dao.observeRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Todos os gastos, usados para sugerir descrições já digitadas. */
    val allExpenses: StateFlow<List<Expense>> = dao.observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val limitCents = MutableStateFlow(settings.limitCents)
    val reminderEnabled = MutableStateFlow(settings.reminderEnabled)
    val reminderHour = MutableStateFlow(settings.reminderHour)

    val lastType: String get() = settings.lastType
    val lastCategory: String get() = settings.lastCategory

    val monthIncomes: StateFlow<List<Income>> = combine(dao.observeIncomes(), month) { list, m ->
        list.filter { inMonth(it.epochDay, m) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overview: StateFlow<Overview> = combine(
        dao.observeExpenses(),
        dao.observeIncomes(),
        dao.observeTypes(),
        month
    ) { expenses, incomes, typeList, m ->
        val fixedNames = typeList.filter { it.fixed }.map { it.name }.toSet()
        val monthExpenses = expenses.filter { inMonth(it.epochDay, m) }
        val fixed = monthExpenses.filter { it.type in fixedNames }.sumOf { it.amountCents }
        val all = monthExpenses.sumOf { it.amountCents }
        val income = incomes.filter { inMonth(it.epochDay, m) }.sumOf { it.amountCents }
        val prev = m.minusMonths(1)
        // No mês atual, compara com o mesmo trecho do mês anterior (até o dia de hoje).
        val upTo: Int? =
            if (m == YearMonth.now()) minOf(LocalDate.now().dayOfMonth, prev.lengthOfMonth()) else null
        val prevSpent = expenses
            .filter { e ->
                val d = LocalDate.ofEpochDay(e.epochDay)
                YearMonth.from(d) == prev && (upTo == null || d.dayOfMonth <= upTo)
            }
            .sumOf { it.amountCents }
        Overview(
            income = income,
            fixed = fixed,
            variable = all - fixed,
            prevSpent = prevSpent,
            prevUpToDay = upTo
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Overview())

    val state: StateFlow<UiState> = combine(dao.observeExpenses(), month, filters) { all, m, f ->
        val q = f.query.norm().trim()
        val from = f.from
        val to = f.to
        val minC = f.minCents
        val maxC = f.maxCents
        val scope = when {
            f.dateActive -> Scope.RANGE
            q.isNotEmpty() -> Scope.SEARCH
            else -> Scope.MONTH
        }
        var list = all.filter { e ->
            val d = LocalDate.ofEpochDay(e.epochDay)
            val inScope = when (scope) {
                Scope.RANGE -> (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to))
                Scope.SEARCH -> true
                Scope.MONTH -> YearMonth.from(d) == m
            }
            inScope &&
                (q.isEmpty() ||
                    e.description.norm().contains(q) ||
                    e.type.norm().contains(q) ||
                    e.category.norm().contains(q)) &&
                (f.types.isEmpty() || e.type in f.types) &&
                (f.categories.isEmpty() || e.category in f.categories) &&
                (minC == null || e.amountCents >= minC) &&
                (maxC == null || e.amountCents <= maxC)
        }
        if (f.sortByValue) list = list.sortedByDescending { it.amountCents }
        val byType = list.groupBy { it.type }
            .map { (name, items) -> name to items.sumOf { it.amountCents } }
            .sortedByDescending { it.second }
        val byCategory = list.groupBy { it.category }
            .map { (name, items) -> name to items.sumOf { it.amountCents } }
            .sortedByDescending { it.second }
        UiState(list, list.sumOf { it.amountCents }, byType, byCategory, scope)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    init {
        if (settings.reminderEnabled) Reminder.schedule(app, settings.reminderHour, replace = false)
    }

    // ---------------------------------------------------------------- busca e filtros

    fun setQuery(q: String) = filters.update { it.copy(query = q) }

    fun applyFilters(f: Filters) {
        filters.value = f
    }

    // ---------------------------------------------------------------- gastos

    /** Salva um gasto. Em um gasto novo, [installments] > 1 divide o valor em parcelas mensais. */
    fun save(e: Expense, installments: Int = 1) {
        viewModelScope.launch {
            val base = LocalDate.ofEpochDay(e.epochDay)
            if (e.id != 0L) {
                dao.update(e)
            } else if (installments > 1) {
                val each = e.amountCents / installments
                val rest = e.amountCents - each * installments
                val list = (0 until installments).map { i ->
                    val m = YearMonth.from(base).plusMonths(i.toLong())
                    e.copy(
                        id = 0,
                        description = "${e.description} (${i + 1}/$installments)",
                        amountCents = each + if (i == 0) rest else 0L,
                        epochDay = dayIn(m, base.dayOfMonth).toEpochDay()
                    )
                }
                dao.insertExpenses(list)
            } else {
                dao.insert(e)
            }
            settings.lastType = e.type
            settings.lastCategory = e.category
            if (!filters.value.dateActive) month.value = YearMonth.from(base)
        }
    }

    fun delete(e: Expense) {
        viewModelScope.launch { dao.delete(e) }
    }

    // ---------------------------------------------------------------- ganhos

    fun saveIncome(i: Income) {
        viewModelScope.launch {
            if (i.id == 0L) dao.insertIncome(i) else dao.updateIncome(i)
            month.value = YearMonth.from(LocalDate.ofEpochDay(i.epochDay))
        }
    }

    fun deleteIncome(i: Income) {
        viewModelScope.launch { dao.deleteIncome(i) }
    }

    // ---------------------------------------------------------------- tipos

    fun addType(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { dao.insertType(ExpenseType(n)) }
    }

    fun setTypeFixed(name: String, fixed: Boolean) {
        viewModelScope.launch { dao.setTypeFixed(name, fixed) }
    }

    fun renameType(old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty()) return
        viewModelScope.launch {
            dao.renameType(old, n)
            filters.update { f -> f.copy(types = f.types.map { if (it == old) n else it }.toSet()) }
        }
    }

    fun deleteType(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (dao.countByType(name) > 0) {
                onResult(false)
            } else {
                dao.deleteType(name)
                filters.update { f -> f.copy(types = f.types - name) }
                onResult(true)
            }
        }
    }

    // ---------------------------------------------------------------- categorias

    fun addCategory(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        viewModelScope.launch { dao.insertCategory(Category(n)) }
    }

    fun renameCategory(old: String, new: String) {
        val n = new.trim()
        if (n.isEmpty()) return
        viewModelScope.launch {
            dao.renameCategory(old, n)
            filters.update { f ->
                f.copy(categories = f.categories.map { if (it == old) n else it }.toSet())
            }
        }
    }

    fun deleteCategory(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (dao.countByCategory(name) > 0) {
                onResult(false)
            } else {
                dao.deleteCategory(name)
                filters.update { f -> f.copy(categories = f.categories - name) }
                onResult(true)
            }
        }
    }

    // ---------------------------------------------------------------- recorrentes

    fun saveRecurring(r: Recurring) {
        viewModelScope.launch {
            if (r.id == 0L) {
                // Começa no mês atual: o lançamento deste mês é criado logo em seguida.
                dao.insertRecurring(r.copy(lastGenerated = YearMonth.now().minusMonths(1).toString()))
            } else {
                dao.updateRecurring(r)
            }
            runRecurring()
        }
    }

    fun deleteRecurring(r: Recurring) {
        viewModelScope.launch { dao.deleteRecurring(r) }
    }

    /** Lança os gastos e ganhos recorrentes dos meses que ainda não foram lançados. */
    fun generateRecurring() {
        viewModelScope.launch { runRecurring() }
    }

    private suspend fun runRecurring() {
        val now = YearMonth.now()
        for (r in dao.recurringOnce()) {
            val last = runCatching { YearMonth.parse(r.lastGenerated) }.getOrNull() ?: now.minusMonths(1)
            var m = last.plusMonths(1)
            var guard = 0
            while (!m.isAfter(now) && guard < 24) {
                val date = dayIn(m, r.dayOfMonth)
                if (r.isIncome) {
                    dao.insertIncome(
                        Income(
                            description = r.description,
                            amountCents = r.amountCents,
                            epochDay = date.toEpochDay()
                        )
                    )
                } else {
                    dao.insert(
                        Expense(
                            type = r.type,
                            category = r.category,
                            description = r.description,
                            amountCents = r.amountCents,
                            epochDay = date.toEpochDay()
                        )
                    )
                }
                m = m.plusMonths(1)
                guard++
            }
            if (last != now) dao.setLastGenerated(r.id, now.toString())
        }
    }

    /** Copia os gastos fixos e os ganhos do mês anterior para o mês exibido, sem duplicar. */
    fun copyFixedFromPreviousMonth(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val target = month.value
            val prev = target.minusMonths(1)
            val fixedNames = dao.typesOnce().filter { it.fixed }.map { it.name }.toSet()
            val expenses = dao.expensesOnce()
            val incomes = dao.incomesOnce()
            val currentExpenses = expenses.filter { inMonth(it.epochDay, target) }
            val currentIncomes = incomes.filter { inMonth(it.epochDay, target) }

            val newExpenses = expenses
                .filter { inMonth(it.epochDay, prev) && it.type in fixedNames }
                .filter { !InstallmentRegex.containsMatchIn(it.description) }
                .filter { e ->
                    currentExpenses.none {
                        it.type == e.type &&
                            it.amountCents == e.amountCents &&
                            it.description.equals(e.description, ignoreCase = true)
                    }
                }
                .map { e ->
                    val day = LocalDate.ofEpochDay(e.epochDay).dayOfMonth
                    e.copy(id = 0, epochDay = dayIn(target, day).toEpochDay())
                }

            val newIncomes = incomes
                .filter { inMonth(it.epochDay, prev) }
                .filter { i ->
                    currentIncomes.none {
                        it.amountCents == i.amountCents &&
                            it.description.equals(i.description, ignoreCase = true)
                    }
                }
                .map { i ->
                    val day = LocalDate.ofEpochDay(i.epochDay).dayOfMonth
                    i.copy(id = 0, epochDay = dayIn(target, day).toEpochDay())
                }

            if (newExpenses.isNotEmpty()) dao.insertExpenses(newExpenses)
            if (newIncomes.isNotEmpty()) dao.insertIncomes(newIncomes)
            onDone(newExpenses.size + newIncomes.size)
        }
    }

    // ---------------------------------------------------------------- limite e lembrete

    fun setLimit(cents: Long) {
        settings.limitCents = cents
        limitCents.value = cents
    }

    fun setReminder(enabled: Boolean, hour: Int) {
        settings.reminderEnabled = enabled
        settings.reminderHour = hour
        reminderEnabled.value = enabled
        reminderHour.value = hour
        if (enabled) Reminder.schedule(app, hour, replace = true) else Reminder.cancel(app)
    }

    // ---------------------------------------------------------------- exportar e backup

    fun exportCsv(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = buildCsv(dao)
                    val out = app.contentResolver.openOutputStream(uri) ?: error("sem saída")
                    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    fun exportBackup(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = buildBackupJson(dao)
                    val out = app.contentResolver.openOutputStream(uri) ?: error("sem saída")
                    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    fun restoreBackup(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val input = app.contentResolver.openInputStream(uri) ?: error("sem entrada")
                    val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    restoreFromJson(dao, text)
                }.getOrDefault(false)
            }
            onDone(ok)
        }
    }
}
