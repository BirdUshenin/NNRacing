package ru.ilyaushenin.nnracing.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Выбор даты (ISO «2026-10-11») в нижней шторке. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initialIso: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    val initial = LocalDate.parse(initialIso).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    Sheet("Дата", onClose) {
        DatePicker(state, modifier = Modifier.fillMaxWidth(), title = null, headline = null, showModeToggle = false)
        AppButton("Готово", {
            state.selectedDateMillis?.let { onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString()) }
            onClose()
        }, Modifier.fillMaxWidth())
    }
}

/** Выбор времени в 24-часовом формате в нижней шторке. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(hour: Int, minute: Int, onPick: (Int, Int) -> Unit, onClose: () -> Unit) {
    val state = rememberTimePickerState(hour, minute, is24Hour = true)
    Sheet("Время", onClose) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) }
        AppButton("Готово", { onPick(state.hour, state.minute); onClose() }, Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}
