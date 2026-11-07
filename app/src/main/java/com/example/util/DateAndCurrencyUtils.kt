package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max

/**
 * Contextes prédéfinis validés pour le journal de Cap Zéro.
 */
object PresetContexts {
    const val COFFEE_MEAL = "Café ou après un repas"
    const val STRESS_WORK = "Stress, pause travail ou ennui"
    const val SOCIAL_OUTING = "Moment social ou sortie"
    const val WAKE_SLEEP = "Au réveil ou avant de dormir"
    const val COMMUTE_HABIT = "En déplacement ou habitude automatique"

    val ALL = listOf(
        COFFEE_MEAL,
        STRESS_WORK,
        SOCIAL_OUTING,
        WAKE_SLEEP,
        COMMUTE_HABIT
    )
}

/**
 * Étape quotidienne d'un plan de réduction.
 */
data class PlanStepPreview(
    val localEpochDay: Long,
    val dayIndex: Int,
    val capCount: Int,
    val isManualOverride: Boolean = false
)

/**
 * Utilitaires de gestion des dates locales (selon le fuseau horaire),
 * du formatage monétaire en TND (3 décimales) et du calcul linéaire du plan.
 * Optimisé avec mise en cache mémoire et ThreadLocal pour éliminer les allocations répétées sur mobile.
 */
object DateAndCurrencyUtils {

    private const val MILLIS_PER_DAY = 86_400_000L
    private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

    // Caches mémoire légers pour éviter les réallocations de Calendar/SimpleDateFormat lors du défilement
    private val ymdCache = ConcurrentHashMap<Long, Triple<Int, Int, Int>>()
    private val shortDateCache = ConcurrentHashMap<Long, String>()
    private val dayAndMonthCache = ConcurrentHashMap<Long, String>()
    private val fullDateCache = ConcurrentHashMap<Long, String>()
    private val mediumDateCache = ConcurrentHashMap<Long, String>()
    private val chartDateCache = ConcurrentHashMap<Long, String>()
    private val friendlyBaseDayCache = ConcurrentHashMap<Long, String>()

    private val utcCalendarThreadLocal = object : ThreadLocal<Calendar>() {
        override fun initialValue(): Calendar = Calendar.getInstance(UTC_TIME_ZONE)
    }

    private val dayAndMonthFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("d MMMM", Locale.FRANCE).apply { timeZone = UTC_TIME_ZONE }
    }

    private val fullDateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("d MMMM yyyy", Locale.FRANCE).apply { timeZone = UTC_TIME_ZONE }
    }

    private val mediumDateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("d MMM yyyy", Locale.FRANCE).apply { timeZone = UTC_TIME_ZONE }
    }

    private val friendlyDayFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("EEEE d MMM yyyy", Locale.FRANCE).apply { timeZone = UTC_TIME_ZONE }
    }

    private val timeFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("HH:mm", Locale.FRANCE)
    }

    fun toLocalEpochDay(
        timestampMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val localCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestampMillis
        }
        val year = localCal.get(Calendar.YEAR)
        val month = localCal.get(Calendar.MONTH)
        val day = localCal.get(Calendar.DAY_OF_MONTH)

        val utcCal = utcCalendarThreadLocal.get()!!.apply {
            clear()
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / MILLIS_PER_DAY
    }

    fun ymdToLocalEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val utcCal = utcCalendarThreadLocal.get()!!.apply {
            clear()
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / MILLIS_PER_DAY
    }

    fun localEpochDayToYmd(localEpochDay: Long): Triple<Int, Int, Int> {
        return ymdCache.getOrPut(localEpochDay) {
            val utcCal = utcCalendarThreadLocal.get()!!.apply {
                timeInMillis = localEpochDay * MILLIS_PER_DAY
            }
            Triple(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    fun buildLocalTimestamp(
        localEpochDay: Long,
        hourOfDay: Int,
        minute: Int,
        second: Int = 0,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val (year, month, day) = localEpochDayToYmd(localEpochDay)
        val localCal = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day, hourOfDay.coerceIn(0, 23), minute.coerceIn(0, 59), second.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    fun extractHourAndMinute(
        timestampMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Int, Int> {
        val localCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestampMillis
        }
        return localCal.get(Calendar.HOUR_OF_DAY) to localCal.get(Calendar.MINUTE)
    }

    /**
     * Formatage monétaire en Dinars Tunisiens (TND) avec exactement 3 décimales.
     */
    fun formatTnd(amountTnd: Double): String {
        return String.format(Locale.FRANCE, "%.3f TND", amountTnd)
    }

    fun formatMillimesAsTnd(millimes: Long): String {
        val tnd = millimes.toDouble() / 1000.0
        return formatTnd(tnd)
    }

    fun formatMillimesToTnd(millimes: Long): String = formatMillimesAsTnd(millimes)

    fun parseTndToMillimes(input: String): Long? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value < 0.0) return null
        return Math.round(value * 1000.0)
    }

    fun formatTime(
        timestampMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val sdf = timeFormatThreadLocal.get()!!
        sdf.timeZone = timeZone
        return sdf.format(Date(timestampMillis))
    }

    fun formatShortDate(localEpochDay: Long): String {
        return shortDateCache.getOrPut(localEpochDay) {
            val (year, month, day) = localEpochDayToYmd(localEpochDay)
            String.format(Locale.FRANCE, "%02d/%02d/%04d", day, month + 1, year)
        }
    }

    /**
     * Formate une date sous la forme « 4 novembre » (pour le badge de l'écran Accueil).
     */
    fun formatDayAndMonthFrench(localEpochDay: Long): String {
        return dayAndMonthCache.getOrPut(localEpochDay) {
            dayAndMonthFormatThreadLocal.get()!!.format(Date(localEpochDay * MILLIS_PER_DAY))
        }
    }

    /**
     * Formate une date complète sous la forme « 3 janvier 2027 » (pour l'écran Plan).
     */
    fun formatFullDateFrench(localEpochDay: Long): String {
        return fullDateCache.getOrPut(localEpochDay) {
            fullDateFormatThreadLocal.get()!!.format(Date(localEpochDay * MILLIS_PER_DAY))
        }
    }

    /**
     * Formate une date moyenne sous la forme « 3 janv. 2027 ».
     */
    fun formatMediumDateFrench(localEpochDay: Long): String {
        return mediumDateCache.getOrPut(localEpochDay) {
            mediumDateFormatThreadLocal.get()!!.format(Date(localEpochDay * MILLIS_PER_DAY))
        }
    }

    fun formatChartDate(localEpochDay: Long): String {
        return chartDateCache.getOrPut(localEpochDay) {
            val (_, month, day) = localEpochDayToYmd(localEpochDay)
            String.format(Locale.FRANCE, "%02d/%02d", day, month + 1)
        }
    }

    fun formatFriendlyDay(localEpochDay: Long, todayEpochDay: Long): String {
        val formatted = friendlyBaseDayCache.getOrPut(localEpochDay) {
            friendlyDayFormatThreadLocal.get()!!
                .format(Date(localEpochDay * MILLIS_PER_DAY))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }
        }
        return when (localEpochDay) {
            todayEpochDay -> "Aujourd'hui · $formatted"
            todayEpochDay - 1L -> "Hier · $formatted"
            todayEpochDay + 1L -> "Demain · $formatted"
            else -> formatted
        }
    }

    /**
     * Formate le temps écoulé de façon concise et dominante comme sur la maquette
     * (ex. « Il y a 1 h 20 », « Il y a 25 min », « Il y a 2 j 4 h » ou « — » si aucune cigarette).
     */
    fun formatDurationSince(lastCigaretteMillis: Long?, nowMillis: Long): String {
        if (lastCigaretteMillis == null) {
            return "—"
        }
        val diffMillis = max(0L, nowMillis - lastCigaretteMillis)
        val totalSeconds = diffMillis / 1000L
        val days = totalSeconds / 86400L
        val hours = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L

        return when {
            days > 0 -> "Il y a ${days} j ${hours} h"
            hours > 0 -> String.format(Locale.FRANCE, "Il y a %d h %02d", hours, minutes)
            minutes > 0 -> "Il y a $minutes min"
            else -> "À l'instant"
        }
    }

    fun generateLinearReductionPlan(
        startEpochDay: Long,
        quitEpochDay: Long,
        referenceDailyCigarettes: Int
    ): List<PlanStepPreview> {
        val safeReference = referenceDailyCigarettes.coerceAtLeast(0)
        if (quitEpochDay <= startEpochDay) {
            return listOf(
                PlanStepPreview(
                    localEpochDay = startEpochDay,
                    dayIndex = 0,
                    capCount = 0
                )
            )
        }

        val totalDays = (quitEpochDay - startEpochDay).toInt()
        val steps = mutableListOf<PlanStepPreview>()
        var previousCap = safeReference

        for (i in 0..totalDays) {
            val day = startEpochDay + i
            val rawCap = if (i == totalDays) {
                0
            } else {
                val fractionRemaining = (totalDays - i).toDouble() / totalDays.toDouble()
                ceil(safeReference.toDouble() * fractionRemaining).toInt()
            }
            val nonIncreasingCap = rawCap.coerceIn(0, previousCap)
            previousCap = nonIncreasingCap
            steps.add(
                PlanStepPreview(
                    localEpochDay = day,
                    dayIndex = i,
                    capCount = nonIncreasingCap,
                    isManualOverride = false
                )
            )
        }
        return steps
    }

    fun adjustStepKeepingNonIncreasing(
        currentSteps: List<PlanStepPreview>,
        targetEpochDay: Long,
        newCap: Int
    ): List<PlanStepPreview> {
        if (currentSteps.isEmpty()) return emptyList()
        val sorted = currentSteps.sortedBy { it.localEpochDay }.toMutableList()
        val lastIndex = sorted.lastIndex
        val targetIndex = sorted.indexOfFirst { it.localEpochDay == targetEpochDay }
        if (targetIndex == -1) return sorted

        if (targetIndex == lastIndex) {
            sorted[lastIndex] = sorted[lastIndex].copy(capCount = 0, isManualOverride = true)
            return sorted
        }

        val sanitizedCap = newCap.coerceAtLeast(0)
        sorted[targetIndex] = sorted[targetIndex].copy(
            capCount = sanitizedCap,
            isManualOverride = true
        )

        for (i in (targetIndex - 1) downTo 0) {
            if (sorted[i].capCount < sorted[i + 1].capCount) {
                sorted[i] = sorted[i].copy(capCount = sorted[i + 1].capCount)
            }
        }
        for (i in (targetIndex + 1) until lastIndex) {
            if (sorted[i].capCount > sorted[i - 1].capCount) {
                sorted[i] = sorted[i].copy(capCount = sorted[i - 1].capCount)
            }
        }
        sorted[lastIndex] = sorted[lastIndex].copy(capCount = 0)
        return sorted
    }
}
