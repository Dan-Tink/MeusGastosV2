package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.rotate
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
                .padding(top = 8.dp, bottom = (if (extraBottom > 0.dp) 22.dp else 8.dp) + extraBottom),
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

/** Altura da barra de abas (sem o botão "+" que sobe acima dela). */
val BottomBarHeight = 72.dp

/** Quanto o botão "+" (com o anel) se estende acima da barra. */
private val FabLift = 52.dp

/**
 * Barra inferior com cinco abas. O botão "+" fica acima da aba do meio (Resumo), num anel da
 * cor da barra que o encaixa nela como parte das abas. Ele abre botões flutuantes para criar
 * despesa, ganho ou dívida, de qualquer aba. Ocupa [BottomBarHeight] + a parte do "+" que sobe;
 * essa faixa de cima é transparente e deixa o toque passar para o conteúdo.
 */
@Composable
fun AppBottomBar(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    addOpen: Boolean,
    onAddOpenChange: (Boolean) -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddDebt: () -> Unit,
    onAddFixed: () -> Unit,
    modifier: Modifier = Modifier
) {
    require(items.size == 5) { "A barra espera 5 abas" }
    Box(modifier.fillMaxWidth().height(BottomBarHeight + FabLift)) {
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp
        ) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(BottomBarHeight - 1.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { i, item -> NavTab(item, selected == i) { onSelect(i) } }
                }
            }
        }

        // Anel (da cor da barra) com o botão redondo dentro, apoiado na borda de cima da barra.
        val ringSize = 60.dp
        val rotation by androidx.compose.animation.core.animateFloatAsState(
            if (addOpen) 45f else 0f, label = "addRotation"
        )
        val gap = with(androidx.compose.ui.platform.LocalDensity.current) { (ringSize + 8.dp).roundToPx() }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(ringSize)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (addOpen) "Fechar opções" else "Adicionar",
                        onClick = { onAddOpenChange(!addOpen) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (addOpen) "Fechar opções" else "Adicionar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation)
                )
            }
        }
        // O menu fica ancorado no anel; o popup fica na tela até a animação de saída terminar.
        Box(Modifier.align(Alignment.TopCenter).size(ringSize)) {
            val menuState = remember { androidx.compose.animation.core.MutableTransitionState(false) }
            menuState.targetState = addOpen
            if (menuState.currentState || menuState.targetState) {
                androidx.compose.ui.window.Popup(
                    alignment = Alignment.BottomCenter,
                    offset = androidx.compose.ui.unit.IntOffset(0, -gap),
                    onDismissRequest = { onAddOpenChange(false) },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visibleState = menuState,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(
                            initialScale = 0.5f,
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        ),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(
                            targetScale = 0.5f,
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        )
                    ) {
                        // Quatro botões redondos em arco sobre o "+": os das pontas mais baixos.
                        Box(Modifier.width(320.dp).height(140.dp)) {
                            AddOption("Ganho", Icons.Filled.Payments,
                                Modifier.align(Alignment.BottomCenter).offset(x = (-112).dp, y = (-4).dp)) {
                                onAddOpenChange(false); onAddIncome()
                            }
                            AddOption("Despesa", Icons.Filled.Receipt,
                                Modifier.align(Alignment.BottomCenter).offset(x = (-38).dp, y = (-50).dp)) {
                                onAddOpenChange(false); onAddExpense()
                            }
                            AddOption("Fixa", Icons.Filled.EventRepeat,
                                Modifier.align(Alignment.BottomCenter).offset(x = 38.dp, y = (-50).dp)) {
                                onAddOpenChange(false); onAddFixed()
                            }
                            AddOption("Dívida", Icons.Filled.AccountBalanceWallet,
                                Modifier.align(Alignment.BottomCenter).offset(x = 112.dp, y = (-4).dp)) {
                                onAddOpenChange(false); onAddDebt()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Botão redondo do menu do "+", com o nome embaixo. */
@Composable
private fun AddOption(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (dark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label,
                    tint = if (dark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
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
        containerColor = sheetColor(),
        shape = SheetShape
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Aparência",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        Text(java.time.YearMonth.now().label(), color = OnHeader, fontWeight = FontWeight.Bold)
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
