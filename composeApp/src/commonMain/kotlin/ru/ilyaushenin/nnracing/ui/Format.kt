package ru.ilyaushenin.nnracing.ui

import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

fun fmtLap(ms: Int): String {
    val minutes = ms / 60000
    val rest = ms - minutes * 60000
    val seconds = (rest / 1000).toString().padStart(if (minutes > 0) 2 else 1, '0')
    val millis = (rest % 1000).toString().padStart(3, '0')
    return if (minutes > 0) "$minutes:$seconds.$millis" else "$seconds.$millis"
}

fun fmtDelta(ms: Int): String {
    val sign = if (ms < 0) "−" else "+"
    val a = abs(ms)
    return "$sign${a / 1000}.${(a % 1000).toString().padStart(3, '0')}"
}

fun fmtDelta(ms: Double): String = fmtDelta(ms.roundToInt())

/** «1:05.432», «65.432», «65,432», «1.05.43» → миллисекунды; null, если не разобрать (для вставки списком). */
fun parseLap(text: String): Int? {
    val s = text.trim().replace(',', '.').replace(" ", "")
    Regex("^(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?$").find(s)?.let { return lapOf(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
    Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{1,3})$").find(s)?.let { return lapOf(it.groupValues[1], it.groupValues[2], it.groupValues[3]) }
    Regex("^(\\d{1,4})(?:\\.(\\d{1,3}))?$").find(s)?.let { return it.groupValues[1].toInt() * 1000 + it.groupValues[2].padEnd(3, '0').toInt() }
    return null
}

private fun lapOf(min: String, sec: String, frac: String): Int? {
    if (sec.toInt() >= 60) return null
    return min.toInt() * 60000 + sec.toInt() * 1000 + frac.padEnd(3, '0').toInt()
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val a = abs(n) % 100
    val b = a % 10
    return if (a in 11..19) many else if (b in 2..4) few else if (b == 1) one else many
}

private val MONTHS_SHORT = listOf("ЯНВ", "ФЕВ", "МАР", "АПР", "МАЯ", "ИЮН", "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК")
private val MONTHS_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
private val DAYS_SHORT = mapOf(
    DayOfWeek.MONDAY to "пн", DayOfWeek.TUESDAY to "вт", DayOfWeek.WEDNESDAY to "ср", DayOfWeek.THURSDAY to "чт",
    DayOfWeek.FRIDAY to "пт", DayOfWeek.SATURDAY to "сб", DayOfWeek.SUNDAY to "вс",
)

data class DateParts(val year: String, val day: Int, val month: String)

fun dateParts(iso: String): DateParts {
    val d = LocalDate.parse(iso)
    return DateParts(d.year.toString(), d.dayOfMonth, MONTHS_SHORT[d.monthNumber - 1])
}

/** «11 окт. 2026» */
fun fmtDate(iso: String): String {
    val d = LocalDate.parse(iso)
    return "${d.dayOfMonth} ${MONTHS_GEN[d.monthNumber - 1].take(3)}. ${d.year}"
}

/** «чт, 22 октября» */
fun fmtLong(epochSeconds: Long): String {
    val d = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${DAYS_SHORT[d.dayOfWeek]}, ${d.dayOfMonth} ${MONTHS_GEN[d.monthNumber - 1]}"
}

fun fmtClock(epochSeconds: Long): String {
    val d = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${d.hour.toString().padStart(2, '0')}:${d.minute.toString().padStart(2, '0')}"
}

/** Время последнего сообщения в списке чатов: сегодня — часы и минуты, в пределах недели — день недели (пн–вс), раньше — дата «11.11.26». */
fun fmtChatListTime(epochSeconds: Long): String {
    val d = dayKey(epochSeconds)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val diff = today.toEpochDays() - d.toEpochDays()
    return when {
        diff <= 0 -> fmtClock(epochSeconds)
        diff < 7 -> DAYS_SHORT[d.dayOfWeek] ?: ""
        else -> "${d.dayOfMonth.toString().padStart(2, '0')}.${d.monthNumber.toString().padStart(2, '0')}.${(d.year % 100).toString().padStart(2, '0')}"
    }
}

fun fmtMessageTime(epochSeconds: Long): String = fmtClock(epochSeconds)

fun todayIso(): String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

fun nowSeconds(): Long = Clock.System.now().epochSeconds

/** Дата и время (в часовом поясе устройства) → секунды Unix. */
fun toEpochSeconds(isoDate: String, hour: Int, minute: Int): Long {
    val local = LocalDate.parse(isoDate)
    return LocalDateTime(local.year, local.monthNumber, local.dayOfMonth, hour, minute).toInstant(TimeZone.currentSystemDefault()).epochSeconds
}

fun tomorrowIso(): String {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return LocalDate.fromEpochDays(t.toEpochDays() + 1).toString()
}

/** «в сети», «был(а) 5 мин назад», «был(а) вчера в 18:20»; пусто, если неизвестно. */
fun fmtSeen(lastSeen: Long?): String {
    if (lastSeen == null) return ""
    val diff = nowSeconds() - lastSeen
    if (diff < 60) return "в сети"
    if (diff < 3600) return "был(а) ${diff / 60} мин назад"
    val tz = TimeZone.currentSystemDefault()
    val d = Instant.fromEpochSeconds(lastSeen).toLocalDateTime(tz)
    val today = Clock.System.now().toLocalDateTime(tz).date
    val days = today.toEpochDays() - d.date.toEpochDays()
    val clock = fmtClock(lastSeen)
    return when {
        days == 0 -> "был(а) в $clock"
        days == 1 -> "был(а) вчера в $clock"
        else -> "был(а) ${d.dayOfMonth} ${MONTHS_GEN[d.monthNumber - 1]}"
    }
}

fun isOnline(lastSeen: Long?): Boolean = lastSeen != null && nowSeconds() - lastSeen < 60

/** Ключ дня (в часовом поясе устройства): сообщения одного дня группируются под одной плашкой. */
fun dayKey(epochSeconds: Long): LocalDate = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault()).date

/** «Сегодня», «Вчера», «19 октября» (с годом, если не текущий). */
fun fmtDayLabel(epochSeconds: Long): String {
    val d = dayKey(epochSeconds)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (today.toEpochDays() - d.toEpochDays()) {
        0 -> "Сегодня"
        1 -> "Вчера"
        else -> "${d.dayOfMonth} ${MONTHS_GEN[d.monthNumber - 1]}" + if (d.year != today.year) " ${d.year}" else ""
    }
}
