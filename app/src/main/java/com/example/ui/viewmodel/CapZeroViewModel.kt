package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CigaretteLogEntity
import com.example.data.local.CravingEventEntity
import com.example.data.local.DailyPlanCapEntity
import com.example.data.local.DailyStatusEntity
import com.example.data.local.PackPurchaseEntity
import com.example.data.preferences.DarkThemeMode
import com.example.data.preferences.UserPreferencesState
import com.example.data.repository.CapZeroRepository
import com.example.util.DateAndCurrencyUtils
import com.example.util.PlanStepPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

sealed interface UndoAction {
    val message: String

    data class AddedCigarette(
        val cigaretteId: Long,
        override val message: String = "Cigarette enregistrée"
    ) : UndoAction

    data class DeletedCigarette(
        val deletedEntity: CigaretteLogEntity,
        override val message: String = "Cigarette supprimée du journal"
    ) : UndoAction

    data class CravingSmoked(
        val cigaretteId: Long,
        val cravingEventId: Long,
        override val message: String = "Cigarette enregistrée depuis « Envie »"
    ) : UndoAction

    data class AddedPackPurchase(
        val purchaseId: Long,
        override val message: String = "Achat de paquet enregistré"
    ) : UndoAction

    data class DeletedPackPurchase(
        val deletedEntity: PackPurchaseEntity,
        override val message: String = "Achat de paquet supprimé"
    ) : UndoAction

    data class AdjustedInventory(
        val previousOffset: Int?,
        override val message: String = "Stock de cigarettes ajusté"
    ) : UndoAction
}

/**
 * Synthèse de l'inventaire de cigarettes et des paquets achetés.
 */
@Immutable
data class CigaretteInventorySummary(
    val remainingCigarettes: Int = 0,
    val totalPacksPurchased: Int = 0,
    val totalCigarettesPurchased: Int = 0,
    val totalSpentMillimes: Long = 0L,
    val totalSpentTnd: Double = 0.0,
    val averagePackPriceTnd: Double? = null,
    val equivalentFullPacksRemaining: Int = 0,
    val looseCigarettesRemaining: Int = 0
)

/**
 * Type explicite d'une journée pour clarifier l'affichage.
 */
enum class DayStatusCategory {
    CURRENT_PROVISIONAL,      // Journée en cours : compteur provisoire
    PAST_UNCONFIRMED,         // Journée passée non confirmée : données potentiellement incomplètes
    PAST_OR_CURRENT_CONFIRMED // Journée complète confirmée (avec 0 ou plusieurs cigarettes)
}

@Immutable
data class DayJournalGroup(
    val localEpochDay: Long,
    val entries: List<CigaretteLogEntity>,
    val isConfirmedComplete: Boolean,
    val isCurrentDay: Boolean,
    val plannedCap: Int?
) {
    val statusCategory: DayStatusCategory
        get() = when {
            isConfirmedComplete -> DayStatusCategory.PAST_OR_CURRENT_CONFIRMED
            isCurrentDay -> DayStatusCategory.CURRENT_PROVISIONAL
            else -> DayStatusCategory.PAST_UNCONFIRMED
        }

    val isZeroConfirmed: Boolean
        get() = entries.isEmpty() && isConfirmedComplete

    val isLoggedDay: Boolean
        get() = entries.isNotEmpty() || isConfirmedComplete
}

@Immutable
data class ContextStatItem(
    val contextName: String,
    val count: Int,
    val percentage: Int
)

@Immutable
data class ChartDayPoint(
    val localEpochDay: Long,
    val label: String,
    val realCount: Int?, // null si jour non renseigné / non confirmé
    val capCount: Int?
)

@Immutable
data class ProgressMetrics(
    val confirmedDaysCount: Int,
    val confirmedZeroDaysCount: Int,
    val totalCigarettesOnConfirmedDays: Int,
    val totalCigarettesAllDays: Int,
    val averagePerConfirmedDay: Double?, // null si 0 journée complète confirmée -> affiche « — »
    val averageCravingIntensity: Double?,
    val referenceDailyCigarettes: Int?,
    val singleCigaretteCostTnd: Double?,
    val avoidedCigarettesOnConfirmedDays: Int?,
    val estimatedSavingsTnd: Double?, // null si données absentes ou 0 jour confirmé -> affiche « — »
    val topContexts: List<ContextStatItem>,
    val chartPoints: List<ChartDayPoint>,
    val hasChartData: Boolean,
    val cravingsOvercomeCount: Int
)

@Immutable
data class CravingTimerState(
    val remainingSeconds: Int = 300, // 5 minutes = 300 secondes
    val isRunning: Boolean = false,
    val targetEndTimestampMillis: Long? = null, // Horodatage cible pour rester exact après passage en arrière-plan
    val selectedActivity: String? = null,
    val sessionResolvedOutcome: String? = null, // null, "PASSED" ou "SMOKED"
    val lastRecordedCigaretteId: Long? = null
)

@Immutable
data class CapZeroUiState(
    val isLoading: Boolean = true,
    val nowMillis: Long = System.currentTimeMillis(),
    val todayEpochDay: Long = DateAndCurrencyUtils.toLocalEpochDay(System.currentTimeMillis()),
    val preferences: UserPreferencesState = UserPreferencesState(),
    val cigarettes: List<CigaretteLogEntity> = emptyList(),
    val packPurchases: List<PackPurchaseEntity> = emptyList(),
    val inventorySummary: CigaretteInventorySummary = CigaretteInventorySummary(),
    val planCaps: List<DailyPlanCapEntity> = emptyList(),
    val dailyStatuses: List<DailyStatusEntity> = emptyList(),
    val cravingEvents: List<CravingEventEntity> = emptyList(),
    val todayCigarettesCount: Int = 0,
    val todayCap: Int? = null,
    val isTodayConfirmedComplete: Boolean = false,
    val isPostQuitDate: Boolean = false,
    val lastCigaretteTimestampMillis: Long? = null,
    val elapsedSinceLastText: String = "Aucune cigarette enregistrée",
    val confirmedObservationDaysCount: Int = 0,
    val computedObservationReference: Int? = null, // null si 0 journée complète confirmée
    val journalGroups: List<DayJournalGroup> = emptyList(),
    val progressMetrics: ProgressMetrics = ProgressMetrics(
        confirmedDaysCount = 0,
        confirmedZeroDaysCount = 0,
        totalCigarettesOnConfirmedDays = 0,
        totalCigarettesAllDays = 0,
        averagePerConfirmedDay = null,
        averageCravingIntensity = null,
        referenceDailyCigarettes = null,
        singleCigaretteCostTnd = null,
        avoidedCigarettesOnConfirmedDays = null,
        estimatedSavingsTnd = null,
        topContexts = emptyList(),
        chartPoints = emptyList(),
        hasChartData = false,
        cravingsOvercomeCount = 0
    ),
    val cravingTimer: CravingTimerState = CravingTimerState(),
    val pendingUndo: UndoAction? = null,
    val statusBannerMessage: String? = null,
    val lastQuickLoggedCigarette: CigaretteLogEntity? = null
)

private data class InternalViewModelState(
    val nowMillis: Long = System.currentTimeMillis(),
    val cravingTimer: CravingTimerState = CravingTimerState(),
    val pendingUndo: UndoAction? = null,
    val statusBannerMessage: String? = null,
    val lastQuickLoggedCigarette: CigaretteLogEntity? = null
)

/**
 * Snapshot métier précalculé uniquement lorsque les données persistées ou le jour calendaire changent.
 * Évite de recalculer les listes du journal, l'inventaire et les graphiques à chaque tick de l'horloge.
 */
private data class ComputedDomainSnapshot(
    val todayEpochDay: Long,
    val preferences: UserPreferencesState,
    val cigarettes: List<CigaretteLogEntity>,
    val packPurchases: List<PackPurchaseEntity>,
    val inventorySummary: CigaretteInventorySummary,
    val planCaps: List<DailyPlanCapEntity>,
    val dailyStatuses: List<DailyStatusEntity>,
    val cravingEvents: List<CravingEventEntity>,
    val todayCigarettesCount: Int,
    val todayCap: Int?,
    val isTodayConfirmedComplete: Boolean,
    val isPostQuitDate: Boolean,
    val lastCigaretteTimestampMillis: Long?,
    val confirmedObservationDaysCount: Int,
    val computedObservationReference: Int?,
    val journalGroups: List<DayJournalGroup>,
    val progressMetrics: ProgressMetrics
)

private data class InventoryChronologicalEvent(
    val timestampMillis: Long,
    val isPackPurchase: Boolean,
    val deltaCigarettes: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class CapZeroViewModel(
    private val repository: CapZeroRepository,
    enableLiveTicker: Boolean = true
) : ViewModel() {

    private val internalState = MutableStateFlow(InternalViewModelState())

    private val todayEpochDayFlow: Flow<Long> = internalState
        .map { repository.currentLocalEpochDay(it.nowMillis) }
        .distinctUntilChanged()

    private val domainSnapshotFlow: Flow<ComputedDomainSnapshot> = combine(
        repository.preferencesFlow,
        repository.cigarettesFlow,
        repository.planCapsFlow,
        repository.dailyStatusesFlow,
        combine(
            repository.cravingEventsFlow,
            repository.packPurchasesFlow,
            todayEpochDayFlow
        ) { cravings, packPurchases, todayDay ->
            Triple(cravings, packPurchases, todayDay)
        }
    ) { prefs, cigarettes, planCaps, statuses, (cravings, packPurchases, todayDay) ->
        buildDomainSnapshot(
            todayDay = todayDay,
            prefs = prefs,
            cigarettes = cigarettes,
            packPurchases = packPurchases,
            planCaps = planCaps,
            statuses = statuses,
            cravings = cravings
        )
    }.distinctUntilChanged()

    val uiState: StateFlow<CapZeroUiState> = combine(
        domainSnapshotFlow,
        internalState
    ) { snapshot, internal ->
        val elapsedText = DateAndCurrencyUtils.formatDurationSince(
            snapshot.lastCigaretteTimestampMillis,
            internal.nowMillis
        )
        CapZeroUiState(
            isLoading = false,
            nowMillis = internal.nowMillis,
            todayEpochDay = snapshot.todayEpochDay,
            preferences = snapshot.preferences,
            cigarettes = snapshot.cigarettes,
            packPurchases = snapshot.packPurchases,
            inventorySummary = snapshot.inventorySummary,
            planCaps = snapshot.planCaps,
            dailyStatuses = snapshot.dailyStatuses,
            cravingEvents = snapshot.cravingEvents,
            todayCigarettesCount = snapshot.todayCigarettesCount,
            todayCap = snapshot.todayCap,
            isTodayConfirmedComplete = snapshot.isTodayConfirmedComplete,
            isPostQuitDate = snapshot.isPostQuitDate,
            lastCigaretteTimestampMillis = snapshot.lastCigaretteTimestampMillis,
            elapsedSinceLastText = elapsedText,
            confirmedObservationDaysCount = snapshot.confirmedObservationDaysCount,
            computedObservationReference = snapshot.computedObservationReference,
            journalGroups = snapshot.journalGroups,
            progressMetrics = snapshot.progressMetrics,
            cravingTimer = internal.cravingTimer,
            pendingUndo = internal.pendingUndo,
            statusBannerMessage = internal.statusBannerMessage,
            lastQuickLoggedCigarette = internal.lastQuickLoggedCigarette
        )
    }.distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CapZeroUiState(isLoading = true)
        )

    init {
        if (enableLiveTicker) {
            // Horloge adaptative :
            // - Tick toutes les 1 000 ms UNIQUEMENT lorsque le minuteur de 5 minutes « Envie » est en cours.
            // - Tick toutes les 30 000 ms (30 s) lorsque le minuteur est à l'arrêt (pour actualiser « Il y a X min » sans ralentir le téléphone).
            viewModelScope.launch {
                internalState
                    .map { it.cravingTimer.isRunning }
                    .distinctUntilChanged()
                    .collectLatest { isRunning ->
                        val delayMillis = if (isRunning) 1_000L else 30_000L
                        while (true) {
                            delay(delayMillis)
                            syncTimeWithTimestamp(System.currentTimeMillis())
                        }
                    }
            }
        }
    }

    /**
     * Synchronise l'horloge interne et recalcule exactement le minuteur d'envie
     * à partir de son horodatage de fin cible (garantit l'exactitude après un passage en arrière-plan).
     */
    fun syncTimeWithTimestamp(currentMillis: Long) {
        internalState.update { current ->
            val timer = current.cravingTimer
            val updatedTimer = if (timer.isRunning && timer.targetEndTimestampMillis != null) {
                val remainingMillis = timer.targetEndTimestampMillis - currentMillis
                if (remainingMillis <= 0L) {
                    timer.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        targetEndTimestampMillis = null
                    )
                } else {
                    val exactRemainingSeconds = ((remainingMillis + 999L) / 1000L).toInt().coerceIn(0, 300)
                    timer.copy(remainingSeconds = exactRemainingSeconds)
                }
            } else {
                timer
            }
            current.copy(
                nowMillis = currentMillis,
                cravingTimer = updatedTimer
            )
        }
    }

    fun syncTimeWithSystemClock() {
        syncTimeWithTimestamp(System.currentTimeMillis())
    }

    private fun computeInventorySummary(
        packPurchases: List<PackPurchaseEntity>,
        cigarettes: List<CigaretteLogEntity>,
        defaultCigarettesPerPack: Int,
        manualInventoryStockOffset: Int? = null
    ): CigaretteInventorySummary {
        val totalPacks = packPurchases.sumOf { it.packsCount }
        val totalCigsPurchased = packPurchases.sumOf { it.packsCount * it.cigarettesPerPack }
        val totalSpentMillimes = packPurchases.sumOf { it.totalPriceMillimes }
        val totalSpentTnd = totalSpentMillimes.toDouble() / 1000.0
        val avgPackPriceTnd = if (totalPacks > 0) {
            totalSpentTnd / totalPacks.toDouble()
        } else {
            null
        }

        // Calcul chronologique du stock restant :
        // chaque paquet acheté ajoute (packsCount * cigarettesPerPack) au stock,
        // chaque cigarette enregistrée déduit 1 du stock disponible (sans descendre sous 0).
        val events = ArrayList<InventoryChronologicalEvent>(packPurchases.size + cigarettes.size)
        for (purchase in packPurchases) {
            events.add(
                InventoryChronologicalEvent(
                    timestampMillis = purchase.timestampMillis,
                    isPackPurchase = true,
                    deltaCigarettes = purchase.packsCount * purchase.cigarettesPerPack
                )
            )
        }
        for (cig in cigarettes) {
            events.add(
                InventoryChronologicalEvent(
                    timestampMillis = cig.timestampMillis,
                    isPackPurchase = false,
                    deltaCigarettes = -1
                )
            )
        }
        events.sortWith(
            compareBy<InventoryChronologicalEvent> { it.timestampMillis }
                .thenByDescending { it.isPackPurchase }
        )

        var currentStock = 0
        for (event in events) {
            currentStock = if (event.isPackPurchase) {
                currentStock + event.deltaCigarettes
            } else {
                max(0, currentStock - 1)
            }
        }

        // Appliquer l'ajustement manuel si spécifié
        if (manualInventoryStockOffset != null) {
            currentStock = max(0, currentStock + manualInventoryStockOffset)
        }

        val packSize = defaultCigarettesPerPack.coerceAtLeast(1)
        val fullPacks = currentStock / packSize
        val looseCigs = currentStock % packSize

        return CigaretteInventorySummary(
            remainingCigarettes = currentStock,
            totalPacksPurchased = totalPacks,
            totalCigarettesPurchased = totalCigsPurchased,
            totalSpentMillimes = totalSpentMillimes,
            totalSpentTnd = totalSpentTnd,
            averagePackPriceTnd = avgPackPriceTnd,
            equivalentFullPacksRemaining = fullPacks,
            looseCigarettesRemaining = looseCigs
        )
    }

    private fun buildDomainSnapshot(
        todayDay: Long,
        prefs: UserPreferencesState,
        cigarettes: List<CigaretteLogEntity>,
        packPurchases: List<PackPurchaseEntity>,
        planCaps: List<DailyPlanCapEntity>,
        statuses: List<DailyStatusEntity>,
        cravings: List<CravingEventEntity>
    ): ComputedDomainSnapshot {
        val cigsByDay = cigarettes.groupBy { it.localEpochDay }
        val capsByDay = planCaps.associateBy { it.localEpochDay }
        val confirmedDaySet = statuses.filter { it.isConfirmedComplete }.map { it.localEpochDay }.toSet()

        val todayCount = cigsByDay[todayDay]?.size ?: 0
        val isTodayConfirmed = confirmedDaySet.contains(todayDay)

        val quitDay = prefs.quitDateEpochDay
        val isPostQuit = quitDay != null && todayDay > quitDay

        val todayCap: Int? = when {
            capsByDay.containsKey(todayDay) -> capsByDay[todayDay]?.capCount
            quitDay != null && planCaps.isNotEmpty() && todayDay >= quitDay -> 0
            else -> null
        }

        // cigarettes sont déjà triées par timestampMillis DESC depuis Room, mais on sécurise maxOfOrNull
        val latestCigaretteMillis = cigarettes.firstOrNull()?.timestampMillis
            ?: cigarettes.maxOfOrNull { it.timestampMillis }

        val inventorySummary = computeInventorySummary(
            packPurchases = packPurchases,
            cigarettes = cigarettes,
            defaultCigarettesPerPack = prefs.cigarettesPerPack ?: 20,
            manualInventoryStockOffset = prefs.manualInventoryStockOffset
        )

        // Calcul de la référence sur les journées complètes confirmées uniquement (jamais à partir de 0 journée confirmée)
        val confirmedDaysCount = confirmedDaySet.size
        val computedObservationRef: Int? = if (confirmedDaySet.isNotEmpty()) {
            val totalInConfirmedDays = confirmedDaySet.sumOf { day -> cigsByDay[day]?.size ?: 0 }
            ceil(totalInConfirmedDays.toDouble() / confirmedDaySet.size.toDouble()).toInt()
        } else {
            null
        }

        // Groupes du Journal : inclut aujourd'hui, hier (pour permettre de confirmer facilement une journée passée),
        // ainsi que tous les jours ayant des cigarettes ou un statut confirmé.
        val allJournalDays = (cigsByDay.keys + confirmedDaySet + setOf(todayDay, todayDay - 1L))
            .toSortedSet(compareByDescending { it })
        val journalGroups = allJournalDays.map { day ->
            val dayEntries = cigsByDay[day] ?: emptyList()
            val capForDay = capsByDay[day]?.capCount ?: if (quitDay != null && planCaps.isNotEmpty() && day >= quitDay) 0 else null
            DayJournalGroup(
                localEpochDay = day,
                entries = dayEntries,
                isConfirmedComplete = confirmedDaySet.contains(day),
                isCurrentDay = day == todayDay,
                plannedCap = capForDay
            )
        }

        // Calcul des moyennes et économies EXCLUSIVEMENT sur les journées complètes confirmées
        val confirmedZeroDaysCount = confirmedDaySet.count { day -> (cigsByDay[day]?.size ?: 0) == 0 }
        val totalCigsOnConfirmedDays = confirmedDaySet.sumOf { day -> cigsByDay[day]?.size ?: 0 }

        val avgPerConfirmedDay: Double? = if (confirmedDaySet.isNotEmpty()) {
            totalCigsOnConfirmedDays.toDouble() / confirmedDaySet.size.toDouble()
        } else {
            null
        }

        val intensities = cigarettes.mapNotNull { it.cravingIntensity }
        val avgIntensity = if (intensities.isNotEmpty()) {
            intensities.average()
        } else {
            null
        }

        val effectiveRef = prefs.referenceDailyCigarettes ?: computedObservationRef
        val costPerCigTnd = prefs.singleCigaretteCostTnd ?: inventorySummary.averagePackPriceTnd?.let { avgPack ->
            val perPack = (prefs.cigarettesPerPack ?: 20).coerceAtLeast(1)
            avgPack / perPack.toDouble()
        }

        val avoidedOnConfirmedDays: Int? = if (effectiveRef != null && confirmedDaySet.isNotEmpty()) {
            confirmedDaySet.sumOf { day ->
                val actualOnDay = cigsByDay[day]?.size ?: 0
                (effectiveRef - actualOnDay)
            }
        } else {
            null
        }

        val estimatedSavingsTnd: Double? = if (
            avoidedOnConfirmedDays != null &&
            costPerCigTnd != null &&
            confirmedDaySet.isNotEmpty()
        ) {
            max(0.0, avoidedOnConfirmedDays.toDouble() * costPerCigTnd)
        } else {
            null
        }

        // Contextes fréquents
        val contextEntries = cigarettes.mapNotNull { it.contextCategory?.takeIf { c -> c.isNotBlank() } }
        val topContexts = if (contextEntries.isNotEmpty()) {
            contextEntries.groupingBy { it }.eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { (name, count) ->
                    ContextStatItem(
                        contextName = name,
                        count = count,
                        percentage = Math.round((count.toDouble() / contextEntries.size.toDouble()) * 100.0).toInt()
                    )
                }
        } else {
            emptyList()
        }

        val loggedOrConfirmedDays = (cigsByDay.keys + confirmedDaySet).toSortedSet()
        val hasChartData = loggedOrConfirmedDays.isNotEmpty() || planCaps.isNotEmpty()

        val chartPoints = if (hasChartData) {
            val chartDays = buildChartDaysList(
                todayDay = todayDay,
                loggedDays = loggedOrConfirmedDays,
                planDays = capsByDay.keys
            )
            chartDays.map { day ->
                val isLoggedOrConfirmed = loggedOrConfirmedDays.contains(day)
                val realVal = if (isLoggedOrConfirmed) (cigsByDay[day]?.size ?: 0) else null
                val capVal = capsByDay[day]?.capCount ?: if (quitDay != null && planCaps.isNotEmpty() && day >= quitDay) 0 else null
                ChartDayPoint(
                    localEpochDay = day,
                    label = DateAndCurrencyUtils.formatChartDate(day),
                    realCount = realVal,
                    capCount = capVal
                )
            }
        } else {
            emptyList()
        }

        val cravingsOvercome = cravings.count { it.outcome == "PASSED" }

        return ComputedDomainSnapshot(
            todayEpochDay = todayDay,
            preferences = prefs,
            cigarettes = cigarettes,
            packPurchases = packPurchases,
            inventorySummary = inventorySummary,
            planCaps = planCaps,
            dailyStatuses = statuses,
            cravingEvents = cravings,
            todayCigarettesCount = todayCount,
            todayCap = todayCap,
            isTodayConfirmedComplete = isTodayConfirmed,
            isPostQuitDate = isPostQuit,
            lastCigaretteTimestampMillis = latestCigaretteMillis,
            confirmedObservationDaysCount = confirmedDaysCount,
            computedObservationReference = computedObservationRef,
            journalGroups = journalGroups,
            progressMetrics = ProgressMetrics(
                confirmedDaysCount = confirmedDaysCount,
                confirmedZeroDaysCount = confirmedZeroDaysCount,
                totalCigarettesOnConfirmedDays = totalCigsOnConfirmedDays,
                totalCigarettesAllDays = cigarettes.size,
                averagePerConfirmedDay = avgPerConfirmedDay,
                averageCravingIntensity = avgIntensity,
                referenceDailyCigarettes = effectiveRef,
                singleCigaretteCostTnd = costPerCigTnd,
                avoidedCigarettesOnConfirmedDays = avoidedOnConfirmedDays,
                estimatedSavingsTnd = estimatedSavingsTnd,
                topContexts = topContexts,
                chartPoints = chartPoints,
                hasChartData = hasChartData,
                cravingsOvercomeCount = cravingsOvercome
            )
        )
    }

    private fun buildChartDaysList(
        todayDay: Long,
        loggedDays: Set<Long>,
        planDays: Set<Long>
    ): List<Long> {
        val combined = (loggedDays + planDays + setOf(todayDay)).sorted()
        if (combined.isEmpty()) return listOf(todayDay)
        val minDay = combined.first().coerceAtMost(todayDay - 4L)
        val maxDay = combined.last().coerceAtMost(todayDay + 6L)
        val range = (minDay..maxDay).toList()
        return if (range.size > 12) {
            ((todayDay - 5L)..(todayDay + 6L)).toList()
        } else {
            range
        }
    }

    // --- Actions Utilisateur : Temps & Rafraîchissement ---

    fun updateNowMillis(nowMillis: Long) {
        syncTimeWithTimestamp(nowMillis)
    }

    fun clearStatusBanner() {
        internalState.update { it.copy(statusBannerMessage = null) }
    }

    fun dismissQuickLogContextPrompt() {
        internalState.update { it.copy(lastQuickLoggedCigarette = null) }
    }

    // --- Actions Utilisateur : Onboarding & Plan ---

    fun completeOnboardingWithPlan(
        referenceDailyCigarettes: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        quitDateEpochDay: Long,
        validatedSteps: List<PlanStepPreview>
    ) {
        viewModelScope.launch {
            val startDay = validatedSteps.firstOrNull()?.localEpochDay ?: uiState.value.todayEpochDay
            repository.setupDirectPlan(
                referenceDailyCigarettes = referenceDailyCigarettes,
                packPriceMillimes = packPriceMillimes,
                cigarettesPerPack = cigarettesPerPack,
                startEpochDay = startDay,
                quitDateEpochDay = quitDateEpochDay,
                validatedSteps = validatedSteps
            )
            internalState.update {
                it.copy(statusBannerMessage = "Votre plan Cap Zéro est validé.")
            }
        }
    }

    fun completeOnboardingWithObservation(
        observationTargetDays: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        quitDateEpochDay: Long
    ) {
        viewModelScope.launch {
            repository.setupObservationMode(
                observationTargetDays = observationTargetDays,
                packPriceMillimes = packPriceMillimes,
                cigarettesPerPack = cigarettesPerPack,
                quitDateEpochDay = quitDateEpochDay
            )
            internalState.update {
                it.copy(statusBannerMessage = "Suivi démarré. Confirmez vos journées complètes pour établir votre plan.")
            }
        }
    }

    fun saveGeneratedOrUpdatedPlan(
        referenceDailyCigarettes: Int,
        startEpochDay: Long,
        quitDateEpochDay: Long,
        steps: List<PlanStepPreview>
    ) {
        viewModelScope.launch {
            repository.saveCustomPlanSteps(
                referenceDailyCigarettes = referenceDailyCigarettes,
                startEpochDay = startEpochDay,
                quitDateEpochDay = quitDateEpochDay,
                steps = steps
            )
            internalState.update {
                it.copy(statusBannerMessage = "Calendrier de réduction validé.")
            }
        }
    }

    fun modifySingleDayPlanCap(localEpochDay: Long, newCap: Int) {
        viewModelScope.launch {
            repository.updateSinglePlanStepAndEnforceOrder(localEpochDay, newCap)
            internalState.update {
                it.copy(statusBannerMessage = "Étape du plan ajustée.")
            }
        }
    }

    // --- Actions Utilisateur : Achats de Paquets & Inventaire ---

    fun recordPackPurchase(
        packsCount: Int,
        cigarettesPerPack: Int,
        unitPriceMillimes: Long,
        note: String? = null,
        timestampMillis: Long = internalState.value.nowMillis
    ) {
        viewModelScope.launch {
            val saved = repository.addPackPurchase(
                timestampMillis = timestampMillis,
                packsCount = packsCount,
                cigarettesPerPack = cigarettesPerPack,
                unitPriceMillimes = unitPriceMillimes,
                note = note
            )
            val addedCigs = saved.packsCount * saved.cigarettesPerPack
            val formattedCost = DateAndCurrencyUtils.formatMillimesToTnd(saved.totalPriceMillimes)
            internalState.update {
                it.copy(
                    pendingUndo = UndoAction.AddedPackPurchase(
                        purchaseId = saved.id,
                        message = "+${saved.packsCount} paquet(s) (+$addedCigs cig. · $formattedCost)"
                    )
                )
            }
        }
    }

    fun deletePackPurchase(id: Long) {
        viewModelScope.launch {
            val deleted = repository.deletePackPurchase(id)
            if (deleted != null) {
                internalState.update {
                    it.copy(
                        pendingUndo = UndoAction.DeletedPackPurchase(
                            deletedEntity = deleted,
                            message = "Achat de ${deleted.packsCount} paquet(s) supprimé"
                        )
                    )
                }
            }
        }
    }

    fun adjustInventoryStock(newTargetStock: Int) {
        viewModelScope.launch {
            val currentInventory = uiState.value.inventorySummary.remainingCigarettes
            val currentOffset = uiState.value.preferences.manualInventoryStockOffset ?: 0
            val naturalStock = currentInventory - currentOffset
            val newOffset = newTargetStock.coerceAtLeast(0) - naturalStock
            repository.setManualInventoryStockOffset(newOffset)
            internalState.update {
                it.copy(
                    pendingUndo = UndoAction.AdjustedInventory(
                        previousOffset = uiState.value.preferences.manualInventoryStockOffset,
                        message = "Stock ajusté à $newTargetStock cigarette(s)"
                    ),
                    statusBannerMessage = "Inventaire mis à jour : $newTargetStock cigarette(s)."
                )
            }
        }
    }

    fun setObservationTargetDays(targetDays: Int) {
        viewModelScope.launch {
            repository.updateObservationTargetDays(targetDays)
            internalState.update {
                it.copy(statusBannerMessage = "Période d'observation définie à $targetDays jour(s).")
            }
        }
    }

    // --- Actions Utilisateur : Cigarettes & Annulation Immédiate ---

    fun recordQuickCigarette(
        nowMillis: Long = internalState.value.nowMillis,
        cravingIntensity: Int? = null,
        contextCategory: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val saved = repository.logCigaretteNow(
                nowMillis = nowMillis,
                cravingIntensity = cravingIntensity,
                contextCategory = contextCategory,
                note = note
            )
            internalState.update {
                it.copy(
                    pendingUndo = UndoAction.AddedCigarette(saved.id),
                    lastQuickLoggedCigarette = saved
                )
            }
        }
    }

    fun addRetroactiveCigarette(
        timestampMillis: Long,
        cravingIntensity: Int?,
        contextCategory: String?,
        note: String?
    ) {
        viewModelScope.launch {
            val result = repository.addRetroactiveCigaretteWithInvalidation(
                timestampMillis = timestampMillis,
                cravingIntensity = cravingIntensity,
                contextCategory = contextCategory,
                note = note
            )
            internalState.update {
                it.copy(
                    pendingUndo = UndoAction.AddedCigarette(
                        cigaretteId = result.entity.id,
                        message = if (result.invalidatedConfirmedDay) {
                            "Cigarette ajoutée. Veuillez reconfirmer cette journée complète."
                        } else {
                            "Cigarette ajoutée au journal"
                        }
                    )
                )
            }
        }
    }

    fun updateCigaretteDetails(
        id: Long,
        timestampMillis: Long,
        cravingIntensity: Int?,
        contextCategory: String?,
        note: String?
    ) {
        viewModelScope.launch {
            val invalidated = repository.updateCigaretteWithInvalidation(
                id = id,
                timestampMillis = timestampMillis,
                cravingIntensity = cravingIntensity,
                contextCategory = contextCategory,
                note = note
            )
            internalState.update {
                it.copy(
                    lastQuickLoggedCigarette = null,
                    statusBannerMessage = if (invalidated) {
                        "Entrée modifiée. Veuillez reconfirmer la complétude de cette journée."
                    } else {
                        "Entrée mise à jour."
                    }
                )
            }
        }
    }

    fun deleteCigarette(id: Long) {
        viewModelScope.launch {
            val (deleted, invalidated) = repository.deleteCigaretteWithInvalidation(id)
            if (deleted != null) {
                internalState.update {
                    it.copy(
                        pendingUndo = UndoAction.DeletedCigarette(
                            deletedEntity = deleted,
                            message = if (invalidated) {
                                "Cigarette supprimée. Veuillez reconfirmer cette journée."
                            } else {
                                "Cigarette supprimée du journal"
                            }
                        ),
                        lastQuickLoggedCigarette = if (it.lastQuickLoggedCigarette?.id == id) null else it.lastQuickLoggedCigarette
                    )
                }
            }
        }
    }

    fun undoLastAction() {
        val undo = internalState.value.pendingUndo ?: return
        viewModelScope.launch {
            when (undo) {
                is UndoAction.AddedCigarette -> {
                    repository.deleteCigarette(undo.cigaretteId)
                }
                is UndoAction.DeletedCigarette -> {
                    repository.restoreCigarette(undo.deletedEntity)
                }
                is UndoAction.CravingSmoked -> {
                    repository.undoCravingSmoked(undo.cigaretteId, undo.cravingEventId)
                    internalState.update {
                        it.copy(
                            cravingTimer = it.cravingTimer.copy(
                                sessionResolvedOutcome = null,
                                lastRecordedCigaretteId = null
                            )
                        )
                    }
                }
                is UndoAction.AddedPackPurchase -> {
                    repository.deletePackPurchase(undo.purchaseId)
                }
                is UndoAction.DeletedPackPurchase -> {
                    repository.restorePackPurchase(undo.deletedEntity)
                }
                is UndoAction.AdjustedInventory -> {
                    repository.setManualInventoryStockOffset(undo.previousOffset)
                }
            }
            internalState.update {
                it.copy(
                    pendingUndo = null,
                    lastQuickLoggedCigarette = null,
                    statusBannerMessage = "Action annulée."
                )
            }
        }
    }

    fun dismissUndo() {
        internalState.update { it.copy(pendingUndo = null) }
    }

    // --- Confirmation des Journées Complètes (0 ou plusieurs cigarettes) ---

    fun setDayConfirmedComplete(localEpochDay: Long, confirmed: Boolean) {
        viewModelScope.launch {
            repository.setDayConfirmed(localEpochDay, confirmed)
            internalState.update {
                it.copy(
                    statusBannerMessage = if (confirmed) {
                        "Journée complète confirmée."
                    } else {
                        "Confirmation de la journée retirée."
                    }
                )
            }
        }
    }

    // --- Écran « Envie » (Minuteur 5 min exact après passage en arrière-plan) ---

    fun toggleCravingTimer() {
        internalState.update { current ->
            val now = current.nowMillis
            val timer = current.cravingTimer
            if (timer.isRunning) {
                // Mise en pause : conserver les secondes restantes exactes
                val exactRemaining = if (timer.targetEndTimestampMillis != null) {
                    (((timer.targetEndTimestampMillis - now) + 999L) / 1000L).toInt().coerceIn(0, 300)
                } else {
                    timer.remainingSeconds
                }
                current.copy(
                    cravingTimer = timer.copy(
                        remainingSeconds = exactRemaining,
                        isRunning = false,
                        targetEndTimestampMillis = null
                    )
                )
            } else {
                // Démarrage / Reprise : calculer l'horodatage cible exact
                val secondsToRun = if (timer.remainingSeconds <= 0) 300 else timer.remainingSeconds
                current.copy(
                    cravingTimer = timer.copy(
                        remainingSeconds = secondsToRun,
                        isRunning = true,
                        targetEndTimestampMillis = now + secondsToRun * 1000L
                    )
                )
            }
        }
    }

    fun resetCravingSession() {
        internalState.update { current ->
            current.copy(
                cravingTimer = CravingTimerState(
                    remainingSeconds = 300,
                    isRunning = false,
                    targetEndTimestampMillis = null,
                    selectedActivity = null,
                    sessionResolvedOutcome = null,
                    lastRecordedCigaretteId = null
                )
            )
        }
    }

    fun selectReplacementActivity(activity: String?) {
        internalState.update { current ->
            current.copy(
                cravingTimer = current.cravingTimer.copy(selectedActivity = activity)
            )
        }
    }

    fun markCravingPassed() {
        val currentTimer = internalState.value.cravingTimer
        if (currentTimer.sessionResolvedOutcome != null) return

        internalState.update { current ->
            current.copy(
                cravingTimer = current.cravingTimer.copy(
                    isRunning = false,
                    targetEndTimestampMillis = null,
                    sessionResolvedOutcome = "PASSED"
                ),
                statusBannerMessage = "Bravo ! Chaque envie traversée renforce votre liberté."
            )
        }

        viewModelScope.launch {
            repository.recordCravingPassed(
                nowMillis = internalState.value.nowMillis,
                replacementActivity = currentTimer.selectedActivity
            )
        }
    }

    fun markCravingSmokedOnce(
        cravingIntensity: Int? = null,
        contextCategory: String? = null,
        note: String? = null
    ) {
        val currentTimer = internalState.value.cravingTimer
        if (currentTimer.sessionResolvedOutcome != null) return

        internalState.update { current ->
            current.copy(
                cravingTimer = current.cravingTimer.copy(
                    isRunning = false,
                    targetEndTimestampMillis = null,
                    sessionResolvedOutcome = "SMOKED"
                )
            )
        }

        viewModelScope.launch {
            val (cig, eventId) = repository.recordCravingSmokedSingle(
                nowMillis = internalState.value.nowMillis,
                cravingIntensity = cravingIntensity,
                contextCategory = contextCategory,
                note = note,
                replacementActivity = currentTimer.selectedActivity
            )
            internalState.update { current ->
                current.copy(
                    cravingTimer = current.cravingTimer.copy(lastRecordedCigaretteId = cig.id),
                    pendingUndo = UndoAction.CravingSmoked(
                        cigaretteId = cig.id,
                        cravingEventId = eventId
                    )
                )
            }
        }
    }

    // --- Paramètres, Export / Import & Réinitialisation ---

    fun updateFinancialAndReferenceSettings(
        referenceDailyCigarettes: Int?,
        packPriceMillimes: Long,
        cigarettesPerPack: Int
    ) {
        viewModelScope.launch {
            repository.updateSettings(
                referenceDailyCigarettes = referenceDailyCigarettes,
                packPriceMillimes = packPriceMillimes,
                cigarettesPerPack = cigarettesPerPack
            )
            internalState.update {
                it.copy(statusBannerMessage = "Paramètres enregistrés.")
            }
        }
    }

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch {
            repository.setDarkThemeMode(mode)
        }
    }

    fun addCustomContext(newContext: String) {
        val trimmed = newContext.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomContext(trimmed)
            internalState.update {
                it.copy(statusBannerMessage = "Contexte « $trimmed » ajouté.")
            }
        }
    }

    fun renameCustomContext(oldContext: String, newContext: String) {
        val trimmed = newContext.trim()
        if (trimmed.isEmpty() || oldContext == trimmed) return
        viewModelScope.launch {
            repository.renameCustomContext(oldContext, trimmed)
            internalState.update {
                it.copy(statusBannerMessage = "Contexte renommé en « $trimmed ».")
            }
        }
    }

    fun deleteCustomContext(contextToRemove: String) {
        viewModelScope.launch {
            repository.removeCustomContext(contextToRemove)
            internalState.update {
                it.copy(statusBannerMessage = "Contexte « $contextToRemove » supprimé de la liste.")
            }
        }
    }

    fun resetCustomContextsToDefault() {
        viewModelScope.launch {
            repository.resetCustomContextsToDefault()
            internalState.update {
                it.copy(statusBannerMessage = "Contextes par défaut restaurés.")
            }
        }
    }

    suspend fun generateJsonExport(): String = repository.exportToJsonString()

    suspend fun generateCsvExport(): String = repository.exportToCsvString()

    fun importJsonData(jsonString: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.importFromJsonString(jsonString)
            result.fold(
                onSuccess = {
                    internalState.update {
                        it.copy(statusBannerMessage = "Données importées avec succès.")
                    }
                    onResult(true, "Données importées avec succès.")
                },
                onFailure = { err ->
                    val msg = "Erreur lors de l'import JSON : ${err.localizedMessage ?: "format invalide"}"
                    internalState.update {
                        it.copy(statusBannerMessage = msg)
                    }
                    onResult(false, msg)
                }
            )
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllUserData()
            internalState.update {
                InternalViewModelState(
                    nowMillis = System.currentTimeMillis(),
                    statusBannerMessage = "Toutes les données ont été supprimées."
                )
            }
        }
    }

    companion object {
        fun provideFactory(repository: CapZeroRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CapZeroViewModel(repository) as T
                }
            }
        }
    }
}
