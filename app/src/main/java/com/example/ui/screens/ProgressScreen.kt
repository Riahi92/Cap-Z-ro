package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.AccessibleHelpIconButton
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.InventorySummaryCard
import com.example.ui.components.PackPurchaseDialog
import com.example.ui.viewmodel.CapZeroUiState
import com.example.ui.viewmodel.ChartDayPoint
import com.example.util.DateAndCurrencyUtils
import java.util.Locale
import kotlin.math.max

@Composable
fun ProgressScreen(
    uiState: CapZeroUiState,
    onAddPackPurchase: (packsCount: Int, cigarettesPerPack: Int, unitPriceMillimes: Long, note: String?) -> Unit = { _, _, _, _ -> },
    onAdjustInventoryStock: (newStock: Int) -> Unit = {}
) {
    var showPackPurchaseDialog by remember { mutableStateOf(false) }
    var showAdjustStockDialog by remember { mutableStateOf(false) }
    val metrics = uiState.progressMetrics

    val savingsFormatted = if (metrics.estimatedSavingsTnd != null) {
        DateAndCurrencyUtils.formatTnd(metrics.estimatedSavingsTnd)
    } else {
        "—"
    }

    val averageFormatted = if (metrics.averagePerConfirmedDay != null) {
        String.format(Locale.FRANCE, "%.1f cig./j", metrics.averagePerConfirmedDay)
    } else {
        "—"
    }

    val detailedFormulaText = remember(metrics, savingsFormatted) {
        buildString {
            val refCigs = metrics.referenceDailyCigarettes
            val unitCost = metrics.singleCigaretteCostTnd
            append("Les moyennes et les économies sont calculées exclusivement sur vos journées complètes confirmées afin de ne jamais fausser les résultats avec des journées partielles ou oubliées.\n\n")
            if (metrics.confirmedDaysCount > 0 && refCigs != null && unitCost != null) {
                append("• Journées complètes confirmées : ${metrics.confirmedDaysCount}\n")
                append("• Référence sur cette période : ${metrics.confirmedDaysCount} j × $refCigs cig./j = ${metrics.confirmedDaysCount * refCigs} cig.\n")
                append("• Cigarettes fumées sur ces jours confirmés : ${metrics.totalCigarettesOnConfirmedDays}\n")
                append("• Cigarettes évitées : ${max(0, metrics.avoidedCigarettesOnConfirmedDays ?: 0)}\n")
                append("• Coût unitaire : ${DateAndCurrencyUtils.formatTnd(unitCost)} / cigarette\n")
                append("• Économies estimées : $savingsFormatted")
            } else {
                append("Tant qu'aucune journée complète n'est confirmée ou que la référence et le prix du paquet ne sont pas renseignés, l'application affiche « — ».")
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
                text = "Mes progrès",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            // 1. Carte Économies Estimées en TND (sur les journées complètes confirmées, ou « — »)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("progress_savings_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Économies estimées",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        AccessibleHelpIconButton(
                            helpTitle = "Calcul des économies et moyennes",
                            helpDetails = detailedFormulaText,
                            testTag = "progress_savings_help_button"
                        )
                    }

                    Text(
                        text = savingsFormatted,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("progress_savings_tnd_value")
                    )

                    Text(
                        text = if (metrics.confirmedDaysCount > 0) {
                            "Basé sur ${metrics.confirmedDaysCount} journée(s) complète(s) confirmée(s)."
                        } else {
                            "Confirmez au moins une journée complète pour estimer vos économies."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("progress_savings_explanation")
                    )
                }
            }

            // 2. Bilan Inventaire de cigarettes & Paquets achetés (avec prix total d'achat en TND)
            InventorySummaryCard(
                inventorySummary = uiState.inventorySummary,
                onBuyPackClick = { showPackPurchaseDialog = true },
                onAdjustStockClick = { showAdjustStockDialog = true },
                testTagPrefix = "progress_inventory"
            )

            // 3. Trois indicateurs présentés séparément :
            // - Envies traversées
            // - Jours confirmés sans cigarette
            // - Consommation moyenne (sur journées complètes confirmées, ou « — »)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("progress_cravings_card")
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
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Envies traversées",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Envies apaisées dans l'onglet Envie",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${metrics.cravingsOvercomeCount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("progress_cravings_overcome_count")
                    )
                }
            }

            CapZeroWhiteCard(
                modifier = Modifier.testTag("progress_zero_days_card")
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
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Jours confirmés sans cigarette",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Journées complètes confirmées à 0 cigarette",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "${metrics.confirmedZeroDaysCount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("progress_confirmed_zero_days_count")
                    )
                }
            }

            CapZeroWhiteCard(
                modifier = Modifier.testTag("progress_average_card")
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
                            imageVector = Icons.Outlined.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Consommation moyenne",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (metrics.confirmedDaysCount > 0) {
                                    "Sur ${metrics.confirmedDaysCount} journée(s) complète(s) confirmée(s)"
                                } else {
                                    "Aucune journée complète confirmée"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = averageFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("progress_avg_per_logged_day")
                    )
                }
            }

            // 4. Graphique Consommation Réelle / Plafond Quotidien (ou état vide explicite)
            CapZeroWhiteCard(
                modifier = Modifier.testTag("progress_chart_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Évolution quotidienne",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Chaque élément de légende sur sa propre ligne
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("progress_chart_legend_column")
                    ) {
                        LegendBadgeRow(
                            color = MaterialTheme.colorScheme.primary,
                            label = "Consommation réelle (journées renseignées)"
                        )
                        LegendBadgeRow(
                            color = MaterialTheme.colorScheme.secondary,
                            label = "Plafond quotidien prévu par votre plan"
                        )
                    }

                    if (!metrics.hasChartData) {
                        // État vide explicite à la place d'un graphique sans données
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("progress_chart_empty_state")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Aucune donnée à afficher pour le moment",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Enregistrez vos premières cigarettes ou validez votre plan pour visualiser votre courbe de progression.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        ConsumptionVsCapChart(
                            points = metrics.chartPoints,
                            barColor = MaterialTheme.colorScheme.primary,
                            capLineColor = MaterialTheme.colorScheme.secondary,
                            gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val firstPoint = metrics.chartPoints.firstOrNull()
                            val lastPoint = metrics.chartPoints.lastOrNull()
                            if (firstPoint != null && lastPoint != null) {
                                Text(
                                    text = firstPoint.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = lastPoint.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 5. Contextes fréquents & Intensité moyenne
            CapZeroWhiteCard(
                modifier = Modifier.testTag("progress_contexts_card")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Contextes fréquents",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intensité moyenne des envies :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (metrics.averageCravingIntensity != null) {
                                String.format(Locale.FRANCE, "%.1f / 5", metrics.averageCravingIntensity)
                            } else {
                                "—"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("progress_avg_intensity_value")
                        )
                    }

                    if (metrics.topContexts.isEmpty()) {
                        Text(
                            text = "Aucun contexte renseigné (—). Ajoutez un contexte dans le Journal pour identifier vos habitudes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        metrics.topContexts.forEach { item ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.contextName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${item.count} (${item.percentage} %)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
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

@Composable
private fun LegendBadgeRow(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConsumptionVsCapChart(
    points: List<ChartDayPoint>,
    barColor: Color,
    capLineColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val maxVal = max(
            1,
            max(
                points.mapNotNull { it.realCount }.maxOrNull() ?: 1,
                points.mapNotNull { it.capCount }.maxOrNull() ?: 1
            )
        )

        val chartWidth = size.width
        val chartHeight = size.height
        val bottomPadding = 12.dp.toPx()
        val topPadding = 12.dp.toPx()
        val usableHeight = (chartHeight - bottomPadding - topPadding).coerceAtLeast(1f)

        for (i in 0..3) {
            val y = topPadding + usableHeight * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val slotWidth = chartWidth / points.size.toFloat()
        val barWidth = (slotWidth * 0.52f).coerceAtMost(28.dp.toPx())

        points.forEachIndexed { index, pt ->
            val centerX = slotWidth * index + slotWidth / 2f
            val real = pt.realCount
            if (real != null) {
                val fraction = (real.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val barHeight = max(4.dp.toPx(), usableHeight * fraction)
                val topY = topPadding + usableHeight - barHeight
                drawRoundRect(
                    color = if (real == 0) capLineColor.copy(alpha = 0.65f) else barColor,
                    topLeft = Offset(centerX - barWidth / 2f, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        val capPath = Path()
        var started = false
        points.forEachIndexed { index, pt ->
            val cap = pt.capCount
            if (cap != null) {
                val centerX = slotWidth * index + slotWidth / 2f
                val fraction = (cap.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val y = topPadding + usableHeight - (usableHeight * fraction)
                if (!started) {
                    capPath.moveTo(centerX, y)
                    started = true
                } else {
                    capPath.lineTo(centerX, y)
                }
                drawCircle(
                    color = capLineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, y)
                )
            }
        }
        if (started) {
            drawPath(
                path = capPath,
                color = capLineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
