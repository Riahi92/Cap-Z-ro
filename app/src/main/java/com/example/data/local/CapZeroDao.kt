package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CapZeroDao {

    // --- Cigarettes ---
    @Query("SELECT * FROM cigarette_logs ORDER BY timestampMillis DESC")
    fun observeAllCigarettes(): Flow<List<CigaretteLogEntity>>

    @Query("SELECT * FROM cigarette_logs ORDER BY timestampMillis ASC")
    suspend fun getAllCigarettesSnapshot(): List<CigaretteLogEntity>

    @Query("SELECT * FROM cigarette_logs WHERE id = :id LIMIT 1")
    suspend fun getCigaretteById(id: Long): CigaretteLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCigarette(entry: CigaretteLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCigarettes(entries: List<CigaretteLogEntity>)

    @Update
    suspend fun updateCigarette(entry: CigaretteLogEntity)

    @Query("UPDATE cigarette_logs SET contextCategory = :newCategory WHERE contextCategory = :oldCategory")
    suspend fun renameContextCategory(oldCategory: String, newCategory: String)

    @Query("DELETE FROM cigarette_logs WHERE id = :id")
    suspend fun deleteCigaretteById(id: Long)

    @Query("DELETE FROM cigarette_logs")
    suspend fun clearAllCigarettes()

    // --- Plan Quotidien ---
    @Query("SELECT * FROM daily_plan_caps ORDER BY localEpochDay ASC")
    fun observeAllPlanCaps(): Flow<List<DailyPlanCapEntity>>

    @Query("SELECT * FROM daily_plan_caps ORDER BY localEpochDay ASC")
    suspend fun getAllPlanCapsSnapshot(): List<DailyPlanCapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlanCap(cap: DailyPlanCapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlanCaps(caps: List<DailyPlanCapEntity>)

    @Query("DELETE FROM daily_plan_caps")
    suspend fun clearAllPlanCaps()

    @Transaction
    suspend fun replaceFullPlan(caps: List<DailyPlanCapEntity>) {
        clearAllPlanCaps()
        upsertPlanCaps(caps)
    }

    // --- Statuts des Jours (Jours sans cigarette confirmés / Jours d'observation confirmés) ---
    @Query("SELECT * FROM daily_statuses ORDER BY localEpochDay ASC")
    fun observeAllDailyStatuses(): Flow<List<DailyStatusEntity>>

    @Query("SELECT * FROM daily_statuses ORDER BY localEpochDay ASC")
    suspend fun getAllDailyStatusesSnapshot(): List<DailyStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStatus(status: DailyStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStatuses(statuses: List<DailyStatusEntity>)

    @Query("DELETE FROM daily_statuses WHERE localEpochDay = :localEpochDay")
    suspend fun deleteDailyStatus(localEpochDay: Long)

    @Query("DELETE FROM daily_statuses")
    suspend fun clearAllDailyStatuses()

    // --- Envies ---
    @Query("SELECT * FROM craving_events ORDER BY timestampMillis DESC")
    fun observeAllCravingEvents(): Flow<List<CravingEventEntity>>

    @Query("SELECT * FROM craving_events ORDER BY timestampMillis ASC")
    suspend fun getAllCravingEventsSnapshot(): List<CravingEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCravingEvent(event: CravingEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCravingEvents(events: List<CravingEventEntity>)

    @Query("DELETE FROM craving_events WHERE id = :id")
    suspend fun deleteCravingEventById(id: Long)

    @Query("DELETE FROM craving_events")
    suspend fun clearAllCravingEvents()

    // --- Achats de Paquets & Inventaire ---
    @Query("SELECT * FROM pack_purchases ORDER BY timestampMillis DESC")
    fun observeAllPackPurchases(): Flow<List<PackPurchaseEntity>>

    @Query("SELECT * FROM pack_purchases ORDER BY timestampMillis ASC")
    suspend fun getAllPackPurchasesSnapshot(): List<PackPurchaseEntity>

    @Query("SELECT * FROM pack_purchases WHERE id = :id LIMIT 1")
    suspend fun getPackPurchaseById(id: Long): PackPurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackPurchase(purchase: PackPurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackPurchases(purchases: List<PackPurchaseEntity>)

    @Query("DELETE FROM pack_purchases WHERE id = :id")
    suspend fun deletePackPurchaseById(id: Long)

    @Query("DELETE FROM pack_purchases")
    suspend fun clearAllPackPurchases()

    // --- Réinitialisation complète ---
    @Transaction
    suspend fun clearAllTables() {
        clearAllCigarettes()
        clearAllPlanCaps()
        clearAllDailyStatuses()
        clearAllCravingEvents()
        clearAllPackPurchases()
    }
}
