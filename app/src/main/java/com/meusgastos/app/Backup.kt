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
    val sb = StringBuilder("﻿")
    sb.append("Data;Movimento;Tipo;Categoria;Descrição;Valor\r\n")
    rows.sortedBy { it.first }.forEach { sb.append(it.second).append("\r\n") }
    return sb.toString()
}

/** Backup completo em JSON, para restaurar em outro aparelho. */
suspend fun buildBackupJson(dao: ExpenseDao): String {
    val types = dao.typesOnce()
    val categories = dao.categoriesOnce()
    val expenses = dao.expensesOnce()
    val incomes = dao.incomesOnce()
    val recurring = dao.recurringOnce()
    val fixedIncomes = dao.fixedIncomesOnce()
    val debts = dao.debtsOnce()
    val debtPayments = dao.debtPaymentsOnce()

    val root = JSONObject()
    root.put("app", "MeusGastos")
    root.put("version", 1)
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
    return root.toString(2)
}

/** Substitui todos os dados atuais pelo conteúdo do backup. Retorna false se o arquivo for inválido. */
suspend fun restoreFromJson(dao: ExpenseDao, text: String): Boolean {
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
        dao.replaceAll(types, categories, expenses, incomes, recurring, fixedIncomes, debts, debtPayments)
        true
    } catch (e: Exception) {
        false
    }
}
