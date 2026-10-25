package com.meusgastos.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PtBR: Locale = Locale("pt", "BR")

fun Long.toMoney(): String = NumberFormat.getCurrencyInstance(PtBR).format(this / 100.0)

fun Long.toPlain(): String = String.format(PtBR, "%.2f", this / 100.0)

/** Converte "4,50", "4.50" ou "1.234,50" em centavos. Retorna null se inválido ou zero. */
fun parseCents(input: String): Long? {
    var t = input.trim().replace("R$", "").replace(" ", "")
    if (t.isEmpty()) return null
    if (t.contains(',')) t = t.replace(".", "").replace(',', '.')
    val value = t.toBigDecimalOrNull() ?: return null
    val cents = value.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    return if (cents > 0) cents else null
}

fun LocalDate.fmt(): String = format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun YearMonth.monthName(): String = month.getDisplayName(TextStyle.FULL, PtBR)

/** Reconhece o sufixo de parcela, como " (2/6)". */
val InstallmentRegex = Regex("\\s*\\(\\d+/\\d+\\)$")

fun YearMonth.label(): String = monthName().replaceFirstChar { it.uppercase() } + " " + year

fun YearMonth.monthIndex(): Int = year * 12 + (monthValue - 1)

fun monthFromIndex(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

/** Minúsculas e sem acentos, para buscas. */
fun String.norm(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
