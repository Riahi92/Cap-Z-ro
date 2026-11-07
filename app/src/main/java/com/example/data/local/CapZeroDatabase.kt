package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CigaretteLogEntity::class,
        DailyPlanCapEntity::class,
        DailyStatusEntity::class,
        CravingEventEntity::class,
        PackPurchaseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CapZeroDatabase : RoomDatabase() {

    abstract fun capZeroDao(): CapZeroDao

    companion object {
        @Volatile
        private var INSTANCE: CapZeroDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pack_purchases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestampMillis` INTEGER NOT NULL,
                        `localEpochDay` INTEGER NOT NULL,
                        `packsCount` INTEGER NOT NULL,
                        `cigarettesPerPack` INTEGER NOT NULL,
                        `unitPriceMillimes` INTEGER NOT NULL,
                        `totalPriceMillimes` INTEGER NOT NULL,
                        `note` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pack_purchases_localEpochDay` ON `pack_purchases` (`localEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pack_purchases_timestampMillis` ON `pack_purchases` (`timestampMillis`)")
            }
        }

        fun getDatabase(context: Context): CapZeroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CapZeroDatabase::class.java,
                    "cap_zero_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
