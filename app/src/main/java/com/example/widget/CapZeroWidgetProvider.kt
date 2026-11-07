package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.CapZeroDao
import com.example.data.local.CapZeroDatabase
import com.example.data.local.CigaretteLogEntity
import com.example.util.DateAndCurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.TimeZone

/**
 * Widget d'écran d'accueil minimaliste pour Cap Zéro.
 * Permet de consulter le compteur et le plafond du jour ou d'enregistrer une cigarette
 * en un seul clic sans ouvrir l'application.
 */
class CapZeroWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateAllWidgetsFromDao(
                    context = context,
                    dao = resolveDao(context),
                    nowMillis = resolveNowMillis(),
                    timeZone = resolveTimeZone()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_QUICK_LOG_CIGARETTE || action == ACTION_REFRESH_WIDGET) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    processWidgetAction(
                        context = context,
                        action = action,
                        dao = resolveDao(context),
                        nowMillis = resolveNowMillis(),
                        timeZone = resolveTimeZone()
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_QUICK_LOG_CIGARETTE = "com.example.widget.ACTION_QUICK_LOG_CIGARETTE"
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        private const val REQUEST_CODE_SMOKE = 1001
        private const val REQUEST_CODE_REFRESH_CAP = 1002
        private const val REQUEST_CODE_OPEN_APP = 1003

        // Overrides optionnels pour les tests unitaires / Robolectric
        @Volatile
        var daoOverrideForTesting: CapZeroDao? = null

        @Volatile
        var timeZoneOverrideForTesting: TimeZone? = null

        @Volatile
        var nowMillisOverrideForTesting: Long? = null

        private fun resolveDao(context: Context): CapZeroDao {
            return daoOverrideForTesting ?: CapZeroDatabase.getDatabase(context.applicationContext).capZeroDao()
        }

        private fun resolveTimeZone(): TimeZone {
            return timeZoneOverrideForTesting ?: TimeZone.getDefault()
        }

        private fun resolveNowMillis(): Long {
            return nowMillisOverrideForTesting ?: System.currentTimeMillis()
        }

        /**
         * Traite une action en un clic du widget (enregistrement d'une cigarette ou actualisation du plafond)
         * sans ouvrir l'application, puis rafraîchit toutes les instances du widget.
         */
        suspend fun processWidgetAction(
            context: Context,
            action: String?,
            dao: CapZeroDao = resolveDao(context),
            nowMillis: Long = resolveNowMillis(),
            timeZone: TimeZone = resolveTimeZone()
        ) {
            val todayEpochDay = DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZone)
            if (action == ACTION_QUICK_LOG_CIGARETTE) {
                val entity = CigaretteLogEntity(
                    timestampMillis = nowMillis,
                    localEpochDay = todayEpochDay,
                    cravingIntensity = null,
                    contextCategory = null,
                    note = null
                )
                dao.insertCigarette(entity)
                // Si la journée en cours avait été marquée confirmée complète, invalider cette confirmation
                val statuses = dao.getAllDailyStatusesSnapshot()
                val wasConfirmed = statuses.any { it.localEpochDay == todayEpochDay && it.isConfirmedComplete }
                if (wasConfirmed) {
                    dao.deleteDailyStatus(todayEpochDay)
                }
            }
            updateAllWidgetsFromDao(
                context = context,
                dao = dao,
                nowMillis = nowMillis,
                timeZone = timeZone
            )
        }

        /**
         * Calcule l'état actuel du jour (compteur + plafond du jour) depuis Room et met à jour tous les widgets.
         */
        suspend fun updateAllWidgetsFromDao(
            context: Context,
            dao: CapZeroDao = resolveDao(context),
            nowMillis: Long = resolveNowMillis(),
            timeZone: TimeZone = resolveTimeZone()
        ) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val componentName = ComponentName(appContext, CapZeroWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            val todayEpochDay = DateAndCurrencyUtils.toLocalEpochDay(nowMillis, timeZone)
            val cigarettes = dao.getAllCigarettesSnapshot()
            val planCaps = dao.getAllPlanCapsSnapshot()

            val todayCount = cigarettes.count { it.localEpochDay == todayEpochDay }
            val todayCap = planCaps.firstOrNull { it.localEpochDay == todayEpochDay }?.capCount
            val lastCigMillis = cigarettes.maxByOrNull { it.timestampMillis }?.timestampMillis

            val views = buildRemoteViews(
                context = appContext,
                todayCount = todayCount,
                todayCap = todayCap,
                lastCigaretteMillis = lastCigMillis,
                nowMillis = nowMillis
            )

            appWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        /**
         * Construit la vue RemoteViews minimaliste du widget Cap Zéro.
         */
        fun buildRemoteViews(
            context: Context,
            todayCount: Int,
            todayCap: Int?,
            lastCigaretteMillis: Long?,
            nowMillis: Long
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_cap_zero)

            val capText = if (todayCap != null) {
                "Plafond du jour : $todayCap"
            } else {
                "Plafond du jour : —"
            }
            val unitLabel = if (todayCount == 1) {
                "cigarette\naujourd'hui"
            } else {
                "cigarettes\naujourd'hui"
            }
            val subtitleText = if (lastCigaretteMillis != null) {
                "Dernière : ${DateAndCurrencyUtils.formatDurationSince(lastCigaretteMillis, nowMillis)}"
            } else if (todayCap != null) {
                val remaining = (todayCap - todayCount).coerceAtLeast(0)
                "Marge repère : $remaining"
            } else {
                context.getString(R.string.widget_tap_cap_hint)
            }

            views.setTextViewText(R.id.widget_today_count, todayCount.toString())
            views.setTextViewText(R.id.widget_today_unit_label, unitLabel)
            views.setTextViewText(R.id.widget_cap_badge, capText)
            views.setTextViewText(R.id.widget_status_subtitle, subtitleText)

            // 1. Clic sur « + J'ai fumé » : enregistre la cigarette en arrière-plan sans ouvrir l'application
            val smokeIntent = Intent(context, CapZeroWidgetProvider::class.java).apply {
                action = ACTION_QUICK_LOG_CIGARETTE
            }
            val smokePendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SMOKE,
                smokeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_smoke_button, smokePendingIntent)

            // 2. Clic sur le badge « Plafond du jour » : actualise immédiatement le plafond et le compteur sans ouvrir l'application
            val refreshCapIntent = Intent(context, CapZeroWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshCapPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_REFRESH_CAP,
                refreshCapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_cap_badge, refreshCapPendingIntent)

            // 3. Clic sur le titre « Cap Zéro » ou le compteur : ouvre l'application si souhaité
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_APP,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_brand_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_stats_container, openAppPendingIntent)

            return views
        }
    }
}
