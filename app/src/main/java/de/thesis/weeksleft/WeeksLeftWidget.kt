package de.thesis.weeksleft

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val PREFS = "deadlines"

/**
 * The deadline saved for one widget. A widget with no date yet gets one month from today,
 * saved straight away so it counts down from then on instead of moving with the calendar.
 */
fun loadDeadline(context: Context, id: Int): LocalDate {
    val day = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("w$id", Long.MIN_VALUE)
    if (day != Long.MIN_VALUE) return LocalDate.ofEpochDay(day)
    return LocalDate.now().plusMonths(1).also { saveDeadline(context, id, it) }
}

fun saveDeadline(context: Context, id: Int, date: LocalDate) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putLong("w$id", date.toEpochDay()).apply()
}

/** At or below this many days left, the widget counts days instead of weeks. */
const val DAYS_MODE_THRESHOLD = 60L

/** Widgets at least this many dp wide and tall get the 2x2 layout; smaller ones get 1x1. */
private const val LARGE_MIN_DP = 110f

class WeeksLeftWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context, manager, it)) }
    }

    // Widgets removed from the home screen: forget their deadlines.
    override fun onDeleted(context: Context, ids: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        ids.forEach { prefs.remove("w$it") }
        prefs.apply()
    }

    // Resized on the home screen. Android 12+ swaps layouts itself; older versions need this.
    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, options: Bundle,
    ) {
        manager.updateAppWidget(id, buildViews(context, manager, id))
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

    private fun buildViews(context: Context, manager: AppWidgetManager, id: Int): RemoteViews {
        val deadline = loadDeadline(context, id)
        // Never goes below zero, so the widget just stops at 0 after the deadline.
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), deadline).coerceAtLeast(0)

        // Over 60 days: whole weeks, rounded down (always 8 or more, so always plural).
        // 60 days or fewer: count down in days instead.
        val (number, unit) = when {
            daysLeft > DAYS_MODE_THRESHOLD -> (daysLeft / 7).toString() to "weeks"
            daysLeft == 1L -> "1" to "day"
            else -> daysLeft.toString() to "days"
        }
        val image = renderNumber(context, number)
        val description = "$number $unit left until thesis submission"

        // Tapping the widget opens the date picker for this widget.
        val openSettings = PendingIntent.getActivity(
            context,
            id,
            Intent(context, DeadlinePickerActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val small = RemoteViews(context.packageName, R.layout.widget_small).apply {
            setImageViewBitmap(R.id.weeks_number, image)
            setTextViewText(R.id.weeks_label, unit)
            setContentDescription(android.R.id.background, description)
            setOnClickPendingIntent(android.R.id.background, openSettings)
        }
        val large = RemoteViews(context.packageName, R.layout.widget_large).apply {
            setImageViewBitmap(R.id.weeks_number, image)
            setTextViewText(R.id.weeks_label, "$unit left")
            setTextViewText(R.id.weeks_due, "due " + deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            setContentDescription(android.R.id.background, description)
            setOnClickPendingIntent(android.R.id.background, openSettings)
        }

        // Android 12+: hand over both; the launcher picks the biggest one that fits, live while resizing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RemoteViews(
                mapOf(
                    SizeF(40f, 40f) to small,
                    SizeF(LARGE_MIN_DP, LARGE_MIN_DP) to large,
                )
            )
        }
        // Older Android: choose from the size the launcher reports.
        val options = manager.getAppWidgetOptions(id)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (width >= LARGE_MIN_DP && height >= LARGE_MIN_DP) large else small
    }

    /**
     * Draws the number in the dot-matrix font as a white image. Launchers often ignore
     * custom fonts in widgets, but always show images. The layout tints the image, so it
     * still follows light/dark mode, and the ImageView scales it to fit the widget.
     */
    private fun renderNumber(context: Context, text: String): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = context.resources.getFont(R.font.doto_extrabold)
            textSize = 200f
            color = Color.WHITE
        }
        // Use the height of all digits, so every number comes out the same height.
        val digits = Rect().also { paint.getTextBounds("0123456789", 0, 10, it) }
        val ink = Rect().also { paint.getTextBounds(text, 0, text.length, it) }

        val bitmap = Bitmap.createBitmap(
            ink.width().coerceAtLeast(1),
            digits.height().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bitmap).drawText(text, -ink.left.toFloat(), -digits.top.toFloat(), paint)
        return bitmap
    }
}
