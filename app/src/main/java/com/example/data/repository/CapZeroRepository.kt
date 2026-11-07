package com.example.data.repository

import android.content.Context
import com.example.data.local.CapZeroDao
import com.example.data.local.CigaretteLogEntity
import com.example.data.local.CravingEventEntity
import com.example.data.local.DailyPlanCapEntity
import com.example.data.local.DailyStatusEntity
import com.example.data.local.PackPurchaseEntity
import com.example.data.preferences.DarkThemeMode
import com.example.data.preferences.OnboardingMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserPreferencesState
import com.example.util.DateAndCurrencyUtils
import com.example.util.PlanStepPreview
import com.example.util.PresetContexts
import com.example.widget.CapZeroWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone

/**
 * Résultat d'une modification de cigarette indiquant si une journée précédemment confirmée
 * a été invalidée et nécessite une nouvelle confirmation de complétude.
 */
data class CigaretteMutationResult(
    val entity: CigaretteLogEntity,
    val invalidatedConfirmedDay: Boolean
)

/**
 * Repository central de Cap Zéro.
 * Orchestre le stockage Room, les préférences DataStore, le calcul des plans,
 * le suivi des paquets achetés et de l'inventaire de cigarettes,
 * la distinction entre journée en cours, journée passée non confirmée et journée complète confirmée,
 * la synchronisation du widget d'écran d'accueil, ainsi que l'export/import JSON & CSV.
 */
class CapZeroRepository(
    private val dao: CapZeroDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val appContext: Context? = null,
    private val timeZoneProvider: () -> TimeZone = { TimeZone.getDefault() }
) {

    private val widgetSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val cigarettesFlow: Flow<List<CigaretteLogEntity>> = dao.observeAllCigarettes()
    val planCapsFlow: Flow<List<DailyPlanCapEntity>> = dao.observeAllPlanCaps()
    val dailyStatusesFlow: Flow<List<DailyStatusEntity>> = dao.observeAllDailyStatuses()
    val cravingEventsFlow: Flow<List<CravingEventEntity>> = dao.observeAllCravingEvents()
    val packPurchasesFlow: Flow<List<PackPurchaseEntity>> = dao.observeAllPackPurchases()
    val preferencesFlow: Flow<UserPreferencesState> = preferencesRepository.preferencesFlow

    fun currentTimeZone(): TimeZone = timeZoneProvider()

    fun currentLocalEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
        return DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZoneProvider())
    }

    private fun syncHomeScreenWidgets(nowMillis: Long = System.currentTimeMillis()) {
        val ctx = appContext ?: return
        widgetSyncScope.launch {
            runCatching {
                CapZeroWidgetProvider.updateAllWidgetsFromDao(
                    context = ctx,
                    dao = dao,
                    nowMillis = nowMillis,
                    timeZone = timeZoneProvider()
                )
            }
        }
    }

    /**
     * Invalide le statut de complétude d'un jour donné s'il était déjà confirmé.
     * Retourne true si le jour était confirmé et nécessite désormais une nouvelle confirmation.
     */
    private suspend fun invalidateDayConfirmationIfConfirmed(localEpochDay: Long): Boolean {
        val statuses = dao.getAllDailyStatusesSnapshot()
        val wasConfirmed = statuses.any { it.localEpochDay == localEpochDay && it.isConfirmedComplete }
        if (wasConfirmed) {
            dao.deleteDailyStatus(localEpochDay)
        }
        return wasConfirmed
    }

    // --- Enregistrement, Modification et Annulation de Cigarettes ---

    suspend fun logCigaretteNow(
        nowMillis: Long = System.currentTimeMillis(),
        cravingIntensity: Int? = null,
        contextCategory: String? = null,
        note: String? = null
    ): CigaretteLogEntity {
        val localDay = DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZoneProvider())
        val entity = CigaretteLogEntity(
            timestampMillis = nowMillis,
            localEpochDay = localDay,
            cravingIntensity = cravingIntensity?.coerceIn(1, 5),
            contextCategory = contextCategory?.takeIf { it.isNotBlank() },
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )
        val generatedId = dao.insertCigarette(entity)
        // Toute nouvelle saisie sur un jour déjà confirmé demande une nouvelle confirmation de complétude
        invalidateDayConfirmationIfConfirmed(localDay)
        syncHomeScreenWidgets(nowMillis)
        return entity.copy(id = generatedId)
    }

    suspend fun addRetroactiveCigaretteWithInvalidation(
        timestampMillis: Long,
        cravingIntensity: Int?,
        contextCategory: String?,
        note: String?
    ): CigaretteMutationResult {
        val localDay = DateAndCurrencyUtils.toLocalEpochDay(timestampMillis, timeZoneProvider())
        val entity = CigaretteLogEntity(
            timestampMillis = timestampMillis,
            localEpochDay = localDay,
            cravingIntensity = cravingIntensity?.coerceIn(1, 5),
            contextCategory = contextCategory?.takeIf { it.isNotBlank() },
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )
        val generatedId = dao.insertCigarette(entity)
        val invalidated = invalidateDayConfirmationIfConfirmed(localDay)
        syncHomeScreenWidgets()
        return CigaretteMutationResult(
            entity = entity.copy(id = generatedId),
            invalidatedConfirmedDay = invalidated
        )
    }

    suspend fun addRetroactiveCigarette(
        timestampMillis: Long,
        cravingIntensity: Int?,
        contextCategory: String?,
        note: String?
    ): CigaretteLogEntity {
        return addRetroactiveCigaretteWithInvalidation(
            timestampMillis = timestampMillis,
            cravingIntensity = cravingIntensity,
            contextCategory = contextCategory,
            note = note
        ).entity
    }

    suspend fun updateCigaretteWithInvalidation(
        id: Long,
        timestampMillis: Long,
        cravingIntensity: Int?,
        contextCategory: String?,
        note: String?
    ): Boolean {
        val existing = dao.getCigaretteById(id)
        val newLocalDay = DateAndCurrencyUtils.toLocalEpochDay(timestampMillis, timeZoneProvider())
        val updated = CigaretteLogEntity(
            id = id,
            timestampMillis = timestampMillis,
            localEpochDay = newLocalDay,
            cravingIntensity = cravingIntensity?.coerceIn(1, 5),
            contextCategory = contextCategory?.takeIf { it.isNotBlank() },
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )
        dao.updateCigarette(updated)
        var invalidated = invalidateDayConfirmationIfConfirmed(newLocalDay)
        if (existing != null && existing.localEpochDay != newLocalDay) {
            invalidated = invalidateDayConfirmationIfConfirmed(existing.localEpochDay) || invalidated
        }
        syncHomeScreenWidgets()
        return invalidated
    }

    suspend fun updateCigarette(
        id: Long,
        timestampMillis: Long,
        cravingIntensity: Int?,
        contextCategory: String?,
        note: String?
    ) {
        updateCigaretteWithInvalidation(id, timestampMillis, cravingIntensity, contextCategory, note)
    }

    suspend fun deleteCigaretteWithInvalidation(id: Long): Pair<CigaretteLogEntity?, Boolean> {
        val existing = dao.getCigaretteById(id)
        var invalidated = false
        if (existing != null) {
            dao.deleteCigaretteById(id)
            invalidated = invalidateDayConfirmationIfConfirmed(existing.localEpochDay)
        }
        syncHomeScreenWidgets()
        return existing to invalidated
    }

    suspend fun deleteCigarette(id: Long): CigaretteLogEntity? {
        return deleteCigaretteWithInvalidation(id).first
    }

    suspend fun restoreCigarette(entity: CigaretteLogEntity) {
        dao.insertCigarette(entity)
        invalidateDayConfirmationIfConfirmed(entity.localEpochDay)
        syncHomeScreenWidgets()
    }

    // --- Achats de Paquets & Inventaire ---

    suspend fun addPackPurchase(
        timestampMillis: Long = System.currentTimeMillis(),
        packsCount: Int,
        cigarettesPerPack: Int,
        unitPriceMillimes: Long,
        note: String? = null
    ): PackPurchaseEntity {
        val safePacks = packsCount.coerceAtLeast(1)
        val safeCigsPerPack = cigarettesPerPack.coerceAtLeast(1)
        val safeUnitPrice = unitPriceMillimes.coerceAtLeast(0L)
        val localDay = DateAndCurrencyUtils.toLocalEpochDay(timestampMillis, timeZoneProvider())
        val entity = PackPurchaseEntity(
            timestampMillis = timestampMillis,
            localEpochDay = localDay,
            packsCount = safePacks,
            cigarettesPerPack = safeCigsPerPack,
            unitPriceMillimes = safeUnitPrice,
            totalPriceMillimes = safePacks.toLong() * safeUnitPrice,
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )
        val id = dao.insertPackPurchase(entity)
        return entity.copy(id = id)
    }

    suspend fun deletePackPurchase(id: Long): PackPurchaseEntity? {
        val existing = dao.getPackPurchaseById(id)
        if (existing != null) {
            dao.deletePackPurchaseById(id)
        }
        return existing
    }

    suspend fun restorePackPurchase(entity: PackPurchaseEntity) {
        dao.insertPackPurchase(entity)
    }

    // --- Confirmation explicite d'une journée complète (0 ou plusieurs cigarettes) ---
    // Ne modifie ni ne supprime jamais les cigarettes enregistrées sur cette journée.

    suspend fun setDayConfirmed(localEpochDay: Long, confirmed: Boolean) {
        if (confirmed) {
            dao.upsertDailyStatus(
                DailyStatusEntity(
                    localEpochDay = localEpochDay,
                    isConfirmedComplete = true
                )
            )
        } else {
            dao.deleteDailyStatus(localEpochDay)
        }
    }

    // --- Gestion des Envies (« Envie ») sans double enregistrement ---

    suspend fun recordCravingPassed(
        nowMillis: Long = System.currentTimeMillis(),
        replacementActivity: String? = null
    ): CravingEventEntity {
        val localDay = DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZoneProvider())
        val event = CravingEventEntity(
            timestampMillis = nowMillis,
            localEpochDay = localDay,
            outcome = "PASSED",
            replacementActivity = replacementActivity
        )
        val id = dao.insertCravingEvent(event)
        return event.copy(id = id)
    }

    /**
     * Enregistre qu'une envie a abouti à « J'ai fumé » en créant UNE SEULE entrée de cigarette
     * et l'événement d'envie associé.
     */
    suspend fun recordCravingSmokedSingle(
        nowMillis: Long = System.currentTimeMillis(),
        cravingIntensity: Int? = null,
        contextCategory: String? = null,
        note: String? = null,
        replacementActivity: String? = null
    ): Pair<CigaretteLogEntity, Long> {
        val localDay = DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZoneProvider())
        val event = CravingEventEntity(
            timestampMillis = nowMillis,
            localEpochDay = localDay,
            outcome = "SMOKED",
            replacementActivity = replacementActivity
        )
        val eventId = dao.insertCravingEvent(event)
        val cig = logCigaretteNow(
            nowMillis = nowMillis,
            cravingIntensity = cravingIntensity,
            contextCategory = contextCategory,
            note = note
        )
        return cig to eventId
    }

    suspend fun undoCravingSmoked(cigaretteId: Long, cravingEventId: Long) {
        dao.deleteCigaretteById(cigaretteId)
        dao.deleteCravingEventById(cravingEventId)
        syncHomeScreenWidgets()
    }

    // --- Onboarding & Plan de Réduction ---

    suspend fun setupDirectPlan(
        referenceDailyCigarettes: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        startEpochDay: Long,
        quitDateEpochDay: Long,
        validatedSteps: List<PlanStepPreview>
    ) {
        preferencesRepository.completeOnboardingDirect(
            referenceDailyCigarettes = referenceDailyCigarettes,
            packPriceMillimes = packPriceMillimes,
            cigarettesPerPack = cigarettesPerPack,
            planStartEpochDay = startEpochDay,
            quitDateEpochDay = quitDateEpochDay
        )
        val entities = validatedSteps.map {
            DailyPlanCapEntity(
                localEpochDay = it.localEpochDay,
                capCount = it.capCount,
                isManualOverride = it.isManualOverride
            )
        }
        dao.replaceFullPlan(entities)
        syncHomeScreenWidgets()
    }

    suspend fun setupObservationMode(
        observationTargetDays: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        quitDateEpochDay: Long
    ) {
        preferencesRepository.completeOnboardingObservation(
            observationTargetDays = observationTargetDays,
            packPriceMillimes = packPriceMillimes,
            cigarettesPerPack = cigarettesPerPack,
            quitDateEpochDay = quitDateEpochDay
        )
        dao.clearAllPlanCaps()
        syncHomeScreenWidgets()
    }

    suspend fun saveCustomPlanSteps(
        referenceDailyCigarettes: Int,
        startEpochDay: Long,
        quitDateEpochDay: Long,
        steps: List<PlanStepPreview>
    ) {
        preferencesRepository.updatePlanDatesAndReference(
            referenceDailyCigarettes = referenceDailyCigarettes,
            planStartEpochDay = startEpochDay,
            quitDateEpochDay = quitDateEpochDay
        )
        val entities = steps.map {
            DailyPlanCapEntity(
                localEpochDay = it.localEpochDay,
                capCount = it.capCount,
                isManualOverride = it.isManualOverride
            )
        }
        dao.replaceFullPlan(entities)
        syncHomeScreenWidgets()
    }

    suspend fun updateSinglePlanStepAndEnforceOrder(
        targetEpochDay: Long,
        newCap: Int
    ) {
        val currentEntities = dao.getAllPlanCapsSnapshot()
        if (currentEntities.isEmpty()) return
        val currentSteps = currentEntities.mapIndexed { idx, entity ->
            PlanStepPreview(
                localEpochDay = entity.localEpochDay,
                dayIndex = idx,
                capCount = entity.capCount,
                isManualOverride = entity.isManualOverride
            )
        }
        val adjusted = DateAndCurrencyUtils.adjustStepKeepingNonIncreasing(
            currentSteps = currentSteps,
            targetEpochDay = targetEpochDay,
            newCap = newCap
        )
        dao.replaceFullPlan(
            adjusted.map {
                DailyPlanCapEntity(
                    localEpochDay = it.localEpochDay,
                    capCount = it.capCount,
                    isManualOverride = it.isManualOverride
                )
            }
        )
        syncHomeScreenWidgets()
    }

    suspend fun updateSettings(
        referenceDailyCigarettes: Int?,
        packPriceMillimes: Long,
        cigarettesPerPack: Int
    ) {
        preferencesRepository.updateFinancialAndReferenceSettings(
            referenceDailyCigarettes = referenceDailyCigarettes,
            packPriceMillimes = packPriceMillimes,
            cigarettesPerPack = cigarettesPerPack
        )
    }

    suspend fun updateObservationTargetDays(targetDays: Int) {
        preferencesRepository.updateObservationTargetDays(targetDays)
    }

    suspend fun setManualInventoryStockOffset(offset: Int?) {
        preferencesRepository.setManualInventoryStockOffset(offset)
    }

    suspend fun addCustomContext(newContext: String): List<String> {
        return preferencesRepository.addCustomContext(newContext)
    }

    suspend fun renameCustomContext(oldContext: String, newContext: String): List<String> {
        val trimmedNew = newContext.trim()
        val updated = preferencesRepository.renameCustomContext(oldContext, trimmedNew)
        if (trimmedNew.isNotEmpty() && oldContext != trimmedNew) {
            dao.renameContextCategory(oldContext, trimmedNew)
        }
        return updated
    }

    suspend fun removeCustomContext(contextToRemove: String): List<String> {
        return preferencesRepository.removeCustomContext(contextToRemove)
    }

    suspend fun resetCustomContextsToDefault(): List<String> {
        return preferencesRepository.resetCustomContextsToDefault()
    }

    suspend fun setDarkThemeMode(mode: DarkThemeMode) {
        preferencesRepository.setDarkThemeMode(mode)
    }

    // --- Export / Import JSON et Export CSV ---

    suspend fun exportToJsonString(): String {
        val prefs = preferencesRepository.getSnapshot()
        val cigarettes = dao.getAllCigarettesSnapshot()
        val planCaps = dao.getAllPlanCapsSnapshot()
        val statuses = dao.getAllDailyStatusesSnapshot()
        val cravings = dao.getAllCravingEventsSnapshot()
        val packPurchases = dao.getAllPackPurchasesSnapshot()

        val root = JSONObject()
        root.put("app", "Cap Zéro")
        root.put("version", 2)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val prefsObj = JSONObject().apply {
            put("isOnboardingCompleted", prefs.isOnboardingCompleted)
            put("onboardingMode", prefs.onboardingMode.name)
            put("observationTargetDays", prefs.observationTargetDays)
            if (prefs.referenceDailyCigarettes != null) put("referenceDailyCigarettes", prefs.referenceDailyCigarettes)
            if (prefs.packPriceMillimes != null) put("packPriceMillimes", prefs.packPriceMillimes)
            if (prefs.cigarettesPerPack != null) put("cigarettesPerPack", prefs.cigarettesPerPack)
            if (prefs.planStartEpochDay != null) put("planStartEpochDay", prefs.planStartEpochDay)
            if (prefs.quitDateEpochDay != null) put("quitDateEpochDay", prefs.quitDateEpochDay)
            put("darkThemeMode", prefs.darkThemeMode.name)
            if (prefs.manualInventoryStockOffset != null) put("manualInventoryStockOffset", prefs.manualInventoryStockOffset)
            val contextsArray = JSONArray()
            prefs.customContexts.forEach { contextsArray.put(it) }
            put("customContexts", contextsArray)
        }
        root.put("preferences", prefsObj)

        val cigArray = JSONArray()
        cigarettes.forEach { cig ->
            val obj = JSONObject().apply {
                put("id", cig.id)
                put("timestampMillis", cig.timestampMillis)
                put("localEpochDay", cig.localEpochDay)
                if (cig.cravingIntensity != null) put("cravingIntensity", cig.cravingIntensity)
                if (cig.contextCategory != null) put("contextCategory", cig.contextCategory)
                if (cig.note != null) put("note", cig.note)
            }
            cigArray.put(obj)
        }
        root.put("cigarettes", cigArray)

        val planArray = JSONArray()
        planCaps.forEach { cap ->
            val obj = JSONObject().apply {
                put("localEpochDay", cap.localEpochDay)
                put("capCount", cap.capCount)
                put("isManualOverride", cap.isManualOverride)
            }
            planArray.put(obj)
        }
        root.put("planCaps", planArray)

        val statusArray = JSONArray()
        statuses.forEach { st ->
            val obj = JSONObject().apply {
                put("localEpochDay", st.localEpochDay)
                put("isConfirmedComplete", st.isConfirmedComplete)
            }
            statusArray.put(obj)
        }
        root.put("dailyStatuses", statusArray)

        val cravingArray = JSONArray()
        cravings.forEach { cr ->
            val obj = JSONObject().apply {
                put("id", cr.id)
                put("timestampMillis", cr.timestampMillis)
                put("localEpochDay", cr.localEpochDay)
                put("outcome", cr.outcome)
                if (cr.replacementActivity != null) put("replacementActivity", cr.replacementActivity)
            }
            cravingArray.put(obj)
        }
        root.put("cravingEvents", cravingArray)

        val purchasesArray = JSONArray()
        packPurchases.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("timestampMillis", p.timestampMillis)
                put("localEpochDay", p.localEpochDay)
                put("packsCount", p.packsCount)
                put("cigarettesPerPack", p.cigarettesPerPack)
                put("unitPriceMillimes", p.unitPriceMillimes)
                put("totalPriceMillimes", p.totalPriceMillimes)
                if (p.note != null) put("note", p.note)
            }
            purchasesArray.put(obj)
        }
        root.put("packPurchases", purchasesArray)

        return root.toString(2)
    }

    suspend fun importFromJsonString(jsonContent: String): Result<Unit> {
        return runCatching {
            val root = JSONObject(jsonContent)
            val prefsObj = root.optJSONObject("preferences")

            val cigarettesList = mutableListOf<CigaretteLogEntity>()
            val cigArray = root.optJSONArray("cigarettes") ?: JSONArray()
            for (i in 0 until cigArray.length()) {
                val item = cigArray.getJSONObject(i)
                val ts = item.getLong("timestampMillis")
                val localDay = if (item.has("localEpochDay")) {
                    item.getLong("localEpochDay")
                } else {
                    DateAndCurrencyUtils.toLocalEpochDay(ts, timeZoneProvider())
                }
                cigarettesList.add(
                    CigaretteLogEntity(
                        id = item.optLong("id", 0L),
                        timestampMillis = ts,
                        localEpochDay = localDay,
                        cravingIntensity = if (item.has("cravingIntensity") && !item.isNull("cravingIntensity")) item.getInt("cravingIntensity").coerceIn(1, 5) else null,
                        contextCategory = item.optString("contextCategory", "").takeIf { it.isNotBlank() },
                        note = item.optString("note", "").takeIf { it.isNotBlank() }
                    )
                )
            }

            val planList = mutableListOf<DailyPlanCapEntity>()
            val planArray = root.optJSONArray("planCaps") ?: JSONArray()
            for (i in 0 until planArray.length()) {
                val item = planArray.getJSONObject(i)
                planList.add(
                    DailyPlanCapEntity(
                        localEpochDay = item.getLong("localEpochDay"),
                        capCount = item.getInt("capCount").coerceAtLeast(0),
                        isManualOverride = item.optBoolean("isManualOverride", false)
                    )
                )
            }

            val statusList = mutableListOf<DailyStatusEntity>()
            val statusArray = root.optJSONArray("dailyStatuses") ?: JSONArray()
            for (i in 0 until statusArray.length()) {
                val item = statusArray.getJSONObject(i)
                statusList.add(
                    DailyStatusEntity(
                        localEpochDay = item.getLong("localEpochDay"),
                        isConfirmedComplete = item.optBoolean("isConfirmedComplete", true)
                    )
                )
            }

            val cravingList = mutableListOf<CravingEventEntity>()
            val cravingArray = root.optJSONArray("cravingEvents") ?: JSONArray()
            for (i in 0 until cravingArray.length()) {
                val item = cravingArray.getJSONObject(i)
                val ts = item.getLong("timestampMillis")
                cravingList.add(
                    CravingEventEntity(
                        id = item.optLong("id", 0L),
                        timestampMillis = ts,
                        localEpochDay = item.optLong("localEpochDay", DateAndCurrencyUtils.toLocalEpochDay(ts, timeZoneProvider())),
                        outcome = item.optString("outcome", "PASSED"),
                        replacementActivity = item.optString("replacementActivity", "").takeIf { it.isNotBlank() }
                    )
                )
            }

            val purchasesList = mutableListOf<PackPurchaseEntity>()
            val purchasesArray = root.optJSONArray("packPurchases") ?: JSONArray()
            for (i in 0 until purchasesArray.length()) {
                val item = purchasesArray.getJSONObject(i)
                val ts = item.getLong("timestampMillis")
                val packsCount = item.optInt("packsCount", 1).coerceAtLeast(1)
                val cigsPerPack = item.optInt("cigarettesPerPack", 20).coerceAtLeast(1)
                val unitMillimes = item.optLong("unitPriceMillimes", 0L).coerceAtLeast(0L)
                purchasesList.add(
                    PackPurchaseEntity(
                        id = item.optLong("id", 0L),
                        timestampMillis = ts,
                        localEpochDay = item.optLong("localEpochDay", DateAndCurrencyUtils.toLocalEpochDay(ts, timeZoneProvider())),
                        packsCount = packsCount,
                        cigarettesPerPack = cigsPerPack,
                        unitPriceMillimes = unitMillimes,
                        totalPriceMillimes = item.optLong("totalPriceMillimes", packsCount.toLong() * unitMillimes),
                        note = item.optString("note", "").takeIf { it.isNotBlank() }
                    )
                )
            }

            dao.clearAllTables()
            if (cigarettesList.isNotEmpty()) dao.insertCigarettes(cigarettesList)
            if (planList.isNotEmpty()) dao.upsertPlanCaps(planList)
            if (statusList.isNotEmpty()) dao.upsertDailyStatuses(statusList)
            if (cravingList.isNotEmpty()) dao.insertCravingEvents(cravingList)
            if (purchasesList.isNotEmpty()) dao.insertPackPurchases(purchasesList)

            if (prefsObj != null) {
                val mode = runCatching {
                    OnboardingMode.valueOf(prefsObj.optString("onboardingMode", OnboardingMode.DIRECT.name))
                }.getOrDefault(OnboardingMode.DIRECT)
                val theme = runCatching {
                    DarkThemeMode.valueOf(prefsObj.optString("darkThemeMode", DarkThemeMode.LIGHT.name))
                }.getOrDefault(DarkThemeMode.LIGHT)

                val restoredContexts = if (prefsObj.has("customContexts")) {
                    val arr = prefsObj.optJSONArray("customContexts") ?: JSONArray()
                    val parsed = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i, "").trim()
                        if (item.isNotEmpty() && parsed.none { it.equals(item, ignoreCase = true) }) {
                            parsed.add(item)
                        }
                    }
                    parsed
                } else {
                    PresetContexts.ALL
                }

                val restoredPrefs = UserPreferencesState(
                    isOnboardingCompleted = prefsObj.optBoolean("isOnboardingCompleted", true),
                    onboardingMode = mode,
                    observationTargetDays = prefsObj.optInt("observationTargetDays", 3).coerceIn(1, 14),
                    referenceDailyCigarettes = if (prefsObj.has("referenceDailyCigarettes")) prefsObj.getInt("referenceDailyCigarettes") else null,
                    packPriceMillimes = if (prefsObj.has("packPriceMillimes")) prefsObj.getLong("packPriceMillimes") else null,
                    cigarettesPerPack = if (prefsObj.has("cigarettesPerPack")) prefsObj.getInt("cigarettesPerPack") else null,
                    planStartEpochDay = if (prefsObj.has("planStartEpochDay")) prefsObj.getLong("planStartEpochDay") else null,
                    quitDateEpochDay = if (prefsObj.has("quitDateEpochDay")) prefsObj.getLong("quitDateEpochDay") else null,
                    darkThemeMode = theme,
                    manualInventoryStockOffset = if (prefsObj.has("manualInventoryStockOffset")) prefsObj.getInt("manualInventoryStockOffset") else null,
                    customContexts = restoredContexts
                )
                preferencesRepository.restoreFromBackup(restoredPrefs)
            }
            syncHomeScreenWidgets()
        }
    }

    suspend fun exportToCsvString(): String {
        val cigarettes = dao.getAllCigarettesSnapshot()
        val planCaps = dao.getAllPlanCapsSnapshot().associateBy { it.localEpochDay }
        val tz = timeZoneProvider()

        val sb = StringBuilder()
        sb.appendLine("ID;Date_Locale;Heure_Locale;Horodatage_Ms;Intensite_Envie_1_5;Contexte;Note;Plafond_Du_Jour")
        cigarettes.forEach { cig ->
            val dateStr = DateAndCurrencyUtils.formatShortDate(cig.localEpochDay)
            val timeStr = DateAndCurrencyUtils.formatTime(cig.timestampMillis, tz)
            val intensity = cig.cravingIntensity?.toString() ?: ""
            val context = (cig.contextCategory ?: "").replace(";", ",")
            val note = (cig.note ?: "").replace(";", ",").replace("\n", " ")
            val cap = planCaps[cig.localEpochDay]?.capCount?.toString() ?: ""
            sb.appendLine("${cig.id};$dateStr;$timeStr;${cig.timestampMillis};$intensity;$context;$note;$cap")
        }
        return sb.toString()
    }

    suspend fun deleteAllUserData() {
        dao.clearAllTables()
        preferencesRepository.clearAllPreferences()
        syncHomeScreenWidgets()
    }
}
