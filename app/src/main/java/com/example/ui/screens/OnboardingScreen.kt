package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.OnboardingMode
import com.example.ui.components.CapZeroBrandLeafIcon
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.ConciseHelpBanner
import com.example.ui.components.DeepGreenOutlinedButton
import com.example.ui.components.EmeraldPrimaryButton
import com.example.ui.components.SoftMintPillBadge
import com.example.util.DateAndCurrencyUtils
import com.example.util.PlanStepPreview
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    todayEpochDay: Long,
    onCompleteDirectPlan: (
        referenceCigarettes: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        quitDateEpochDay: Long,
        validatedSteps: List<PlanStepPreview>
    ) -> Unit,
    onCompleteObservationMode: (
        observationTargetDays: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        quitDateEpochDay: Long
    ) -> Unit
) {
    // Aucune valeur personnelle ni date d'arrêt par défaut n'est imposée sans validation
    var selectedMode by remember { mutableStateOf(OnboardingMode.DIRECT) }
    var referenceInput by remember { mutableStateOf("") }
    var observationDays by remember { mutableIntStateOf(3) }
    var packPriceTndInput by remember { mutableStateOf("") }
    var cigarettesPerPackInput by remember { mutableStateOf("") }
    var selectedQuitEpochDay by remember { mutableStateOf<Long?>(null) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var isPreviewStep by remember { mutableStateOf(false) }
    var previewSteps by remember { mutableStateOf<List<PlanStepPreview>>(emptyList()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CapZeroBrandLeafIcon(modifier = Modifier.size(28.dp))
                Text(
                    text = "Cap Zéro",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // En-tête vert profond #123D32
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF123D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Bienvenue sur Cap Zéro",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Suivi bienveillant et réduction progressive à votre rythme.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }

            ConciseHelpBanner(
                shortSummary = "Accompagnement personnel (ne remplace pas un médecin).",
                helpTitle = "À propos de Cap Zéro",
                helpDetails = stringResource(id = R.string.health_disclaimer) + "\n\n" +
                    stringResource(id = R.string.plan_disclaimer),
                testTagPrefix = "onboarding_health_help"
            )

            if (!isPreviewStep) {
                // ÉTAPE 1 : Choix du départ
                CapZeroWhiteCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "1. Consommation de départ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedMode == OnboardingMode.DIRECT,
                                onClick = {
                                    selectedMode = OnboardingMode.DIRECT
                                    validationError = null
                                },
                                label = { Text("Saisie manuelle") },
                                leadingIcon = if (selectedMode == OnboardingMode.DIRECT) {
                                    { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.testTag("onboarding_mode_direct_chip")
                            )
                            FilterChip(
                                selected = selectedMode == OnboardingMode.OBSERVATION,
                                onClick = {
                                    selectedMode = OnboardingMode.OBSERVATION
                                    validationError = null
                                },
                                label = { Text("Période d'observation") },
                                leadingIcon = if (selectedMode == OnboardingMode.OBSERVATION) {
                                    { Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.testTag("onboarding_mode_observation_chip")
                            )
                        }

                        if (selectedMode == OnboardingMode.DIRECT) {
                            OutlinedTextField(
                                value = referenceInput,
                                onValueChange = { value ->
                                    if (value.all { it.isDigit() } && value.length <= 3) {
                                        referenceInput = value
                                        validationError = null
                                    }
                                },
                                label = { Text("Cigarettes par jour habituellement") },
                                placeholder = { Text("Ex. 15") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_reference_input")
                            )
                        } else {
                            Text(
                                text = "Journées complètes à confirmer (1 à 14 j) :",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (1..14).forEach { days ->
                                    FilterChip(
                                        selected = observationDays == days,
                                        onClick = { observationDays = days },
                                        label = { Text("$days j") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.testTag("onboarding_obs_days_$days")
                                    )
                                }
                            }
                            Text(
                                text = "Votre référence sera calculée uniquement après confirmation de vos journées complètes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ÉTAPE 2 : Informations sur le paquet (en TND)
                CapZeroWhiteCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "2. Votre paquet (en TND)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = packPriceTndInput,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() || it == '.' || it == ',' } && value.length <= 10) {
                                    packPriceTndInput = value
                                    validationError = null
                                }
                            },
                            label = { Text("Prix du paquet (TND)") },
                            placeholder = { Text("Ex. 10,500") },
                            suffix = { Text("TND") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_pack_price_input")
                        )

                        OutlinedTextField(
                            value = cigarettesPerPackInput,
                            onValueChange = { value ->
                                if (value.all { it.isDigit() } && value.length <= 3) {
                                    cigarettesPerPackInput = value
                                    validationError = null
                                }
                            },
                            label = { Text("Cigarettes par paquet") },
                            placeholder = { Text("Ex. 20") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_cigs_per_pack_input")
                        )
                    }
                }

                // ÉTAPE 3 : Date d'arrêt souhaitée (sans date par défaut non validée)
                CapZeroWhiteCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "3. Date d'arrêt souhaitée",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sélectionnez la date visée pour atteindre 0 cigarette :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(7 to "Dans 7 j", 14 to "Dans 14 j", 21 to "Dans 21 j", 30 to "Dans 30 j", 60 to "Dans 60 j").forEach { (daysOffset, label) ->
                                val targetDay = todayEpochDay + daysOffset
                                val isSelected = selectedQuitEpochDay == targetDay
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedQuitEpochDay = targetDay
                                        validationError = null
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("onboarding_quit_preset_$daysOffset")
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showDatePickerDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("onboarding_open_datepicker_button")
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedQuitEpochDay != null) {
                                    "Date choisie : ${DateAndCurrencyUtils.formatShortDate(selectedQuitEpochDay!!)}"
                                } else {
                                    "Choisir une date au calendrier"
                                },
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val priceMillimes = DateAndCurrencyUtils.parseTndToMillimes(packPriceTndInput)
                        val cigsPerPack = cigarettesPerPackInput.trim().toIntOrNull()
                        val quitDay = selectedQuitEpochDay

                        if (priceMillimes == null || priceMillimes <= 0L) {
                            validationError = "Veuillez indiquer un prix de paquet valide en TND (ex: 10,500)."
                            return@Button
                        }
                        if (cigsPerPack == null || cigsPerPack <= 0) {
                            validationError = "Veuillez indiquer le nombre de cigarettes par paquet."
                            return@Button
                        }
                        if (quitDay == null || quitDay <= todayEpochDay) {
                            validationError = "Veuillez choisir explicitement votre date d'arrêt."
                            return@Button
                        }

                        if (selectedMode == OnboardingMode.DIRECT) {
                            val refCount = referenceInput.trim().toIntOrNull()
                            if (refCount == null || refCount <= 0) {
                                validationError = "Veuillez saisir votre consommation quotidienne habituelle."
                                return@Button
                            }
                            previewSteps = DateAndCurrencyUtils.generateLinearReductionPlan(
                                startEpochDay = todayEpochDay,
                                quitEpochDay = quitDay,
                                referenceDailyCigarettes = refCount
                            )
                            validationError = null
                            isPreviewStep = true
                        } else {
                            onCompleteObservationMode(
                                observationDays,
                                priceMillimes,
                                cigsPerPack,
                                quitDay
                            )
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("onboarding_continue_button")
                ) {
                    Text(
                        text = if (selectedMode == OnboardingMode.DIRECT) {
                            "Prévisualiser mes plafonds"
                        } else {
                            "Démarrer l'observation"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                // ÉTAPE D'APERÇU ET VALIDATION EXPLICITE
                val quitDay = selectedQuitEpochDay ?: (todayEpochDay + 14L)
                CapZeroWhiteCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Prévisualisation des plafonds",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${previewSteps.size} jours jusqu'à 0 cigarette le ${DateAndCurrencyUtils.formatShortDate(quitDay)}. Ajustez une étape si besoin :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

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
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = DateAndCurrencyUtils.formatFriendlyDay(step.localEpochDay, todayEpochDay),
                                                style = MaterialTheme.typography.titleMedium,
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
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (!isQuitDay) {
                                                IconButton(
                                                    onClick = {
                                                        val newCap = (step.capCount - 1).coerceAtLeast(0)
                                                        previewSteps = DateAndCurrencyUtils.adjustStepKeepingNonIncreasing(
                                                            currentSteps = previewSteps,
                                                            targetEpochDay = step.localEpochDay,
                                                            newCap = newCap
                                                        )
                                                    },
                                                    enabled = step.capCount > 0,
                                                    modifier = Modifier.testTag("preview_step_minus_$idx")
                                                ) {
                                                    Icon(Icons.Filled.Remove, contentDescription = "Diminuer le plafond")
                                                }
                                            }

                                            SoftMintPillBadge(text = "≤ ${step.capCount}")

                                            if (!isQuitDay) {
                                                IconButton(
                                                    onClick = {
                                                        val newCap = step.capCount + 1
                                                        previewSteps = DateAndCurrencyUtils.adjustStepKeepingNonIncreasing(
                                                            currentSteps = previewSteps,
                                                            targetEpochDay = step.localEpochDay,
                                                            newCap = newCap
                                                        )
                                                    },
                                                    modifier = Modifier.testTag("preview_step_plus_$idx")
                                                ) {
                                                    Icon(Icons.Filled.Add, contentDescription = "Augmenter le plafond")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmeraldPrimaryButton(
                        text = "Valider mon plan",
                        onClick = {
                            val refCount = referenceInput.trim().toIntOrNull() ?: previewSteps.firstOrNull()?.capCount ?: 1
                            val priceMillimes = DateAndCurrencyUtils.parseTndToMillimes(packPriceTndInput) ?: 0L
                            val cigsPerPack = cigarettesPerPackInput.trim().toIntOrNull() ?: 20
                            onCompleteDirectPlan(
                                refCount,
                                priceMillimes,
                                cigsPerPack,
                                quitDay,
                                previewSteps
                            )
                        },
                        leadingIcon = Icons.Filled.CheckCircle,
                        modifier = Modifier.testTag("onboarding_validate_plan_button")
                    )

                    DeepGreenOutlinedButton(
                        text = "Retour aux paramètres",
                        onClick = { isPreviewStep = false },
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        modifier = Modifier.testTag("onboarding_back_to_form_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
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
                            validationError = null
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Confirmer")
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
