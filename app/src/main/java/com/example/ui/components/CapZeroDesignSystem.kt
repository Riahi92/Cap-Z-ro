package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepGreenOutlineBorderLight
import com.example.ui.theme.EmeraldActionLight
import com.example.ui.theme.SoftMintPillLight
import com.example.ui.theme.VeryDarkGreenTextLight
import com.example.ui.theme.WarmAmberBadgeBgLight
import com.example.ui.theme.WarmAmberBadgeTextLight

/**
 * Carte blanche signature de Cap Zéro :
 * Fond blanc (#FFFFFF), bordure discrète (1.dp, #E2ECE8), ombre légère et angles arrondis de 20.dp.
 */
@Composable
fun CapZeroWhiteCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Bouton principal émeraude (#239878) en forme pilule, texte blanc (#FFFFFF), sans mots coupés.
 */
@Composable
fun EmeraldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldActionLight,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmeraldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    EmeraldPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

/**
 * Bouton secondaire à contour vert profond (#1F5E4B / #123D32) sur fond blanc (#FFFFFF) en forme pilule,
 * exactement comme les boutons « Une envie ? » et « Modifier mon plan » de la maquette.
 */
@Composable
fun DeepGreenOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, DeepGreenOutlineBorderLight),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = VeryDarkGreenTextLight
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DeepGreenOutlinedButton(
    text: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    DeepGreenOutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = EmeraldActionLight,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

/**
 * Badge pilule vert menthe doux (#DCF2E6, ex. Date du jour « 4 novembre » ou plafond « ≤ 12 »).
 */
@Composable
fun SoftMintPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        color = SoftMintPillLight,
        shape = CircleShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VeryDarkGreenTextLight,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = VeryDarkGreenTextLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Badge pilule ambre doux (#F9EBD2 / #9E5A16, ex. « Objectif personnel » ou date d'arrêt « 3 janv. 2027 »).
 */
@Composable
fun WarmAmberPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        color = WarmAmberBadgeBgLight,
        shape = CircleShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = WarmAmberBadgeTextLight,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = WarmAmberBadgeTextLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Icône vectorielle de la feuille « Cap Zéro » bicolore pour l'en-tête.
 */
@Composable
fun CapZeroBrandLeafIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(26.dp)) {
        val w = size.width
        val h = size.height

        // Moitié gauche vert profond (#145A43)
        val leftLeaf = Path().apply {
            moveTo(w * 0.18f, h * 0.85f)
            cubicTo(w * 0.05f, h * 0.45f, w * 0.35f, h * 0.12f, w * 0.86f, h * 0.12f)
            lineTo(w * 0.18f, h * 0.85f)
            close()
        }
        drawPath(path = leftLeaf, color = Color(0xFF145A43))

        // Moitié droite émeraude lumineux (#4CC39A)
        val rightLeaf = Path().apply {
            moveTo(w * 0.18f, h * 0.85f)
            lineTo(w * 0.86f, h * 0.12f)
            cubicTo(w * 0.92f, h * 0.62f, w * 0.58f, h * 0.92f, w * 0.18f, h * 0.85f)
            close()
        }
        drawPath(path = rightLeaf, color = Color(0xFF4CC39A))

        // Nervure centrale
        drawLine(
            color = Color(0xFF123D32),
            start = Offset(w * 0.14f, h * 0.89f),
            end = Offset(w * 0.84f, h * 0.14f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Icône vectorielle du souffle / brise (utilisée dans le bouton « Une envie ? »,
 * l'onglet « Envie » et le minuteur central).
 */
@Composable
fun WindBreathVectorIcon(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Dp = 2.2.dp
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = strokeWidthDp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Ligne supérieure avec boucle vers le haut
        val topWave = Path().apply {
            moveTo(w * 0.12f, h * 0.36f)
            lineTo(w * 0.66f, h * 0.36f)
            cubicTo(w * 0.82f, h * 0.36f, w * 0.84f, h * 0.16f, w * 0.68f, h * 0.16f)
            cubicTo(w * 0.56f, h * 0.16f, w * 0.55f, h * 0.26f, w * 0.60f, h * 0.28f)
        }
        drawPath(path = topWave, color = color, style = stroke)

        // Ligne médiane longue avec boucle à droite
        val midWave = Path().apply {
            moveTo(w * 0.08f, h * 0.54f)
            lineTo(w * 0.78f, h * 0.54f)
            cubicTo(w * 0.95f, h * 0.54f, w * 0.95f, h * 0.76f, w * 0.78f, h * 0.76f)
            cubicTo(w * 0.66f, h * 0.76f, w * 0.65f, h * 0.66f, w * 0.70f, h * 0.64f)
        }
        drawPath(path = midWave, color = color, style = stroke)

        // Ligne inférieure douce
        val bottomWave = Path().apply {
            moveTo(w * 0.18f, h * 0.72f)
            lineTo(w * 0.52f, h * 0.72f)
            cubicTo(w * 0.64f, h * 0.72f, w * 0.64f, h * 0.88f, w * 0.52f, h * 0.88f)
        }
        drawPath(path = bottomWave, color = color, style = stroke)
    }
}

/**
 * Illustration décorative de jeunes pousses vert tendre (affichée en haut à droite de l'Accueil
 * et au bas de l'écran Envie comme sur la maquette).
 */
@Composable
fun DecorativeSproutIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Colline douce en arrière-plan (#E6F4EC)
        val hillPath = Path().apply {
            moveTo(w * 0.05f, h)
            quadraticTo(w * 0.55f, h * 0.42f, w, h * 0.72f)
            lineTo(w, h)
            close()
        }
        drawPath(path = hillPath, color = Color(0xFFE5F3EC))

        // Tige principale
        val stemPath = Path().apply {
            moveTo(w * 0.58f, h * 0.95f)
            quadraticTo(w * 0.56f, h * 0.55f, w * 0.62f, h * 0.25f)
        }
        drawPath(
            path = stemPath,
            color = Color(0xFF79C4A0),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Feuille gauche
        val leftLeaf = Path().apply {
            moveTo(w * 0.58f, h * 0.58f)
            cubicTo(w * 0.30f, h * 0.52f, w * 0.26f, h * 0.24f, w * 0.48f, h * 0.28f)
            cubicTo(w * 0.56f, h * 0.32f, w * 0.59f, h * 0.46f, w * 0.58f, h * 0.58f)
            close()
        }
        drawPath(path = leftLeaf, color = Color(0xFFA5DFC2))

        // Feuille droite
        val rightLeaf = Path().apply {
            moveTo(w * 0.60f, h * 0.46f)
            cubicTo(w * 0.78f, h * 0.16f, w * 0.96f, h * 0.22f, w * 0.86f, h * 0.46f)
            cubicTo(w * 0.78f, h * 0.58f, w * 0.66f, h * 0.54f, w * 0.60f, h * 0.46f)
            close()
        }
        drawPath(path = rightLeaf, color = Color(0xFF88D1AE))
    }
}

/**
 * Illustration vectorielle du sommet avec drapeau pour la carte « Cap sur zéro » de l'écran Plan.
 */
@Composable
fun MountainPeakFlagIllustration(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Montagne arrière claire
        val backPeak = Path().apply {
            moveTo(w * 0.05f, h)
            lineTo(w * 0.42f, h * 0.44f)
            lineTo(w * 0.75f, h)
            close()
        }
        drawPath(path = backPeak, color = Color(0xFFD9F0E3))

        // Montagne principale
        val mainPeak = Path().apply {
            moveTo(w * 0.28f, h)
            lineTo(w * 0.68f, h * 0.28f)
            lineTo(w * 0.98f, h)
            close()
        }
        drawPath(path = mainPeak, color = Color(0xFFA7DFC2))

        // Versant ombré droit
        val rightSlope = Path().apply {
            moveTo(w * 0.68f, h * 0.28f)
            lineTo(w * 0.98f, h)
            lineTo(w * 0.66f, h)
            close()
        }
        drawPath(path = rightSlope, color = Color(0xFF7BC8A2))

        // Sentier sinueux blanc vers le sommet
        val trail = Path().apply {
            moveTo(w * 0.55f, h)
            quadraticTo(w * 0.72f, h * 0.72f, w * 0.64f, h * 0.54f)
            quadraticTo(w * 0.60f, h * 0.42f, w * 0.68f, h * 0.28f)
        }
        drawPath(
            path = trail,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Mât du drapeau
        drawLine(
            color = Color(0xFFA6631B),
            start = Offset(w * 0.68f, h * 0.28f),
            end = Offset(w * 0.68f, h * 0.06f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Fanion orange/ambre
        val flag = Path().apply {
            moveTo(w * 0.68f, h * 0.06f)
            lineTo(w * 0.86f, h * 0.12f)
            lineTo(w * 0.68f, h * 0.19f)
            close()
        }
        drawPath(path = flag, color = Color(0xFFE6932A))
    }
}
