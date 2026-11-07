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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.AccessTime
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

enum class JournalViewMode {
    CALENDAR,
    LIST
}

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

    var viewMode by remember { mutableStateOf(JournalViewMode.CALENDAR) }
    val (todayYear, todayMonth, _) = remember(uiState.todayEpochDay) {
        DateAndCurrencyUtils.localEpochDayToYmd(uiState.todayEpochDay)
    }
    var currentYear by remember(uiState.todayEpochDay) { mutableIntStateOf(todayYear) }
    var currentMonth by remember(uiState.todayEpochDay) { mutableIntStateOf(todayMonth) }
    var selectedDateEpochDay by remember(uiState.todayEpochDay) {
        mutableLongStateOf(uiState.todayEpochDay)
    }
    var targetRetroactiveEpochDay by remember(uiState.todayEpochDay) {
        mutableLongStateOf(uiState.todayEpochDay)
    }

    val monthInfo = remember(currentYear, currentMonth) {
        DateAndCurrencyUtils.getCalendarMonthInfo(currentYear, currentMonth)
    }
    val cigarettesByDay = remember(uiState.cigarettes) {
        uiState.cigarettes.groupBy { it.localEpochDay }
    }
    val confirmedDaysSet = remember(uiState.dailyStatuses) {
        uiState.dailyStatuses.filter { it.isConfirmedComplete }.map { it.localEpochDay }.toSet()
    }
    val selectedDayGroup = remember(
        selectedDateEpochDay,
        uiState.journalGroups,
        uiState.cigarettes,
        uiState.dailyStatuses,
        uiState.planCaps
    ) {
        uiState.journalGroups.find { it.localEpochDay == selectedDateEpochDay } ?: run {
            val dayCigs = cigarettesByDay[selectedDateEpochDay]?.sortedBy { it.timestampMillis } ?: emptyList()
            val isConfirmed = confirmedDaysSet.contains(selectedDateEpochDay)
            val isCurrent = selectedDateEpochDay == uiState.todayEpochDay
            val planCap = uiState.planCaps.find { it.localEpochDay == selectedDateEpochDay }
            val intervalStats = DateAndCurrencyUtils.computeDayIntervalStats(dayCigs.map { it.timestampMillis })
            DayJournalGroup(
                localEpochDay = selectedDateEpochDay,
                entries = dayCigs,
                isConfirmedComplete = isConfirmed,
                isCurrentDay = isCurrent,
                plannedCap = planCap?.capCount,
                intervalStats = intervalStats
            )
        }
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

            // Bannière du Total Cumulé des cigarettes fumées
            item {
                CapZeroWhiteCard(
                    modifier = Modifier.testTag("journal_total_cigarettes_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmokingRooms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Total de cigarettes fumées",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Volume cumulé enregistré dans l'application",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${uiState.cigarettes.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("journal_total_cigarettes_count")
                            )
                        }
                    }
                }
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

            // Commutateur de mode d'affichage (Calendrier 📅 vs Liste 📋)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_view_mode_selector")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isCal = viewMode == JournalViewMode.CALENDAR
                        Surface(
                            color = if (isCal) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = if (isCal) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewMode = JournalViewMode.CALENDAR }
                                .testTag("journal_view_mode_calendar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isCal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calendrier",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isCal) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isList = viewMode == JournalViewMode.LIST
                        Surface(
                            color = if (isList) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = if (isList) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewMode = JournalViewMode.LIST }
                                .testTag("journal_view_mode_list")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = if (isList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Liste chronologique",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isList) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (viewMode == JournalViewMode.CALENDAR) {
                // Grille du Calendrier
                item {
                    CapZeroWhiteCard(
                        modifier = Modifier.testTag("journal_calendar_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Navigation Mois
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 0) {
                                            currentYear -= 1
                                            currentMonth = 11
                                        } else {
                                            currentMonth -= 1
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("journal_calendar_prev_month")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Mois précédent",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = monthInfo.monthNameFormatted,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("journal_calendar_month_title")
                                    )
                                    if (currentYear != todayYear || currentMonth != todayMonth) {
                                        TextButton(
                                            onClick = {
                                                currentYear = todayYear
                                                currentMonth = todayMonth
                                                selectedDateEpochDay = uiState.todayEpochDay
                                                targetRetroactiveEpochDay = uiState.todayEpochDay
                                            },
                                            modifier = Modifier.testTag("journal_calendar_today_button")
                                        ) {
                                            Text(
                                                text = "Aller à aujourd'hui",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (currentMonth == 11) {
                                            currentYear += 1
                                            currentMonth = 0
                                        } else {
                                            currentMonth += 1
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("journal_calendar_next_month")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Mois suivant",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // En-tête des jours de la semaine (Lundi à Dimanche)
                            val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekDays.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                            // Grille des jours (7 colonnes)
                            val totalCells = monthInfo.firstDayOffsetMondayZero + monthInfo.daysInMonth
                            val rowsCount = kotlin.math.ceil(totalCells / 7.0).toInt()

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (rowIndex in 0 until rowsCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (colIndex in 0..6) {
                                            val cellIndex = rowIndex * 7 + colIndex
                                            val dayNumber = cellIndex - monthInfo.firstDayOffsetMondayZero + 1

                                            if (dayNumber in 1..monthInfo.daysInMonth) {
                                                val dayEpochDay = DateAndCurrencyUtils.ymdToLocalEpochDay(currentYear, currentMonth, dayNumber)
                                                val isSelected = dayEpochDay == selectedDateEpochDay
                                                val isToday = dayEpochDay == uiState.todayEpochDay
                                                val dayCigs = cigarettesByDay[dayEpochDay] ?: emptyList()
                                                val cigsCount = dayCigs.size
                                                val isConfirmed = confirmedDaysSet.contains(dayEpochDay)

                                                Surface(
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                                        isToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        else -> MaterialTheme.colorScheme.surface
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = when {
                                                        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                                        isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            selectedDateEpochDay = dayEpochDay
                                                            targetRetroactiveEpochDay = dayEpochDay
                                                        }
                                                        .testTag("journal_calendar_day_${dayEpochDay}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(2.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = "$dayNumber",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                            color = when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                isToday -> MaterialTheme.colorScheme.primary
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )

                                                        if (cigsCount > 0) {
                                                            Surface(
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                                shape = CircleShape,
                                                                modifier = Modifier
                                                                    .padding(top = 1.dp)
                                                                    .size(17.dp)
                                                                    .testTag("journal_calendar_badge_${dayEpochDay}")
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = "$cigsCount",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White
                                                                    )
                                                                }
                                                            }
                                                        } else if (isConfirmed) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Check,
                                                                contentDescription = "Journée sans fumer confirmée",
                                                                tint = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier
                                                                    .size(13.dp)
                                                                    .testTag("journal_calendar_badge_${dayEpochDay}")
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Affichage direct de l'historique de la date sélectionnée sous le calendrier
                item {
                    DayJournalCard(
                        group = selectedDayGroup,
                        todayEpochDay = uiState.todayEpochDay,
                        onEditEntry = { editingCigarette = it },
                        onDeleteEntry = { onDeleteCigarette(it.id) },
                        onToggleDayConfirmed = { confirmed ->
                            onToggleDayConfirmed(selectedDayGroup.localEpochDay, confirmed)
                        },
                        onAddEntryForDay = {
                            targetRetroactiveEpochDay = selectedDateEpochDay
                            showAddRetroactiveDialog = true
                        },
                        modifier = Modifier.testTag("journal_selected_day_card")
                    )
                }

                if (uiState.pendingUndo != null) {
                    item {
                        DeepGreenOutlinedButton(
                            text = "Annuler la dernière action",
                            onClick = onUndoLastAction,
                            leadingIcon = Icons.Filled.Undo,
                            modifier = Modifier.testTag("journal_undo_button")
                        )
                    }
                }
            } else {
                // Vue Liste Chronologique classique
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
                                onClick = {
                                    targetRetroactiveEpochDay = uiState.todayEpochDay
                                    showAddRetroactiveDialog = true
                                },
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
    }

    if (showAddRetroactiveDialog) {
        val (currentHour, currentMinute) = DateAndCurrencyUtils.extractHourAndMinute(uiState.nowMillis)
        CigaretteEntryDialog(
            title = "Ajouter une cigarette",
            initialEpochDay = targetRetroactiveEpochDay,
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
    onToggleDayConfirmed: (Boolean) -> Unit,
    onAddEntryForDay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
        modifier = modifier.then(Modifier.testTag("journal_day_card_${group.localEpochDay}"))
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

            // Espacement des cigarettes de cette journée (strictement intra-journée)
            if (group.intervalStats != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_day_interval_stats_${group.localEpochDay}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Espacement : Min ${DateAndCurrencyUtils.formatIntervalDuration(group.intervalStats.minMillis)} · Moy ${DateAndCurrencyUtils.formatIntervalDuration(group.intervalStats.avgMillis)} · Max ${DateAndCurrencyUtils.formatIntervalDuration(group.intervalStats.maxMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (count == 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_day_interval_stats_${group.localEpochDay}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Espacement : 1 seule cigarette (aucun intervalle intra-journée)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

            if (onAddEntryForDay != null) {
                EmeraldPrimaryButton(
                    text = "Ajouter une cigarette à cette date",
                    onClick = onAddEntryForDay,
                    leadingIcon = Icons.Filled.Add,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_selected_day_add_button")
                )
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
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_empty_day_message_${group.localEpochDay}")
                ) {
                    Text(
                        text = "Aucune cigarette enregistrée pour ce jour.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
