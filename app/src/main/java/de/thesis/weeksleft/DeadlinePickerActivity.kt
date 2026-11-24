package de.thesis.weeksleft

import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import java.time.LocalDate
import java.time.ZoneId

/**
 * The widget's settings: just a date picker. Opens when the widget is added, when it is
 * tapped, and from the launcher's widget settings (Android 12+).
 */
class DeadlinePickerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Until a date is picked, backing out of the very first setup cancels adding the widget.
        setResult(RESULT_CANCELED, resultFor(id))

        val current = loadDeadline(this, id)
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                // DatePicker months are 0-based.
                saveDeadline(this, id, LocalDate.of(year, month + 1, day))
                redraw(id)
                setResult(RESULT_OK, resultFor(id))
            },
            current.year, current.monthValue - 1, current.dayOfMonth,
        )
        dialog.setTitle(R.string.picker_title)
        // No deadlines in the past.
        dialog.datePicker.minDate = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialog.setOnDismissListener { finish() }
        dialog.show()
    }

    private fun resultFor(id: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

    /** Asks the widget to redraw with the new date. */
    private fun redraw(id: Int) {
        sendBroadcast(
            Intent(this, WeeksLeftWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id)),
        )
    }
}
