package ch.rhosys.lyra.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String,
)

enum class LogLevel { INFO, WARN, ERROR }

@Singleton
class AppLogger @Inject constructor() {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    private val maxEntries = 200

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun info(message: String) = append(LogLevel.INFO, message)
    fun warn(message: String) = append(LogLevel.WARN, message)
    fun error(message: String) = append(LogLevel.ERROR, message)
    fun error(message: String, throwable: Throwable) = append(LogLevel.ERROR, "$message: ${throwable.message}")

    fun clear() { _entries.value = emptyList() }

    private fun append(level: LogLevel, message: String) {
        val entry = LogEntry(
            timestamp = formatter.format(Instant.now()),
            level = level,
            message = message,
        )
        _entries.update { current ->
            (current + entry).takeLast(maxEntries)
        }
    }
}
