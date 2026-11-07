package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.OnboardingMode
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.CigaretteEntryDialog
import com.example.ui.components.ConciseHelpBanner
import com.example.ui.components.DecorativeSproutIllustration
import com.example.ui.components.DeepGreenOutlinedButton
import com.example.ui.components.EmeraldPrimaryButton
import com.example.ui.components.InventorySummaryCard
import com.example.ui.components.PackPurchaseDialog
import com.example.ui.components.SoftMintPillBadge
import com.example.ui.components.WindBreathVectorIcon
import com.example.ui.theme.DeepGreenPrimaryLight
import com.example.ui.viewmodel.CapZeroUiState
import com.example.util.DateAndCurrencyUtils

@Composable
fun TodayScreen(
    uiState: CapZeroUiState,
    onQuickSmokeClick: () -> Unit,
    onUndoLastAction: () -> Unit,
    onUpdateCigaretteDetails: (id: Long, timestampMillis: Long, intensity: Int?, context: String?, note: String?) -> Unit,
    onDismissQuickContextPrompt: () -> Unit,
    onToggleConfirmToday: (Boolean) -> Unit,
    onNavigateToCraving: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onAddPackPurchase: (packsCount: Int, cigarettesPerPack: Int, unitPriceMillimes: Long, note: String?) -> Unit = { _, _, _, _ -> },
    onAdjustInventoryStock: (newStock: Int) -> Unit = {},
    onAddCustomContext: (String) -> Unit = {},
    onRenameCustomContext: (oldContext: String, newContext: String) -> Unit = { _, _ -> },
    onDeleteCustomContext: (String) -> Unit = {},
    onResetCustomContexts: () -> Unit = {}
) {
    var showDetailDialogForQuickLog by remember { mutableStateOf(false) }
    var showPackPurchaseDialog by remember { mutableStateOf(false) }
    var showAdjustStockDialog by remember { mutableStateOf(false) }

    val todayCount = uiState.todayCigarettesCount
    val todayCap = uiState.todayCap
    val isExceeded = todayCap != null && todayCount > todayCap
    val isObservationMode = uiState.preferences.onboardingMode == OnboardingMode.OBSERVATION && uiState.planCaps.isEmpty()

    val savingsText = if (uiState.progressMetrics.estimatedSavingsTnd != null) {
        DateAndCurrencyUtils.formatTnd(uiState.progressMetrics.estimatedSavingsTnd)
    } else {
        "—"
    }

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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. En-tête d'accueil avec badge de date et illustration végétale
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                DecorativeSproutIllustration(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(110.dp, 86.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 76.dp)
                ) {
                    Text(
                        text = "Bienvenue",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Chaque pas compte.",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    SoftMintPillBadge(
                        text = DateAndCurrencyUtils.formatDayAndMonthFrench(uiState.todayEpochDay),
                        icon = Icons.Outlined.CalendarToday
                    )
                }
            }

            // 2. Carte Principale Vert Profond (#123D32, angles 20.dp) : Compteur & Plafond du jour
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepGreenPrimaryLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Chiffre dominant
                        Text(
                            text = "$todayCount",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 58.sp,
                                lineHeight = 62.sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("today_real_counter_text")
                        )

                        // Séparateur vertical fin
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(64.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        // Intitulé et plafond du jour
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "cigarettes\naujourd'hui",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    todayCap != null -> "Plafond du jour : $todayCap"
                                    isObservationMode -> "Observation (${uiState.confirmedObservationDaysCount}/${uiState.preferences.observationTargetDays} j confirmés)"
                                    uiState.isPostQuitDate -> "Plafond du jour : 0"
                                    else -> "Aucun plafond actif"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA7E8D2),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("today_cap_text")
                            )
                            Text(
                                text = if (uiState.isTodayConfirmedComplete) {
                                    "Journée complète confirmée"
                                } else {
                                    "Journée en cours · provisoire"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.testTag("today_day_status_badge")
                            )
                        }
                    }

                    // Barre de progression arrondie en bas de la carte
                    val progressFraction = when {
                        todayCap != null && todayCap > 0 -> (todayCount.toFloat() / todayCap.toFloat()).coerceIn(0f, 1f)
                        todayCap == 0 -> if (todayCount == 0) 0f else 1f
                        else -> (todayCount.toFloat() / 20f).coerceIn(0f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceAtLeast(if (todayCount > 0) 0.08f else 0f))
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF43B894))
                        )
                    }
                }
            }

            // Message bienveillant si le plafond est dépassé
            if (isExceeded) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_exceeded_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Plafond dépassé aujourd'hui : vos progrès restent acquis et votre plan ne change pas automatiquement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Deux petites cartes blanches pour la dernière cigarette et les économies (adaptatives sur petit écran)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompactWidth = maxWidth < 330.dp
                if (isCompactWidth) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LastCigaretteSmallCard(
                            elapsedText = uiState.elapsedSinceLastText,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SavingsSmallCard(
                            savingsText = savingsText,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LastCigaretteSmallCard(
                            elapsedText = uiState.elapsedSinceLastText,
                            modifier = Modifier.weight(1f)
                        )
                        SavingsSmallCard(
                            savingsText = savingsText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Boutons principaux « + J'ai fumé » (#239878) et « Une envie ? » (contour #123D32)
            EmeraldPrimaryButton(
                text = stringResource(id = R.string.btn_i_smoked),
                onClick = onQuickSmokeClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.testTag("today_i_smoked_button")
            )

            // Retour immédiat après clic sur « J'ai fumé » : Annuler ou Préciser contexte/note
            val quickLogged = uiState.lastQuickLoggedCigarette
            if (quickLogged != null) {
                CapZeroWhiteCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.testTag("today_quick_log_feedback_card")
                ) {
                    Text(
                        text = "Saisie enregistrée à ${DateAndCurrencyUtils.formatTime(quickLogged.timestampMillis)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showDetailDialogForQuickLog = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("today_enrich_quick_log_button")
                        ) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Préciser l'envie ou le contexte")
                        }
                        OutlinedButton(
                            onClick = onUndoLastAction,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("today_undo_quick_log_button")
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Annuler cette saisie")
                        }
                    }
                }
            }

            DeepGreenOutlinedButton(
                text = "Une envie ?",
                onClick = onNavigateToCraving,
                leadingIcon = {
                    WindBreathVectorIcon(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.testTag("today_craving_help_button")
            )

            // 5. Carte « Inventaire de cigarettes & Paquets achetés » (déduction automatique à chaque cigarette)
            InventorySummaryCard(
                inventorySummary = uiState.inventorySummary,
                onBuyPackClick = { showPackPurchaseDialog = true },
                onAdjustStockClick = { showAdjustStockDialog = true },
                testTagPrefix = "today_inventory"
            )

            // 6. Carte « Le conseil du jour » (fond menthe doux #E3F4EC, bordure #CFE9DC, angles 20.dp)
            Surface(
                color = Color(0xFFE3F4EC),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFE9DC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Eco,
                        contentDescription = null,
                        tint = Color(0xFF1E8768),
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Le conseil du jour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF152C27)
                        )
                        Text(
                            text = "Une envie dure souvent quelques minutes. Respirez, elle passe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF25433B)
                        )
                    }
                }
            }

            // 7. Carte de confirmation de la journée complète
            CapZeroWhiteCard(
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isTodayConfirmedComplete) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.CheckCircleOutline
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isTodayConfirmedComplete) {
                                "Journée complète confirmée ($todayCount cig.)"
                            } else {
                                "Journée en cours : compteur provisoire"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Confirmez la journée complète en fin de soirée pour mettre à jour vos moyennes et économies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onToggleConfirmToday(!uiState.isTodayConfirmedComplete) },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("today_confirm_day_button")
                ) {
                    Text(
                        text = if (uiState.isTodayConfirmedComplete) {
                            "Retirer la confirmation du jour"
                        } else {
                            "Confirmer cette journée complète"
                        }
                    )
                }

                if (isObservationMode && uiState.confirmedObservationDaysCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToPlan,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("today_generate_plan_from_obs_button")
                    ) {
                        Text("Créer mon plan dans l'onglet Plan")
                    }
                }
            }

            ConciseHelpBanner(
                shortSummary = stringResource(id = R.string.plan_disclaimer_short),
                helpTitle = "Suivi bienveillant & Santé",
                helpDetails = stringResource(id = R.string.plan_disclaimer) + "\n\n" +
                    stringResource(id = R.string.health_disclaimer),
                testTagPrefix = "today_help"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    val quickCig = uiState.lastQuickLoggedCigarette
    if (showDetailDialogForQuickLog && quickCig != null) {
        val (hour, minute) = DateAndCurrencyUtils.extractHourAndMinute(quickCig.timestampMillis)
        CigaretteEntryDialog(
            title = "Préciser cette cigarette",
            initialEpochDay = quickCig.localEpochDay,
            initialHour = hour,
            initialMinute = minute,
            initialIntensity = quickCig.cravingIntensity,
            initialContext = quickCig.contextCategory,
            initialNote = quickCig.note,
            allowDateChange = false,
            todayEpochDay = uiState.todayEpochDay,
            availableContexts = uiState.preferences.customContexts,
            onAddCustomContext = onAddCustomContext,
            onRenameCustomContext = onRenameCustomContext,
            onDeleteCustomContext = onDeleteCustomContext,
            onResetCustomContexts = onResetCustomContexts,
            onDismiss = {
                showDetailDialogForQuickLog = false
                onDismissQuickContextPrompt()
            },
            onConfirm = { ts, intensity, context, note ->
                onUpdateCigaretteDetails(quickCig.id, ts, intensity, context, note)
                showDetailDialogForQuickLog = false
            }
        )
    }

    if (showPackPurchaseDialog) {
        PackPurchaseDialog(
            defaultPackPriceMillimes = uiState.preferences.packPriceMillimes,
            defaultCigarettesPerPack = uiState.preferences.cigarettesPerPack,
            onDismiss = { showPackPurchaseDialog = false },
            onConfirm = { packsCount, cigsPerPack, unitPriceMillimes, note ->
                onAddPackPurchase(packsCount, cigsPerPack, unitPriceMillimes, note)
                showPackPurchaseDialog = false
            }
        )
    }

    if (showAdjustStockDialog) {
        com.example.ui.components.AdjustInventoryDialog(
            currentStock = uiState.inventorySummary.remainingCigarettes,
            cigarettesPerPack = uiState.preferences.cigarettesPerPack ?: 20,
            onDismiss = { showAdjustStockDialog = false },
            onConfirm = { newStock ->
                onAdjustInventoryStock(newStock)
                showAdjustStockDialog = false
            }
        )
    }
}

@Composable
private fun LastCigaretteSmallCard(
    elapsedText: String,
    modifier: Modifier = Modifier
) {
    CapZeroWhiteCard(
        contentPadding = PaddingValues(14.dp),
        modifier = modifier.testTag("today_elapsed_time_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = "Dernière cigarette",
                tint = Color(0xFF1A5A47),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Dernière\ncigarette",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF152C27),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = elapsedText,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF145A43),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("today_elapsed_time_value")
        )
    }
}

@Composable
private fun SavingsSmallCard(
    savingsText: String,
    modifier: Modifier = Modifier
) {
    CapZeroWhiteCard(
        contentPadding = PaddingValues(14.dp),
        modifier = modifier.testTag("today_savings_small_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Savings,
                contentDescription = "Économies estimées",
                tint = Color(0xFF1A5A47),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Économies\nestimées",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF152C27),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = savingsText,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF17795B),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("today_savings_value_text")
        )
    }
}
