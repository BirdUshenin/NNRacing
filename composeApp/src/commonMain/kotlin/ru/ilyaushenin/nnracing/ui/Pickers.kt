package ru.ilyaushenin.nnracing.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Выбор даты (ISO «2026-10-11»). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initialIso: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    val initial = LocalDate.parse(initialIso).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
                onClose()
            }) { Txt("Готово", color = Rc.Accent, weight = androidx.compose.ui.text.font.FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Txt("Отмена", color = Rc.Muted) } },
    ) { DatePicker(state) }
}

/** Выбор времени в 24-часовом формате. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(hour: Int, minute: Int, onPick: (Int, Int) -> Unit, onClose: () -> Unit) {
    val state = rememberTimePickerState(hour, minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Rc.Surface2,
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute); onClose() }) { Txt("Готово", color = Rc.Accent, weight = androidx.compose.ui.text.font.FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onClose) { Txt("Отмена", color = Rc.Muted) } },
        text = { TimePicker(state) },
    )
}
