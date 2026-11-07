package com.example

import com.example.util.DateAndCurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ExampleUnitTest {

    @Test
    fun `linear reduction plan is non-increasing and reaches zero at quit date`() {
        val startDay = 20000L
        val quitDay = 20010L // 10 jours d'écart
        val reference = 15

        val plan = DateAndCurrencyUtils.generateLinearReductionPlan(
            startEpochDay = startDay,
            quitEpochDay = quitDay,
            referenceDailyCigarettes = reference
        )

        assertEquals(11, plan.size)
        assertEquals(15, plan.first().capCount)
        assertEquals(0, plan.last().capCount)

        // Vérifier que les plafonds sont non croissants
        for (i in 1 until plan.size) {
            assertTrue(
                "Le plafond du jour $i (${plan[i].capCount}) doit être <= au jour précédent (${plan[i - 1].capCount})",
                plan[i].capCount <= plan[i - 1].capCount
            )
        }
    }

    @Test
    fun `manual plan step adjustment preserves non-increasing order and zero at quit date`() {
        val startDay = 20000L
        val quitDay = 20005L
        val initial = DateAndCurrencyUtils.generateLinearReductionPlan(
            startEpochDay = startDay,
            quitEpochDay = quitDay,
            referenceDailyCigarettes = 10
        )

        // Abaisser une étape intermédiaire à 2
        val adjusted = DateAndCurrencyUtils.adjustStepKeepingNonIncreasing(
            currentSteps = initial,
            targetEpochDay = startDay + 2L,
            newCap = 2
        )

        assertEquals(2, adjusted[2].capCount)
        assertEquals(0, adjusted.last().capCount)
        for (i in 1 until adjusted.size) {
            assertTrue(adjusted[i].capCount <= adjusted[i - 1].capCount)
        }
    }

    @Test
    fun `tnd currency formatting displays three decimals`() {
        assertEquals("10,500 TND", DateAndCurrencyUtils.formatMillimesAsTnd(10500L))
        assertEquals("0,525 TND", DateAndCurrencyUtils.formatTnd(0.525))
        assertEquals(10500L, DateAndCurrencyUtils.parseTndToMillimes("10,500"))
        assertEquals(10500L, DateAndCurrencyUtils.parseTndToMillimes("10.5"))
    }

    @Test
    fun `timezone local epoch day handles day boundary accurately`() {
        val tunisTz = TimeZone.getTimeZone("Africa/Tunis") // UTC+1
        // 2026-11-03 23:30:00 UTC -> 2026-11-04 00:30:00 à Tunis
        val utcCal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, java.util.Calendar.NOVEMBER, 3, 23, 30, 0)
        }
        val ts = utcCal.timeInMillis

        val utcDay = DateAndCurrencyUtils.toLocalEpochDay(ts, TimeZone.getTimeZone("UTC"))
        val tunisDay = DateAndCurrencyUtils.toLocalEpochDay(ts, tunisTz)

        assertEquals(utcDay + 1L, tunisDay)
    }
}
