package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entrée de cigarette enregistrée par l'utilisateur.
 * Conserve l'horodatage exact (UTC ms) ET le jour local (Epoch Day selon le fuseau horaire au moment de la saisie).
 */
@Entity(
    tableName = "cigarette_logs",
    indices = [Index(value = ["localEpochDay"]), Index(value = ["timestampMillis"])]
)
data class CigaretteLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val cravingIntensity: Int? = null, // 1 à 5 (facultatif lors du clic rapide, modifiable dans le journal)
    val contextCategory: String? = null, // Contexte prédéfini (facultatif)
    val note: String? = null // Note libre (facultative)
)

/**
 * Plafond quotidien prévu dans le calendrier du plan de réduction.
 */
@Entity(tableName = "daily_plan_caps")
data class DailyPlanCapEntity(
    @PrimaryKey val localEpochDay: Long,
    val capCount: Int,
    val isManualOverride: Boolean = false
)

/**
 * Statut explicite d'une journée locale.
 * Permet de distinguer un jour sans cigarette confirmé (ou un jour d'observation complet confirmé)
 * d'un jour simplement non renseigné.
 */
@Entity(tableName = "daily_statuses")
data class DailyStatusEntity(
    @PrimaryKey val localEpochDay: Long,
    val isConfirmedComplete: Boolean = true
)

/**
 * Historique des envies gérées via l'écran « Une envie ? ».
 */
@Entity(
    tableName = "craving_events",
    indices = [Index(value = ["localEpochDay"])]
)
data class CravingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val outcome: String, // "PASSED" ou "SMOKED"
    val replacementActivity: String? = null
)

/**
 * Achat de paquet(s) de cigarettes enregistré par l'utilisateur.
 * Permet de suivre le nombre de paquets achetés, leur prix d'achat en TND (stocké en millimes)
 * et l'inventaire automatique de cigarettes restantes.
 */
@Entity(
    tableName = "pack_purchases",
    indices = [Index(value = ["localEpochDay"]), Index(value = ["timestampMillis"])]
)
data class PackPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val localEpochDay: Long,
    val packsCount: Int,
    val cigarettesPerPack: Int,
    val unitPriceMillimes: Long,
    val totalPriceMillimes: Long,
    val note: String? = null
)
