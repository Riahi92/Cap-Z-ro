package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.util.PlanStepPreview
import java.util.TimeZone

enum class PlanReferenceSource {
    MANUAL,
    OBSERVED_CONFIRMED_DAYS
}

/**
 * Parcours unique de création / mise à jour du plan de réduction :
 * 1. Choisir une consommation de référence saisie manuellement ou calculée sur les journées complètes d'observation.
 * 2. Choisir la date d'arrêt (sans date par défaut non validée).
 * 3. Prévisualiser les plafonds quotidiens (et les ajuster si souhaité).
 * 4. Valider explicitement.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedPlanFlowCard(
    todayEpochDay: Long,
    initialManualReference: Int?,
    confirmedObservationDaysCount: Int,
    computedObservationReference: Int?, // null si 0 journée complète confirmée
    onValidatePlan: (
        referenceCigarettes: Int,
        startEpochDay: Long,
        quitDateEpochDay: Long,
        validatedSteps: List<PlanStepPreview>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var referenceSource by remember {
        mutableStateOf(
            if (initialManualReference == null && computedObservationReference != null && confirmedObservationDaysCount > 0) {
                PlanReferenceSource.OBSERVED_CONFIRMED_DAYS
            } else {
                PlanReferenceSource.MANUAL
            }
        )
    }
    var manualReferenceInput by remember(initialManualReference) {
        mutableStateOf(initialManualReference?.toString() ?: "")
    }
    // Ne jamais imposer de date d'arrêt par défaut sans choix explicite de l'utilisateur
    var selectedQuitEpochDay by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var previewSteps by remember { mutableStateOf<List<PlanStepPreview>>(emptyList()) }
    var isPreviewVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val effectiveReference: Int? = when (referenceSource) {
        PlanReferenceSource.MANUAL -> manualReferenceInput.trim().toIntOrNull()?.takeIf { it > 0 }
        PlanReferenceSource.OBSERVED_CONFIRMED_DAYS -> {
            if (confirmedObservationDaysCount > 0) computedObservationReference?.takeIf { it > 0 } else null
        }
    }

    CapZeroWhiteCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Configurer mon plan vers zéro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ÉTAPE 1 : Consommation de référence
            Text(
                text = "1. Consommation quotidienne de référence",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = referenceSource == PlanReferenceSource.MANUAL,
                    onClick = {
                        referenceSource = PlanReferenceSource.MANUAL
                        isPreviewVisible = false
                        errorMessage = null
                    },
                    label = { Text("Saisie manuelle") },
                    modifier = Modifier.testTag("plan_flow_source_manual_chip")
                )

                FilterChip(
                    selected = referenceSource == PlanReferenceSource.OBSERVED_CONFIRMED_DAYS,
                    onClick = {
                        referenceSource = PlanReferenceSource.OBSERVED_CONFIRMED_DAYS
                        isPreviewVisible = false
                        errorMessage = null
                    },
                    label = { Text("Calculée sur mes journées confirmées") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("plan_flow_source_observed_chip")
                )
            }

            if (referenceSource == PlanReferenceSource.MANUAL) {
                OutlinedTextField(
                    value = manualReferenceInput,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 3) {
                            manualReferenceInput = value
                            isPreviewVisible = false
                            errorMessage = null
                        }
                    },
                    label = { Text("Cigarettes par jour (référence)") },
                    placeholder = { Text("Ex. 15") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_flow_manual_ref_input")
                )
            } else {
                Surface(
                    color = if (confirmedObservationDaysCount > 0 && computedObservationReference != null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_flow_observed_ref_box")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (confirmedObservationDaysCount == 0 || computedObservationReference == null) {
                            Text(
                                text = "Aucune journée complète confirmée (0 jour)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Confirmez au moins une journée complète dans Accueil ou Journal pour calculer une référence observée, ou choisissez la saisie manuelle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Référence calculée : $computedObservationReference cig./jour",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Basée sur $confirmedObservationDaysCount journée(s) complète(s) confirmée(s).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            // ÉTAPE 2 : Choisir la date d'arrêt (sans date par défaut non choisie)
            Text(
                text = "2. Choisir la date d'arrêt",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(7 to "Dans 7 j", 14 to "Dans 14 j", 21 to "Dans 21 j", 30 to "Dans 30 j").forEach { (daysOffset, label) ->
                    val targetDay = todayEpochDay + daysOffset
                    FilterChip(
                        selected = selectedQuitEpochDay == targetDay,
                        onClick = {
                            selectedQuitEpochDay = targetDay
                            isPreviewVisible = false
                            errorMessage = null
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("plan_flow_quit_preset_$daysOffset")
                    )
                }
            }

            OutlinedButton(
                onClick = { showDatePickerDialog = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("plan_flow_open_datepicker_button")
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedQuitEpochDay != null) {
                        "Date choisie : ${DateAndCurrencyUtils.formatShortDate(selectedQuitEpochDay!!)}"
                    } else {
                        "Sélectionner une date dans le calendrier"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ÉTAPE 3 : Bouton de prévisualisation des plafonds quotidiens
            DeepGreenOutlinedButton(
                text = "3. Prévisualiser les plafonds quotidiens",
                onClick = {
                    val refVal = effectiveReference
                    val quitDay = selectedQuitEpochDay
                    if (refVal == null || refVal <= 0) {
                        errorMessage = if (referenceSource == PlanReferenceSource.OBSERVED_CONFIRMED_DAYS) {
                            "Aucune journée complète confirmée. Confirmez au moins une journée complète ou saisissez une référence manuelle."
                        } else {
                            "Veuillez saisir une consommation quotidienne de référence supérieure à 0."
                        }
                        return@DeepGreenOutlinedButton
                    }
                    if (quitDay == null || quitDay <= todayEpochDay) {
                        errorMessage = "Veuillez choisir une date d'arrêt (à partir de demain)."
                        return@DeepGreenOutlinedButton
                    }
                    previewSteps = DateAndCurrencyUtils.generateLinearReductionPlan(
                        startEpochDay = todayEpochDay,
                        quitEpochDay = quitDay,
                        referenceDailyCigarettes = refVal
                    )
                    errorMessage = null
                    isPreviewVisible = true
                },
                modifier = Modifier.testTag("plan_flow_preview_button")
            )

            // Affichage de la prévisualisation et ÉTAPE 4 : Validation explicite
            if (isPreviewVisible && previewSteps.isNotEmpty() && selectedQuitEpochDay != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                Text(
                    text = "Aperçu du calendrier (${previewSteps.size} jours jusqu'à 0 cigarette le ${DateAndCurrencyUtils.formatShortDate(selectedQuitEpochDay!!)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    previewSteps.forEachIndexed { idx, step ->
                        val isQuitDay = idx == previewSteps.lastIndex
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = DateAndCurrencyUtils.formatFriendlyDay(step.localEpochDay, todayEpochDay),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isQuitDay) "Jour d'arrêt (Cap Zéro)" else "Jour ${idx + 1}/${previewSteps.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isQuitDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (!isQuitDay) {
                                        IconButton(
                                            onClick = {
                                                previewSteps = DateAndCurrencyUtils.adjustStepKeepingNonIncreasing(
                                                    currentSteps = previewSteps,
                                                    targetEpochDay = step.localEpochDay,
                                                    newCap = (step.capCount - 1).coerceAtLeast(0)
                                                )
                                            },
                                            enabled = step.capCount > 0,
                                            modifier = Modifier.testTag("plan_flow_step_minus_$idx")
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Diminuer le plafond")
                                        }
                                    }

                                    SoftMintPillBadge(text = "≤ ${step.capCount}")

                                    if (!isQuitDay) {
                                        IconButton(
                                            onClick = {
                                                previewSteps = DateAndCurrencyUtils.adjustStepKeepingNonIncreasing(
                                                    currentSteps = previewSteps,
                                                    targetEpochDay = step.localEpochDay,
                                                    newCap = step.capCount + 1
                                                )
                                            },
                                            modifier = Modifier.testTag("plan_flow_step_plus_$idx")
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Augmenter le plafond")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                EmeraldPrimaryButton(
                    text = "4. Valider ce plan de réduction",
                    onClick = {
                        val refVal = effectiveReference ?: previewSteps.firstOrNull()?.capCount ?: 1
                        val quitDay = selectedQuitEpochDay ?: return@EmeraldPrimaryButton
                        onValidatePlan(refVal, todayEpochDay, quitDay, previewSteps)
                        isPreviewVisible = false
                    },
                    leadingIcon = Icons.Filled.CheckCircle,
                    modifier = Modifier.testTag("plan_flow_validate_button")
                )
            }
        }
    }

    if (showDatePickerDialog) {
        val initialMillis = (selectedQuitEpochDay ?: (todayEpochDay + 14L)) * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis
                        if (pickedMillis != null) {
                            val pickedEpochDay = DateAndCurrencyUtils.toLocalEpochDay(
                                pickedMillis,
                                TimeZone.getTimeZone("UTC")
                            )
                            selectedQuitEpochDay = pickedEpochDay.coerceAtLeast(todayEpochDay + 1L)
                            isPreviewVisible = false
                            errorMessage = null
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Choisir cette date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
