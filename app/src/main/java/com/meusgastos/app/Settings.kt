package com.meusgastos.app

import android.content.Context

/** Preferências simples guardadas no aparelho. */
class Settings(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("meus_gastos", Context.MODE_PRIVATE)

    /** Limite mensal para gastos variáveis, em centavos. 0 = sem limite. */
    var limitCents: Long
        get() = prefs.getLong("limit", 0L)
        set(value) {
            prefs.edit().putLong("limit", value).apply()
        }

    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminderEnabled", false)
        set(value) {
            prefs.edit().putBoolean("reminderEnabled", value).apply()
        }

    var reminderHour: Int
        get() = prefs.getInt("reminderHour", 21)
        set(value) {
            prefs.edit().putInt("reminderHour", value).apply()
        }

    var lastType: String
        get() = prefs.getString("lastType", "") ?: ""
        set(value) {
            prefs.edit().putString("lastType", value).apply()
        }

    var lastCategory: String
        get() = prefs.getString("lastCategory", "") ?: ""
        set(value) {
            prefs.edit().putString("lastCategory", value).apply()
        }
}
