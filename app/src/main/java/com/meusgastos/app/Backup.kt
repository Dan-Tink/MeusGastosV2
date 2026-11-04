package com.meusgastos.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun csvCell(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

/** Planilha CSV (abre direto no Excel / Google Planilhas). */
suspend fun buildCsv(dao: ExpenseDao): String {
    val rows = ArrayList<Pair<Long, String>>()
    dao.expensesOnce().forEach { e ->
        val cells = listOf(
            LocalDate.ofEpochDay(e.epochDay).fmt(),
            "Gasto",
            e.type,
            e.category,
            e.title(),
            e.amountCents.toPlain()
        )
        rows.add(e.epochDay to cells.joinToString(";") { csvCell(it) })
    }
    dao.incomesOnce().forEach { i ->
        val cells = listOf(
            LocalDate.ofEpochDay(i.epochDay).fmt(),
            "Ganho",
            "",
            "",
            i.description,
            i.amountCents.toPlain()
        )
        rows.add(i.epochDay to cells.joinToString(";") { csvCell(it) })
    }
    // Ganhos fixos: uma linha por mês, do início até o mês atual (ou até o fim do ganho).
    val nowMonth = YearMonth.now()
    dao.fixedIncomesOnce().forEach { f ->
        val start = runCatching { YearMonth.parse(f.startMonth) }.getOrNull() ?: return@forEach
        val end = f.endMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?.let { if (it.isAfter(nowMonth)) nowMonth else it } ?: nowMonth
        var m = start
        while (!m.isAfter(end)) {
            val day = m.atDay(minOf(f.dayOfMonth, m.lengthOfMonth()))
            val cells = listOf(day.fmt(), "Ganho fixo", "", "", f.description, f.amountCents.toPlain())
            rows.add(day.toEpochDay() to cells.joinToString(";") { csvCell(it) })
            m = m.plusMonths(1)
        }
    }
    // Despesas fixas: uma linha por mês, do início até o mês atual (ou até o fim da despesa).
    dao.fixedExpensesOnce().forEach { f ->
        val start = runCatching { YearMonth.parse(f.startMonth) }.getOrNull() ?: return@forEach
        val end = f.endMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?.let { if (it.isAfter(nowMonth)) nowMonth else it } ?: nowMonth
        var m = start
        while (!m.isAfter(end)) {
            val day = m.atDay(minOf(f.dayOfMonth.coerceAtLeast(1), m.lengthOfMonth()))
            val cells = listOf(day.fmt(), "Gasto fixo", f.type, f.category, f.description, f.amountCents.toPlain())
            rows.add(day.toEpochDay() to cells.joinToString(";") { csvCell(it) })
            m = m.plusMonths(1)
        }
    }
    val sb = StringBuilder("﻿")
    sb.append("Data;Movimento;Tipo;Categoria;Descrição;Valor\r\n")
    rows.sortedBy { it.first }.forEach { sb.append(it.second).append("\r\n") }
    return sb.toString()
}

/** Backup completo em JSON, para restaurar em outro aparelho. */
suspend fun buildBackupJson(dao: ExpenseDao, settings: Settings): String {
    val types = dao.typesOnce()
    val categories = dao.categoriesOnce()
    val expenses = dao.expensesOnce()
    val incomes = dao.incomesOnce()
    val recurring = dao.recurringOnce()
    val fixedIncomes = dao.fixedIncomesOnce()
    val debts = dao.debtsOnce()
    val debtPayments = dao.debtPaymentsOnce()
    val pending = dao.pendingOnce()
    val fixedExpenses = dao.fixedExpensesOnce()

    val root = JSONObject()
    root.put("app", "MeusGastos")
    root.put("version", 2)
    root.put(
        "types",
        JSONArray(types.map { JSONObject().put("name", it.name).put("fixed", it.fixed) })
    )
    root.put("categories", JSONArray(categories.map { it.name }))
    root.put(
        "expenses",
        JSONArray(expenses.map {
            JSONObject()
                .put("type", it.type)
                .put("category", it.category)
                .put("description", it.description)
                .put("amountCents", it.amountCents)
                .put("epochDay", it.epochDay)
                .put("groupId", it.groupId)
                .put("installmentNo", it.installmentNo)
                .put("installmentCount", it.installmentCount)
                .put("isCredit", it.isCredit)
                .put("receipt", it.receipt ?: JSONObject.NULL)
                .put("icon", it.icon ?: JSONObject.NULL)
        })
    )
    root.put(
        "incomes",
        JSONArray(incomes.map {
            JSONObject()
                .put("description", it.description)
                .put("amountCents", it.amountCents)
                .put("epochDay", it.epochDay)
        })
    )
    root.put(
        "recurring",
        JSONArray(recurring.map {
            JSONObject()
                .put("isIncome", it.isIncome)
                .put("type", it.type)
                .put("category", it.category)
                .put("description", it.description)
                .put("amountCents", it.amountCents)
                .put("dayOfMonth", it.dayOfMonth)
                .put("lastGenerated", it.lastGenerated)
        })
    )
    root.put(
        "fixedIncomes",
        JSONArray(fixedIncomes.map {
            JSONObject()
                .put("description", it.description)
                .put("amountCents", it.amountCents)
                .put("dayOfMonth", it.dayOfMonth)
                .put("startMonth", it.startMonth)
                .put("endMonth", it.endMonth ?: JSONObject.NULL)
        })
    )
    // Dívidas e histórico de pagamentos. Os ids ligam cada pagamento à sua dívida.
    root.put(
        "debts",
        JSONArray(debts.map {
            JSONObject()
                .put("id", it.id)
                .put("creditor", it.creditor)
                .put("description", it.description)
                .put("originalCents", it.originalCents)
                .put("createdDay", it.createdDay)
        })
    )
    root.put(
        "debtPayments",
        JSONArray(debtPayments.map {
            JSONObject()
                .put("id", it.id)
                .put("debtId", it.debtId)
                .put("amountCents", it.amountCents)
                .put("epochDay", it.epochDay)
                .put("note", it.note)
        })
    )
    root.put(
        "pending",
        JSONArray(pending.map {
            JSONObject()
                .put("source", it.source)
                .put("amountCents", it.amountCents)
                .put("isIncome", it.isIncome)
                .put("description", it.description)
                .put("method", it.method)
                .put("epochDay", it.epochDay)
                .put("createdAt", it.createdAt)
                .put("type", it.type)
                .put("category", it.category)
                .put("status", it.status)
        })
    )
    root.put(
        "fixedExpenses",
        JSONArray(fixedExpenses.map {
            JSONObject()
                .put("type", it.type)
                .put("category", it.category)
                .put("description", it.description)
                .put("amountCents", it.amountCents)
                .put("dayOfMonth", it.dayOfMonth)
                .put("startMonth", it.startMonth)
                .put("endMonth", it.endMonth ?: JSONObject.NULL)
                .put("icon", it.icon ?: JSONObject.NULL)
        })
    )
    root.put("settings", settings.toJson())
    return root.toString(2)
}

/** Todas as preferências do app (limite, lembrete, captura, tema e cor). */
private fun Settings.toJson(): JSONObject = JSONObject()
    .put("limitCents", limitCents)
    .put("reminderEnabled", reminderEnabled)
    .put("reminderHour", reminderHour)
    .put("lastType", lastType)
    .put("lastCategory", lastCategory)
    .put("captureEnabled", captureEnabled)
    .put("captureAuto", captureAuto)
    .put("captureDisabled", JSONArray(captureDisabled.toList()))
    .put("captureTipDismissed", captureTipDismissed)
    .put("themeMode", themeMode)
    .put("accent", accent)

/** Aplica só o que existir no arquivo; o que faltar (backup antigo) fica como está. */
private fun Settings.applyJson(o: JSONObject) {
    if (o.has("limitCents")) limitCents = o.getLong("limitCents").coerceAtLeast(0L)
    if (o.has("reminderEnabled")) reminderEnabled = o.getBoolean("reminderEnabled")
    if (o.has("reminderHour")) reminderHour = o.getInt("reminderHour").coerceIn(0, 23)
    if (o.has("lastType")) lastType = o.getString("lastType")
    if (o.has("lastCategory")) lastCategory = o.getString("lastCategory")
    if (o.has("captureEnabled")) captureEnabled = o.getBoolean("captureEnabled")
    if (o.has("captureAuto")) captureAuto = o.getBoolean("captureAuto")
    o.optJSONArray("captureDisabled")?.let { arr ->
        captureDisabled = (0 until arr.length()).map { arr.getString(it) }.toSet()
    }
    if (o.has("captureTipDismissed")) captureTipDismissed = o.getBoolean("captureTipDismissed")
    if (o.has("themeMode")) themeMode = ThemeMode.from(o.getString("themeMode")).key
    if (o.has("accent")) accent = AccentTheme.from(o.getString("accent")).key
}

/** Substitui todos os dados atuais pelo conteúdo do backup. Retorna false se o arquivo for inválido. */
suspend fun restoreFromJson(dao: ExpenseDao, settings: Settings, text: String): Boolean {
    return try {
        val root = JSONObject(text)
        if (root.optString("app") != "MeusGastos") return false

        val types = (root.optJSONArray("types") ?: JSONArray()).objects().map {
            ExpenseType(it.getString("name"), it.optBoolean("fixed", false))
        }
        val categories = (root.optJSONArray("categories") ?: JSONArray()).let { arr ->
            (0 until arr.length()).map { Category(arr.getString(it)) }
        }
        val expenses = (root.optJSONArray("expenses") ?: JSONArray()).objects().map {
            Expense(
                type = it.getString("type"),
                category = it.optString("category", "Outros"),
                groupId = if (it.has("groupId")) it.getLong("groupId") else null,
                installmentNo = it.optInt("installmentNo", 0),
                installmentCount = it.optInt("installmentCount", 0),
                isCredit = it.optBoolean("isCredit", false),
                receipt = if (it.isNull("receipt")) null else it.getString("receipt"),
                icon = if (it.isNull("icon")) null else it.getString("icon"),
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                epochDay = it.getLong("epochDay")
            )
        }
        val incomes = (root.optJSONArray("incomes") ?: JSONArray()).objects().map {
            Income(
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                epochDay = it.getLong("epochDay")
            )
        }
        val allRecurring = (root.optJSONArray("recurring") ?: JSONArray()).objects()
        val recurring = allRecurring.filter { !it.getBoolean("isIncome") }.map {
            Recurring(
                isIncome = it.getBoolean("isIncome"),
                type = it.getString("type"),
                category = it.getString("category"),
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                dayOfMonth = it.getInt("dayOfMonth"),
                lastGenerated = it.getString("lastGenerated")
            )
        }
        val fixedIncomes = ArrayList<FixedIncome>()
        (root.optJSONArray("fixedIncomes") ?: JSONArray()).objects().forEach {
            fixedIncomes.add(
                FixedIncome(
                    description = it.getString("description"),
                    amountCents = it.getLong("amountCents"),
                    dayOfMonth = it.getInt("dayOfMonth"),
                    startMonth = it.getString("startMonth"),
                    endMonth = if (it.isNull("endMonth")) null else it.getString("endMonth")
                )
            )
        }
        // Backups antigos guardavam ganhos recorrentes: viram ganhos fixos a partir do mês seguinte.
        allRecurring.filter { it.getBoolean("isIncome") }.forEach {
            val last = runCatching { YearMonth.parse(it.getString("lastGenerated")) }.getOrNull()
                ?: YearMonth.now()
            fixedIncomes.add(
                FixedIncome(
                    description = it.getString("description"),
                    amountCents = it.getLong("amountCents"),
                    dayOfMonth = it.getInt("dayOfMonth"),
                    startMonth = last.plusMonths(1).toString()
                )
            )
        }
        // Backups antigos não têm dívidas: nesse caso as listas ficam vazias.
        val debts = (root.optJSONArray("debts") ?: JSONArray()).objects().map {
            Debt(
                id = it.getLong("id"),
                creditor = it.getString("creditor"),
                description = it.optString("description", ""),
                originalCents = it.getLong("originalCents"),
                createdDay = it.getLong("createdDay")
            )
        }
        val debtIds = debts.map { it.id }.toSet()
        val debtPayments = (root.optJSONArray("debtPayments") ?: JSONArray()).objects()
            .filter { it.getLong("debtId") in debtIds }
            .map {
                DebtPayment(
                    id = it.getLong("id"),
                    debtId = it.getLong("debtId"),
                    amountCents = it.getLong("amountCents"),
                    epochDay = it.getLong("epochDay"),
                    note = it.optString("note", "")
                )
            }
        val pending = root.optJSONArray("pending")?.objects()?.map {
            PendingTx(
                source = it.getString("source"),
                amountCents = it.getLong("amountCents"),
                isIncome = it.getBoolean("isIncome"),
                description = it.getString("description"),
                method = it.getString("method"),
                epochDay = it.getLong("epochDay"),
                createdAt = it.getLong("createdAt"),
                type = it.getString("type"),
                category = it.getString("category"),
                status = it.optInt("status", 0)
            )
        }
        val fixedExpenses = root.optJSONArray("fixedExpenses")?.objects()?.map {
            FixedExpense(
                type = it.getString("type"),
                category = it.getString("category"),
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                dayOfMonth = it.getInt("dayOfMonth"),
                startMonth = it.getString("startMonth"),
                endMonth = if (it.isNull("endMonth")) null else it.getString("endMonth"),
                icon = if (it.isNull("icon")) null else it.getString("icon")
            )
        }
        dao.replaceAll(
            types, categories, expenses, incomes, recurring, fixedIncomes, debts, debtPayments,
            pending, fixedExpenses
        )
        root.optJSONObject("settings")?.let { runCatching { settings.applyJson(it) } }
        true
    } catch (e: Exception) {
        false
    }
}
