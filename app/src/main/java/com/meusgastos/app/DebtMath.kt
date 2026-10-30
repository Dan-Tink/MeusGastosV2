package com.meusgastos.app

/*
 * Cálculos das dívidas. Tudo em centavos inteiros (Long): nunca Double nem Float para dinheiro.
 * Este arquivo não depende de Android nem de banco de dados, para poder ser testado sozinho.
 *
 * Regras:
 *  - "Já pago" é sempre a soma dos pagamentos do histórico. Não existe um segundo número guardado
 *    que possa ficar diferente do histórico.
 *  - Saldo devedor = valor original - já pago, nunca abaixo de zero.
 *  - A dívida só está quitada quando o saldo é zero.
 *  - Um pagamento não pode ser maior que o saldo devedor.
 */

enum class PaymentCheck {
    OK,

    /** Valor zero, negativo ou não informado. */
    INVALID_AMOUNT,

    /** Valor maior que o saldo devedor. */
    EXCEEDS_BALANCE,

    /** A dívida já está quitada: não há o que pagar. */
    ALREADY_SETTLED,

    /** A dívida não existe mais (foi excluída enquanto a tela estava aberta). */
    DEBT_NOT_FOUND
}

/** Situação de uma dívida, calculada a partir do valor original e do total já pago. */
data class DebtStatus(val originalCents: Long, val paidCents: Long) {
    /** Quanto ainda falta pagar. */
    val balanceCents: Long get() = maxOf(0L, originalCents - paidCents)

    /** Quanto foi pago além do valor original (só acontece se o valor original for reduzido depois). */
    val overpaidCents: Long get() = maxOf(0L, paidCents - originalCents)

    /** Quitada somente quando o saldo chegou a zero. */
    val settled: Boolean get() = balanceCents == 0L

    /** Parte já paga, de 0 a 1, para a barra de progresso. */
    val progress: Float
        get() = if (originalCents <= 0L) 0f else (minOf(paidCents, originalCents).toDouble() / originalCents).toFloat()
}

/**
 * Confere se um pagamento de [amountCents] pode ser registrado numa dívida de valor [originalCents]
 * que já recebeu [paidCents]. Não altera nada: só diz se pode ou por que não.
 */
fun checkPayment(originalCents: Long, paidCents: Long, amountCents: Long): PaymentCheck {
    if (amountCents <= 0L) return PaymentCheck.INVALID_AMOUNT
    val balance = DebtStatus(originalCents, paidCents).balanceCents
    if (balance == 0L) return PaymentCheck.ALREADY_SETTLED
    if (amountCents > balance) return PaymentCheck.EXCEEDS_BALANCE
    return PaymentCheck.OK
}

/** Totais mostrados no resumo da aba Dívidas. */
data class DebtTotals(
    /** Soma dos saldos devedores (o que ainda falta pagar), não dos valores originais. */
    val openBalanceCents: Long,
    /** Soma de tudo que já foi pago, em todas as dívidas (em aberto e quitadas). */
    val totalPaidCents: Long,
    /** Quantidade de dívidas ainda em aberto. */
    val openCount: Int,
    /** Quantidade de dívidas quitadas. */
    val settledCount: Int
)

fun summarizeDebts(debts: List<DebtStatus>): DebtTotals {
    var open = 0L
    var paid = 0L
    var openCount = 0
    var settledCount = 0
    for (d in debts) {
        paid += d.paidCents
        if (d.settled) {
            settledCount++
        } else {
            openCount++
            open += d.balanceCents
        }
    }
    return DebtTotals(open, paid, openCount, settledCount)
}

/**
 * Saldo devedor depois de cada pagamento, na ordem em que aconteceram. Recebe os valores já em
 * ordem cronológica e devolve, para cada um, quanto ainda faltava pagar logo depois dele.
 */
fun balancesAfterPayments(originalCents: Long, amountsInOrder: List<Long>): List<Long> {
    var paid = 0L
    return amountsInOrder.map { a ->
        paid += a
        maxOf(0L, originalCents - paid)
    }
}
