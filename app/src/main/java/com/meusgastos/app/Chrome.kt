package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------
// Peças da identidade visual que se repetem nas abas: cabeçalho verde, barra inferior com o
// botão "+" redondo no centro e a escolha de tema.
// ---------------------------------------------------------------------------------------

/** Cabeçalho verde de cantos curvos embaixo. O conteúdo recebe a cor de texto do cabeçalho. */
@Composable
fun HeaderBar(extraBottom: Dp = 0.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = HeaderBackground,
        contentColor = OnHeader,
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    ) {
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp, bottom = 22.dp + extraBottom),
            content = content
        )
    }
}

class NavItem(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val badge: Int = 0
)

/**
 * Barra inferior: duas abas, o botão "+" no centro e mais duas abas. O "+" faz a ação da aba
 * aberta (novo gasto, novo ganho ou nova dívida), por isso o texto de acessibilidade muda.
 */
@Composable
fun AppBottomBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    addLabel: String,
    onAdd: () -> Unit
) {
    require(items.size == 4) { "A barra espera 4 abas" }
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab(items[0], selected == 0) { onSelect(0) }
            NavTab(items[1], selected == 1) { onSelect(1) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(56.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(role = Role.Button, onClickLabel = addLabel, onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = addLabel,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            NavTab(items[2], selected == 2) { onSelect(2) }
            NavTab(items[3], selected == 3) { onSelect(3) }
        }
    }
}

@Composable
private fun RowScope.NavTab(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val color =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(badge = {
            if (item.badge > 0) Badge { Text(item.badge.toString()) }
        }) {
            Icon(
                if (selected) item.iconSelected else item.icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1
        )
    }
}

/** Escolha do tema. A mudança vale na hora e fica salva para a próxima vez que abrir o app. */
@Composable
fun ThemeDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "Automático", "Segue o modo claro ou escuro do celular"),
        Triple(ThemeMode.LIGHT, "Claro", "Fundo claro, como na referência (padrão)"),
        Triple(ThemeMode.DARK, "Escuro", "Fundo escuro, mais confortável à noite")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema do aplicativo") },
        text = {
            Column {
                options.forEach { (mode, label, hint) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == mode,
                                role = Role.RadioButton,
                                onClick = { onSelect(mode) }
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == mode, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(label, fontWeight = FontWeight.SemiBold)
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
