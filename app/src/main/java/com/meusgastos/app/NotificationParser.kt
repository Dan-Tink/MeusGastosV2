package com.meusgastos.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer

/**
 * Leitura de notificações de bancos. Este arquivo não depende de nada do Android,
 * para poder ser testado sozinho.
 */

/** Apps de banco monitorados: nome do pacote -> nome exibido. */
object BankApps {
    val packages: Map<String, String> = linkedMapOf(
        "com.nu.production" to "Nubank",
        "br.com.intermedium" to "Inter",
        "com.itau" to "Itaú",
        "com.bradesco" to "Bradesco",
        "com.santander.app" to "Santander",
        "com.c6bank.app" to "C6 Bank",
        "br.com.bb.android" to "Banco do Brasil",
        "br.com.gabba.Caixa" to "Caixa",
        "com.mercadopago.wallet" to "Mercado Pago",
        "com.picpay" to "PicPay",
        "br.com.uol.ps.myaccount" to "PagBank",
        "br.com.neon" to "Neon"
    )
}

data class ParsedNotification(
    val amountCents: Long,
    val isIncome: Boolean,
    /** Onde o dinheiro foi gasto (ou de quem veio), já limpo. */
    val merchant: String,
    /** "Pix", "Débito", "Crédito", "Boleto" ou "Compra". */
    val method: String
)

private fun String.plain(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

private val AmountRegex = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?|\d+(?:,\d{1,2})?)""")

/** Converte "1.234,56" em centavos. Retorna null se for inválido ou zero. */
private fun brlToCents(raw: String): Long? {
    val t = raw.replace(".", "").replace(',', '.')
    val v = t.toBigDecimalOrNull() ?: return null
    val cents = v.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    return if (cents > 0) cents else null
}

// Mensagens que não são movimentação: ofertas, avisos, códigos, faturas e lembretes.
private val IgnoreWords = listOf(
    "fatura", "limite", "oferta", "promocao", "ganhe", "emprestimo", "credito pre-aprovado",
    "codigo", "senha", "token", "vence", "vencimento", "venceu", "agendad", "simul",
    "estorno", "reembolso", "cashback", "convite", "seguro", "investimento", "rendeu",
    "rendimento", "disponivel para"
)

private val IncomeWords = listOf(
    "voce recebeu", "pix recebido", "transferencia recebida", "recebeu uma transferencia",
    "recebeu um pix", "recebeu um pagamento", "pagamento recebido", "deposito", "salario",
    "credito em conta", "transferencia de"
)

private val ExpenseWords = listOf(
    "compra", "pagamento", "voce pagou", "pagou", "debito", "voce enviou", "enviou",
    "pix enviado", "transferencia enviada", "saque", "boleto pago", "paga"
)

private fun method(plain: String): String = when {
    "pix" in plain -> "Pix"
    "debito" in plain -> "Débito"
    "credito" in plain -> "Crédito"
    "boleto" in plain -> "Boleto"
    else -> "Compra"
}

/** Procura o nome do estabelecimento (ou da pessoa) no trecho que vem depois do valor. */
private val MerchantPatterns = listOf(
    Regex("""\b(?:aprovad[ao]|realizad[ao]|efetuad[ao]|confirmad[ao])\s+(?:em|no|na)\s+(.+)""", RegexOption.IGNORE_CASE),
    Regex("""\bpara\s+(.+)""", RegexOption.IGNORE_CASE),
    Regex("""\bde\s+(.+)""", RegexOption.IGNORE_CASE),
    Regex("""\b(?:em|no|na)\s+(.+)""", RegexOption.IGNORE_CASE)
)

// "cartão final 1234", "conta corrente" etc. não são nomes de estabelecimento.
private val NotAMerchant = Regex("""^(?:cart[aã]o|final|conta|sua conta|seu cart[aã]o)\b""", RegexOption.IGNORE_CASE)

private val BeforeAmountPattern =
    Regex("""\bem\s+(.+?)\s+(?:no\s+)?valor\s+de\s*$""", RegexOption.IGNORE_CASE)

private val MerchantStop = Regex(
    """\s+(?:com|no|na|às|as|via|cart[aã]o|final|hoje|ontem|usando|pelo|pela|\d{1,2}/\d{1,2})\b.*$""",
    RegexOption.IGNORE_CASE
)

private fun cleanMerchant(raw: String): String {
    var s = raw.replace('*', ' ').replace(Regex("""[.!?]+\s.*$"""), "")
    s = s.trim().trimEnd('.', '!', ',', ':', ';')
    s = MerchantStop.replace(s, "")
    s = s.replace(Regex("""\s+"""), " ").trim()
    // "IFOOD IFOOD" (comum em "IFOOD *IFOOD") vira "IFOOD".
    val words = s.split(' ').filter { it.isNotEmpty() }
    val dedup = ArrayList<String>()
    for (w in words) if (dedup.isEmpty() || !dedup.last().equals(w, ignoreCase = true)) dedup.add(w)
    s = dedup.joinToString(" ")
    // Tudo em maiúsculas vira "Capitalizado".
    if (s.length > 3 && s == s.uppercase()) {
        s = s.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }
    return s.take(40).trim()
}

/**
 * Interpreta o título e o texto de uma notificação de banco.
 * Retorna null quando não parece ser uma compra, pagamento ou recebimento com valor.
 */
fun parseBankNotification(title: String, text: String): ParsedNotification? {
    val full = "$title. $text".replace(' ', ' ').replace(Regex("""\s+"""), " ").trim()
    val plain = full.plain()
    if (IgnoreWords.any { it in plain }) return null

    val m = AmountRegex.find(full) ?: return null
    val cents = brlToCents(m.groupValues[1]) ?: return null

    val income = IncomeWords.any { it in plain }
    val expense = ExpenseWords.any { it in plain }
    if (!income && !expense) return null

    val after = full.substring(m.range.last + 1)
    var merchant = ""
    for (p in MerchantPatterns) {
        val found = p.find(after)?.groupValues?.get(1) ?: continue
        val cleaned = cleanMerchant(found)
        if (cleaned.length >= 2 && !NotAMerchant.containsMatchIn(cleaned)) {
            merchant = cleaned
            break
        }
    }
    if (merchant.isEmpty()) {
        // Formato "Compra aprovada em LOJA no valor de R$ 10,00": o nome vem antes do valor.
        val before = full.substring(0, m.range.first)
        val found = BeforeAmountPattern.find(before)?.groupValues?.get(1)
        if (found != null) {
            val cleaned = cleanMerchant(found)
            if (cleaned.length >= 2) merchant = cleaned
        }
    }
    val method = method(plain)
    if (merchant.isEmpty()) merchant = if (income) "Recebimento ($method)" else "$method sem nome"

    return ParsedNotification(cents, income, merchant, method)
}

/** Palpite de categoria a partir do nome do estabelecimento. Retorna null se não souber. */
fun guessCategory(merchant: String): String? {
    val p = merchant.plain()
    fun has(vararg k: String) = k.any { it in p }
    return when {
        has("ifood", "rappi", "restaurante", "lanchonete", "padaria", "pizzaria", "hamburg",
            "mercado", "supermercado", "atacad", "acougue", "sorvete", "cafe", "burger", "mcdonald") -> "Alimentação"
        has("uber", "99", "posto", "combustivel", "shell", "ipiranga", "estacionamento",
            "pedagio", "sem parar", "metro", "onibus") -> "Transporte"
        has("farmacia", "drogaria", "droga", "hospital", "clinica", "laboratorio", "dentista") -> "Saúde"
        has("netflix", "spotify", "cinema", "ingresso", "steam", "playstation", "xbox",
            "youtube", "disney", "hbo", "prime video") -> "Lazer"
        has("amazon", "shopee", "mercado livre", "magalu", "magazine", "americanas", "shein",
            "aliexpress", "renner", "c&a", "zara") -> "Compras"
        has("energia", "enel", "cemig", "agua", "sabesp", "internet", "vivo", "claro", "tim ",
            "oi ", "telefon", "condominio") -> if (has("condominio")) "Moradia" else "Contas"
        has("aluguel", "imobiliaria") -> "Moradia"
        else -> null
    }
}
