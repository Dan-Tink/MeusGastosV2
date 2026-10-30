package com.meusgastos.app

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val description: String,
    val amountCents: Long,
    val epochDay: Long,
    val category: String = "Outros",
    /** Parcelas da mesma compra compartilham o mesmo groupId. Null = gasto avulso. */
    val groupId: Long? = null,
    /** Número desta parcela (1..installmentCount). 0 = gasto avulso. */
    val installmentNo: Int = 0,
    val installmentCount: Int = 0
)

@Entity(tableName = "types")
data class ExpenseType(
    @PrimaryKey val name: String,
    /** true = conta como "gasto fixo" no painel do mês; false = "gasto variável". */
    @ColumnInfo(defaultValue = "0") val fixed: Boolean = false
)

@Entity(tableName = "categories")
data class Category(@PrimaryKey val name: String)

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val epochDay: Long
)

/**
 * Ganho fixo: vale em todo mês de [startMonth] até [endMonth] (null = sem fim).
 * Os meses usam o formato "2026-10". Nada é lançado mês a mês: o valor é somado ao mês na hora
 * de calcular, então ele aparece sozinho em qualquer mês, inclusive nos futuros.
 */
@Entity(tableName = "fixed_incomes")
data class FixedIncome(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val dayOfMonth: Int,
    val startMonth: String,
    val endMonth: String? = null
)

/** Este ganho fixo vale no mês [m]? */
fun FixedIncome.activeIn(m: YearMonth): Boolean {
    val start = runCatching { YearMonth.parse(startMonth) }.getOrNull() ?: return false
    val end = endMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
    return !m.isBefore(start) && (end == null || !m.isAfter(end))
}

/**
 * Dívida sem data de vencimento (ex.: R$ 300 que devo à minha irmã). Não é uma despesa: os
 * pagamentos ficam em [DebtPayment] e não entram nos gastos nem nos relatórios do mês.
 * O "já pago" e o saldo não são guardados aqui: saem da soma dos pagamentos (veja DebtMath.kt).
 */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Pessoa ou instituição a quem devo. */
    val creditor: String,
    /** Motivo ou observação. Pode ficar vazio. */
    val description: String,
    /** Valor total da dívida, em centavos. */
    val originalCents: Long,
    /** Dia do cadastro (epochDay), preenchido sozinho. */
    val createdDay: Long
)

/** Um pagamento (parcial ou final) de uma dívida. */
@Entity(tableName = "debt_payments")
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amountCents: Long,
    val epochDay: Long,
    /** Observação opcional. Pode ficar vazia. */
    val note: String
)

/** Gasto ou ganho que se repete todo mês (aluguel, internet, salário...). */
@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isIncome: Boolean,
    val type: String,
    val category: String,
    val description: String,
    val amountCents: Long,
    val dayOfMonth: Int,
    /** Último mês já lançado, no formato "2026-10". */
    val lastGenerated: String
)

/**
 * Movimentação detectada numa notificação de banco, esperando confirmação.
 * Só guarda o que foi interpretado (valor, nome, banco); o texto original da notificação é descartado.
 * Linhas confirmadas ou dispensadas ficam alguns dias, para não lançar duas vezes a mesma notificação.
 */
@Entity(tableName = "pending")
data class PendingTx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nome do banco, como "Nubank". */
    val source: String,
    val amountCents: Long,
    val isIncome: Boolean,
    val description: String,
    /** "Pix", "Débito", "Crédito", "Boleto" ou "Compra". */
    val method: String,
    val epochDay: Long,
    val createdAt: Long,
    val type: String,
    val category: String,
    /** 0 = esperando, 1 = confirmado, 2 = dispensado. */
    @ColumnInfo(defaultValue = "0") val status: Int = 0
)

/** Tipo e categoria do último gasto com a mesma descrição. */
data class DescMatch(val type: String, val category: String)

@Dao
abstract class ExpenseDao {
    // ---- leitura em tempo real ----
    @Query("SELECT * FROM expenses ORDER BY epochDay DESC, id DESC")
    abstract fun observeExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM types ORDER BY name")
    abstract fun observeTypes(): Flow<List<ExpenseType>>

    @Query("SELECT * FROM categories ORDER BY name")
    abstract fun observeCategories(): Flow<List<Category>>

    @Query("SELECT * FROM incomes ORDER BY epochDay DESC, id DESC")
    abstract fun observeIncomes(): Flow<List<Income>>

    @Query("SELECT * FROM recurring ORDER BY isIncome DESC, description")
    abstract fun observeRecurring(): Flow<List<Recurring>>

    // ---- leitura única ----
    @Query("SELECT * FROM expenses")
    abstract suspend fun expensesOnce(): List<Expense>

    @Query("SELECT * FROM incomes")
    abstract suspend fun incomesOnce(): List<Income>

    @Query("SELECT * FROM types")
    abstract suspend fun typesOnce(): List<ExpenseType>

    @Query("SELECT * FROM categories")
    abstract suspend fun categoriesOnce(): List<Category>

    @Query("SELECT * FROM recurring")
    abstract suspend fun recurringOnce(): List<Recurring>

    @Query("SELECT COUNT(*) FROM expenses WHERE epochDay = :day")
    abstract suspend fun countOnDay(day: Long): Int

    // ---- gastos ----
    @Insert
    abstract suspend fun insert(expense: Expense)

    @Insert
    abstract suspend fun insertExpenses(list: List<Expense>)

    @Update
    abstract suspend fun update(expense: Expense)

    @Delete
    abstract suspend fun delete(expense: Expense)

    // ---- parcelas em grupo ----
    @Query("DELETE FROM expenses WHERE groupId = :groupId AND installmentNo >= :fromNo")
    abstract suspend fun deleteGroupFrom(groupId: Long, fromNo: Int)

    @Query(
        "UPDATE expenses SET description = :description, type = :type, category = :category, " +
            "amountCents = :amountCents WHERE groupId = :groupId AND installmentNo >= :fromNo"
    )
    abstract suspend fun updateGroupFrom(
        groupId: Long,
        fromNo: Int,
        description: String,
        type: String,
        category: String,
        amountCents: Long
    )

    // ---- ganhos ----
    @Insert
    abstract suspend fun insertIncome(income: Income)

    @Insert
    abstract suspend fun insertIncomes(list: List<Income>)

    @Update
    abstract suspend fun updateIncome(income: Income)

    @Delete
    abstract suspend fun deleteIncome(income: Income)

    // ---- recorrentes ----
    @Insert
    abstract suspend fun insertRecurring(r: Recurring)

    @Insert
    abstract suspend fun insertRecurringList(list: List<Recurring>)

    @Update
    abstract suspend fun updateRecurring(r: Recurring)

    @Delete
    abstract suspend fun deleteRecurring(r: Recurring)

    @Query("UPDATE recurring SET lastGenerated = :value WHERE id = :id")
    abstract suspend fun setLastGenerated(id: Long, value: String)

    // ---- tipos ----
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertType(type: ExpenseType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTypes(list: List<ExpenseType>)

    @Query("DELETE FROM types WHERE name = :name")
    abstract suspend fun deleteType(name: String)

    @Query("UPDATE types SET fixed = :fixed WHERE name = :name")
    abstract suspend fun setTypeFixed(name: String, fixed: Boolean)

    @Query("UPDATE expenses SET type = :newName WHERE type = :oldName")
    abstract suspend fun renameTypeInExpenses(oldName: String, newName: String)

    @Query("UPDATE recurring SET type = :newName WHERE type = :oldName")
    abstract suspend fun renameTypeInRecurring(oldName: String, newName: String)

    @Query("UPDATE types SET name = :newName WHERE name = :oldName")
    abstract suspend fun renameTypeRow(oldName: String, newName: String)

    @Query("SELECT COUNT(*) FROM types WHERE name = :name")
    abstract suspend fun typeExists(name: String): Int

    @Query(
        "SELECT (SELECT COUNT(*) FROM expenses WHERE type = :name) + " +
            "(SELECT COUNT(*) FROM recurring WHERE type = :name)"
    )
    abstract suspend fun countByType(name: String): Int

    @Transaction
    open suspend fun renameType(oldName: String, newName: String) {
        if (oldName == newName) return
        renameTypeInExpenses(oldName, newName)
        renameTypeInRecurring(oldName, newName)
        if (typeExists(newName) > 0) deleteType(oldName) else renameTypeRow(oldName, newName)
    }

    // ---- categorias ----
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCategory(c: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategories(list: List<Category>)

    @Query("DELETE FROM categories WHERE name = :name")
    abstract suspend fun deleteCategory(name: String)

    @Query("UPDATE expenses SET category = :newName WHERE category = :oldName")
    abstract suspend fun renameCategoryInExpenses(oldName: String, newName: String)

    @Query("UPDATE recurring SET category = :newName WHERE category = :oldName")
    abstract suspend fun renameCategoryInRecurring(oldName: String, newName: String)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName")
    abstract suspend fun renameCategoryRow(oldName: String, newName: String)

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name")
    abstract suspend fun categoryExists(name: String): Int

    @Query(
        "SELECT (SELECT COUNT(*) FROM expenses WHERE category = :name) + " +
            "(SELECT COUNT(*) FROM recurring WHERE category = :name)"
    )
    abstract suspend fun countByCategory(name: String): Int

    @Transaction
    open suspend fun renameCategory(oldName: String, newName: String) {
        if (oldName == newName) return
        renameCategoryInExpenses(oldName, newName)
        renameCategoryInRecurring(oldName, newName)
        if (categoryExists(newName) > 0) deleteCategory(oldName) else renameCategoryRow(oldName, newName)
    }

    // ---- dívidas ----
    @Query("SELECT * FROM debts ORDER BY createdDay DESC, id DESC")
    abstract fun observeDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debt_payments ORDER BY epochDay DESC, id DESC")
    abstract fun observeDebtPayments(): Flow<List<DebtPayment>>

    @Query("SELECT * FROM debts")
    abstract suspend fun debtsOnce(): List<Debt>

    @Query("SELECT * FROM debt_payments")
    abstract suspend fun debtPaymentsOnce(): List<DebtPayment>

    @Query("SELECT * FROM debts WHERE id = :id")
    abstract suspend fun debtById(id: Long): Debt?

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM debt_payments WHERE debtId = :debtId")
    abstract suspend fun paidOfDebt(debtId: Long): Long

    @Insert
    abstract suspend fun insertDebt(d: Debt): Long

    @Insert
    abstract suspend fun insertDebts(list: List<Debt>)

    @Insert
    abstract suspend fun insertDebtPayment(p: DebtPayment): Long

    @Insert
    abstract suspend fun insertDebtPayments(list: List<DebtPayment>)

    @Update
    abstract suspend fun updateDebt(d: Debt)

    @Delete
    abstract suspend fun deleteDebtRow(d: Debt)

    @Delete
    abstract suspend fun deleteDebtPaymentRow(p: DebtPayment)

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId")
    abstract suspend fun deletePaymentsOfDebt(debtId: Long)

    @Query("DELETE FROM debts")
    abstract suspend fun deleteAllDebts()

    @Query("DELETE FROM debt_payments")
    abstract suspend fun deleteAllDebtPayments()

    /**
     * Registra um pagamento. A conferência e a gravação acontecem na mesma transação, então dois
     * toques seguidos não conseguem passar do saldo. Se algo estiver errado, nada é gravado.
     */
    @Transaction
    open suspend fun payDebt(debtId: Long, amountCents: Long, epochDay: Long, note: String): PaymentCheck {
        val debt = debtById(debtId) ?: return PaymentCheck.DEBT_NOT_FOUND
        val check = checkPayment(debt.originalCents, paidOfDebt(debtId), amountCents)
        if (check == PaymentCheck.OK) {
            insertDebtPayment(
                DebtPayment(debtId = debtId, amountCents = amountCents, epochDay = epochDay, note = note)
            )
        }
        return check
    }

    /** Exclui a dívida junto com o histórico dela. Não mexe em despesas nem em ganhos. */
    @Transaction
    open suspend fun deleteDebtWithPayments(d: Debt) {
        deletePaymentsOfDebt(d.id)
        deleteDebtRow(d)
    }

    // ---- ganhos fixos ----
    @Query("SELECT * FROM fixed_incomes ORDER BY description")
    abstract fun observeFixedIncomes(): Flow<List<FixedIncome>>

    @Query("SELECT * FROM fixed_incomes")
    abstract suspend fun fixedIncomesOnce(): List<FixedIncome>

    @Insert
    abstract suspend fun insertFixedIncome(f: FixedIncome): Long

    @Insert
    abstract suspend fun insertFixedIncomes(list: List<FixedIncome>)

    @Update
    abstract suspend fun updateFixedIncome(f: FixedIncome)

    @Delete
    abstract suspend fun deleteFixedIncome(f: FixedIncome)

    @Query("DELETE FROM fixed_incomes")
    abstract suspend fun deleteAllFixedIncomes()

    /**
     * Muda um ganho fixo a partir do mês [from]: os meses anteriores continuam com o valor antigo.
     * Se o ganho começou em [from] (ou depois), apenas atualiza.
     */
    @Transaction
    open suspend fun reviseFixedIncome(old: FixedIncome, updated: FixedIncome, from: YearMonth) {
        val start = runCatching { YearMonth.parse(old.startMonth) }.getOrNull() ?: from
        if (!from.isAfter(start)) {
            updateFixedIncome(
                updated.copy(id = old.id, startMonth = old.startMonth, endMonth = old.endMonth)
            )
        } else {
            updateFixedIncome(old.copy(endMonth = from.minusMonths(1).toString()))
            insertFixedIncome(updated.copy(id = 0, startMonth = from.toString(), endMonth = old.endMonth))
        }
    }

    /** Para o ganho fixo a partir do mês [from]. Os meses anteriores continuam contando. */
    @Transaction
    open suspend fun endFixedIncome(old: FixedIncome, from: YearMonth) {
        val start = runCatching { YearMonth.parse(old.startMonth) }.getOrNull() ?: from
        if (!from.isAfter(start)) {
            deleteFixedIncome(old)
        } else {
            updateFixedIncome(old.copy(endMonth = from.minusMonths(1).toString()))
        }
    }

    // ---- captura por notificação ----
    @Query("SELECT * FROM pending WHERE status = 0 ORDER BY createdAt DESC")
    abstract fun observePending(): Flow<List<PendingTx>>

    @Insert
    abstract suspend fun insertPending(p: PendingTx): Long

    /** Muda o status só se ainda estiver esperando. Devolve 0 se outro toque já resolveu. */
    @Query("UPDATE pending SET status = :status WHERE id = :id AND status = 0")
    abstract suspend fun claimPending(id: Long, status: Int): Int

    /** Confirma um pendente como veio da notificação. Seguro contra toque duplo. */
    @Transaction
    open suspend fun confirmPending(p: PendingTx) {
        if (claimPending(p.id, 1) == 0) return
        if (p.isIncome) {
            insertIncome(Income(description = p.description, amountCents = p.amountCents, epochDay = p.epochDay))
        } else {
            insert(
                Expense(
                    type = p.type,
                    description = p.description,
                    amountCents = p.amountCents,
                    epochDay = p.epochDay,
                    category = p.category
                )
            )
        }
    }

    /** Confirma um pendente usando o gasto que o usuário ajustou na tela de edição. */
    @Transaction
    open suspend fun confirmPendingAs(p: PendingTx, e: Expense) {
        if (claimPending(p.id, 1) == 0) return
        insert(e.copy(id = 0))
    }

    @Query(
        "SELECT COUNT(*) FROM pending WHERE source = :source AND amountCents = :cents " +
            "AND isIncome = :income AND description = :desc AND createdAt >= :since"
    )
    abstract suspend fun countRecentPending(
        source: String,
        cents: Long,
        income: Boolean,
        desc: String,
        since: Long
    ): Int

    @Query("DELETE FROM pending WHERE createdAt < :before AND status <> 0")
    abstract suspend fun purgePending(before: Long)

    @Query(
        "SELECT type, category FROM expenses WHERE LOWER(description) = LOWER(:desc) " +
            "ORDER BY id DESC LIMIT 1"
    )
    abstract suspend fun lastForDescription(desc: String): DescMatch?

    // ---- restaurar backup ----
    @Query("DELETE FROM expenses")
    abstract suspend fun deleteAllExpenses()

    @Query("DELETE FROM incomes")
    abstract suspend fun deleteAllIncomes()

    @Query("DELETE FROM types")
    abstract suspend fun deleteAllTypes()

    @Query("DELETE FROM categories")
    abstract suspend fun deleteAllCategories()

    @Query("DELETE FROM recurring")
    abstract suspend fun deleteAllRecurring()

    @Transaction
    open suspend fun replaceAll(
        types: List<ExpenseType>,
        categories: List<Category>,
        expenses: List<Expense>,
        incomes: List<Income>,
        recurring: List<Recurring>,
        fixedIncomes: List<FixedIncome>,
        debts: List<Debt>,
        debtPayments: List<DebtPayment>
    ) {
        deleteAllExpenses()
        deleteAllIncomes()
        deleteAllFixedIncomes()
        deleteAllDebtPayments()
        deleteAllDebts()
        deleteAllRecurring()
        deleteAllTypes()
        deleteAllCategories()
        insertTypes(types)
        insertCategories(categories)
        insertExpenses(expenses)
        insertIncomes(incomes)
        insertRecurringList(recurring)
        insertFixedIncomes(fixedIncomes)
        insertDebts(debts)
        insertDebtPayments(debtPayments)
    }
}

val DefaultCategories = listOf(
    "Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Compras", "Contas", "Outros"
)

private fun categoriesInsertSql(): String =
    "INSERT INTO categories(name) VALUES " + DefaultCategories.joinToString(", ") { "('$it')" }

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE types ADD COLUMN fixed INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE types SET fixed = 1 WHERE name = 'Fixa'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS incomes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "amountCents INTEGER NOT NULL, " +
                "epochDay INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN category TEXT NOT NULL DEFAULT 'Outros'")
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (name TEXT NOT NULL, PRIMARY KEY(name))")
        db.execSQL(categoriesInsertSql())
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS recurring (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "isIncome INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "amountCents INTEGER NOT NULL, " +
                "dayOfMonth INTEGER NOT NULL, " +
                "lastGenerated TEXT NOT NULL)"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN groupId INTEGER")
        db.execSQL("ALTER TABLE expenses ADD COLUMN installmentNo INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE expenses ADD COLUMN installmentCount INTEGER NOT NULL DEFAULT 0")

        // Religa parcelas antigas, que só tinham o sufixo "(n/N)" no texto da descrição.
        class OldRow(val id: Long, val base: String, val no: Int, val count: Int, val type: String)
        class OldGroup(val id: Long) {
            val used = HashSet<Int>()
        }

        val regex = Regex("^(.*) \\((\\d+)/(\\d+)\\)$")
        val rows = ArrayList<OldRow>()
        db.query("SELECT id, description, type FROM expenses ORDER BY id").use { c ->
            while (c.moveToNext()) {
                val m = regex.find(c.getString(1)) ?: continue
                val no = m.groupValues[2].toInt()
                val count = m.groupValues[3].toInt()
                if (count < 2 || no < 1 || no > count) continue
                rows.add(OldRow(c.getLong(0), m.groupValues[1], no, count, c.getString(2)))
            }
        }

        val groups = HashMap<String, MutableList<OldGroup>>()
        var nextId = 1L
        for (r in rows) {
            val key = "${r.type}|${r.count}|${r.base}"
            val list = groups.getOrPut(key) { ArrayList() }
            val group = list.firstOrNull { r.no !in it.used } ?: OldGroup(nextId++).also { list.add(it) }
            group.used.add(r.no)
            db.execSQL(
                "UPDATE expenses SET description = ?, groupId = ?, installmentNo = ?, " +
                    "installmentCount = ? WHERE id = ?",
                arrayOf<Any?>(r.base, group.id, r.no, r.count, r.id)
            )
        }
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS pending (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "source TEXT NOT NULL, " +
                "amountCents INTEGER NOT NULL, " +
                "isIncome INTEGER NOT NULL, " +
                "description TEXT NOT NULL, " +
                "method TEXT NOT NULL, " +
                "epochDay INTEGER NOT NULL, " +
                "createdAt INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "status INTEGER NOT NULL DEFAULT 0)"
        )
    }
}

/**
 * Ganhos recorrentes antigos viram ganhos fixos. O ganho já lançado no último mês gerado vira o
 * primeiro mês do ganho fixo (para não contar duas vezes); se ele não existir mais, o ganho fixo
 * começa no mês seguinte. Meses anteriores continuam como estavam.
 */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS fixed_incomes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "amountCents INTEGER NOT NULL, " +
                "dayOfMonth INTEGER NOT NULL, " +
                "startMonth TEXT NOT NULL, " +
                "endMonth TEXT)"
        )

        class Old(val description: String, val cents: Long, val day: Int, val last: String)

        val olds = ArrayList<Old>()
        db.query(
            "SELECT description, amountCents, dayOfMonth, lastGenerated FROM recurring WHERE isIncome = 1"
        ).use { c ->
            while (c.moveToNext()) olds.add(Old(c.getString(0), c.getLong(1), c.getInt(2), c.getString(3)))
        }

        for (o in olds) {
            val last = runCatching { YearMonth.parse(o.last) }.getOrNull() ?: YearMonth.now()
            val from = last.atDay(1).toEpochDay()
            val to = last.atEndOfMonth().toEpochDay()
            val existing: Long? = db.query(
                "SELECT id FROM incomes WHERE description = ? AND amountCents = ? " +
                    "AND epochDay BETWEEN ? AND ? LIMIT 1",
                arrayOf<Any?>(o.description, o.cents, from, to)
            ).use { c -> if (c.moveToFirst()) c.getLong(0) else null }

            val start = if (existing != null) {
                db.execSQL("DELETE FROM incomes WHERE id = ?", arrayOf<Any?>(existing))
                last
            } else {
                last.plusMonths(1)
            }
            db.execSQL(
                "INSERT INTO fixed_incomes(description, amountCents, dayOfMonth, startMonth, endMonth) " +
                    "VALUES (?, ?, ?, ?, NULL)",
                arrayOf<Any?>(o.description, o.cents, o.day, start.toString())
            )
        }
        db.execSQL("DELETE FROM recurring WHERE isIncome = 1")
    }
}

/** Só cria as tabelas das dívidas. Nenhum dado existente é lido, alterado ou apagado. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS debts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "creditor TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "originalCents INTEGER NOT NULL, " +
                "createdDay INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS debt_payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "debtId INTEGER NOT NULL, " +
                "amountCents INTEGER NOT NULL, " +
                "epochDay INTEGER NOT NULL, " +
                "note TEXT NOT NULL)"
        )
    }
}

@Database(
    entities = [
        Expense::class, ExpenseType::class, Category::class, Income::class,
        Recurring::class, PendingTx::class, FixedIncome::class, Debt::class, DebtPayment::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meus_gastos.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("INSERT INTO types(name, fixed) VALUES ('Fixa', 1), ('Variável', 0)")
                            db.execSQL(categoriesInsertSql())
                        }
                    }).build().also { instance = it }
            }
    }
}
