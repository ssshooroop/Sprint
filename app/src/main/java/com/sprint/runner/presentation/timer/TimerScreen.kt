package com.sprint.runner.presentation.timer

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.sprint.runner.domain.timer.IntervalConfig
import com.sprint.runner.domain.timer.Phase
import com.sprint.runner.domain.timer.TimerSnapshot
import com.sprint.runner.presentation.common.StepperRow
import kotlin.math.ceil

private val ColorWork = Color(0xFF34C759)
private val ColorPrep = Color(0xFFFF9800)
private val ColorRest = Color(0xFF2196F3)
private val ColorIdle = Color(0xFF9E9E9E)

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDistance: () -> Unit
) {
    val runState by viewModel.runState.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val config by viewModel.config.collectAsState()

    // Keep the screen on while a workout runs, otherwise the watch sleeps and
    // the timer freezes. Released when idle/done.
    val view = LocalView.current
    DisposableEffect(runState) {
        view.keepScreenOn = runState == RunState.RUNNING
        onDispose { view.keepScreenOn = false }
    }

    val context = LocalContext.current
    val feedback = remember { TimerFeedback(context) }
    DisposableEffect(Unit) { onDispose { feedback.release() } }
    LaunchedEffect(Unit) { viewModel.cues.collect { feedback.play(it) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (runState == RunState.IDLE) {
            TimeSetup(config, onNavigateToDistance, viewModel::updateConfig, viewModel::start)
        } else {
            RunningFace(runState, snapshot, config) { action ->
                when (action) {
                    Action.START -> viewModel.start()
                    Action.PAUSE -> viewModel.pause()
                    Action.STOP -> viewModel.stop()
                    Action.RESET -> viewModel.reset()
                }
            }
        }
    }
}

// ---- Idle: inline plan editing -------------------------------------------

@Composable
private fun TimeSetup(
    config: IntervalConfig,
    onNavigateToDistance: () -> Unit,
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
                text = "Время · Дистанция →",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onNavigateToDistance() }
            )
        }
        item {
            StepperRow("Спринт", "${config.workMs / 1000}с",
                onMinus = { onUpdate(config.copy(workMs = stepSec(config.workMs, -5, 5, 120))) },
                onPlus = { onUpdate(config.copy(workMs = stepSec(config.workMs, +5, 5, 120))) })
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

private enum class Action { START, PAUSE, STOP, RESET }

@Composable
private fun RunningFace(
    runState: RunState,
    snapshot: TimerSnapshot,
    config: IntervalConfig,
    onAction: (Action) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = snapshot.phaseProgress,
                modifier = Modifier.fillMaxSize(),
                startAngle = 270f,
                indicatorColor = phaseColor(snapshot.phase),
                trackColor = Color(0xFF2A2A2A),
                strokeWidth = 6.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bigText(snapshot),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = phaseLabel(runState, snapshot),
                    color = phaseColor(snapshot.phase),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (runState) {
                RunState.PAUSED -> {
                    ControlButton("ПУСК", ColorWork) { onAction(Action.START) }
                    ControlButton("СТОП", ColorRest) { onAction(Action.STOP) }
                }
                RunState.RUNNING -> {
                    ControlButton("ПАУЗА", Color(0xFFFFC107)) { onAction(Action.PAUSE) }
                    ControlButton("СТОП", Color(0xFFF44336)) { onAction(Action.STOP) }
                }
                RunState.DONE -> ControlButton("НОВЫЙ", ColorWork) { onAction(Action.RESET) }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (runState == RunState.DONE) "Σ ${formatUp(snapshot.totalElapsedMs)}" else "По времени",
            color = Color.Gray,
            fontSize = 10.sp
        )
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

/** Step a millisecond value by `deltaSec`, clamped to [minSec, maxSec]. */
private fun stepSec(currentMs: Long, deltaSec: Int, minSec: Int, maxSec: Int): Long {
    val sec = (currentMs / 1000).toInt() + deltaSec
    return sec.coerceIn(minSec, maxSec) * 1000L
}

private fun bigText(s: TimerSnapshot): String = when (s.phase) {
    Phase.PREP -> ceilSeconds(s.phaseRemainingMs).toString()
    Phase.WORK -> if (s.phaseElapsedMs < 700) "GO" else formatUp(s.phaseElapsedMs)
    Phase.REST -> ceilSeconds(s.phaseRemainingMs).toString()
    Phase.DONE -> formatUp(s.totalElapsedMs)
    else -> "00.0"
}

private fun phaseLabel(runState: RunState, s: TimerSnapshot): String = when {
    runState == RunState.PAUSED -> "Пауза"
    s.phase == Phase.PREP -> "Подготовка"
    s.phase == Phase.WORK -> "Спринт ${s.round}/${s.totalRounds}"
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
