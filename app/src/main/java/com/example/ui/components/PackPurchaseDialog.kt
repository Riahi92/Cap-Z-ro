package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepGreenOutlineBorderLight
import com.example.ui.theme.EmeraldActionLight
import com.example.ui.theme.SoftMintPillLight
import com.example.ui.theme.VeryDarkGreenTextLight
import com.example.ui.viewmodel.CigaretteInventorySummary
import com.example.util.DateAndCurrencyUtils
import java.util.Locale

/**
 * Carte récapitulative de l'inventaire de cigarettes et des paquets achetés,
 * respectant la charte visuelle Cap Zéro (fond blanc #FFFFFF, bordure #E2ECE8, angles 20.dp).
 */
@Composable
fun InventorySummaryCard(
    inventorySummary: CigaretteInventorySummary,
    onBuyPackClick: () -> Unit,
    onAdjustStockClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "inventory"
) {
    val totalSpentText = DateAndCurrencyUtils.formatTnd(inventorySummary.totalSpentTnd)

    CapZeroWhiteCard(
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.testTag("${testTagPrefix}_summary_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = SoftMintPillLight,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = "Inventaire de cigarettes",
                        tint = VeryDarkGreenTextLight,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Inventaire & Paquets",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Déduction auto. à chaque cigarette fumée",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SoftMintPillBadge(
                text = "${inventorySummary.remainingCigarettes} cig. en stock",
                modifier = Modifier.testTag("${testTagPrefix}_stock_badge")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Cigarettes restantes en stock
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "En stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${inventorySummary.remainingCigarettes} cig.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("${testTagPrefix}_remaining_cigs_value")
                    )
                }
            }

            // 2. Nombre de paquets achetés
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Paquets achetés",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${inventorySummary.totalPacksPurchased}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("${testTagPrefix}_packs_bought_value")
                    )
                }
            }

            // 3. Prix total d'achat en TND
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1.25f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Prix d'achat",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalSpentText,
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldActionLight,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("${testTagPrefix}_total_spent_value")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBuyPackClick,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, DeepGreenOutlineBorderLight),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = VeryDarkGreenTextLight
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("${testTagPrefix}_buy_pack_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = EmeraldActionLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Acheter",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onAdjustStockClick != null) {
                OutlinedButton(
                    onClick = onAdjustStockClick,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, DeepGreenOutlineBorderLight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = VeryDarkGreenTextLight
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_adjust_stock_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajuster stock",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Fenêtre de dialogue pour modifier manuellement le stock de cigarettes restantes.
 */
@Composable
fun AdjustInventoryDialog(
    currentStock: Int,
    cigarettesPerPack: Int = 20,
    onDismiss: () -> Unit,
    onConfirm: (newStock: Int) -> Unit
) {
    var stockValue by remember { mutableIntStateOf(currentStock.coerceAtLeast(0)) }
    var textInput by remember { mutableStateOf(stockValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modifier le stock de cigarettes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Ajustez manuellement le nombre de cigarettes actuellement en votre possession.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (stockValue > 0) {
                                    stockValue -= 1
                                    textInput = stockValue.toString()
                                }
                            },
                            enabled = stockValue > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("adjust_stock_dialog_decrement")
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Diminuer")
                        }

                        Text(
                            text = "$stockValue cig.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("adjust_stock_dialog_count_text")
                        )

                        IconButton(
                            onClick = {
                                stockValue += 1
                                textInput = stockValue.toString()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("adjust_stock_dialog_increment")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Augmenter")
                        }
                    }
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 4) {
                            textInput = value
                            stockValue = value.toIntOrNull() ?: 0
                        }
                    },
                    label = { Text("Nombre exact de cigarettes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_stock_dialog_input")
                )

                val fullPacks = stockValue / cigarettesPerPack.coerceAtLeast(1)
                val remainingCigs = stockValue % cigarettesPerPack.coerceAtLeast(1)

                Surface(
                    color = SoftMintPillLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Équivaut à $fullPacks paquet(s) et $remainingCigs cigarette(s) entamée(s).",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeryDarkGreenTextLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(stockValue.coerceAtLeast(0)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldActionLight,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("adjust_stock_dialog_confirm_button")
            ) {
                Text("Enregistrer le stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("adjust_stock_dialog_cancel_button")
            ) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Fenêtre de saisie d'un achat de paquet(s) de cigarettes avec nombre de paquets,
 * prix d'achat unitaire en TND (3 décimales), nombre de cigarettes par paquet et note facultative.
 */
@Composable
fun PackPurchaseDialog(
    defaultPackPriceMillimes: Long?,
    defaultCigarettesPerPack: Int?,
    onDismiss: () -> Unit,
    onConfirm: (packsCount: Int, cigarettesPerPack: Int, unitPriceMillimes: Long, note: String?) -> Unit
) {
    var packsCount by remember { mutableIntStateOf(1) }
    var unitPriceText by remember {
        val defaultTnd = (defaultPackPriceMillimes ?: 10_000L).toDouble() / 1000.0
        mutableStateOf(String.format(Locale.FRANCE, "%.3f", defaultTnd))
    }
    var cigarettesPerPackText by remember {
        mutableStateOf((defaultCigarettesPerPack ?: 20).toString())
    }
    var noteText by remember { mutableStateOf("") }

    val parsedUnitPriceMillimes = DateAndCurrencyUtils.parseTndToMillimes(unitPriceText) ?: (defaultPackPriceMillimes ?: 10_000L)
    val parsedCigarettesPerPack = (cigarettesPerPackText.toIntOrNull() ?: (defaultCigarettesPerPack ?: 20)).coerceIn(1, 100)
    val totalMillimesPreview = packsCount.toLong() * parsedUnitPriceMillimes.coerceAtLeast(0L)
    val totalCigarettesAdded = packsCount * parsedCigarettesPerPack

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enregistrer un achat de paquet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Nombre de paquets achetés
                Text(
                    text = "Nombre de paquets achetés",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (packsCount > 1) packsCount -= 1 },
                            enabled = packsCount > 1,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pack_dialog_decrement_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Diminuer le nombre de paquets"
                            )
                        }
                        Text(
                            text = "$packsCount paquet(s)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("pack_dialog_count_text")
                        )
                        IconButton(
                            onClick = { if (packsCount < 50) packsCount += 1 },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pack_dialog_increment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Augmenter le nombre de paquets"
                            )
                        }
                    }
                }

                // 2. Prix d'achat du paquet en TND (3 décimales)
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = it },
                    label = { Text("Prix d'achat par paquet (TND)") },
                    placeholder = { Text("Ex. 10,000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_dialog_price_input")
                )

                // 3. Cigarettes par paquet
                OutlinedTextField(
                    value = cigarettesPerPackText,
                    onValueChange = { valDigits ->
                        if (valDigits.length <= 3 && valDigits.all { it.isDigit() }) {
                            cigarettesPerPackText = valDigits
                        }
                    },
                    label = { Text("Cigarettes par paquet") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_dialog_cigs_per_pack_input")
                )

                // 4. Note facultative
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (facultatif)") },
                    placeholder = { Text("Ex. Marque, bureau de tabac...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_dialog_note_input")
                )

                // 5. Récapitulatif de l'ajout à l'inventaire et du coût total en TND
                Surface(
                    color = SoftMintPillLight,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ajout à l'inventaire : +$totalCigarettesAdded cigarettes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeryDarkGreenTextLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("pack_dialog_total_cigs_preview")
                        )
                        Text(
                            text = "Coût total d'achat : ${DateAndCurrencyUtils.formatMillimesToTnd(totalMillimesPreview)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VeryDarkGreenTextLight,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("pack_dialog_total_cost_preview")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        packsCount.coerceAtLeast(1),
                        parsedCigarettesPerPack,
                        parsedUnitPriceMillimes.coerceAtLeast(0L),
                        noteText.trim().takeIf { it.isNotEmpty() }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldActionLight,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("pack_dialog_confirm_button")
            ) {
                Text("Enregistrer l'achat", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("pack_dialog_cancel_button")
            ) {
                Text("Annuler")
            }
        }
    )
}
