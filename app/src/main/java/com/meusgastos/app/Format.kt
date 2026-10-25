package com.meusgastos.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

val PtBR: Locale = Locale("pt", "BR")

fun Long.toMoney(): String = NumberFormat.getCurrencyInstance(PtBR).format(this / 100.0)

fun Long.toPlain(): String = String.format(PtBR, "%.2f", this / 100.0)

/** Converte "4,50", "4.50" ou "1.234,50" em centavos. Retorna null se inválido ou zero. */
fun parseCents(input: String): Long? {
    var t = input.trim().replace("R$", "").replace(" ", "")
    if (t.isEmpty()) return null
    if (t.contains(',')) {
        t = t.replace(".", "").replace(',', '.')
    } else if (Regex("^\\d{1,3}(\\.\\d{3})+$").matches(t)) {
        // "1.500" sem vírgula = mil e quinhentos (ponto como separador de milhar).
        t = t.replace(".", "")
    }
    val value = t.toBigDecimalOrNull() ?: return null
    val cents = value.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    return if (cents > 0) cents else null
}

fun LocalDate.fmt(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun YearMonth.monthName(): String = month.getDisplayName(TextStyle.FULL, PtBR)

/** Reconhece o sufixo de parcela, como " (2/6)". */
val InstallmentRegex = Regex("\\s*\\(\\d+/\\d+\\)$")

fun YearMonth.label(): String = monthName().replaceFirstChar { it.uppercase() } + " " + year

/** Texto exibido do gasto; em parcelas inclui o número, como "Moto (3/48)". */
fun Expense.title(): String =
    if (installmentCount > 1) "$description ($installmentNo/$installmentCount)" else description

/** Aviso de vencimento de uma conta pendente; null se já está paga. */
fun Expense.dueText(today: LocalDate): String? {
    if (paid) return null
    val days = ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(epochDay))
    return when {
        days == -1L -> "Atrasada há 1 dia"
        days < 0L -> "Atrasada há ${-days} dias"
        days == 0L -> "Vence hoje"
        days == 1L -> "Vence amanhã"
        days <= 7L -> "Vence em $days dias"
        else -> "Pendente"
    }
}

fun YearMonth.monthIndex(): Int = year * 12 + (monthValue - 1)

fun monthFromIndex(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

/** Minúsculas e sem acentos, para buscas. */
fun String.norm(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
