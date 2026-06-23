package com.sprint.runner.presentation.distance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.sprint.runner.domain.timer.IntervalConfig
import com.sprint.runner.domain.timer.Phase
import com.sprint.runner.presentation.common.StepperRow
import kotlin.math.ceil
import kotlin.math.roundToInt

private val ColorPrep = Color(0xFFFF9800)
private val ColorWork = Color(0xFF34C759)
private val ColorRest = Color(0xFF2196F3)
private val ColorIdle = Color(0xFF9E9E9E)

@Composable
fun DistanceScreen(
    viewModel: DistanceViewModel = hiltViewModel(),
    onNavigateToTime: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!hasPermission) {
            PermissionPrompt(
                onRequest = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                onBack = onNavigateToTime
            )
        } else {
            DistanceContent(viewModel, onNavigateToTime)
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(20.dp)
    ) {
        Text(
            text = "Режиму «По дистанции» нужен доступ к геолокации (GPS)",
            color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(backgroundColor = ColorWork)) {
            Text("Разрешить")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("← Время", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.clickable { onBack() })
    }
}

@Composable
private fun DistanceContent(viewModel: DistanceViewModel, onNavigateToTime: () -> Unit) {
    val runState by viewModel.runState.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val config by viewModel.config.collectAsState()

    val view = LocalView.current
    DisposableEffect(runState) {
        view.keepScreenOn = runState == DistanceRunState.RUNNING
        onDispose { view.keepScreenOn = false }
    }

    val context = LocalContext.current
    val feedback = remember { com.sprint.runner.presentation.timer.TimerFeedback(context) }
    DisposableEffect(Unit) { onDispose { feedback.release() } }
    LaunchedEffect(Unit) { viewModel.cues.collect { feedback.play(it) } }

    if (runState == DistanceRunState.IDLE) {
        DistanceSetup(config, onNavigateToTime, viewModel::updateConfig, viewModel::start)
    } else {
        DistanceFace(runState, snapshot) { stop -> if (stop) viewModel.stop() else viewModel.reset() }
    }
}

// ---- Idle setup -----------------------------------------------------------

@Composable
private fun DistanceSetup(
    config: IntervalConfig,
    onNavigateToTime: () -> Unit,
    onUpdate: (IntervalConfig) -> Unit,
    onStart: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "← Время · Дистанция",
                color = Color.Gray, fontSize = 12.sp,
                modifier = Modifier.clickable { onNavigateToTime() }
            )
        }
        item {
            StepperRow("Дистанция", "${config.distanceM}м",
                onMinus = { onUpdate(config.copy(distanceM = (config.distanceM - 10).coerceIn(20, 400))) },
                onPlus = { onUpdate(config.copy(distanceM = (config.distanceM + 10).coerceIn(20, 400))) })
        }
        item {
            StepperRow("Отдых", "${config.restMs / 1000}с",
                onMinus = { onUpdate(config.copy(restMs = stepSec(config.restMs, -5, 5, 180))) },
                onPlus = { onUpdate(config.copy(restMs = stepSec(config.restMs, +5, 5, 180))) })
        }
        item {
            StepperRow("Раунды", "${config.rounds}",
                onMinus = { onUpdate(config.copy(rounds = (config.rounds - 1).coerceIn(1, 20))) },
                onPlus = { onUpdate(config.copy(rounds = (config.rounds + 1).coerceIn(1, 20))) })
        }
        item {
            StepperRow("Подготовка", "${config.prepMs / 1000}с",
                onMinus = { onUpdate(config.copy(prepMs = stepSec(config.prepMs, -1, 0, 15))) },
                onPlus = { onUpdate(config.copy(prepMs = stepSec(config.prepMs, +1, 0, 15))) })
        }
        item {
            Button(
                onClick = onStart,
                modifier = Modifier.size(64.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = ColorWork),
                shape = CircleShape
            ) { Text("СТАРТ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ---- Running / done face --------------------------------------------------

@Composable
private fun DistanceFace(
    runState: DistanceRunState,
    snapshot: DistanceSnapshot,
    onAction: (stop: Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = snapshot.progress,
                modifier = Modifier.fillMaxSize(),
                startAngle = 270f,
                indicatorColor = phaseColor(snapshot.phase),
                trackColor = Color(0xFF2A2A2A),
                strokeWidth = 6.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bigText(snapshot),
                    color = Color.White, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText(runState, snapshot),
                    color = phaseColor(snapshot.phase), fontSize = 11.sp
                )
            }
        }

        // GPS diagnostics — confirms data is flowing during the field test.
        Text(
            text = "gps n=${snapshot.sampleCount} · ${"%.1f".format(snapshot.lastSpeedMps)} м/с",
            color = Color(0xFF6B7280), fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (runState) {
                DistanceRunState.RUNNING -> ControlButton("СТОП", Color(0xFFF44336)) { onAction(true) }
                DistanceRunState.DONE -> ControlButton("НОВЫЙ", ColorWork) { onAction(false) }
                else -> {}
            }
        }

        if (runState == DistanceRunState.DONE && snapshot.results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${snapshot.results.size} рывк. · Σ ${formatUp(snapshot.results.sumOf { it.timeMs })}",
                color = Color.Gray, fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ControlButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = CircleShape
    ) {
        Text(text = text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ---- helpers --------------------------------------------------------------

private fun stepSec(currentMs: Long, deltaSec: Int, minSec: Int, maxSec: Int): Long {
    val sec = (currentMs / 1000).toInt() + deltaSec
    return sec.coerceIn(minSec, maxSec) * 1000L
}

private fun bigText(s: DistanceSnapshot): String = when (s.phase) {
    Phase.PREP -> ceilSeconds(s.phaseRemainingMs).toString()
    Phase.WORK -> "${s.distanceM.roundToInt()} м"
    Phase.REST -> ceilSeconds(s.phaseRemainingMs).toString()
    Phase.DONE -> s.results.lastOrNull()?.let { formatUp(it.timeMs) } ?: "—"
    else -> "${s.targetM} м"
}

private fun subText(runState: DistanceRunState, s: DistanceSnapshot): String = when {
    s.phase == Phase.PREP -> "Подготовка"
    s.phase == Phase.WORK -> "Спринт ${s.round}/${s.totalRounds} · ${formatUp(s.workElapsedMs)}"
    s.phase == Phase.REST -> "Отдых ${s.round}/${s.totalRounds}"
    s.phase == Phase.DONE -> "Готово!"
    else -> ""
}

private fun phaseColor(phase: Phase): Color = when (phase) {
    Phase.PREP -> ColorPrep
    Phase.WORK -> ColorWork
    Phase.REST -> ColorRest
    else -> ColorIdle
}

private fun ceilSeconds(ms: Long): Int = ceil(ms / 1000.0).toInt().coerceAtLeast(0)

private fun formatUp(ms: Long): String {
    val totalTenths = ms / 100
    val tenths = totalTenths % 10
    val totalSeconds = totalTenths / 10
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return if (minutes > 0) String.format("%d:%02d.%d", minutes, seconds, tenths)
    else String.format("%02d.%d", seconds, tenths)
}
