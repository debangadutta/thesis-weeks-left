package de.thesis.weeksleft

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Change this if the submission date ever moves. */
val DEADLINE: LocalDate = LocalDate.of(2027, 4, 28)

/** At or below this many days left, the widget counts days instead of weeks. */
const val DAYS_MODE_THRESHOLD = 60L

class WeeksLeftWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    // Refresh immediately when the phone's clock or time zone changes.
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeeksLeftWidget::class.java))
            onUpdate(context, manager, ids)
        }
    }

    private fun buildViews(context: Context): RemoteViews {
        // Never goes below zero, so the widget just stops at 0 after the deadline.
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), DEADLINE).coerceAtLeast(0)

        // Over 60 days: whole weeks, rounded down (always 8 or more, so always plural).
        // 60 days or fewer: count down in days instead.
        val (number, label) = when {
            daysLeft > DAYS_MODE_THRESHOLD -> (daysLeft / 7).toString() to "weeks left"
            daysLeft == 1L -> "1" to "day left"
            else -> daysLeft.toString() to "days left"
        }

        return RemoteViews(context.packageName, R.layout.widget_weeks_left).apply {
            setTextViewText(R.id.weeks_number, number)
            setTextViewText(R.id.weeks_label, label)
        }
    }
}
