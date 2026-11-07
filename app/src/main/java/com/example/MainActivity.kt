package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CapZeroDatabase
import com.example.data.preferences.DarkThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.capZeroDataStore
import com.example.data.repository.CapZeroRepository
import com.example.ui.components.CapZeroBrandLeafIcon
import com.example.ui.components.WindBreathVectorIcon
import com.example.ui.screens.CravingScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.CapZeroTheme
import com.example.ui.theme.DeepGreenPrimaryLight
import com.example.ui.theme.EmeraldActionLight
import com.example.ui.theme.NavActiveMintPillLight
import com.example.ui.theme.VeryDarkGreenTextLight
import com.example.ui.viewmodel.CapZeroViewModel

/**
 * Onglets de navigation inférieure demandés : Accueil, Journal, Plan, Progrès, Envie (+ Paramètres en haut à droite).
 */
enum class CapZeroDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isBottomTab: Boolean = true
) {
    TODAY(
        route = "today",
        title = "Accueil",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    JOURNAL(
        route = "journal",
        title = "Journal",
        selectedIcon = Icons.Outlined.Description,
        unselectedIcon = Icons.Outlined.Description
    ),
    PLAN(
        route = "plan",
        title = "Plan",
        selectedIcon = Icons.Filled.CalendarToday,
        unselectedIcon = Icons.Outlined.CalendarToday
    ),
    PROGRESS(
        route = "progress",
        title = "Progrès",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    CRAVING(
        route = "craving",
        title = "Envie",
        selectedIcon = Icons.Outlined.Home, // rendu via WindBreathVectorIcon
        unselectedIcon = Icons.Outlined.Home
    ),
    SETTINGS(
        route = "settings",
        title = "Paramètres",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Filled.Settings,
        isBottomTab = false
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val database = CapZeroDatabase.getDatabase(applicationContext)
        val prefsRepository = UserPreferencesRepository(applicationContext.capZeroDataStore)
        val repository = CapZeroRepository(
            dao = database.capZeroDao(),
            preferencesRepository = prefsRepository,
            appContext = applicationContext
        )

        setContent {
            val capZeroViewModel: CapZeroViewModel = viewModel(
                factory = CapZeroViewModel.provideFactory(repository)
            )
            CapZeroApp(viewModel = capZeroViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapZeroApp(
    viewModel: CapZeroViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Synchroniser l'horloge et le minuteur dès le retour au premier plan après un passage en arrière-plan
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncTimeWithSystemClock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (uiState.preferences.darkThemeMode) {
        DarkThemeMode.LIGHT -> false
        DarkThemeMode.DARK -> true
        DarkThemeMode.SYSTEM -> systemDark
    }

    var currentDestination by rememberSaveable { mutableStateOf(CapZeroDestination.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = currentDestination != CapZeroDestination.TODAY) {
        currentDestination = CapZeroDestination.TODAY
    }

    LaunchedEffect(uiState.pendingUndo) {
        val undo = uiState.pendingUndo
        if (undo != null) {
            val result = snackbarHostState.showSnackbar(
                message = undo.message,
                actionLabel = "Annuler",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastAction()
            } else {
                viewModel.dismissUndo()
            }
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearStatusBanner()
        }
    }

    CapZeroTheme(darkTheme = useDarkTheme) {
        if (!uiState.isLoading && !uiState.preferences.isOnboardingCompleted) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    OnboardingScreen(
                        todayEpochDay = uiState.todayEpochDay,
                        onCompleteDirectPlan = { refCigs, priceMillimes, cigsPerPack, quitDay, steps ->
                            viewModel.completeOnboardingWithPlan(
                                referenceDailyCigarettes = refCigs,
                                packPriceMillimes = priceMillimes,
                                cigarettesPerPack = cigsPerPack,
                                quitDateEpochDay = quitDay,
                                validatedSteps = steps
                            )
                        },
                        onCompleteObservationMode = { obsDays, priceMillimes, cigsPerPack, quitDay ->
                            viewModel.completeOnboardingWithObservation(
                                observationTargetDays = obsDays,
                                packPriceMillimes = priceMillimes,
                                cigarettesPerPack = cigsPerPack,
                                quitDateEpochDay = quitDay
                            )
                        }
                    )
                }
            }
        } else if (!uiState.isLoading) {
            val bottomTabs = remember {
                CapZeroDestination.entries.filter { it.isBottomTab }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CapZeroBrandLeafIcon(modifier = Modifier.size(26.dp))
                                Text(
                                    text = "Cap Zéro",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        },
                        navigationIcon = {
                            if (currentDestination == CapZeroDestination.SETTINGS) {
                                IconButton(
                                    onClick = { currentDestination = CapZeroDestination.TODAY },
                                    modifier = Modifier.testTag("top_bar_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Retour à Accueil",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        },
                        actions = {
                            if (currentDestination != CapZeroDestination.SETTINGS) {
                                IconButton(
                                    onClick = { currentDestination = CapZeroDestination.SETTINGS },
                                    modifier = Modifier.testTag("top_bar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonOutline,
                                        contentDescription = "Paramètres",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    // Barre de navigation inférieure fidèle à la photo :
                    // Fond clair (#F7F9F8), carré arrondi vert menthe (#D4F0E0) englobant l'icône et le texte de l'onglet actif
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_navigation_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomTabs.forEach { destination ->
                                val isSelected = currentDestination == destination
                                val activeBg = if (useDarkTheme) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    NavActiveMintPillLight
                                }
                                val activeIconTint = if (useDarkTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    EmeraldActionLight
                                }
                                val activeTextColor = if (useDarkTheme) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    VeryDarkGreenTextLight
                                }
                                val inactiveColor = if (useDarkTheme) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    DeepGreenPrimaryLight
                                }

                                Surface(
                                    color = if (isSelected) activeBg else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { currentDestination = destination }
                                        .semantics {
                                            role = Role.Tab
                                            selected = isSelected
                                        }
                                        .testTag("nav_tab_${destination.route}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (destination == CapZeroDestination.CRAVING) {
                                            WindBreathVectorIcon(
                                                color = if (isSelected) activeIconTint else inactiveColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                                contentDescription = destination.title,
                                                tint = if (isSelected) activeIconTint else inactiveColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Text(
                                            text = destination.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Visible,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) activeTextColor else inactiveColor,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentDestination) {
                        CapZeroDestination.TODAY -> {
                            TodayScreen(
                                uiState = uiState,
                                onQuickSmokeClick = { viewModel.recordQuickCigarette() },
                                onUndoLastAction = { viewModel.undoLastAction() },
                                onUpdateCigaretteDetails = { id, ts, intensity, context, note ->
                                    viewModel.updateCigaretteDetails(id, ts, intensity, context, note)
                                },
                                onDismissQuickContextPrompt = { viewModel.dismissQuickLogContextPrompt() },
                                onToggleConfirmToday = { confirmed ->
                                    viewModel.setDayConfirmedComplete(uiState.todayEpochDay, confirmed)
                                },
                                onNavigateToCraving = { currentDestination = CapZeroDestination.CRAVING },
                                onNavigateToPlan = { currentDestination = CapZeroDestination.PLAN },
                                onAddPackPurchase = { packsCount, cigsPerPack, unitPriceMillimes, note ->
                                    viewModel.recordPackPurchase(
                                        packsCount = packsCount,
                                        cigarettesPerPack = cigsPerPack,
                                        unitPriceMillimes = unitPriceMillimes,
                                        note = note
                                    )
                                },
                                onAdjustInventoryStock = { newStock ->
                                    viewModel.adjustInventoryStock(newStock)
                                },
                                onAddCustomContext = { viewModel.addCustomContext(it) },
                                onRenameCustomContext = { oldCtx, newCtx -> viewModel.renameCustomContext(oldCtx, newCtx) },
                                onDeleteCustomContext = { viewModel.deleteCustomContext(it) },
                                onResetCustomContexts = { viewModel.resetCustomContextsToDefault() }
                            )
                        }
                        CapZeroDestination.JOURNAL -> {
                            JournalScreen(
                                uiState = uiState,
                                onAddRetroactiveCigarette = { ts, intensity, context, note ->
                                    viewModel.addRetroactiveCigarette(ts, intensity, context, note)
                                },
                                onUpdateCigarette = { id, ts, intensity, context, note ->
                                    viewModel.updateCigaretteDetails(id, ts, intensity, context, note)
                                },
                                onDeleteCigarette = { id ->
                                    viewModel.deleteCigarette(id)
                                },
                                onToggleDayConfirmed = { day, confirmed ->
                                    viewModel.setDayConfirmedComplete(day, confirmed)
                                },
                                onUndoLastAction = { viewModel.undoLastAction() },
                                onAddPackPurchase = { packsCount, cigsPerPack, unitPriceMillimes, note ->
                                    viewModel.recordPackPurchase(
                                        packsCount = packsCount,
                                        cigarettesPerPack = cigsPerPack,
                                        unitPriceMillimes = unitPriceMillimes,
                                        note = note
                                    )
                                },
                                onDeletePackPurchase = { id ->
                                    viewModel.deletePackPurchase(id)
                                },
                                onAdjustInventoryStock = { newStock ->
                                    viewModel.adjustInventoryStock(newStock)
                                },
                                onAddCustomContext = { viewModel.addCustomContext(it) },
                                onRenameCustomContext = { oldCtx, newCtx -> viewModel.renameCustomContext(oldCtx, newCtx) },
                                onDeleteCustomContext = { viewModel.deleteCustomContext(it) },
                                onResetCustomContexts = { viewModel.resetCustomContextsToDefault() }
                            )
                        }
                        CapZeroDestination.PLAN -> {
                            PlanScreen(
                                uiState = uiState,
                                onModifySingleDayCap = { day, newCap ->
                                    viewModel.modifySingleDayPlanCap(day, newCap)
                                },
                                onSaveNewOrRecalculatedPlan = { refCigs, startDay, quitDay, steps ->
                                    viewModel.saveGeneratedOrUpdatedPlan(refCigs, startDay, quitDay, steps)
                                }
                            )
                        }
                        CapZeroDestination.PROGRESS -> {
                            ProgressScreen(
                                uiState = uiState,
                                onAddPackPurchase = { packsCount, cigsPerPack, unitPriceMillimes, note ->
                                    viewModel.recordPackPurchase(
                                        packsCount = packsCount,
                                        cigarettesPerPack = cigsPerPack,
                                        unitPriceMillimes = unitPriceMillimes,
                                        note = note
                                    )
                                },
                                onAdjustInventoryStock = { newStock ->
                                    viewModel.adjustInventoryStock(newStock)
                                }
                            )
                        }
                        CapZeroDestination.CRAVING -> {
                            CravingScreen(
                                uiState = uiState,
                                onToggleTimer = { viewModel.toggleCravingTimer() },
                                onResetSession = { viewModel.resetCravingSession() },
                                onSelectActivity = { viewModel.selectReplacementActivity(it) },
                                onCravingPassedClick = { viewModel.markCravingPassed() },
                                onCravingSmokedSingleClick = { viewModel.markCravingSmokedOnce() },
                                onUndoLastAction = { viewModel.undoLastAction() }
                            )
                        }
                        CapZeroDestination.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                onUpdateFinancialSettings = { refCigs, priceMillimes, perPack ->
                                    viewModel.updateFinancialAndReferenceSettings(refCigs, priceMillimes, perPack)
                                },
                                onSetDarkThemeMode = { mode ->
                                    viewModel.setDarkThemeMode(mode)
                                },
                                onGenerateJsonExport = { viewModel.generateJsonExport() },
                                onGenerateCsvExport = { viewModel.generateCsvExport() },
                                onImportJsonData = { json -> viewModel.importJsonData(json) },
                                onDeleteAllData = {
                                    viewModel.deleteAllData()
                                    currentDestination = CapZeroDestination.TODAY
                                },
                                onAdjustInventoryStock = { newStock ->
                                    viewModel.adjustInventoryStock(newStock)
                                },
                                onUpdateObservationTargetDays = { targetDays ->
                                    viewModel.setObservationTargetDays(targetDays)
                                },
                                onAddCustomContext = { viewModel.addCustomContext(it) },
                                onRenameCustomContext = { oldCtx, newCtx -> viewModel.renameCustomContext(oldCtx, newCtx) },
                                onDeleteCustomContext = { viewModel.deleteCustomContext(it) },
                                onResetCustomContexts = { viewModel.resetCustomContextsToDefault() }
                            )
                        }
                    }
                }
            }
        }
    }
}
