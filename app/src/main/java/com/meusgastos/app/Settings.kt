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

    /** Liga ou desliga a leitura de notificações de banco (a permissão do sistema é separada). */
    var captureEnabled: Boolean
        get() = prefs.getBoolean("captureEnabled", true)
        set(value) {
            prefs.edit().putBoolean("captureEnabled", value).apply()
        }

    /** true = lança sozinho; false (padrão) = deixa em "Pendentes" para você confirmar. */
    var captureAuto: Boolean
        get() = prefs.getBoolean("captureAuto", false)
        set(value) {
            prefs.edit().putBoolean("captureAuto", value).apply()
        }

    /** Pacotes de bancos que o usuário desligou. Por padrão, todos os conhecidos ficam ligados. */
    var captureDisabled: Set<String>
        get() = HashSet(prefs.getStringSet("captureDisabled", emptySet()) ?: emptySet())
        set(value) {
            prefs.edit().putStringSet("captureDisabled", HashSet(value)).apply()
        }

    var captureTipDismissed: Boolean
        get() = prefs.getBoolean("captureTipDismissed", false)
        set(value) {
            prefs.edit().putBoolean("captureTipDismissed", value).apply()
        }

    /** "system", "light" ou "dark". Guardado como texto para sobreviver a mudanças no app. */
    var themeMode: String
        get() = prefs.getString("themeMode", ThemeMode.LIGHT.key) ?: ThemeMode.LIGHT.key
        set(value) {
            prefs.edit().putString("themeMode", value).apply()
        }

    /** Cor de destaque do app (chave de AccentTheme). */
    var accent: String
        get() = prefs.getString("accent", AccentTheme.VERDE.key) ?: AccentTheme.VERDE.key
        set(value) {
            prefs.edit().putString("accent", value).apply()
        }

    var lastCategory: String
        get() = prefs.getString("lastCategory", "") ?: ""
        set(value) {
            prefs.edit().putString("lastCategory", value).apply()
        }
}
