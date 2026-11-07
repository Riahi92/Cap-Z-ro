package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.DarkThemeMode
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.ConciseHelpBanner
import com.example.ui.components.DeepGreenOutlinedButton
import com.example.ui.components.EmeraldPrimaryButton
import com.example.ui.viewmodel.CapZeroUiState
import com.example.util.DateAndCurrencyUtils
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: CapZeroUiState,
    onUpdateFinancialSettings: (referenceCigarettes: Int?, packPriceMillimes: Long, cigarettesPerPack: Int) -> Unit,
    onSetDarkThemeMode: (DarkThemeMode) -> Unit,
    onGenerateJsonExport: suspend () -> String,
    onGenerateCsvExport: suspend () -> String,
    onImportJsonData: (String) -> Unit,
    onDeleteAllData: () -> Unit,
    onAdjustInventoryStock: (newStock: Int) -> Unit = {},
    onUpdateObservationTargetDays: (targetDays: Int) -> Unit = {},
    onAddCustomContext: (String) -> Unit = {},
    onRenameCustomContext: (oldContext: String, newContext: String) -> Unit = { _, _ -> },
    onDeleteCustomContext: (String) -> Unit = {},
    onResetCustomContexts: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = uiState.preferences

    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var newCustomContextInput by remember { mutableStateOf("") }
    var editingContextOriginal by remember { mutableStateOf<String?>(null) }
    var editingContextInput by remember { mutableStateOf("") }

    var referenceInput by remember(prefs.referenceDailyCigarettes) {
        mutableStateOf(prefs.referenceDailyCigarettes?.toString() ?: "")
    }
    var packPriceInput by remember(prefs.packPriceMillimes) {
        val formatted = prefs.packPriceMillimes?.let {
            String.format(Locale.FRANCE, "%.3f", it.toDouble() / 1000.0)
        } ?: ""
        mutableStateOf(formatted)
    }
    var cigsPerPackInput by remember(prefs.cigarettesPerPack) {
        mutableStateOf(prefs.cigarettesPerPack?.toString() ?: "")
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportedTextModalTitle by remember { mutableStateOf<String?>(null) }
    var exportedTextModalContent by remember { mutableStateOf("") }
    var showImportJsonTextModal by remember { mutableStateOf(false) }
    var importJsonTextInput by remember { mutableStateOf("") }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val json = onGenerateJsonExport()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                if (!content.isNullOrBlank()) {
                    onImportJsonData(content)
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val csv = onGenerateCsvExport()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Paramètres",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            // 1. Apparence & Mode Sombre
            CapZeroWhiteCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Apparence",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DarkThemeMode.SYSTEM to "Système",
                            DarkThemeMode.LIGHT to "Clair",
                            DarkThemeMode.DARK to "Sombre"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = prefs.darkThemeMode == mode,
                                onClick = { onSetDarkThemeMode(mode) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // 1b. Widget d'écran d'accueil minimaliste (Carte vert profond #123D32 et blanc #FFFFFF)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("settings_widget_preview_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Widget d'écran d'accueil",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Carte minimaliste vert profond (#123D32) et blanc (#FFFFFF) : consultez le plafond du jour ou enregistrez une cigarette en un clic sans ouvrir l'application.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Rendu fidèle de la carte du widget (#FFFFFF + bloc #123D32)
                    androidx.compose.material3.Surface(
                        color = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE2ECE8)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cap Zéro",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = androidx.compose.ui.graphics.Color(0xFF123D32),
                                    fontWeight = FontWeight.Bold
                                )
                                androidx.compose.material3.Surface(
                                    color = androidx.compose.ui.graphics.Color(0xFFD6EFE3),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (uiState.todayCap != null) {
                                            "Plafond du jour : ${uiState.todayCap}"
                                        } else {
                                            "Plafond du jour : —"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = androidx.compose.ui.graphics.Color(0xFF123D32),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.Surface(
                                    color = androidx.compose.ui.graphics.Color(0xFF123D32),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.todayCigarettesCount}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Column {
                                            Text(
                                                text = if (uiState.todayCigarettesCount == 1) "cigarette\naujourd'hui" else "cigarettes\naujourd'hui",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = androidx.compose.ui.graphics.Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val intent = android.content.Intent(
                                            context,
                                            com.example.widget.CapZeroWidgetProvider::class.java
                                        ).apply {
                                            action = com.example.widget.CapZeroWidgetProvider.ACTION_QUICK_LOG_CIGARETTE
                                        }
                                        context.sendBroadcast(intent)
                                    },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = androidx.compose.ui.graphics.Color(0xFF239878),
                                        contentColor = androidx.compose.ui.graphics.Color.White
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_widget_preview_smoke_button")
                                ) {
                                    Text(
                                        text = "+ J'ai fumé",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        DeepGreenOutlinedButton(
                            text = "Ajouter le widget à l'écran d'accueil",
                            onClick = {
                                runCatching {
                                    val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                    val provider = android.content.ComponentName(
                                        context,
                                        com.example.widget.CapZeroWidgetProvider::class.java
                                    )
                                    if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                        appWidgetManager.requestPinAppWidget(provider, null, null)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("settings_pin_widget_button")
                        )
                    }
                }
            }

            // 2. Préférences personnelles & Prix en TND (3 décimales)
            CapZeroWhiteCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Préférences & Tarification (TND)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 3) {
                                referenceInput = value
                            }
                        },
                        label = { Text("Consommation quotidienne de référence") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_reference_input")
                    )

                    OutlinedTextField(
                        value = packPriceInput,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() || it == '.' || it == ',' } && value.length <= 10) {
                                packPriceInput = value
                            }
                        },
                        label = { Text("Prix du paquet (TND, 3 décimales)") },
                        suffix = { Text("TND") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_pack_price_input")
                    )

                    OutlinedTextField(
                        value = cigsPerPackInput,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 3) {
                                cigsPerPackInput = value
                            }
                        },
                        label = { Text("Cigarettes par paquet") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_cigs_per_pack_input")
                    )

                    val unitCost = prefs.singleCigaretteCostTnd
                    if (unitCost != null) {
                        Text(
                            text = "Coût par cigarette : ${DateAndCurrencyUtils.formatTnd(unitCost)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    EmeraldPrimaryButton(
                        text = "Enregistrer les préférences",
                        onClick = {
                            val refCigs = referenceInput.trim().toIntOrNull()
                            val priceMillimes = DateAndCurrencyUtils.parseTndToMillimes(packPriceInput)
                                ?: prefs.packPriceMillimes
                                ?: 0L
                            val perPack = cigsPerPackInput.trim().toIntOrNull()
                                ?: prefs.cigarettesPerPack
                                ?: 20
                            onUpdateFinancialSettings(refCigs, priceMillimes, perPack)
                        },
                        leadingIcon = Icons.Filled.Save,
                        modifier = Modifier.testTag("settings_save_preferences_button")
                    )
                }
            }

            // 2b. Inventaire de cigarettes (Modification manuelle)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("settings_inventory_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Inventaire de cigarettes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Stock actuel : ${uiState.inventorySummary.remainingCigarettes} cigarette(s) en possession.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DeepGreenOutlinedButton(
                        text = "Modifier le stock de cigarettes",
                        onClick = { showAdjustStockDialog = true },
                        leadingIcon = Icons.Outlined.Description,
                        modifier = Modifier.testTag("settings_modify_inventory_button")
                    )
                }
            }

            // 2c. Période d'observation (1 à 14 jours)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("settings_observation_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Période d'observation (1 à 14 jours)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Durée recommandée de journées complètes pour observer votre rythme : ${prefs.observationTargetDays} jour(s).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..14).forEach { days ->
                            FilterChip(
                                selected = prefs.observationTargetDays == days,
                                onClick = { onUpdateObservationTargetDays(days) },
                                label = { Text("$days j") },
                                modifier = Modifier.testTag("settings_obs_days_$days")
                            )
                        }
                    }
                }
            }

            // 2d. Liste des contextes personnalisés (Journal)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("settings_custom_contexts_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Liste des contextes (Journal)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ajoutez, modifiez ou supprimez librement les contextes proposés lors de la saisie d'une cigarette.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (prefs.customContexts.isEmpty()) {
                        Text(
                            text = "Aucun contexte défini. Ajoutez vos propres contextes ci-dessous ou restaurez la liste par défaut.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            prefs.customContexts.forEachIndexed { index, contextItem ->
                                if (editingContextOriginal == contextItem) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editingContextInput,
                                            onValueChange = { if (it.length <= 50) editingContextInput = it },
                                            label = { Text("Modifier le contexte") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_rename_context_input")
                                        )
                                        IconButton(
                                            onClick = {
                                                val trimmed = editingContextInput.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    onRenameCustomContext(contextItem, trimmed)
                                                    editingContextOriginal = null
                                                }
                                            },
                                            modifier = Modifier.testTag("settings_confirm_rename_context_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Valider",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { editingContextOriginal = null }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Annuler"
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_context_item_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = contextItem,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        editingContextOriginal = contextItem
                                                        editingContextInput = contextItem
                                                    },
                                                    modifier = Modifier.testTag("settings_edit_context_$index")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = "Modifier $contextItem",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        onDeleteCustomContext(contextItem)
                                                    },
                                                    modifier = Modifier.testTag("settings_delete_context_$index")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "Supprimer $contextItem",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomContextInput,
                            onValueChange = { if (it.length <= 50) newCustomContextInput = it },
                            label = { Text("Nouveau contexte") },
                            placeholder = { Text("Ex. En voiture, Après le sport...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_new_context_input")
                        )
                        Button(
                            onClick = {
                                val trimmed = newCustomContextInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onAddCustomContext(trimmed)
                                    newCustomContextInput = ""
                                }
                            },
                            enabled = newCustomContextInput.trim().isNotEmpty(),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("settings_add_context_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter")
                        }
                    }

                    DeepGreenOutlinedButton(
                        text = "Réinitialiser les contextes par défaut",
                        onClick = {
                            editingContextOriginal = null
                            onResetCustomContexts()
                        },
                        leadingIcon = Icons.Outlined.RestartAlt,
                        modifier = Modifier.testTag("settings_reset_contexts_button")
                    )
                }
            }

            // 3. Sauvegarde, Export/Import JSON et Export CSV
            CapZeroWhiteCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Export et Import des données",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    EmeraldPrimaryButton(
                        text = "Exporter en JSON",
                        onClick = { exportJsonLauncher.launch("cap_zero_sauvegarde.json") },
                        leadingIcon = Icons.Filled.Download,
                        modifier = Modifier.testTag("settings_export_json_file_button")
                    )

                    DeepGreenOutlinedButton(
                        text = "Importer un fichier JSON",
                        onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        leadingIcon = Icons.Filled.Upload,
                        modifier = Modifier.testTag("settings_import_json_file_button")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    exportedTextModalContent = onGenerateJsonExport()
                                    exportedTextModalTitle = "Aperçu JSON"
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_preview_json_button")
                        ) {
                            Text("Copier JSON", color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedButton(
                            onClick = {
                                importJsonTextInput = ""
                                showImportJsonTextModal = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("settings_paste_json_button")
                        ) {
                            Text("Coller JSON", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                    EmeraldPrimaryButton(
                        text = "Exporter le journal en CSV",
                        onClick = { exportCsvLauncher.launch("cap_zero_journal.csv") },
                        leadingIcon = Icons.Filled.Download,
                        modifier = Modifier.testTag("settings_export_csv_file_button")
                    )

                    DeepGreenOutlinedButton(
                        text = "Voir / Copier CSV",
                        onClick = {
                            coroutineScope.launch {
                                exportedTextModalContent = onGenerateCsvExport()
                                exportedTextModalTitle = "Aperçu CSV"
                            }
                        },
                        modifier = Modifier.testTag("settings_preview_csv_button")
                    )
                }
            }

            // 4. Suppression de toutes les données avec confirmation
            CapZeroWhiteCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Réinitialisation complète",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("settings_delete_all_data_button")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supprimer toutes les données", fontWeight = FontWeight.Bold)
                    }
                }
            }

            ConciseHelpBanner(
                shortSummary = "Application hors connexion • Données stockées localement.",
                helpTitle = "Confidentialité & Accompagnement Santé",
                helpDetails = stringResource(id = R.string.health_disclaimer) + "\n\n" +
                    "Toutes vos données sont stockées exclusivement sur votre appareil.",
                testTagPrefix = "settings_help"
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Confirmer la suppression",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Voulez-vous supprimer définitivement toutes vos données (journal, plan, journées confirmées et préférences) ?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("settings_confirm_delete_all_button")
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("settings_cancel_delete_all_button")
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    if (exportedTextModalTitle != null) {
        AlertDialog(
            onDismissRequest = { exportedTextModalTitle = null },
            title = {
                Text(
                    text = exportedTextModalTitle!!,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = exportedTextModalContent,
                    onValueChange = {},
                    readOnly = true,
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_export_preview_textfield")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { exportedTextModalTitle = null },
                    modifier = Modifier.testTag("settings_close_export_preview_button")
                ) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showImportJsonTextModal) {
        AlertDialog(
            onDismissRequest = { showImportJsonTextModal = false },
            title = {
                Text(
                    text = "Importer un JSON",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = importJsonTextInput,
                    onValueChange = { importJsonTextInput = it },
                    label = { Text("Contenu JSON") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_import_json_textfield")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonTextInput.isNotBlank()) {
                            onImportJsonData(importJsonTextInput)
                            showImportJsonTextModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("settings_confirm_import_json_button")
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonTextModal = false }) {
                    Text("Annuler")
                }
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
