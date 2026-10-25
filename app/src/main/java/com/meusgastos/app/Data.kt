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
import java.time.LocalDate

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
    val installmentCount: Int = 0,
    /** false = conta pendente (ainda não paga). */
    @ColumnInfo(defaultValue = "1") val paid: Boolean = true,
    /** Nome do arquivo do comprovante, guardado na pasta do app. Null = sem comprovante. */
    val receipt: String? = null
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

/** Dívida sem prazo: só um lembrete de quanto você deve e a quem. */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** A quem você deve. */
    val creditor: String,
    val note: String,
    /** Valor original da dívida. */
    val amountCents: Long,
    /** Quanto já foi pago (abatido). */
    val paidCents: Long,
    val epochDay: Long,
    /** Dia em que foi quitada; null enquanto estiver em aberto. */
    val settledDay: Long?
) {
    val remainingCents: Long get() = (amountCents - paidCents).coerceAtLeast(0)
    val settled: Boolean get() = settledDay != null
}

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

    @Query("SELECT * FROM debts ORDER BY (settledDay IS NOT NULL), epochDay DESC, id DESC")
    abstract fun observeDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    abstract suspend fun debtsOnce(): List<Debt>

    @Insert
    abstract suspend fun insertDebt(debt: Debt)

    @Insert
    abstract suspend fun insertDebts(list: List<Debt>)

    @Update
    abstract suspend fun updateDebt(debt: Debt)

    @Delete
    abstract suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts")
    abstract suspend fun deleteAllDebts()

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
    @Query("UPDATE expenses SET paid = :paid WHERE id = :id")
    abstract suspend fun setPaid(id: Long, paid: Boolean)

    /** Contas pendentes com vencimento até o dia informado (inclui as atrasadas). */
    @Query("SELECT COUNT(*) FROM expenses WHERE paid = 0 AND epochDay <= :untilDay")
    abstract suspend fun countPendingUntil(untilDay: Long): Int

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
        debts: List<Debt>? = null
    ) {
        if (debts != null) {
            deleteAllDebts()
            insertDebts(debts)
        }
        deleteAllExpenses()
        deleteAllIncomes()
        deleteAllRecurring()
        deleteAllTypes()
        deleteAllCategories()
        insertTypes(types)
        insertCategories(categories)
        insertExpenses(expenses)
        insertIncomes(incomes)
        insertRecurringList(recurring)
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
        // Tudo que já existe passa a valer como pago, menos o que tem data futura.
        db.execSQL("ALTER TABLE expenses ADD COLUMN paid INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE expenses ADD COLUMN receipt TEXT")
        db.execSQL("UPDATE expenses SET paid = 0 WHERE epochDay > ${LocalDate.now().toEpochDay()}")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS debts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "creditor TEXT NOT NULL, " +
                "note TEXT NOT NULL, " +
                "amountCents INTEGER NOT NULL, " +
                "paidCents INTEGER NOT NULL, " +
                "epochDay INTEGER NOT NULL, " +
                "settledDay INTEGER)"
        )
    }
}

@Database(
    entities = [
        Expense::class, ExpenseType::class, Category::class, Income::class,
        Recurring::class, Debt::class
    ],
    version = 6,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("INSERT INTO types(name, fixed) VALUES ('Fixa', 1), ('Variável', 0)")
                            db.execSQL(categoriesInsertSql())
                        }
                    }).build().also { instance = it }
            }
    }
}
