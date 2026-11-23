package de.thesis.weeksleft

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

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
        val (number, unit) = when {
            daysLeft > DAYS_MODE_THRESHOLD -> (daysLeft / 7).toString() to "weeks"
            daysLeft == 1L -> "1" to "day"
            else -> daysLeft.toString() to "days"
        }

        return RemoteViews(context.packageName, R.layout.widget_weeks_left).apply {
            setImageViewBitmap(R.id.weeks_number, renderNumber(context, number))
            setTextViewText(R.id.weeks_label, unit)
            setContentDescription(android.R.id.background, "$number $unit left until thesis submission")
        }
    }

    /**
     * Draws the number in the dot-matrix font as a white image. Launchers often ignore
     * custom fonts in widgets, but always show images. The layout tints the image, so it
     * still follows light/dark mode, and the ImageView scales it to fit the widget.
     */
    private fun renderNumber(context: Context, text: String): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = context.resources.getFont(R.font.doto_extrabold)
            textSize = 160f
            color = Color.WHITE
        }
        // Use the height of all digits, so every number comes out the same height.
        val digits = Rect().also { paint.getTextBounds("0123456789", 0, 10, it) }
        val ink = Rect().also { paint.getTextBounds(text, 0, text.length, it) }

        val bitmap = Bitmap.createBitmap(
            ceil(ink.width().toFloat()).toInt().coerceAtLeast(1),
            digits.height().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bitmap).drawText(text, -ink.left.toFloat(), -digits.top.toFloat(), paint)
        return bitmap
    }
}
