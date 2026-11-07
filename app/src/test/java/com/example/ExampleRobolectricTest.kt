package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CapZeroDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CapZeroRepository
import com.example.ui.viewmodel.CapZeroViewModel
import com.example.ui.viewmodel.DayStatusCategory
import com.example.util.DateAndCurrencyUtils
import com.example.util.PresetContexts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.TimeZone
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: CapZeroDatabase
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var prefsFile: File
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var repository: CapZeroRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        val directExecutor = Executors.newSingleThreadExecutor()
        database = Room.inMemoryDatabaseBuilder(context, CapZeroDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()
        dataStoreScope = CoroutineScope(testDispatcher + SupervisorJob())
        prefsFile = File(context.cacheDir, "test_prefs_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { prefsFile }
        )
        prefsRepository = UserPreferencesRepository(dataStore)
        repository = CapZeroRepository(
            dao = database.capZeroDao(),
            preferencesRepository = prefsRepository,
            timeZoneProvider = { TimeZone.getTimeZone("Africa/Tunis") }
        )
    }

    @After
    fun tearDown() {
        database.close()
        dataStoreScope.cancel()
        Dispatchers.resetMain()
        if (prefsFile.exists()) {
            prefsFile.delete()
        }
    }

    @Test
    fun `app name and updated navigation tab labels are in French`() {
        assertEquals("Cap Zéro", context.getString(R.string.app_name))
        assertEquals("Accueil", context.getString(R.string.nav_today))
        assertEquals("Journal", context.getString(R.string.nav_journal))
        assertEquals("Plan", context.getString(R.string.nav_plan))
        assertEquals("Progrès", context.getString(R.string.nav_progress))
        assertEquals("Envie", context.getString(R.string.nav_craving))
    }

    @Test
    fun `verify logging, undo, edit invalidation, day confirmation, day change, persistence and plan reaching zero`() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        val day1Millis = 1_762_200_000_000L // Jour 1
        viewModel.updateNowMillis(day1Millis)

        val day1Epoch = repository.currentLocalEpochDay(day1Millis)
        val quitDayEpoch = day1Epoch + 5L

        // Tant qu'aucune journée complète n'est confirmée, aucune référence observée ni moyenne/économie n'est calculée (« — »)
        val initialState = withTimeout(5000L) {
            viewModel.uiState.first { !it.isLoading }
        }
        assertNull(initialState.computedObservationReference)
        assertNull(initialState.progressMetrics.averagePerConfirmedDay)
        assertNull(initialState.progressMetrics.estimatedSavingsTnd)
        assertFalse(initialState.progressMetrics.hasChartData)

        // 1. Validation explicite du plan atteignant zéro à la date choisie
        val steps = DateAndCurrencyUtils.generateLinearReductionPlan(
            startEpochDay = day1Epoch,
            quitEpochDay = quitDayEpoch,
            referenceDailyCigarettes = 10
        )
        assertEquals(0, steps.last().capCount)

        repository.setupDirectPlan(
            referenceDailyCigarettes = 10,
            packPriceMillimes = 10000L, // 10,000 TND
            cigarettesPerPack = 20,     // 0,500 TND / cigarette
            startEpochDay = day1Epoch,
            quitDateEpochDay = quitDayEpoch,
            validatedSteps = steps
        )

        // 2. Enregistrement de 2 cigarettes le Jour 1
        val cig1 = repository.logCigaretteNow(nowMillis = day1Millis)
        val cig2 = repository.logCigaretteNow(nowMillis = day1Millis + 30_000L)
        assertEquals(2, repository.cigarettesFlow.first { it.size == 2 }.size)

        // Annulation (suppression) et restauration de la 2e cigarette
        val (deleted, _) = repository.deleteCigaretteWithInvalidation(cig2.id)
        assertNotNull(deleted)
        assertEquals(1, repository.cigarettesFlow.first { it.size == 1 }.size)
        repository.restoreCigarette(deleted!!)
        assertEquals(2, repository.cigarettesFlow.first { it.size == 2 }.size)

        // 3. Confirmation de la Journée 1 complète : confirme le total (2 cigarettes) sans le remplacer par zéro
        repository.setDayConfirmed(day1Epoch, true)
        val stateAfterDay1Confirmed = withTimeout(5000L) {
            viewModel.uiState.first { state ->
                state.progressMetrics.confirmedDaysCount == 1 &&
                    state.progressMetrics.totalCigarettesOnConfirmedDays == 2
            }
        }
        assertEquals(2, stateAfterDay1Confirmed.todayCigarettesCount)
        assertEquals(2.0, stateAfterDay1Confirmed.progressMetrics.averagePerConfirmedDay ?: -1.0, 0.001)
        // Économies sur Jour 1 confirmé : (10 - 2) = 8 cig. * 0,500 TND = 4,000 TND
        assertEquals(4.000, stateAfterDay1Confirmed.progressMetrics.estimatedSavingsTnd ?: -1.0, 0.0001)

        // 4. Une modification ultérieure sur une journée confirmée recalcule les statistiques et demande une nouvelle confirmation
        val invalidated = repository.updateCigaretteWithInvalidation(
            id = cig1.id,
            timestampMillis = day1Millis + 60_000L,
            cravingIntensity = 4,
            contextCategory = PresetContexts.COFFEE_MEAL,
            note = "Après le repas"
        )
        assertTrue("La modification d'une journée confirmée doit invalider sa confirmation", invalidated)

        val stateAfterEditInvalidation = withTimeout(5000L) {
            viewModel.uiState.first { state ->
                state.progressMetrics.confirmedDaysCount == 0
            }
        }
        assertFalse(stateAfterEditInvalidation.isTodayConfirmedComplete)
        assertNull(stateAfterEditInvalidation.progressMetrics.averagePerConfirmedDay)
        assertNull(stateAfterEditInvalidation.progressMetrics.estimatedSavingsTnd)

        // Reconfirmation de la Journée 1 complète après modification
        repository.setDayConfirmed(day1Epoch, true)

        // 5. Changement de jour (Jour 2 confirmé à 0 cigarette, Jour 3 en cours provisoire)
        val day2Millis = day1Millis + 86_400_000L
        val day3Millis = day1Millis + 2 * 86_400_000L
        val day2Epoch = repository.currentLocalEpochDay(day2Millis)
        val day3Epoch = repository.currentLocalEpochDay(day3Millis)

        repository.setDayConfirmed(day2Epoch, true) // 0 cigarette confirmée le Jour 2
        viewModel.updateNowMillis(day3Millis)

        val stateOnDay3 = withTimeout(5000L) {
            viewModel.uiState.first { state ->
                !state.isLoading &&
                    state.todayEpochDay == day3Epoch &&
                    state.progressMetrics.confirmedDaysCount == 2
            }
        }
        // Vérifier la distinction des journées dans le journal
        val day3Group = stateOnDay3.journalGroups.first { it.localEpochDay == day3Epoch }
        val day2Group = stateOnDay3.journalGroups.first { it.localEpochDay == day2Epoch }
        val day1Group = stateOnDay3.journalGroups.first { it.localEpochDay == day1Epoch }

        assertEquals(DayStatusCategory.CURRENT_PROVISIONAL, day3Group.statusCategory)
        assertEquals(DayStatusCategory.PAST_OR_CURRENT_CONFIRMED, day2Group.statusCategory)
        assertEquals(DayStatusCategory.PAST_OR_CURRENT_CONFIRMED, day1Group.statusCategory)
        assertEquals(2, day1Group.entries.size) // Toujours 2 cigarettes, jamais remplacé par zéro

        // Moyenne sur les 2 journées complètes confirmées : (2 + 0) / 2 = 1,0 cig./j
        val metrics = stateOnDay3.progressMetrics
        assertEquals(2, metrics.confirmedDaysCount)
        assertEquals(1, metrics.confirmedZeroDaysCount)
        assertEquals(1.0, metrics.averagePerConfirmedDay ?: -1.0, 0.001)
        // Économies sur les 2 journées confirmées : (10*2 - 2) = 18 cig. * 0,500 TND = 9,000 TND
        assertEquals(18, metrics.avoidedCigarettesOnConfirmedDays)
        assertEquals(9.000, metrics.estimatedSavingsTnd ?: -1.0, 0.0001)

        // 6. Persistance après fermeture (nouvelle instance de ViewModel sur le même Repository)
        val restoredViewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        restoredViewModel.updateNowMillis(day3Millis)
        val restoredState = withTimeout(5000L) {
            restoredViewModel.uiState.first { state ->
                !state.isLoading &&
                    state.preferences.isOnboardingCompleted &&
                    state.cigarettes.size == 2 &&
                    state.planCaps.isNotEmpty() &&
                    state.progressMetrics.confirmedDaysCount == 2
            }
        }
        assertEquals(0, restoredState.planCaps.last().capCount)
        assertEquals(9.000, restoredState.progressMetrics.estimatedSavingsTnd ?: -1.0, 0.0001)
    }

    @Test
    fun `craving timer remains exact after background passage and prevents double recording`() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        val startMillis = 1_762_200_000_000L
        viewModel.updateNowMillis(startMillis)

        // Démarrer le minuteur de 5 minutes (300 secondes)
        viewModel.toggleCravingTimer()
        val runningState = withTimeout(5000L) {
            viewModel.uiState.first { it.cravingTimer.isRunning }
        }
        assertEquals(300, runningState.cravingTimer.remainingSeconds)

        // Simuler un passage en arrière-plan de 125 secondes (2 min 05 s) sans ticks intermédiaires
        val afterBackgroundMillis = startMillis + 125_000L
        viewModel.syncTimeWithTimestamp(afterBackgroundMillis)

        val resumedState = withTimeout(5000L) {
            viewModel.uiState.first { it.nowMillis == afterBackgroundMillis }
        }
        assertTrue(resumedState.cravingTimer.isRunning)
        assertEquals(175, resumedState.cravingTimer.remainingSeconds) // 300 - 125 = 175 s exactes

        // Simuler la fin des 5 minutes en arrière-plan (+200 secondes supplémentaires)
        val finishedMillis = afterBackgroundMillis + 200_000L
        viewModel.syncTimeWithTimestamp(finishedMillis)
        val finishedState = withTimeout(5000L) {
            viewModel.uiState.first { it.nowMillis == finishedMillis }
        }
        assertFalse(finishedState.cravingTimer.isRunning)
        assertEquals(0, finishedState.cravingTimer.remainingSeconds)

        // Vérifier l'absence de double enregistrement sur « J'ai fumé »
        viewModel.markCravingSmokedOnce()
        viewModel.markCravingSmokedOnce()
        val stateAfterCraving = withTimeout(5000L) {
            viewModel.uiState.first { state ->
                state.cravingTimer.sessionResolvedOutcome == "SMOKED" &&
                    state.cigarettes.size == 1
            }
        }
        assertEquals(1, stateAfterCraving.cigarettes.size)
    }

    @Test
    fun `home screen widget logs cigarette in one click without opening app and displays daily cap`() = runBlocking {
        val nowMillis = 1_762_200_000_000L
        val timeZone = TimeZone.getTimeZone("Africa/Tunis")
        val todayEpochDay = DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZone)

        // Configurer un plan avec un plafond du jour = 12
        val steps = DateAndCurrencyUtils.generateLinearReductionPlan(
            startEpochDay = todayEpochDay,
            quitEpochDay = todayEpochDay + 6L,
            referenceDailyCigarettes = 12
        )
        repository.setupDirectPlan(
            referenceDailyCigarettes = 12,
            packPriceMillimes = 10000L,
            cigarettesPerPack = 20,
            startEpochDay = todayEpochDay,
            quitDateEpochDay = todayEpochDay + 6L,
            validatedSteps = steps
        )
        repository.setDayConfirmed(todayEpochDay, true)

        // Simuler un clic sur « + J'ai fumé » depuis le widget d'écran d'accueil sans ouvrir l'application
        com.example.widget.CapZeroWidgetProvider.processWidgetAction(
            context = context,
            action = com.example.widget.CapZeroWidgetProvider.ACTION_QUICK_LOG_CIGARETTE,
            dao = database.capZeroDao(),
            nowMillis = nowMillis,
            timeZone = timeZone
        )

        val cigarettesAfterClick = database.capZeroDao().getAllCigarettesSnapshot()
        assertEquals(1, cigarettesAfterClick.size)
        assertEquals(todayEpochDay, cigarettesAfterClick.first().localEpochDay)

        // La confirmation de complétude de la journée en cours est automatiquement invalidée suite à l'ajout depuis le widget
        val statusesAfterClick = database.capZeroDao().getAllDailyStatusesSnapshot()
        assertFalse(statusesAfterClick.any { it.localEpochDay == todayEpochDay && it.isConfirmedComplete })

        // Vérifier le rendu de la RemoteViews du widget (compteur = 1 et Plafond du jour = 12)
        val remoteViews = com.example.widget.CapZeroWidgetProvider.buildRemoteViews(
            context = context,
            todayCount = cigarettesAfterClick.size,
            todayCap = 12,
            lastCigaretteMillis = nowMillis,
            nowMillis = nowMillis + 60_000L
        )
        val appliedView = remoteViews.apply(context, android.widget.FrameLayout(context))
        val countTextView = appliedView.findViewById<android.widget.TextView>(R.id.widget_today_count)
        val capBadgeTextView = appliedView.findViewById<android.widget.TextView>(R.id.widget_cap_badge)
        val smokeButtonTextView = appliedView.findViewById<android.widget.TextView>(R.id.widget_quick_smoke_button)

        assertEquals("1", countTextView.text.toString())
        assertEquals("Plafond du jour : 12", capBadgeTextView.text.toString())
        assertEquals("+ J'ai fumé", smokeButtonTextView.text.toString())
    }

    @Test
    fun `pack purchase records packs count and TND price and automatically deducts inventory on each cigarette smoked`() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        val baseMillis = 1_762_200_000_000L
        viewModel.updateNowMillis(baseMillis)

        // 1. Acheter 2 paquets de 20 cigarettes à 9,500 TND (9500 millimes) le paquet -> 40 cigarettes en stock, 19,000 TND dépensés
        viewModel.recordPackPurchase(
            packsCount = 2,
            cigarettesPerPack = 20,
            unitPriceMillimes = 9500L,
            note = "Bureau de tabac",
            timestampMillis = baseMillis
        )

        val stateAfterPurchase = withTimeout(5000L) {
            viewModel.uiState.first { !it.isLoading && it.packPurchases.size == 1 }
        }
        assertEquals(2, stateAfterPurchase.inventorySummary.totalPacksPurchased)
        assertEquals(40, stateAfterPurchase.inventorySummary.totalCigarettesPurchased)
        assertEquals(40, stateAfterPurchase.inventorySummary.remainingCigarettes)
        assertEquals(19_000L, stateAfterPurchase.inventorySummary.totalSpentMillimes)
        assertEquals(19.000, stateAfterPurchase.inventorySummary.totalSpentTnd, 0.0001)
        assertEquals("19,000 TND", DateAndCurrencyUtils.formatTnd(stateAfterPurchase.inventorySummary.totalSpentTnd))

        // 2. Cliquer sur « J'ai fumé » deux fois -> déduction automatique de 2 cigarettes (40 -> 38 restantes)
        viewModel.recordQuickCigarette(nowMillis = baseMillis + 10_000L)
        viewModel.recordQuickCigarette(nowMillis = baseMillis + 20_000L)

        val stateAfterTwoSmoked = withTimeout(5000L) {
            viewModel.uiState.first { it.cigarettes.size == 2 && it.inventorySummary.remainingCigarettes == 38 }
        }
        assertEquals(38, stateAfterTwoSmoked.inventorySummary.remainingCigarettes)
        assertEquals(1, stateAfterTwoSmoked.inventorySummary.equivalentFullPacksRemaining)
        assertEquals(18, stateAfterTwoSmoked.inventorySummary.looseCigarettesRemaining)

        // 3. Annuler la dernière cigarette -> le stock remonte automatiquement à 39 cigarettes
        viewModel.undoLastAction()
        val stateAfterUndo = withTimeout(5000L) {
            viewModel.uiState.first { it.cigarettes.size == 1 && it.inventorySummary.remainingCigarettes == 39 }
        }
        assertEquals(39, stateAfterUndo.inventorySummary.remainingCigarettes)

        // 4. Modification manuelle du stock de cigarettes restantes (ex: fixé à 25 cigarettes)
        viewModel.adjustInventoryStock(25)
        val stateAfterManualAdjustment = withTimeout(5000L) {
            viewModel.uiState.first { it.inventorySummary.remainingCigarettes == 25 }
        }
        assertEquals(25, stateAfterManualAdjustment.inventorySummary.remainingCigarettes)
        assertEquals(1, stateAfterManualAdjustment.inventorySummary.equivalentFullPacksRemaining)
        assertEquals(5, stateAfterManualAdjustment.inventorySummary.looseCigarettesRemaining)

        // Annuler l'ajustement manuel -> revient au stock précédent (39)
        viewModel.undoLastAction()
        val stateAfterUndoAdjustment = withTimeout(5000L) {
            viewModel.uiState.first { it.inventorySummary.remainingCigarettes == 39 }
        }
        assertEquals(39, stateAfterUndoAdjustment.inventorySummary.remainingCigarettes)
    }

    @Test
    fun `observation target days supports 1 to 14 days range`() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)

        viewModel.setObservationTargetDays(1)
        val state1 = withTimeout(5000L) {
            viewModel.uiState.first { it.preferences.observationTargetDays == 1 }
        }
        assertEquals(1, state1.preferences.observationTargetDays)

        viewModel.setObservationTargetDays(14)
        val state14 = withTimeout(5000L) {
            viewModel.uiState.first { it.preferences.observationTargetDays == 14 }
        }
        assertEquals(14, state14.preferences.observationTargetDays)
    }

    @Test
    fun `custom contexts can be added renamed deleted and reset to default`() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        val baseMillis = 1_760_000_000_000L

        // 1. Ajouter un contexte personnalisé « En voiture »
        viewModel.addCustomContext("En voiture")
        val stateAfterAdd = withTimeout(5000L) {
            viewModel.uiState.first { "En voiture" in it.preferences.customContexts }
        }
        assertTrue("En voiture" in stateAfterAdd.preferences.customContexts)

        // Enregistrer une cigarette avec ce contexte personnalisé
        viewModel.recordQuickCigarette(
            nowMillis = baseMillis,
            contextCategory = "En voiture"
        )
        val stateWithCig = withTimeout(5000L) {
            viewModel.uiState.first { it.cigarettes.any { c -> c.contextCategory == "En voiture" } }
        }
        assertEquals("En voiture", stateWithCig.cigarettes.first().contextCategory)

        // 2. Renommer « En voiture » en « Trajet voiture » -> met à jour la liste et la cigarette enregistrée
        viewModel.renameCustomContext("En voiture", "Trajet voiture")
        val stateAfterRename = withTimeout(5000L) {
            viewModel.uiState.first {
                "Trajet voiture" in it.preferences.customContexts &&
                    it.cigarettes.any { c -> c.contextCategory == "Trajet voiture" }
            }
        }
        assertFalse("En voiture" in stateAfterRename.preferences.customContexts)
        assertTrue("Trajet voiture" in stateAfterRename.preferences.customContexts)

        // 3. Supprimer un contexte de la liste
        viewModel.deleteCustomContext("Trajet voiture")
        val stateAfterDelete = withTimeout(5000L) {
            viewModel.uiState.first { "Trajet voiture" !in it.preferences.customContexts }
        }
        assertFalse("Trajet voiture" in stateAfterDelete.preferences.customContexts)

        // 4. Réinitialiser les contextes par défaut
        viewModel.resetCustomContextsToDefault()
        val stateAfterReset = withTimeout(5000L) {
            viewModel.uiState.first { it.preferences.customContexts == com.example.util.PresetContexts.ALL }
        }
        assertEquals(com.example.util.PresetContexts.ALL, stateAfterReset.preferences.customContexts)
    }

    @Test
    fun `intra day interval calculation strictly ignores intervals between separate days`() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        val tz = repository.currentTimeZone()

        val day1 = 20_000L
        val day2 = 20_001L

        // Day 1: cigarette à 23h00 (une seule cigarette le jour 1)
        val tsDay1Night = DateAndCurrencyUtils.buildLocalTimestamp(day1, 23, 0, 0, tz)
        viewModel.addRetroactiveCigarette(tsDay1Night, 3, null, null)

        // Day 2: cigarette à 07h00 (une seule cigarette le jour 2)
        val tsDay2Morning = DateAndCurrencyUtils.buildLocalTimestamp(day2, 7, 0, 0, tz)
        viewModel.addRetroactiveCigarette(tsDay2Morning, 4, null, null)

        val stateAfterTwoDays = withTimeout(5000L) {
            viewModel.uiState.first { it.cigarettes.size == 2 }
        }

        // Vérification stricte : aucun intervalle calculé entre 23h et 7h car jours séparés !
        val groupDay1 = stateAfterTwoDays.journalGroups.first { it.localEpochDay == day1 }
        val groupDay2 = stateAfterTwoDays.journalGroups.first { it.localEpochDay == day2 }
        assertNull(groupDay1.intervalStats)
        assertNull(groupDay2.intervalStats)
        assertNull(stateAfterTwoDays.progressMetrics.overallIntervalStats)

        // Maintenant, ajoutons une 2e et 3e cigarette le jour 2 :
        // Day 2 : 07h00, 09h30 (+2h30 = 150 min), 13h00 (+3h30 = 210 min)
        val tsDay2Second = DateAndCurrencyUtils.buildLocalTimestamp(day2, 9, 30, 0, tz)
        val tsDay2Third = DateAndCurrencyUtils.buildLocalTimestamp(day2, 13, 0, 0, tz)
        viewModel.addRetroactiveCigarette(tsDay2Second, 2, null, null)
        viewModel.addRetroactiveCigarette(tsDay2Third, 1, null, null)

        val stateWithDay2Intervals = withTimeout(5000L) {
            viewModel.uiState.first { it.cigarettes.size == 4 }
        }

        val updatedGroupDay2 = stateWithDay2Intervals.journalGroups.first { it.localEpochDay == day2 }
        val statsDay2 = checkNotNull(updatedGroupDay2.intervalStats)
        assertEquals(2, statsDay2.countIntervals)
        assertEquals(150 * 60_000L, statsDay2.minMillis) // 2h30
        assertEquals(210 * 60_000L, statsDay2.maxMillis) // 3h30
        assertEquals(180 * 60_000L, statsDay2.avgMillis) // Moyenne = (150 + 210) / 2 = 180 min (3h00)

        // Formatage en heures et minutes
        assertEquals("2 h 30", DateAndCurrencyUtils.formatIntervalDuration(statsDay2.minMillis))
        assertEquals("3 h", DateAndCurrencyUtils.formatIntervalDuration(statsDay2.avgMillis))
        assertEquals("3 h 30", DateAndCurrencyUtils.formatIntervalDuration(statsDay2.maxMillis))

        // Synthèse globale Progrès
        val overallStats = checkNotNull(stateWithDay2Intervals.progressMetrics.overallIntervalStats)
        assertEquals(150 * 60_000L, overallStats.minMillis)
        assertEquals(210 * 60_000L, overallStats.maxMillis)
        assertEquals(180 * 60_000L, overallStats.avgMillis)
    }

    @Test
    fun testCalendarMonthInfoAndCumulativeTotalCigarettes() = runBlocking {
        val viewModel = CapZeroViewModel(repository = repository, enableLiveTicker = false)
        val tz = repository.currentTimeZone()

        // 1. Test des métadonnées du calendrier pour Novembre 2026 (Novembre = mois 10 en index 0)
        // Le 1er Novembre 2026 est un Dimanche -> Dans une semaine débutant le Lundi (index 0), Dimanche est à l'index 6
        val nov2026Info = DateAndCurrencyUtils.getCalendarMonthInfo(2026, 10, tz)
        assertEquals(30, nov2026Info.daysInMonth)
        assertEquals(6, nov2026Info.firstDayOffsetMondayZero)
        assertEquals("Novembre 2026", nov2026Info.monthNameFormatted)

        // Décembre 2026 (mois 11 en index 0) : 1er Décembre 2026 est un Mardi -> index 1
        val dec2026Info = DateAndCurrencyUtils.getCalendarMonthInfo(2026, 11, tz)
        assertEquals(31, dec2026Info.daysInMonth)
        assertEquals(1, dec2026Info.firstDayOffsetMondayZero)
        assertEquals("Décembre 2026", dec2026Info.monthNameFormatted)

        // 2. Test du total cumulé des cigarettes
        val day1 = DateAndCurrencyUtils.ymdToLocalEpochDay(2026, 10, 3)
        val day2 = DateAndCurrencyUtils.ymdToLocalEpochDay(2026, 10, 4)

        viewModel.addRetroactiveCigarette(
            DateAndCurrencyUtils.buildLocalTimestamp(day1, 10, 0, 0, tz),
            3,
            null,
            null
        )
        viewModel.addRetroactiveCigarette(
            DateAndCurrencyUtils.buildLocalTimestamp(day1, 14, 0, 0, tz),
            4,
            null,
            null
        )
        viewModel.addRetroactiveCigarette(
            DateAndCurrencyUtils.buildLocalTimestamp(day2, 9, 0, 0, tz),
            2,
            null,
            null
        )

        val state = withTimeout(5000L) {
            viewModel.uiState.first { it.cigarettes.size == 3 }
        }

        assertEquals(3, state.cigarettes.size)
        assertEquals(3, state.progressMetrics.totalCigarettesAllDays)
    }
}


