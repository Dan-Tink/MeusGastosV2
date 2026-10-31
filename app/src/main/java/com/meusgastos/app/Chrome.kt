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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
      Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
}

@Composable
private fun RowScope.NavTab(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val color =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pílula atrás do ícone da aba aberta.
        Box(
            Modifier
                .width(56.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(badge = {
                if (item.badge > 0) Badge { Text(item.badge.toString()) }
            }) {
                Icon(
                    if (selected) item.iconSelected else item.icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Aparência: modo (automático, claro, escuro) e cor de destaque. Tudo vale na hora, então o
 * próprio painel serve de prévia, e a prévia em miniatura mostra como ficam cabeçalho, cartão,
 * botão e chip. As escolhas ficam salvas.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(
    mode: ThemeMode,
    onMode: (ThemeMode) -> Unit,
    accent: AccentTheme,
    onAccent: (AccentTheme) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    val modeNames = listOf("Automático", "Claro", "Escuro")
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Aparência", style = MaterialTheme.typography.titleLarge)

            // ---- Prévia em miniatura ----
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = CardBorder
            ) {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                HeaderBackground,
                                RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                            )
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 28.dp)
                    ) {
                        Text("Outubro 2026", color = OnHeader, fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .offset(y = (-18).dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Saldo do mês",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "R$ 1.210,00",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PositiveColor
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Salvar",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp)
                            .offset(y = (-8).dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillChip(true, {}, "Selecionado")
                        PillChip(false, {}, "Normal")
                    }
                }
            }

            // ---- Modo ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Modo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SegmentedChoice(
                    options = modeNames,
                    selectedIndex = modes.indexOf(mode).coerceAtLeast(0),
                    onSelect = { onMode(modes[it]) }
                )
            }

            // ---- Cor ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cor do aplicativo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth()) {
                    AccentTheme.entries.forEach { a ->
                        val sel = a == accent
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = sel,
                                    role = Role.RadioButton,
                                    onClick = { onAccent(a) }
                                )
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (sel) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(a.swatch),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) {
                                    Icon(
                                        androidx.compose.material.icons.Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                a.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
