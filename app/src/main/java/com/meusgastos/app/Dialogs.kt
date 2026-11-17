@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.meusgastos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------------------
// Tipos de despesa (fixo / variável)
// ---------------------------------------------------------------------------------------

@Composable
fun TypesDialog(
    types: List<ExpenseType>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onToggleFixed: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tipos de despesa") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(typeColor(t.name))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name)
                            Text(
                                if (t.fixed) "Fixo (toque p/ alternar)" else "Variável (toque p/ alternar)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onToggleFixed(t.name, !t.fixed) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        IconButton(onClick = { renaming = t.name; input = t.name }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renomear")
                        }
                        IconButton(onClick = { onDelete(t.name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (renaming != null) "Novo nome" else "Novo tipo") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    val r = renaming
                    if (r != null) onRename(r, input) else onAdd(input)
                    input = ""
                    renaming = null
                }
            ) { Text(if (renaming != null) "Salvar" else "Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

// ---------------------------------------------------------------------------------------
// Categorias (Alimentação, Transporte, Lazer...)
// ---------------------------------------------------------------------------------------

@Composable
fun CategoriesDialog(
    categories: List<Category>,
    onAdd: (String, String?) -> Unit,
    onSetIcon: (String, String?) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }
    var newIcon by remember { mutableStateOf<String?>(null) }
    /** Quem está escolhendo ícone: o nome de uma categoria existente, ou "" para a nova. */
    var pickingFor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categorias") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clickable { pickingFor = c.name }) {
                            CategoryBadge(c.name, size = 36.dp, iconKey = c.icon)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            c.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { renaming = c.name; input = c.name }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renomear")
                        }
                        IconButton(onClick = { onDelete(c.name) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (renaming == null) {
                        Box(Modifier.clickable { pickingFor = "" }) {
                            CategoryBadge(input.ifBlank { "Nova" }, size = 44.dp, iconKey = newIcon)
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(if (renaming != null) "Novo nome" else "Nova categoria") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                if (renaming == null) {
                    Text(
                        "Toque no círculo para escolher o ícone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = input.isNotBlank(),
                onClick = {
                    val r = renaming
                    if (r != null) onRename(r, input) else onAdd(input, newIcon)
                    input = ""
                    newIcon = null
                    renaming = null
                }
            ) { Text(if (renaming != null) "Salvar" else "Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )

    val target = pickingFor
    if (target != null) {
        val current = if (target.isEmpty()) newIcon else categories.firstOrNull { it.name == target }?.icon
        AlertDialog(
            onDismissRequest = { pickingFor = null },
            title = { Text("Ícone da categoria") },
            text = {
                FlowRow(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseIcons.forEach { (key, vector) ->
                        val sel = current == key
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                                )
                                .clickable {
                                    if (target.isEmpty()) newIcon = key else onSetIcon(target, key)
                                    pickingFor = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                vector,
                                contentDescription = key,
                                tint = if (sel) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (target.isEmpty()) newIcon = null else onSetIcon(target, null)
                    pickingFor = null
                }) { Text("Padrão") }
            },
            dismissButton = { TextButton(onClick = { pickingFor = null }) { Text("Cancelar") } }
        )
    }
}

// ---------------------------------------------------------------------------------------
// Limite mensal e lembrete diário
// ---------------------------------------------------------------------------------------

@Composable
fun SettingsDialog(
    limitCents: Long,
    reminderEnabled: Boolean,
    reminderHour: Int,
    onSaveLimit: (Long) -> Unit,
    onSetReminder: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var limitText by remember { mutableStateOf(if (limitCents > 0) limitCents.toPlain() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limite e lembrete") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Limite de despesas do mês",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Soma de despesas fixas e variáveis. O painel muda de cor ao chegar em 80% " +
                        "e ao passar do limite. Deixe vazio para não usar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Limite") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lembrete diário", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Uma notificação por dia, só se você ainda não lançou nada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { onSetReminder(it, reminderHour) }
                    )
                }
                if (reminderEnabled) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(12, 18, 20, 21, 22).forEach { h ->
                            FilterChip(
                                selected = reminderHour == h,
                                onClick = { onSetReminder(true, h) },
                                label = { Text("${h}h") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveLimit(parseCents(limitText) ?: 0L)
                onDismiss()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
