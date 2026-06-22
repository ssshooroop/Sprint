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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.sprint.runner.domain.timer.Phase
import com.sprint.runner.domain.timer.TimerSnapshot
import kotlin.math.ceil

// Phase palette — kept in sync with the web preview.
private val ColorPrep = Color(0xFFFF9800)
private val ColorWork = Color(0xFF34C759)
private val ColorRest = Color(0xFF2196F3)
private val ColorIdle = Color(0xFF9E9E9E)

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val runState by viewModel.runState.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val config by viewModel.config.collectAsState()

    // Wire one-shot cues to haptics + tones.
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = if (runState == RunState.IDLE) 0f else snapshot.phaseProgress,
                    modifier = Modifier.fillMaxSize(),
                    startAngle = 270f,
                    indicatorColor = phaseColor(snapshot.phase),
                    trackColor = Color(0xFF2A2A2A),
                    strokeWidth = 6.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = bigText(runState, snapshot),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phaseLabel(runState, snapshot, config.rounds),
                        color = phaseColor(snapshot.phase),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (runState) {
                    RunState.IDLE -> ControlButton("СТАРТ", ColorWork) { viewModel.start() }
                    RunState.PAUSED -> {
                        ControlButton("ПУСК", ColorWork) { viewModel.start() }
                        ControlButton("СТОП", ColorRest) { viewModel.stop() }
                    }
                    RunState.RUNNING -> {
                        ControlButton("ПАУЗА", Color(0xFFFFC107)) { viewModel.pause() }
                        ControlButton("СТОП", Color(0xFFF44336)) { viewModel.stop() }
                    }
                    RunState.DONE -> ControlButton("НОВЫЙ", ColorWork) { viewModel.reset() }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // While idle the plan doubles as the entry to the shared settings hub.
            val planModifier = if (runState == RunState.IDLE) {
                Modifier.clickable { onNavigateToSettings() }
            } else {
                Modifier
            }
            Text(
                text = planSummary(runState, snapshot, config.workMs, config.restMs, config.rounds),
                color = if (runState == RunState.IDLE) Color(0xFFB0B8C4) else Color.Gray,
                fontSize = 11.sp,
                modifier = planModifier
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

/** Big central readout: countdown digits, the GO flash, or count-up time. */
private fun bigText(runState: RunState, s: TimerSnapshot): String = when {
    runState == RunState.IDLE -> "00.0"
    s.phase == Phase.PREP -> ceilSeconds(s.phaseRemainingMs).toString()
    s.phase == Phase.WORK -> if (s.phaseElapsedMs < 700) "GO" else formatUp(s.phaseElapsedMs)
    s.phase == Phase.REST -> ceilSeconds(s.phaseRemainingMs).toString()
    s.phase == Phase.DONE -> formatUp(s.totalElapsedMs)
    else -> "00.0"
}

private fun phaseLabel(runState: RunState, s: TimerSnapshot, rounds: Int): String = when {
    runState == RunState.IDLE -> "Готов"
    runState == RunState.PAUSED -> "Пауза"
    s.phase == Phase.PREP -> "Подготовка"
    s.phase == Phase.WORK -> "Спринт ${s.round}/${s.totalRounds}"
    s.phase == Phase.REST -> "Отдых ${s.round}/${s.totalRounds}"
    s.phase == Phase.DONE -> "Готово!"
    else -> ""
}

private fun planSummary(
    runState: RunState,
    s: TimerSnapshot,
    workMs: Long,
    restMs: Long,
    rounds: Int
): String = when (runState) {
    RunState.IDLE -> "${workMs / 1000}с / ${restMs / 1000}с × $rounds  ⚙"
    RunState.DONE -> "Σ ${formatUp(s.totalElapsedMs)}"
    else -> "По времени"
}

private fun phaseColor(phase: Phase): Color = when (phase) {
    Phase.PREP -> ColorPrep
    Phase.WORK -> ColorWork
    Phase.REST -> ColorRest
    else -> ColorIdle
}

private fun ceilSeconds(ms: Long): Int = ceil(ms / 1000.0).toInt().coerceAtLeast(0)

/** SS.t for sub-minute, M:SS.t otherwise (tenths of a second). */
private fun formatUp(ms: Long): String {
    val totalTenths = ms / 100
    val tenths = totalTenths % 10
    val totalSeconds = totalTenths / 10
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return if (minutes > 0) {
        String.format("%d:%02d.%d", minutes, seconds, tenths)
    } else {
        String.format("%02d.%d", seconds, tenths)
    }
}
