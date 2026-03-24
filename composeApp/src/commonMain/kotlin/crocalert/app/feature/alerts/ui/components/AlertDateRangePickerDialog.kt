package crocalert.app.feature.alerts.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import crocalert.app.feature.alerts.presentation.DateRange

/**
 * A [DatePickerDialog] wrapping a [DateRangePicker].
 *
 * Opens in [DisplayMode.Input] (text-field entry) by default so the dialog
 * stays compact. The user can tap the calendar icon to switch to the full
 * calendar picker.
 *
 * Confirm is disabled until both a start and an end date are chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDateRangePickerDialog(
    onRangeSelected: (startMs: Long, endMs: Long) -> Unit,
    onDismiss: () -> Unit,
    initialRange: DateRange? = null,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.startMs,
        initialSelectedEndDateMillis = initialRange?.endMs,
        initialDisplayMode = DisplayMode.Picker,
    )

    val canConfirm = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onRangeSelected(start, end)
                    }
                },
                enabled = canConfirm,
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            title = { Text("Seleccionar rango", modifier = Modifier.padding(start = 4.dp, top = 8.dp)) },
            headline = null,
            showModeToggle = true,
        )
    }
}
