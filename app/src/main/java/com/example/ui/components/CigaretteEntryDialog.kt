package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.util.DateAndCurrencyUtils
import com.example.util.PresetContexts

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CigaretteEntryDialog(
    title: String,
    initialEpochDay: Long,
    initialHour: Int,
    initialMinute: Int,
    initialIntensity: Int?,
    initialContext: String?,
    initialNote: String?,
    allowDateChange: Boolean = true,
    todayEpochDay: Long,
    availableContexts: List<String> = PresetContexts.ALL,
    onAddCustomContext: (String) -> Unit = {},
    onRenameCustomContext: (oldContext: String, newContext: String) -> Unit = { _, _ -> },
    onDeleteCustomContext: (String) -> Unit = {},
    onResetCustomContexts: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (timestampMillis: Long, intensity: Int?, context: String?, note: String?) -> Unit
) {
    var selectedEpochDay by remember { mutableLongStateOf(initialEpochDay) }
    var hourText by remember { mutableStateOf(String.format("%02d", initialHour.coerceIn(0, 23))) }
    var minuteText by remember { mutableStateOf(String.format("%02d", initialMinute.coerceIn(0, 59))) }
    var selectedIntensity by remember { mutableStateOf(initialIntensity) }
    var selectedContext by remember { mutableStateOf(initialContext) }
    var noteText by remember { mutableStateOf(initialNote ?: "") }

    // État local synchronisé avec availableContexts pour permettre un retour visuel instantané
    val localContexts = remember { mutableStateListOf<String>().apply { addAll(availableContexts) } }
    LaunchedEffect(availableContexts) {
        localContexts.clear()
        localContexts.addAll(availableContexts)
    }

    var showAddContextInput by remember { mutableStateOf(false) }
    var newContextText by remember { mutableStateOf("") }
    var isManagingContexts by remember { mutableStateOf(false) }
    var editingContextOriginal by remember { mutableStateOf<String?>(null) }
    var editingContextText by remember { mutableStateOf("") }

    // Inclure le contexte initial s'il provenait d'une ancienne saisie supprimée de la liste active
    val displayedContexts = remember(localContexts.toList(), selectedContext) {
        val base = localContexts.toMutableList()
        val currentSelected = selectedContext?.trim()
        if (!currentSelected.isNullOrEmpty() && base.none { it.equals(currentSelected, ignoreCase = true) }) {
            base.add(currentSelected)
        }
        base
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (allowDateChange) {
                    Text(
                        text = "Jour de la saisie",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { selectedEpochDay -= 1L },
                                modifier = Modifier.testTag("dialog_prev_day_button")
                            ) {
                                Text("-1 j")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = "Date sélectionnée",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateAndCurrencyUtils.formatShortDate(selectedEpochDay),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (selectedEpochDay < todayEpochDay) {
                                        selectedEpochDay += 1L
                                    }
                                },
                                enabled = selectedEpochDay < todayEpochDay,
                                modifier = Modifier.testTag("dialog_next_day_button")
                            ) {
                                Text("+1 j")
                            }
                        }
                    }
                }

                // Heure automatique ou modifiable
                Text(
                    text = "Heure (automatique, modifiable)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = "Heure",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                hourText = value
                            }
                        },
                        label = { Text("Heure (0-23)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_hour_input")
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                minuteText = value
                            }
                        },
                        label = { Text("Min (0-59)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_minute_input")
                    )
                }

                // Intensité de l'envie de 1 à 5 (facultative)
                Text(
                    text = "Intensité de l'envie (1 à 5, facultatif)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (level in 1..5) {
                        val isSelected = selectedIntensity == level
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedIntensity = if (isSelected) null else level
                            },
                            label = { Text("$level") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_intensity_chip_$level")
                        )
                    }
                }

                // Contexte personnalisable (facultatif)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Contexte (facultatif)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            isManagingContexts = !isManagingContexts
                            editingContextOriginal = null
                        },
                        modifier = Modifier.testTag("dialog_toggle_manage_contexts_button")
                    ) {
                        Text(
                            text = if (isManagingContexts) "Terminé" else "Gérer ma liste",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isManagingContexts) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        displayedContexts.forEachIndexed { index, contextLabel ->
                            val isSelected = selectedContext == contextLabel
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedContext = if (isSelected) null else contextLabel
                                },
                                label = { Text(contextLabel, style = MaterialTheme.typography.bodyMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("dialog_context_chip_$index")
                            )
                        }

                        AssistChip(
                            onClick = { showAddContextInput = !showAddContextInput },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text("Ajouter") },
                            modifier = Modifier.testTag("dialog_open_add_context_chip")
                        )
                    }

                    if (showAddContextInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newContextText,
                                onValueChange = { if (it.length <= 50) newContextText = it },
                                label = { Text("Nouveau contexte") },
                                placeholder = { Text("Ex. En voiture, Après le sport...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialog_new_context_input")
                            )
                            Button(
                                onClick = {
                                    val trimmed = newContextText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        val existing = localContexts.firstOrNull { it.equals(trimmed, ignoreCase = true) }
                                        if (existing != null) {
                                            selectedContext = existing
                                        } else {
                                            localContexts.add(trimmed)
                                            onAddCustomContext(trimmed)
                                            selectedContext = trimmed
                                        }
                                        newContextText = ""
                                        showAddContextInput = false
                                    }
                                },
                                enabled = newContextText.trim().isNotEmpty(),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_confirm_add_context_button")
                            ) {
                                Text("Ajouter")
                            }
                        }
                    }
                } else {
                    // Mode gestion complète de la liste des contextes directement dans la fenêtre de saisie
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_manage_contexts_panel")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Personnalisez vos contextes (ajouter, renommer ou supprimer) :",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (localContexts.isEmpty()) {
                                Text(
                                    text = "Aucun contexte dans votre liste.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            localContexts.forEachIndexed { idx, item ->
                                if (editingContextOriginal == item) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editingContextText,
                                            onValueChange = { if (it.length <= 50) editingContextText = it },
                                            label = { Text("Modifier le contexte") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("dialog_rename_context_input")
                                        )
                                        IconButton(
                                            onClick = {
                                                val trimmed = editingContextText.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    val targetIndex = localContexts.indexOf(item)
                                                    if (targetIndex != -1) {
                                                        localContexts[targetIndex] = trimmed
                                                    }
                                                    if (selectedContext == item) {
                                                        selectedContext = trimmed
                                                    }
                                                    onRenameCustomContext(item, trimmed)
                                                    editingContextOriginal = null
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("dialog_confirm_rename_context_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Valider la modification",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { editingContextOriginal = null },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Annuler la modification"
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        editingContextOriginal = item
                                                        editingContextText = item
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("dialog_edit_context_$idx")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = "Renommer $item",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        localContexts.remove(item)
                                                        if (selectedContext == item) {
                                                            selectedContext = null
                                                        }
                                                        onDeleteCustomContext(item)
                                                    },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("dialog_delete_context_$idx")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "Supprimer $item",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Champ d'ajout rapide dans le panneau de gestion
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newContextText,
                                    onValueChange = { if (it.length <= 50) newContextText = it },
                                    label = { Text("Ajouter un contexte") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialog_manage_new_context_input")
                                )
                                Button(
                                    onClick = {
                                        val trimmed = newContextText.trim()
                                        if (trimmed.isNotEmpty()) {
                                            if (localContexts.none { it.equals(trimmed, ignoreCase = true) }) {
                                                localContexts.add(trimmed)
                                                onAddCustomContext(trimmed)
                                            }
                                            selectedContext = trimmed
                                            newContextText = ""
                                        }
                                    },
                                    enabled = newContextText.trim().isNotEmpty(),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("dialog_manage_add_context_button")
                                ) {
                                    Text("Ajouter")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    localContexts.clear()
                                    localContexts.addAll(PresetContexts.ALL)
                                    onResetCustomContexts()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("dialog_reset_contexts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Réinitialiser les 5 contextes par défaut")
                            }
                        }
                    }
                }

                // Note libre facultative
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note personnelle (facultatif)") },
                    placeholder = { Text("Ex. Discussion avec un collègue, ressenti...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour = (hourText.toIntOrNull() ?: initialHour).coerceIn(0, 23)
                    val minute = (minuteText.toIntOrNull() ?: initialMinute).coerceIn(0, 59)
                    val timestamp = DateAndCurrencyUtils.buildLocalTimestamp(
                        localEpochDay = selectedEpochDay,
                        hourOfDay = hour,
                        minute = minute
                    )
                    onConfirm(
                        timestamp,
                        selectedIntensity,
                        selectedContext,
                        noteText.trim().takeIf { it.isNotEmpty() }
                    )
                },
                modifier = Modifier.testTag("dialog_save_cigarette_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Fermer")
            }
        }
    )
}
