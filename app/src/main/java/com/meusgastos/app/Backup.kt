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
            e.title(),
            e.amountCents.toPlain(),
            if (e.paid) "Pago" else "Pendente",
            if (e.receipt != null) "Sim" else "Não"
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
            i.amountCents.toPlain(),
            "Recebido",
            ""
        )
        rows.add(i.epochDay to cells.joinToString(";") { csvCell(it) })
    }
    val sb = StringBuilder("﻿")
    sb.append("Data;Movimento;Tipo;Categoria;Descrição;Valor;Situação;Comprovante\r\n")
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
    val debts = dao.debtsOnce()

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
                .put("paid", it.paid)
                .put("receipt", it.receipt)
        })
    )
    root.put(
        "incomes",
        JSONArray(incomes.map {
            JSONObject()
                .put("description", it.description)
                .put("amountCents", it.amountCents)
                .put("epochDay", it.epochDay)
                .put("recurringId", it.recurringId ?: JSONObject.NULL)
                .put("ym", it.ym ?: JSONObject.NULL)
        })
    )
    root.put(
        "recurring",
        JSONArray(recurring.map {
            JSONObject()
                .put("id", it.id)
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
        "debts",
        JSONArray(debts.map {
            JSONObject()
                .put("creditor", it.creditor)
                .put("note", it.note)
                .put("amountCents", it.amountCents)
                .put("paidCents", it.paidCents)
                .put("epochDay", it.epochDay)
                .put("settledDay", it.settledDay ?: JSONObject.NULL)
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
                paid = it.optBoolean("paid", true),
                receipt = if (it.has("receipt")) it.getString("receipt") else null,
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                epochDay = it.getLong("epochDay")
            )
        }
        val incomes = (root.optJSONArray("incomes") ?: JSONArray()).objects().map {
            Income(
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                epochDay = it.getLong("epochDay"),
                recurringId = if (it.isNull("recurringId")) null else it.getLong("recurringId"),
                ym = if (it.isNull("ym")) null else it.getString("ym")
            )
        }
        val recurring = (root.optJSONArray("recurring") ?: JSONArray()).objects().map {
            Recurring(
                // Mantém o id original: os ganhos fixos apontam para ele.
                id = it.optLong("id", 0L),
                isIncome = it.getBoolean("isIncome"),
                type = it.getString("type"),
                category = it.getString("category"),
                description = it.getString("description"),
                amountCents = it.getLong("amountCents"),
                dayOfMonth = it.getInt("dayOfMonth"),
                lastGenerated = it.getString("lastGenerated")
            )
        }
        // Backup antigo (sem a chave "debts") não apaga as dívidas atuais.
        val debts = root.optJSONArray("debts")?.objects()?.map {
            Debt(
                creditor = it.getString("creditor"),
                note = it.optString("note", ""),
                amountCents = it.getLong("amountCents"),
                paidCents = it.getLong("paidCents"),
                epochDay = it.getLong("epochDay"),
                settledDay = if (it.isNull("settledDay")) null else it.getLong("settledDay")
            )
        }
        dao.replaceAll(types, categories, expenses, incomes, recurring, debts)
        true
    } catch (e: Exception) {
        false
    }
}
