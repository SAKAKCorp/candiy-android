package com.candiy.candiyshdexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.candiy.candiyshd.SamsungHealthDataManager
import com.candiy.candiyshd.data.model.SamsungHealthSnapshot
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val samsungHealthDataManager by lazy {
        SamsungHealthDataManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SamsungHealthDataScreen(
                manager = samsungHealthDataManager,
                activity = this
            )
        }
    }
}

private data class SamsungHealthUiState(
    val hasPermissions: Boolean = false,
    val loading: Boolean = false,
    val status: String = "Ready",
    val snapshot: SamsungHealthSnapshot? = null,
    val exportPath: String? = null
)

@Composable
private fun SamsungHealthDataScreen(
    manager: SamsungHealthDataManager,
    activity: ComponentActivity
) {
    var uiState by remember { mutableStateOf(SamsungHealthUiState()) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshPermissions() {
        coroutineScope.launch {
            uiState = uiState.copy(loading = true, status = "Checking permissions")
            uiState = try {
                uiState.copy(
                    hasPermissions = manager.hasAllPermissions(),
                    loading = false,
                    status = "Permission check complete"
                )
            } catch (throwable: Throwable) {
                if (manager.resolveIfPossible(activity, throwable)) {
                    uiState.copy(
                        loading = false,
                        status = "Samsung Health needs user action. Try again after resolving it."
                    )
                } else {
                    uiState.copy(
                        loading = false,
                        status = manager.userReadableError(throwable)
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Header(uiState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.loading,
                        onClick = {
                            coroutineScope.launch {
                                uiState = uiState.copy(loading = true, status = "Requesting permissions")
                                uiState = try {
                                    manager.requestPermissions(activity)
                                    uiState.copy(
                                        hasPermissions = manager.hasAllPermissions(),
                                        loading = false,
                                        status = "Permissions updated"
                                    )
                                } catch (throwable: Throwable) {
                                    if (manager.resolveIfPossible(activity, throwable)) {
                                        uiState.copy(
                                            loading = false,
                                            status = "Samsung Health opened a resolver. Retry after completing it."
                                        )
                                    } else {
                                        uiState.copy(
                                            loading = false,
                                            status = manager.userReadableError(throwable)
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Request permissions")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.loading,
                        onClick = { refreshPermissions() }
                    ) {
                        Text("Refresh")
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.loading && uiState.hasPermissions,
                    onClick = {
                        coroutineScope.launch {
                            uiState = uiState.copy(loading = true, status = "Reading watch data")
                            uiState = try {
                                val now = LocalDateTime.now()
                                val snapshot = manager.readSnapshot(
                                    startTime = now.minusDays(1),
                                    endTime = now,
                                    watchOnly = true
                                )
                                uiState.copy(
                                    loading = false,
                                    status = "Loaded watch data",
                                    snapshot = snapshot
                                )
                            } catch (throwable: Throwable) {
                                uiState.copy(
                                    loading = false,
                                    status = manager.userReadableError(throwable)
                                )
                            }
                        }
                    }
                ) {
                    Text("Load Galaxy Watch data")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.loading && uiState.hasPermissions,
                    onClick = {
                        coroutineScope.launch {
                            uiState = uiState.copy(loading = true, status = "Exporting raw debug bundle")
                            uiState = try {
                                val exportPath = exportDebugBundle(manager, activity)
                                uiState.copy(
                                    loading = false,
                                    status = "Exported raw data and sync timeline",
                                    exportPath = exportPath
                                )
                            } catch (throwable: Throwable) {
                                uiState.copy(
                                    loading = false,
                                    status = manager.userReadableError(throwable)
                                )
                            }
                        }
                    }
                ) {
                    Text("Export 7-day bundle to files")
                }

                uiState.exportPath?.let { exportPath ->
                    ExportPathCard(exportPath)
                }

                SnapshotSummary(uiState.snapshot)
            }
        }
    }
}

private suspend fun exportDebugBundle(
    manager: SamsungHealthDataManager,
    activity: ComponentActivity
): String {
    val exportDir = File(activity.getExternalFilesDir(null), "candiy-shd-debug")
    exportDir.mkdirs()

    val timestamp = System.currentTimeMillis()
    val timeline = JSONArray()

    fun recordEvent(name: String, details: JSONObject = JSONObject()) {
        timeline.put(
            JSONObject()
                .put("at", Instant.now().toString())
                .put("event", name)
                .put("details", details)
        )
    }

    val window = manager.sevenDayWindow()
    val changeEndTime = Instant.now()
    val changeStartTime = changeEndTime.minus(Duration.ofDays(7))

    recordEvent(
        name = "EXPORT_START",
        details = JSONObject()
            .put("rawReadStartTime", window.start.toString())
            .put("rawReadEndTime", window.end.toString())
            .put("sleepReadStartTime", window.sleepStart.toString())
            .put("changeStartTime", changeStartTime.toString())
            .put("changeEndTime", changeEndTime.toString())
            .put("watchOnly", true)
    )

    recordEvent("PERMISSION_CHECK_START")
    val hasPermissions = manager.hasAllPermissions()
    recordEvent("PERMISSION_CHECK_END", JSONObject().put("hasAllPermissions", hasPermissions))
    if (!hasPermissions) {
        throw IllegalStateException("Samsung Health permissions are missing. Request permissions first.")
    }

    recordEvent("READ_RAW_DATA_START")
    // No limitPerType: the point of the export is a complete seven days. Any cap here would
    // trim after pagination and look identical to a short week.
    //
    // watchOnly = false: the watch source filter drops anything Samsung Health recorded from
    // the phone or a third-party app. Every point carries sourceAppId/sourceDeviceId, so an
    // unfiltered read loses nothing and the split can be made after the fact.
    val rawJson = manager.readRawDataAsJson(
        window = window,
        watchOnly = false
    )
    val rawFile = File(exportDir, "raw_data_$timestamp.json")
    rawFile.writeText(rawJson)
    recordEvent(
        "READ_RAW_DATA_END",
        JSONObject()
            .put("file", rawFile.absolutePath)
            .put("bytes", rawFile.length())
    )

    recordEvent("READ_CHANGES_START")
    val changesJson = manager.readRawChangesAsJson(
        startTime = changeStartTime,
        endTime = changeEndTime
    )
    val changesFile = File(exportDir, "sync_changes_$timestamp.json")
    changesFile.writeText(changesJson)
    recordEvent(
        "READ_CHANGES_END",
        JSONObject()
            .put("file", changesFile.absolutePath)
            .put("bytes", changesFile.length())
    )

    val summaryFile = File(exportDir, "sync_timeline_$timestamp.json")
    recordEvent("EXPORT_END", JSONObject().put("dir", exportDir.absolutePath))
    summaryFile.writeText(timeline.toString(2))

    return exportDir.absolutePath
}

@Composable
private fun Header(uiState: SamsungHealthUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Samsung Health Data SDK",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Permission: ${if (uiState.hasPermissions) "granted" else "missing"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = uiState.status,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569)
        )
    }
}

@Composable
private fun ExportPathCard(exportPath: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Debug bundle exported",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = exportPath,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF475569)
        )
    }
}

@Composable
private fun SnapshotSummary(snapshot: SamsungHealthSnapshot?) {
    if (snapshot == null) {
        EmptyState()
        return
    }

    val latestHeartRate = snapshot.heartRates.lastOrNull()?.beatsPerMinute
    val averageSpo2 = snapshot.oxygenSaturations
        .map { it.percentage }
        .takeIf { it.isNotEmpty() }
        ?.average()
    val totalSleepMinutes = snapshot.sleepSessions
        .mapNotNull { it.duration }
        .sumOf { duration ->
            runCatching { java.time.Duration.parse(duration).toMinutes() }.getOrDefault(0L)
        }

    MetricCard(
        title = "Steps",
        value = snapshot.steps?.count?.toString() ?: "0",
        detail = "Last 24 hours"
    )
    MetricCard(
        title = "Heart rate",
        value = latestHeartRate?.roundToInt()?.toString() ?: "-",
        detail = "${snapshot.heartRates.size} samples from watch"
    )
    MetricCard(
        title = "Blood oxygen",
        value = averageSpo2?.let { "${it.roundToInt()}%" } ?: "-",
        detail = "${snapshot.oxygenSaturations.size} samples from watch"
    )
    MetricCard(
        title = "Sleep",
        value = "${totalSleepMinutes / 60}h ${totalSleepMinutes % 60}m",
        detail = "${snapshot.sleepSessions.size} sessions in last 7 days"
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No data loaded",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Grant permissions, make sure Galaxy Watch data is synced to Samsung Health, then load data.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun MetricCard(title: String, value: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )
        }
    }
}
