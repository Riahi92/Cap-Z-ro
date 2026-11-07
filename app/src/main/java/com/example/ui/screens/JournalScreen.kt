package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CigaretteLogEntity
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.CigaretteEntryDialog
import com.example.ui.components.ConciseHelpBanner
import com.example.ui.components.DeepGreenOutlinedButton
import com.example.ui.components.EmeraldPrimaryButton
import com.example.ui.components.InventorySummaryCard
import com.example.ui.components.PackPurchaseDialog
import com.example.ui.viewmodel.CapZeroUiState
import com.example.ui.viewmodel.DayJournalGroup
import com.example.ui.viewmodel.DayStatusCategory
import com.example.util.DateAndCurrencyUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    uiState: CapZeroUiState,
    onAddRetroactiveCigarette: (timestampMillis: Long, intensity: Int?, context: String?, note: String?) -> Unit,
    onUpdateCigarette: (id: Long, timestampMillis: Long, intensity: Int?, context: String?, note: String?) -> Unit,
    onDeleteCigarette: (id: Long) -> Unit,
    onToggleDayConfirmed: (localEpochDay: Long, confirmed: Boolean) -> Unit,
    onUndoLastAction: () -> Unit,
    onAddPackPurchase: (packsCount: Int, cigarettesPerPack: Int, unitPriceMillimes: Long, note: String?) -> Unit = { _, _, _, _ -> },
    onDeletePackPurchase: (id: Long) -> Unit = {},
    onAdjustInventoryStock: (newStock: Int) -> Unit = {},
    onAddCustomContext: (String) -> Unit = {},
    onRenameCustomContext: (oldContext: String, newContext: String) -> Unit = { _, _ -> },
    onDeleteCustomContext: (String) -> Unit = {},
    onResetCustomContexts: () -> Unit = {}
) {
    var showAddRetroactiveDialog by remember { mutableStateOf(false) }
    var showPackPurchaseDialog by remember { mutableStateOf(false) }
    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var editingCigarette by remember { mutableStateOf<CigaretteLogEntity?>(null) }
    var pastDaySelectorEpochDay by remember(uiState.todayEpochDay) {
        mutableLongStateOf(uiState.todayEpochDay - 1L)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Journal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // 1. Inventaire de cigarettes & Paquets achetés
            item {
                InventorySummaryCard(
                    inventorySummary = uiState.inventorySummary,
                    onBuyPackClick = { showPackPurchaseDialog = true },
                    onAdjustStockClick = { showAdjustStockDialog = true },
                    testTagPrefix = "journal_inventory"
                )
            }

            // Historique des paquets achetés (affiché dès qu'au moins un achat est enregistré)
            if (uiState.packPurchases.isNotEmpty()) {
                item {
                    CapZeroWhiteCard(
                        modifier = Modifier.testTag("journal_pack_purchases_history_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingBag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Historique des paquets achetés (${uiState.packPurchases.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            uiState.packPurchases.forEach { purchase ->
                                val addedCigs = purchase.packsCount * purchase.cigarettesPerPack
                                val totalPriceFormatted = DateAndCurrencyUtils.formatMillimesToTnd(purchase.totalPriceMillimes)
                                val unitPriceFormatted = DateAndCurrencyUtils.formatMillimesToTnd(purchase.unitPriceMillimes)

                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("journal_pack_purchase_item_${purchase.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${purchase.packsCount} paquet(s) · +$addedCigs cigarettes",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Prix d'achat : $totalPriceFormatted ($unitPriceFormatted / paq.)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${DateAndCurrencyUtils.formatShortDate(purchase.localEpochDay)} à ${DateAndCurrencyUtils.formatTime(purchase.timestampMillis)}" +
                                                    (if (!purchase.note.isNullOrBlank()) " · « ${purchase.note} »" else ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeletePackPurchase(purchase.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("journal_delete_pack_purchase_${purchase.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Supprimer cet achat de paquet",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                CapZeroWhiteCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Suivi quotidien & confirmation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        EmeraldPrimaryButton(
                            text = "Ajouter une cigarette",
                            onClick = { showAddRetroactiveDialog = true },
                            leadingIcon = Icons.Filled.Add,
                            modifier = Modifier.testTag("journal_add_retroactive_button")
                        )

                        if (uiState.pendingUndo != null) {
                            DeepGreenOutlinedButton(
                                text = "Annuler la dernière action",
                                onClick = onUndoLastAction,
                                leadingIcon = Icons.Filled.Undo,
                                modifier = Modifier.testTag("journal_undo_button")
                            )
                        }
                    }
                }
            }

            item {
                ConciseHelpBanner(
                    shortSummary = "Distinction entre journée en cours, passée non confirmée et complète confirmée.",
                    helpTitle = "Statut des journées dans le Journal",
                    helpDetails = "1. Journée en cours : compteur provisoire évoluant au fil de la journée.\n\n" +
                        "2. Journée passée non confirmée : données potentiellement incomplètes (exclues des moyennes et économies tant qu'elles ne sont pas confirmées).\n\n" +
                        "3. Journée passée complète confirmée : valide votre total réel du jour (0 ou plusieurs cigarettes) sans jamais remplacer vos cigarettes saisies par zéro.\n\n" +
                        "4. Toute modification ultérieure d'une journée annule sa confirmation et vous invite à la reconfirmer.",
                    testTagPrefix = "journal_help"
                )
            }

            // Sélecteur pour confirmer n'importe quelle journée passée complète (avec 0 ou plusieurs cigarettes)
            item {
                val countOnSelectedPastDay = uiState.cigarettes.count { it.localEpochDay == pastDaySelectorEpochDay }
                val isSelectedPastDayConfirmed = uiState.dailyStatuses.any {
                    it.localEpochDay == pastDaySelectorEpochDay && it.isConfirmedComplete
                }

                CapZeroWhiteCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sélectionner une journée passée",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { pastDaySelectorEpochDay -= 1L },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.testTag("journal_past_day_prev")
                            ) {
                                Text("-1 j", color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = DateAndCurrencyUtils.formatShortDate(pastDaySelectorEpochDay),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$countOnSelectedPastDay cigarette(s) enregistrée(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (pastDaySelectorEpochDay < uiState.todayEpochDay - 1L) {
                                        pastDaySelectorEpochDay += 1L
                                    }
                                },
                                enabled = pastDaySelectorEpochDay < uiState.todayEpochDay - 1L,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.testTag("journal_past_day_next")
                            ) {
                                Text("+1 j")
                            }
                        }

                        Button(
                            onClick = { onToggleDayConfirmed(pastDaySelectorEpochDay, !isSelectedPastDayConfirmed) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelectedPastDayConfirmed) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                contentColor = if (isSelectedPastDayConfirmed) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondary
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("journal_confirm_past_zero_day_button")
                        ) {
                            Icon(
                                imageVector = if (isSelectedPastDayConfirmed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSelectedPastDayConfirmed) {
                                    "Journée complète confirmée ($countOnSelectedPastDay cig.) · Retirer"
                                } else {
                                    "Confirmer cette journée complète"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            items(
                items = uiState.journalGroups,
                key = { it.localEpochDay }
            ) { group ->
                DayJournalCard(
                    group = group,
                    todayEpochDay = uiState.todayEpochDay,
                    onEditEntry = { editingCigarette = it },
                    onDeleteEntry = { onDeleteCigarette(it.id) },
                    onToggleDayConfirmed = { confirmed ->
                        onToggleDayConfirmed(group.localEpochDay, confirmed)
                    }
                )
            }
        }
    }

    if (showAddRetroactiveDialog) {
        val (currentHour, currentMinute) = DateAndCurrencyUtils.extractHourAndMinute(uiState.nowMillis)
        CigaretteEntryDialog(
            title = "Ajouter une cigarette",
            initialEpochDay = uiState.todayEpochDay,
            initialHour = currentHour,
            initialMinute = currentMinute,
            initialIntensity = null,
            initialContext = null,
            initialNote = null,
            allowDateChange = true,
            todayEpochDay = uiState.todayEpochDay,
            availableContexts = uiState.preferences.customContexts,
            onAddCustomContext = onAddCustomContext,
            onRenameCustomContext = onRenameCustomContext,
            onDeleteCustomContext = onDeleteCustomContext,
            onResetCustomContexts = onResetCustomContexts,
            onDismiss = { showAddRetroactiveDialog = false },
            onConfirm = { ts, intensity, context, note ->
                onAddRetroactiveCigarette(ts, intensity, context, note)
                showAddRetroactiveDialog = false
            }
        )
    }

    val cigToEdit = editingCigarette
    if (cigToEdit != null) {
        val (hour, minute) = DateAndCurrencyUtils.extractHourAndMinute(cigToEdit.timestampMillis)
        CigaretteEntryDialog(
            title = "Modifier l'entrée",
            initialEpochDay = cigToEdit.localEpochDay,
            initialHour = hour,
            initialMinute = minute,
            initialIntensity = cigToEdit.cravingIntensity,
            initialContext = cigToEdit.contextCategory,
            initialNote = cigToEdit.note,
            allowDateChange = true,
            todayEpochDay = uiState.todayEpochDay,
            availableContexts = uiState.preferences.customContexts,
            onAddCustomContext = onAddCustomContext,
            onRenameCustomContext = onRenameCustomContext,
            onDeleteCustomContext = onDeleteCustomContext,
            onResetCustomContexts = onResetCustomContexts,
            onDismiss = { editingCigarette = null },
            onConfirm = { ts, intensity, context, note ->
                onUpdateCigarette(cigToEdit.id, ts, intensity, context, note)
                editingCigarette = null
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayJournalCard(
    group: DayJournalGroup,
    todayEpochDay: Long,
    onEditEntry: (CigaretteLogEntity) -> Unit,
    onDeleteEntry: (CigaretteLogEntity) -> Unit,
    onToggleDayConfirmed: (Boolean) -> Unit
) {
    val count = group.entries.size
    val statusBadgeText = when (group.statusCategory) {
        DayStatusCategory.CURRENT_PROVISIONAL -> "Journée en cours : compteur provisoire ($count cig.)"
        DayStatusCategory.PAST_UNCONFIRMED -> "Journée passée non confirmée : données potentiellement incomplètes ($count cig.)"
        DayStatusCategory.PAST_OR_CURRENT_CONFIRMED -> {
            if (count == 0) {
                "Journée complète confirmée : 0 cigarette"
            } else {
                "Journée complète confirmée : $count cigarette(s)"
            }
        }
    }

    CapZeroWhiteCard(
        modifier = Modifier.testTag("journal_day_card_${group.localEpochDay}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateAndCurrencyUtils.formatFriendlyDay(group.localEpochDay, todayEpochDay),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (group.plannedCap != null) {
                        Text(
                            text = "Plafond du jour : ${group.plannedCap}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Badge explicite du statut de la journée
            Surface(
                color = when (group.statusCategory) {
                    DayStatusCategory.PAST_OR_CURRENT_CONFIRMED -> MaterialTheme.colorScheme.secondaryContainer
                    DayStatusCategory.CURRENT_PROVISIONAL -> MaterialTheme.colorScheme.surfaceVariant
                    DayStatusCategory.PAST_UNCONFIRMED -> MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_day_status_${group.localEpochDay}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (group.statusCategory) {
                            DayStatusCategory.PAST_OR_CURRENT_CONFIRMED -> Icons.Filled.CheckCircle
                            DayStatusCategory.CURRENT_PROVISIONAL -> Icons.Outlined.CheckCircleOutline
                            DayStatusCategory.PAST_UNCONFIRMED -> Icons.Outlined.WarningAmber
                        },
                        contentDescription = null,
                        tint = when (group.statusCategory) {
                            DayStatusCategory.PAST_OR_CURRENT_CONFIRMED -> MaterialTheme.colorScheme.secondary
                            DayStatusCategory.CURRENT_PROVISIONAL -> MaterialTheme.colorScheme.primary
                            DayStatusCategory.PAST_UNCONFIRMED -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (group.statusCategory) {
                            DayStatusCategory.PAST_OR_CURRENT_CONFIRMED -> MaterialTheme.colorScheme.onSecondaryContainer
                            DayStatusCategory.CURRENT_PROVISIONAL -> MaterialTheme.colorScheme.onSurfaceVariant
                            DayStatusCategory.PAST_UNCONFIRMED -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Bouton « Confirmer cette journée complète » (confirme le total réel 0 ou N sans remplacer par zéro)
            if (!group.isConfirmedComplete) {
                Button(
                    onClick = { onToggleDayConfirmed(true) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("journal_confirm_complete_day_${group.localEpochDay}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (count > 0) {
                            "Confirmer cette journée complète ($count cig.)"
                        } else {
                            "Confirmer cette journée complète (0 cig.)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onToggleDayConfirmed(false) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("journal_unconfirm_day_${group.localEpochDay}")
                ) {
                    Text(
                        text = "Retirer la confirmation de complétude",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (group.entries.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                group.entries.forEach { cig ->
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_entry_item_${cig.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = DateAndCurrencyUtils.formatTime(cig.timestampMillis),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onEditEntry(cig) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("journal_edit_entry_${cig.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Modifier l'entrée",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteEntry(cig) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("journal_delete_entry_${cig.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Supprimer l'entrée",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (cig.cravingIntensity != null) {
                                    AssistChip(
                                        onClick = { onEditEntry(cig) },
                                        label = { Text("Envie : ${cig.cravingIntensity}/5") }
                                    )
                                }
                                if (!cig.contextCategory.isNullOrBlank()) {
                                    AssistChip(
                                        onClick = { onEditEntry(cig) },
                                        label = { Text(cig.contextCategory) }
                                    )
                                }
                            }

                            if (!cig.note.isNullOrBlank()) {
                                Text(
                                    text = "« ${cig.note} »",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
