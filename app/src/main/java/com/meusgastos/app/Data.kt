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

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val description: String,
    val amountCents: Long,
    val epochDay: Long,
    val category: String = "Outros"
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
        recurring: List<Recurring>
    ) {
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

@Database(
    entities = [Expense::class, ExpenseType::class, Category::class, Income::class, Recurring::class],
    version = 3,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("INSERT INTO types(name, fixed) VALUES ('Fixa', 1), ('Variável', 0)")
                            db.execSQL(categoriesInsertSql())
                        }
                    }).build().also { instance = it }
            }
    }
}
