package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.PresetContexts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.capZeroDataStore: DataStore<Preferences> by preferencesDataStore(name = "cap_zero_preferences")

enum class OnboardingMode {
    DIRECT,
    OBSERVATION
}

enum class DarkThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferencesState(
    val isOnboardingCompleted: Boolean = false,
    val onboardingMode: OnboardingMode = OnboardingMode.DIRECT,
    val observationTargetDays: Int = 3,
    val referenceDailyCigarettes: Int? = null,
    val packPriceMillimes: Long? = null, // 1 TND = 1000 millimes (3 décimales exactes)
    val cigarettesPerPack: Int? = null,
    val planStartEpochDay: Long? = null,
    val quitDateEpochDay: Long? = null,
    val darkThemeMode: DarkThemeMode = DarkThemeMode.LIGHT,
    val manualInventoryStockOffset: Int? = null,
    val customContexts: List<String> = PresetContexts.ALL
) {
    /**
     * Coût d'une cigarette en TND (ou null si non renseigné ou invalide).
     */
    val singleCigaretteCostTnd: Double?
        get() {
            val priceMillimes = packPriceMillimes ?: return null
            val perPack = cigarettesPerPack ?: return null
            if (perPack <= 0) return null
            return (priceMillimes.toDouble() / 1000.0) / perPack.toDouble()
        }
}

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val ONBOARDING_MODE = stringPreferencesKey("onboarding_mode")
        val OBSERVATION_TARGET_DAYS = intPreferencesKey("observation_target_days")
        val REFERENCE_DAILY_CIGARETTES = intPreferencesKey("reference_daily_cigarettes")
        val PACK_PRICE_MILLIMES = longPreferencesKey("pack_price_millimes")
        val CIGARETTES_PER_PACK = intPreferencesKey("cigarettes_per_pack")
        val PLAN_START_EPOCH_DAY = longPreferencesKey("plan_start_epoch_day")
        val QUIT_DATE_EPOCH_DAY = longPreferencesKey("quit_date_epoch_day")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val MANUAL_INVENTORY_STOCK_OFFSET = intPreferencesKey("manual_inventory_stock_offset")
        val CUSTOM_CONTEXTS_JSON = stringPreferencesKey("custom_contexts_json")
    }

    private fun parseCustomContextsJson(rawJson: String?): List<String> {
        if (rawJson == null) return PresetContexts.ALL
        return runCatching {
            val arr = JSONArray(rawJson)
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "").trim()
                if (item.isNotEmpty() && result.none { it.equals(item, ignoreCase = true) }) {
                    result.add(item)
                }
            }
            result
        }.getOrDefault(PresetContexts.ALL)
    }

    private fun serializeCustomContextsJson(contexts: List<String>): String {
        val arr = JSONArray()
        contexts.forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isNotEmpty()) {
                arr.put(trimmed)
            }
        }
        return arr.toString()
    }

    val preferencesFlow: Flow<UserPreferencesState> = dataStore.data.map { prefs ->
        val modeStr = prefs[Keys.ONBOARDING_MODE] ?: OnboardingMode.DIRECT.name
        val mode = runCatching { OnboardingMode.valueOf(modeStr) }.getOrDefault(OnboardingMode.DIRECT)
        val themeStr = prefs[Keys.DARK_THEME_MODE] ?: DarkThemeMode.LIGHT.name
        val parsedTheme = runCatching { DarkThemeMode.valueOf(themeStr) }.getOrDefault(DarkThemeMode.LIGHT)
        val themeMode = if (parsedTheme == DarkThemeMode.SYSTEM) DarkThemeMode.LIGHT else parsedTheme
        val customContexts = parseCustomContextsJson(prefs[Keys.CUSTOM_CONTEXTS_JSON])

        UserPreferencesState(
            isOnboardingCompleted = prefs[Keys.IS_ONBOARDING_COMPLETED] ?: false,
            onboardingMode = mode,
            observationTargetDays = (prefs[Keys.OBSERVATION_TARGET_DAYS] ?: 3).coerceIn(1, 14),
            referenceDailyCigarettes = prefs[Keys.REFERENCE_DAILY_CIGARETTES],
            packPriceMillimes = prefs[Keys.PACK_PRICE_MILLIMES],
            cigarettesPerPack = prefs[Keys.CIGARETTES_PER_PACK],
            planStartEpochDay = prefs[Keys.PLAN_START_EPOCH_DAY],
            quitDateEpochDay = prefs[Keys.QUIT_DATE_EPOCH_DAY],
            darkThemeMode = themeMode,
            manualInventoryStockOffset = prefs[Keys.MANUAL_INVENTORY_STOCK_OFFSET],
            customContexts = customContexts
        )
    }

    suspend fun getSnapshot(): UserPreferencesState = preferencesFlow.first()

    suspend fun completeOnboardingDirect(
        referenceDailyCigarettes: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        planStartEpochDay: Long,
        quitDateEpochDay: Long
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_ONBOARDING_COMPLETED] = true
            prefs[Keys.ONBOARDING_MODE] = OnboardingMode.DIRECT.name
            prefs[Keys.REFERENCE_DAILY_CIGARETTES] = referenceDailyCigarettes.coerceAtLeast(0)
            prefs[Keys.PACK_PRICE_MILLIMES] = packPriceMillimes.coerceAtLeast(0L)
            prefs[Keys.CIGARETTES_PER_PACK] = cigarettesPerPack.coerceAtLeast(1)
            prefs[Keys.PLAN_START_EPOCH_DAY] = planStartEpochDay
            prefs[Keys.QUIT_DATE_EPOCH_DAY] = quitDateEpochDay
        }
    }

    suspend fun completeOnboardingObservation(
        observationTargetDays: Int,
        packPriceMillimes: Long,
        cigarettesPerPack: Int,
        quitDateEpochDay: Long
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_ONBOARDING_COMPLETED] = true
            prefs[Keys.ONBOARDING_MODE] = OnboardingMode.OBSERVATION.name
            prefs[Keys.OBSERVATION_TARGET_DAYS] = observationTargetDays.coerceIn(1, 14)
            prefs.remove(Keys.REFERENCE_DAILY_CIGARETTES)
            prefs[Keys.PACK_PRICE_MILLIMES] = packPriceMillimes.coerceAtLeast(0L)
            prefs[Keys.CIGARETTES_PER_PACK] = cigarettesPerPack.coerceAtLeast(1)
            prefs.remove(Keys.PLAN_START_EPOCH_DAY)
            prefs[Keys.QUIT_DATE_EPOCH_DAY] = quitDateEpochDay
        }
    }

    suspend fun updateObservationTargetDays(observationTargetDays: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.OBSERVATION_TARGET_DAYS] = observationTargetDays.coerceIn(1, 14)
        }
    }

    suspend fun setManualInventoryStockOffset(offset: Int?) {
        dataStore.edit { prefs ->
            if (offset != null) {
                prefs[Keys.MANUAL_INVENTORY_STOCK_OFFSET] = offset
            } else {
                prefs.remove(Keys.MANUAL_INVENTORY_STOCK_OFFSET)
            }
        }
    }

    suspend fun setCustomContexts(contexts: List<String>) {
        val sanitized = mutableListOf<String>()
        contexts.forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isNotEmpty() && sanitized.none { it.equals(trimmed, ignoreCase = true) }) {
                sanitized.add(trimmed)
            }
        }
        dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_CONTEXTS_JSON] = serializeCustomContextsJson(sanitized)
        }
    }

    suspend fun addCustomContext(newContext: String): List<String> {
        val trimmed = newContext.trim()
        if (trimmed.isEmpty()) return getSnapshot().customContexts
        var updatedList: List<String> = emptyList()
        dataStore.edit { prefs ->
            val current = parseCustomContextsJson(prefs[Keys.CUSTOM_CONTEXTS_JSON]).toMutableList()
            if (current.none { it.equals(trimmed, ignoreCase = true) }) {
                current.add(trimmed)
            }
            updatedList = current
            prefs[Keys.CUSTOM_CONTEXTS_JSON] = serializeCustomContextsJson(current)
        }
        return updatedList
    }

    suspend fun renameCustomContext(oldContext: String, newContext: String): List<String> {
        val trimmedNew = newContext.trim()
        if (trimmedNew.isEmpty()) return getSnapshot().customContexts
        var updatedList: List<String> = emptyList()
        dataStore.edit { prefs ->
            val current = parseCustomContextsJson(prefs[Keys.CUSTOM_CONTEXTS_JSON]).toMutableList()
            val targetIdx = current.indexOfFirst { it == oldContext }
            if (targetIdx != -1) {
                val duplicateExists = current.withIndex().any { (idx, item) ->
                    idx != targetIdx && item.equals(trimmedNew, ignoreCase = true)
                }
                if (duplicateExists) {
                    current.removeAt(targetIdx)
                } else {
                    current[targetIdx] = trimmedNew
                }
            }
            updatedList = current
            prefs[Keys.CUSTOM_CONTEXTS_JSON] = serializeCustomContextsJson(current)
        }
        return updatedList
    }

    suspend fun removeCustomContext(contextToRemove: String): List<String> {
        var updatedList: List<String> = emptyList()
        dataStore.edit { prefs ->
            val current = parseCustomContextsJson(prefs[Keys.CUSTOM_CONTEXTS_JSON]).toMutableList()
            current.removeAll { it == contextToRemove }
            updatedList = current
            prefs[Keys.CUSTOM_CONTEXTS_JSON] = serializeCustomContextsJson(current)
        }
        return updatedList
    }

    suspend fun resetCustomContextsToDefault(): List<String> {
        dataStore.edit { prefs ->
            prefs.remove(Keys.CUSTOM_CONTEXTS_JSON)
        }
        return PresetContexts.ALL
    }

    suspend fun updateFinancialAndReferenceSettings(
        referenceDailyCigarettes: Int?,
        packPriceMillimes: Long,
        cigarettesPerPack: Int
    ) {
        dataStore.edit { prefs ->
            if (referenceDailyCigarettes != null) {
                prefs[Keys.REFERENCE_DAILY_CIGARETTES] = referenceDailyCigarettes.coerceAtLeast(0)
            }
            prefs[Keys.PACK_PRICE_MILLIMES] = packPriceMillimes.coerceAtLeast(0L)
            prefs[Keys.CIGARETTES_PER_PACK] = cigarettesPerPack.coerceAtLeast(1)
        }
    }

    suspend fun updatePlanDatesAndReference(
        referenceDailyCigarettes: Int,
        planStartEpochDay: Long,
        quitDateEpochDay: Long
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_MODE] = OnboardingMode.DIRECT.name
            prefs[Keys.REFERENCE_DAILY_CIGARETTES] = referenceDailyCigarettes.coerceAtLeast(0)
            prefs[Keys.PLAN_START_EPOCH_DAY] = planStartEpochDay
            prefs[Keys.QUIT_DATE_EPOCH_DAY] = quitDateEpochDay
        }
    }

    suspend fun setDarkThemeMode(mode: DarkThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME_MODE] = mode.name
        }
    }

    suspend fun restoreFromBackup(state: UserPreferencesState) {
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.IS_ONBOARDING_COMPLETED] = state.isOnboardingCompleted
            prefs[Keys.ONBOARDING_MODE] = state.onboardingMode.name
            prefs[Keys.OBSERVATION_TARGET_DAYS] = state.observationTargetDays.coerceIn(1, 14)
            state.referenceDailyCigarettes?.let { prefs[Keys.REFERENCE_DAILY_CIGARETTES] = it }
            state.packPriceMillimes?.let { prefs[Keys.PACK_PRICE_MILLIMES] = it }
            state.cigarettesPerPack?.let { prefs[Keys.CIGARETTES_PER_PACK] = it }
            state.planStartEpochDay?.let { prefs[Keys.PLAN_START_EPOCH_DAY] = it }
            state.quitDateEpochDay?.let { prefs[Keys.QUIT_DATE_EPOCH_DAY] = it }
            prefs[Keys.DARK_THEME_MODE] = state.darkThemeMode.name
            state.manualInventoryStockOffset?.let { prefs[Keys.MANUAL_INVENTORY_STOCK_OFFSET] = it }
            prefs[Keys.CUSTOM_CONTEXTS_JSON] = serializeCustomContextsJson(state.customContexts)
        }
    }

    suspend fun clearAllPreferences() {
        dataStore.edit { it.clear() }
    }
}
