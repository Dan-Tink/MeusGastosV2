package com.meusgastos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeusGastosTheme {
                HomeScreen(vm)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Lança os gastos e ganhos recorrentes que estiverem pendentes.
        vm.generateRecurring()
    }
}
