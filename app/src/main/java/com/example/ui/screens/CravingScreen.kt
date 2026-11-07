package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CapZeroWhiteCard
import com.example.ui.components.ConciseHelpBanner
import com.example.ui.components.DecorativeSproutIllustration
import com.example.ui.components.DeepGreenOutlinedButton
import com.example.ui.components.EmeraldPrimaryButton
import com.example.ui.components.WindBreathVectorIcon
import com.example.ui.viewmodel.CapZeroUiState
import java.util.Locale

private data class FeaturedReplacementActivity(
    val title: String,
    val icon: ImageVector
)

private val FEATURED_ACTIVITIES = listOf(
    FeaturedReplacementActivity("Boire\nde l'eau", Icons.Outlined.WaterDrop),
    FeaturedReplacementActivity("Marcher", Icons.Outlined.DirectionsWalk),
    FeaturedReplacementActivity("Faire\nune pause", Icons.Outlined.LocalCafe)
)

private val EXTRA_ACTIVITIES = listOf(
    "10 respirations profondes",
    "Étirer les épaules",
    "Occuper ses mains"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CravingScreen(
    uiState: CapZeroUiState,
    onToggleTimer: () -> Unit,
    onResetSession: () -> Unit,
    onSelectActivity: (String?) -> Unit,
    onCravingPassedClick: () -> Unit,
    onCravingSmokedSingleClick: () -> Unit,
    onUndoLastAction: () -> Unit
) {
    val timer = uiState.cravingTimer
    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60
    val formattedCountdown = String.format(Locale.FRANCE, "%02d:%02d", minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "breathing_transition")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val subtitleInsideRing = when {
        !timer.isRunning && timer.remainingSeconds == 300 -> "À votre rythme"
        breathScale < 0.8f -> "Inspirez lentement..."
        else -> "Expirez doucement..."
    }

    // Progression de l'arc circulaire (de 0.25 par défaut au repos comme sur la maquette, ou proportionnelle au temps)
    val arcSweepAngle = if (!timer.isRunning && timer.remainingSeconds == 300) {
        95f
    } else {
        val elapsedFraction = ((300 - timer.remainingSeconds).toFloat() / 300f).coerceIn(0.05f, 1f)
        360f * elapsedFraction
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // En-tête « Une envie ? » / « Prenons un moment »
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Une envie ?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Prenons un moment",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // 1. Minuteur circulaire central (fidèle à la maquette)
            val ringTrackColor = androidx.compose.ui.graphics.Color(0xFFCBEEDB)
            val ringProgressColor = androidx.compose.ui.graphics.Color(0xFF239878)
            val innerFillColor = androidx.compose.ui.graphics.Color(0xFFEAF6F0)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .testTag("craving_timer_card")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 14.dp.toPx()
                    val diameter = size.minDimension - strokePx
                    val topLeftOffset = androidx.compose.ui.geometry.Offset(strokePx / 2f, strokePx / 2f)
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Halo intérieur doux
                    drawCircle(
                        color = innerFillColor,
                        radius = (diameter / 2f) * (if (timer.isRunning) (0.85f + 0.12f * breathScale) else 0.94f)
                    )

                    // Anneau complet vert menthe (#CBEEDB)
                    drawArc(
                        color = ringTrackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // Arc de progression émeraude (#239878)
                    drawArc(
                        color = ringProgressColor,
                        startAngle = -90f,
                        sweepAngle = arcSweepAngle,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WindBreathVectorIcon(
                        color = androidx.compose.ui.graphics.Color(0xFF1D6E55),
                        modifier = Modifier.size(44.dp),
                        strokeWidthDp = 2.8.dp
                    )
                    Text(
                        text = formattedCountdown,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            lineHeight = 52.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("craving_timer_display")
                    )
                    Text(
                        text = subtitleInsideRing,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Bouton principal émeraude « Commencer / Pause »
            EmeraldPrimaryButton(
                text = if (timer.isRunning) "Pause" else "Commencer",
                onClick = onToggleTimer,
                leadingIcon = {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                modifier = Modifier.testTag("craving_toggle_timer_button")
            )

            if (timer.remainingSeconds < 300 || timer.isRunning) {
                TextButton(
                    onClick = onResetSession,
                    modifier = Modifier.testTag("craving_reset_session_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Réinitialiser le minuteur (05:00)")
                }
            }

            // 3. Trois cartes d'activités de remplacement (Boire de l'eau, Marcher, Faire une pause)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isVeryNarrow = maxWidth < 310.dp
                if (isVeryNarrow) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FEATURED_ACTIVITIES.forEachIndexed { idx, item ->
                            val cleanName = item.title.replace("\n", " ")
                            val isSelected = timer.selectedActivity == cleanName
                            ReplacementActivityCard(
                                title = cleanName,
                                icon = item.icon,
                                isSelected = isSelected,
                                onClick = {
                                    onSelectActivity(if (isSelected) null else cleanName)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("craving_activity_chip_$idx")
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FEATURED_ACTIVITIES.forEachIndexed { idx, item ->
                            val cleanName = item.title.replace("\n", " ")
                            val isSelected = timer.selectedActivity == cleanName
                            ReplacementActivityCard(
                                title = item.title,
                                icon = item.icon,
                                isSelected = isSelected,
                                onClick = {
                                    onSelectActivity(if (isSelected) null else cleanName)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("craving_activity_chip_$idx")
                            )
                        }
                    }
                }
            }

            // Puces d'activités complémentaires
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                EXTRA_ACTIVITIES.forEachIndexed { idx, act ->
                    val isSelected = timer.selectedActivity == act
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectActivity(if (isSelected) null else act) },
                        label = { Text(act) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("craving_activity_chip_${idx + 3}")
                    )
                }
            }

            // 4. Boutons « L'envie est passée » et « J'ai fumé » (sans double enregistrement)
            if (timer.sessionResolvedOutcome == null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmeraldPrimaryButton(
                        text = stringResource(id = R.string.btn_craving_passed),
                        onClick = onCravingPassedClick,
                        leadingIcon = {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        },
                        modifier = Modifier.testTag("craving_passed_button")
                    )

                    DeepGreenOutlinedButton(
                        text = stringResource(id = R.string.btn_i_smoked),
                        onClick = onCravingSmokedSingleClick,
                        leadingIcon = {
                            Icon(Icons.Filled.SmokingRooms, contentDescription = null)
                        },
                        modifier = Modifier.testTag("craving_smoked_button")
                    )
                }
            } else {
                CapZeroWhiteCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.testTag("craving_session_resolved_card")
                ) {
                    Text(
                        text = if (timer.sessionResolvedOutcome == "PASSED") {
                            "Bravo ! Vous avez laissé passer cette envie."
                        } else {
                            "Cigarette enregistrée une seule fois dans le journal."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (timer.sessionResolvedOutcome == "SMOKED" && uiState.pendingUndo != null) {
                            OutlinedButton(
                                onClick = onUndoLastAction,
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("craving_undo_smoked_button")
                            ) {
                                Icon(Icons.Filled.Undo, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Annuler la saisie")
                            }
                        }
                        Button(
                            onClick = onResetSession,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("craving_new_session_button")
                        ) {
                            Text("Nouvelle session")
                        }
                    }
                }
            }

            ConciseHelpBanner(
                shortSummary = "Le minuteur reste exact même en arrière-plan.",
                helpTitle = "Comment traverser une envie",
                helpDetails = "Une envie intense dure rarement plus de 5 minutes. Lancez le minuteur, choisissez une activité de remplacement et validez lorsque l'envie est passée.",
                testTagPrefix = "craving_help"
            )

            DecorativeSproutIllustration(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(96.dp, 64.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReplacementActivityCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .heightIn(min = 104.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
