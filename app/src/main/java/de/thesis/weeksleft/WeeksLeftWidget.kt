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
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), DEADLINE)
        val weeks = daysLeft / 7 // whole weeks, rounded down

        val (number, label) = when {
            daysLeft < 0 -> "✓" to "submitted"
            weeks == 0L -> "0" to "weeks · final week"
            weeks == 1L -> "1" to "week left"
            else -> weeks.toString() to "weeks left"
        }

        return RemoteViews(context.packageName, R.layout.widget_weeks_left).apply {
            setTextViewText(R.id.weeks_number, number)
            setTextViewText(R.id.weeks_label, label)
        }
    }
}
