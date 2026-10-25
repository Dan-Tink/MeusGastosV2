package com.meusgastos.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

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
            e.description,
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
        val recurring = (root.optJSONArray("recurring") ?: JSONArray()).objects().map {
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
        dao.replaceAll(types, categories, expenses, incomes, recurring)
        true
    } catch (e: Exception) {
        false
    }
}
