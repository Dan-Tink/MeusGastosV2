package com.meusgastos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeusGastosTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        if (tab == 0) HomeScreen(vm) else DebtsScreen(vm)
                    }
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                            label = { Text("Gastos") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                            label = { Text("Dívidas") }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Lança os gastos e ganhos recorrentes que estiverem pendentes.
        vm.generateRecurring()
    }
}
