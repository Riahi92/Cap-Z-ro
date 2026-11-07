package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DailyPlanCapEntity
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.ConciseHelpBanner
import com.example.ui.components.DeepGreenOutlinedButton
import com.example.ui.components.MountainPeakFlagIllustration
import com.example.ui.components.SoftMintPillBadge
import com.example.ui.components.UnifiedPlanFlowCard
import com.example.ui.components.WarmAmberPillBadge
import com.example.ui.viewmodel.CapZeroUiState
import com.example.util.DateAndCurrencyUtils
import com.example.util.PlanStepPreview
import kotlin.math.max

@Composable
fun PlanScreen(
    uiState: CapZeroUiState,
    onModifySingleDayCap: (localEpochDay: Long, newCap: Int) -> Unit,
    onSaveNewOrRecalculatedPlan: (
        referenceCigarettes: Int,
        startEpochDay: Long,
        quitDateEpochDay: Long,
        steps: List<PlanStepPreview>
    ) -> Unit
) {
    var showPlanEditor by remember { mutableStateOf(false) }
    val cigsByDay = remember(uiState.cigarettes) {
        uiState.cigarettes.groupBy { it.localEpochDay }.mapValues { it.value.size }
    }
    val sortedCaps = remember(uiState.planCaps) {
        uiState.planCaps.sortedBy { it.localEpochDay }
    }
    val hasValidatedPlan = sortedCaps.isNotEmpty()

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
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Mon plan",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            if (hasValidatedPlan) {
                val startCapEntity = sortedCaps.first()
                val quitCapEntity = sortedCaps.last()
                val currentStepEntity = sortedCaps.firstOrNull { it.localEpochDay == uiState.todayEpochDay }
                    ?: sortedCaps.lastOrNull { it.localEpochDay <= uiState.todayEpochDay }
                    ?: startCapEntity

                val nextReductionEntity = sortedCaps.firstOrNull {
                    it.localEpochDay > currentStepEntity.localEpochDay && it.capCount < currentStepEntity.capCount
                } ?: quitCapEntity

                // Carte principale « Cap sur zéro » avec graphique en paliers réel et prochaines étapes réelles
                item {
                    CapZeroWhiteCard(
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier.testTag("plan_main_overview_card")
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MountainPeakFlagIllustration(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(116.dp, 80.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 90.dp)
                            ) {
                                WarmAmberPillBadge(
                                    text = "Objectif personnel",
                                    icon = Icons.Outlined.TrackChanges
                                )
                                Text(
                                    text = "Cap sur zéro",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = DateAndCurrencyUtils.formatFullDateFrench(quitCapEntity.localEpochDay),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Parcours de réduction validé (${sortedCaps.size} jours)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Graphique en paliers construit exclusivement avec les dates et valeurs réelles du plan validé
                        StepReductionPlanChart(
                            planSteps = sortedCaps,
                            lineColor = MaterialTheme.colorScheme.primary,
                            fillColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            gridColor = MaterialTheme.colorScheme.outline,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .testTag("plan_step_reduction_chart")
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        // Ligne 1 : Étape actuelle (valeur réelle du jour)
                        PlanMilestoneRow(
                            icon = Icons.Outlined.CalendarToday,
                            title = "Étape actuelle",
                            subtitle = "Rester à ${currentStepEntity.capCount} cigarette(s) ou moins.",
                            badgeContent = {
                                SoftMintPillBadge(text = "≤ ${currentStepEntity.capCount}")
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        // Ligne 2 : Prochaine étape (prochaine réduction réelle dans le plan)
                        PlanMilestoneRow(
                            icon = Icons.Outlined.Flag,
                            title = "Prochaine étape",
                            subtitle = if (nextReductionEntity.localEpochDay == quitCapEntity.localEpochDay) {
                                "Atteindre 0 cigarette le ${DateAndCurrencyUtils.formatShortDate(quitCapEntity.localEpochDay)}."
                            } else {
                                "Passer à ${nextReductionEntity.capCount} cig./j le ${DateAndCurrencyUtils.formatShortDate(nextReductionEntity.localEpochDay)}."
                            },
                            badgeContent = {
                                SoftMintPillBadge(text = "≤ ${nextReductionEntity.capCount}")
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                        // Ligne 3 : Date d'arrêt réelle
                        PlanMilestoneRow(
                            icon = Icons.Outlined.TrackChanges,
                            title = "Date d'arrêt",
                            subtitle = "Objectif d'arrêt complet.",
                            badgeContent = {
                                WarmAmberPillBadge(
                                    text = DateAndCurrencyUtils.formatMediumDateFrench(quitCapEntity.localEpochDay)
                                )
                            }
                        )
                    }
                }

                // Bouton « Modifier mon plan » comme sur la maquette
                item {
                    DeepGreenOutlinedButton(
                        text = if (showPlanEditor) "Masquer la modification du plan" else "Modifier mon plan",
                        onClick = { showPlanEditor = !showPlanEditor },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.testTag("plan_open_recalculate_dialog_button")
                    )
                }
            }

            // Configurateur unifié (affiché directement si aucun plan n'est validé, ou après clic sur « Modifier mon plan »)
            if (!hasValidatedPlan || showPlanEditor) {
                item {
                    UnifiedPlanFlowCard(
                        todayEpochDay = uiState.todayEpochDay,
                        initialManualReference = uiState.preferences.referenceDailyCigarettes,
                        confirmedObservationDaysCount = uiState.confirmedObservationDaysCount,
                        computedObservationReference = uiState.computedObservationReference,
                        onValidatePlan = { refCount, startDay, quitDay, steps ->
                            onSaveNewOrRecalculatedPlan(refCount, startDay, quitDay, steps)
                            showPlanEditor = false
                        }
                    )
                }
            }

            item {
                ConciseHelpBanner(
                    shortSummary = stringResource(id = R.string.plan_disclaimer_short),
                    helpTitle = "Fonctionnement du plan",
                    helpDetails = stringResource(id = R.string.plan_disclaimer),
                    testTagPrefix = "plan_help"
                )
            }

            // Liste détaillée des étapes quotidiennes réelles permettant d'ajuster manuellement chaque jour
            if (hasValidatedPlan) {
                item {
                    Text(
                        text = "Détail des étapes quotidiennes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(
                    items = uiState.planCaps,
                    key = { _, item -> item.localEpochDay }
                ) { index, capEntity ->
                    val isQuitDay = index == uiState.planCaps.lastIndex
                    val isToday = capEntity.localEpochDay == uiState.todayEpochDay
                    val actualSmoked = cigsByDay[capEntity.localEpochDay] ?: 0
                    val hasSmokedData = cigsByDay.containsKey(capEntity.localEpochDay)

                    CapZeroWhiteCard(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        containerColor = if (isToday) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.testTag("plan_day_row_${capEntity.localEpochDay}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = DateAndCurrencyUtils.formatFriendlyDay(
                                        capEntity.localEpochDay,
                                        uiState.todayEpochDay
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isToday || isQuitDay) FontWeight.Bold else FontWeight.SemiBold
                                )
                                Text(
                                    text = buildString {
                                        if (isQuitDay) {
                                            append("Date d'arrêt Cap Zéro (0 cigarette)")
                                        } else {
                                            append("Étape ${index + 1}/${uiState.planCaps.size}")
                                        }
                                        if (hasSmokedData || isToday) {
                                            append(" • Réel : $actualSmoked")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!isQuitDay) {
                                    IconButton(
                                        onClick = {
                                            onModifySingleDayCap(
                                                capEntity.localEpochDay,
                                                (capEntity.capCount - 1).coerceAtLeast(0)
                                            )
                                        },
                                        enabled = capEntity.capCount > 0,
                                        modifier = Modifier.testTag("plan_step_minus_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Diminuer le plafond"
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "≤ ${capEntity.capCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("plan_step_cap_value_$index")
                                    )
                                }

                                if (!isQuitDay) {
                                    IconButton(
                                        onClick = {
                                            onModifySingleDayCap(
                                                capEntity.localEpochDay,
                                                capEntity.capCount + 1
                                            )
                                        },
                                        modifier = Modifier.testTag("plan_step_plus_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Augmenter le plafond"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanMilestoneRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        badgeContent()
    }
}

/**
 * Graphique de réduction en paliers (escalier) dessiné en Jetpack Compose Canvas
 * exclusivement à partir des étapes réelles du plan validé.
 */
@Composable
private fun StepReductionPlanChart(
    planSteps: List<DailyPlanCapEntity>,
    lineColor: Color,
    fillColor: Color,
    gridColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    if (planSteps.isEmpty()) return

    val maxCap = max(1, planSteps.maxOf { it.capCount })
    val firstDay = planSteps.first().localEpochDay
    val midDay = planSteps[planSteps.size / 2].localEpochDay
    val lastDay = planSteps.last().localEpochDay

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Axe Y : valeurs réelles des plafonds
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(130.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "$maxCap", style = MaterialTheme.typography.labelMedium, color = textColor)
                Text(text = "${maxCap / 2}", style = MaterialTheme.typography.labelMedium, color = textColor)
                Text(text = "0", style = MaterialTheme.typography.labelMedium, color = textColor)
            }

            // Courbe en paliers (escalier)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 8.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val usableH = (h - topPad - bottomPad).coerceAtLeast(1f)

                // Axes de référence
                drawLine(
                    color = gridColor,
                    start = Offset(0f, topPad),
                    end = Offset(0f, topPad + usableH),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, topPad + usableH),
                    end = Offset(w, topPad + usableH),
                    strokeWidth = 1.dp.toPx()
                )

                val n = planSteps.size
                fun pointFor(index: Int): Offset {
                    val x = if (n <= 1) w else (index.toFloat() / (n - 1).toFloat()) * w
                    val cap = planSteps[index].capCount
                    val y = topPad + usableH - (cap.toFloat() / maxCap.toFloat()).coerceIn(0f, 1f) * usableH
                    return Offset(x, y)
                }

                val stepLinePath = Path()
                val fillPath = Path()

                val firstPt = pointFor(0)
                stepLinePath.moveTo(firstPt.x, firstPt.y)
                fillPath.moveTo(firstPt.x, topPad + usableH)
                fillPath.lineTo(firstPt.x, firstPt.y)

                for (i in 1 until n) {
                    val prevPt = pointFor(i - 1)
                    val currPt = pointFor(i)
                    // Palier horizontal puis descente verticale (style escalier de la maquette)
                    stepLinePath.lineTo(currPt.x, prevPt.y)
                    stepLinePath.lineTo(currPt.x, currPt.y)

                    fillPath.lineTo(currPt.x, prevPt.y)
                    fillPath.lineTo(currPt.x, currPt.y)
                }

                val lastPt = pointFor(n - 1)
                fillPath.lineTo(lastPt.x, topPad + usableH)
                fillPath.close()

                drawPath(path = fillPath, color = fillColor)
                drawPath(
                    path = stepLinePath,
                    color = lineColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Points circulaires au départ et à l'arrivée (0 cigarette)
                listOf(firstPt, lastPt).forEach { pt ->
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                    drawCircle(
                        color = lineColor,
                        radius = 5.dp.toPx(),
                        center = pt,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Axe X : dates réelles du plan validé
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = DateAndCurrencyUtils.formatChartDate(firstDay),
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
            if (planSteps.size > 2) {
                Text(
                    text = DateAndCurrencyUtils.formatChartDate(midDay),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
            }
            Text(
                text = DateAndCurrencyUtils.formatMediumDateFrench(lastDay),
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
