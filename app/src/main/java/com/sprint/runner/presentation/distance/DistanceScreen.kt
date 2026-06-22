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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import com.sprint.runner.domain.timer.Phase
import com.sprint.runner.presentation.timer.TimerFeedback
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!hasPermission) {
            PermissionPrompt(
                onRequest = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                onBack = onNavigateToTime
            )
        } else {
            DistanceContent(viewModel, onNavigateToTime, onNavigateToSettings)
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
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(backgroundColor = ColorWork)
        ) { Text("Разрешить") }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "← Время",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.clickable { onBack() }
        )
    }
}

@Composable
private fun DistanceContent(
    viewModel: DistanceViewModel,
    onNavigateToTime: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val runState by viewModel.runState.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val config by viewModel.config.collectAsState()

    val context = LocalContext.current
    val feedback = remember { TimerFeedback(context) }
    DisposableEffect(Unit) { onDispose { feedback.release() } }
    LaunchedEffect(Unit) { viewModel.cues.collect { feedback.play(it) } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
    ) {
        if (runState == DistanceRunState.IDLE) {
            Text(
                text = "← Время",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onNavigateToTime() }
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = if (runState == DistanceRunState.IDLE) 0f else snapshot.progress,
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
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText(runState, snapshot),
                    color = phaseColor(snapshot.phase),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (runState) {
                DistanceRunState.IDLE -> ControlButton("СТАРТ", ColorWork) { viewModel.start() }
                DistanceRunState.RUNNING -> ControlButton("СТОП", Color(0xFFF44336)) { viewModel.stop() }
                DistanceRunState.DONE -> ControlButton("НОВЫЙ", ColorWork) { viewModel.reset() }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val planModifier = if (runState == DistanceRunState.IDLE) {
            Modifier.clickable { onNavigateToSettings() }
        } else Modifier
        Text(
            text = planText(runState, snapshot, config.distanceM, config.restMs, config.rounds),
            color = if (runState == DistanceRunState.IDLE) Color(0xFFB0B8C4) else Color.Gray,
            fontSize = 11.sp,
            modifier = planModifier
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

private fun bigText(runState: DistanceRunState, s: DistanceSnapshot): String = when {
    runState == DistanceRunState.IDLE -> "${s.targetM} м"
    s.phase == Phase.PREP -> ceilSeconds(s.phaseRemainingMs).toString()
    s.phase == Phase.WORK -> "${s.distanceM.roundToInt()} м"
    s.phase == Phase.REST -> ceilSeconds(s.phaseRemainingMs).toString()
    s.phase == Phase.DONE -> s.results.lastOrNull()?.let { fmtUp(it.timeMs) } ?: "—"
    else -> "${s.targetM} м"
}

private fun subText(runState: DistanceRunState, s: DistanceSnapshot): String = when {
    runState == DistanceRunState.IDLE -> "Дистанция"
    s.phase == Phase.PREP -> "Подготовка"
    s.phase == Phase.WORK -> "Спринт ${s.round}/${s.totalRounds} · ${fmtUp(s.workElapsedMs)}"
    s.phase == Phase.REST -> "Отдых ${s.round}/${s.totalRounds}"
    s.phase == Phase.DONE -> "Готово!"
    else -> ""
}

private fun planText(
    runState: DistanceRunState,
    s: DistanceSnapshot,
    distanceM: Int,
    restMs: Long,
    rounds: Int
): String = when (runState) {
    DistanceRunState.IDLE -> "${distanceM}м × $rounds · отдых ${restMs / 1000}с  ⚙"
    DistanceRunState.DONE -> "${s.results.size} рывк. · Σ ${fmtUp(s.results.sumOf { it.timeMs })}"
    else -> "цель ${s.targetM} м"
}

private fun phaseColor(phase: Phase): Color = when (phase) {
    Phase.PREP -> ColorPrep
    Phase.WORK -> ColorWork
    Phase.REST -> ColorRest
    else -> ColorIdle
}

private fun ceilSeconds(ms: Long): Int = ceil(ms / 1000.0).toInt().coerceAtLeast(0)

private fun fmtUp(ms: Long): String {
    val totalTenths = ms / 100
    val tenths = totalTenths % 10
    val totalSeconds = totalTenths / 10
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    return if (minutes > 0) String.format("%d:%02d.%d", minutes, seconds, tenths)
    else String.format("%02d.%d", seconds, tenths)
}
