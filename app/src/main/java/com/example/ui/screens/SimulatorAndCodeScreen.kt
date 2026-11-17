package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.DronePhysicsEngine
import com.example.domain.VisualProgrammingSandbox
import com.example.ui.LearningUiState
import com.example.ui.SimulatorTab
import com.example.ui.gl.FlightSimulator3DView
import com.example.ui.gl.Sim3DWorldState
import com.example.ui.gl.SimCameraMode
import com.example.ui.gl.SurveyGate3D
import com.example.ui.theme.LocalSemanticStatusColors
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

@Composable
fun SimulatorAndCodeScreen(
  uiState: LearningUiState,
  onSelectTab: (SimulatorTab) -> Unit,
  onSelectCameraMode: (SimCameraMode) -> Unit,
  onResetCamera: () -> Unit,
  onUpdateSimInput: ((DronePhysicsEngine.SimControlInput) -> DronePhysicsEngine.SimControlInput) -> Unit,
  onStartSimIfIdle: () -> Unit,
  onSetFlightHoldAxis: (yawRateCmd: Float?, pitchCmd: Float?, rollCmd: Float?) -> Unit,
  onAdjustThrottle: (Float) -> Unit,
  onNudgeSimControl: (yawRateCmd: Float?, pitchCmd: Float?, rollCmd: Float?) -> Unit,
  onToggleSimRunning: () -> Unit,
  onStepSimOnce: () -> Unit,
  onResetSim: () -> Unit,
  onVerifyReplay: () -> Unit,
  onSelectScenario: (String?) -> Unit,
  onAddProgramBlock: (VisualProgrammingSandbox.AstNode) -> Unit,
  onRemoveLastProgramBlock: () -> Unit,
  onLoadPresetProgram: (Boolean) -> Unit,
  onRunVisualProgram: () -> Unit,
  onAnalyzeCurrentLog: () -> Unit,
  onTestIncompleteLog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    TabRow(selectedTabIndex = uiState.simulatorTab.ordinal) {
      SimulatorTab.entries.forEach { tab ->
        Tab(
          selected = uiState.simulatorTab == tab,
          onClick = { onSelectTab(tab) },
          text = { Text(tab.titleAr) },
          modifier = Modifier.testTag("sim_tab_${tab.name}"),
        )
      }
    }

    when (uiState.simulatorTab) {
      SimulatorTab.FLIGHT_SIM -> FlightSimulatorContent(
        uiState = uiState,
        onSelectCameraMode = onSelectCameraMode,
        onResetCamera = onResetCamera,
        onUpdateSimInput = onUpdateSimInput,
        onStartSimIfIdle = onStartSimIfIdle,
        onSetFlightHoldAxis = onSetFlightHoldAxis,
        onAdjustThrottle = onAdjustThrottle,
        onNudgeSimControl = onNudgeSimControl,
        onToggleSimRunning = onToggleSimRunning,
        onStepSimOnce = onStepSimOnce,
        onResetSim = onResetSim,
        onVerifyReplay = onVerifyReplay,
        onSelectScenario = onSelectScenario,
      )

      SimulatorTab.VISUAL_CODE -> VisualProgrammingContent(
        uiState = uiState,
        onAddProgramBlock = onAddProgramBlock,
        onRemoveLastProgramBlock = onRemoveLastProgramBlock,
        onLoadPresetProgram = onLoadPresetProgram,
        onRunVisualProgram = onRunVisualProgram,
      )

      SimulatorTab.LOG_ANALYZER -> LogAnalyzerContent(
        uiState = uiState,
        onAnalyzeCurrentLog = onAnalyzeCurrentLog,
        onTestIncompleteLog = onTestIncompleteLog,
      )
    }
  }
}

@Composable
private fun FlightSimulatorContent(
  uiState: LearningUiState,
  onSelectCameraMode: (SimCameraMode) -> Unit,
  onResetCamera: () -> Unit,
  onUpdateSimInput: ((DronePhysicsEngine.SimControlInput) -> DronePhysicsEngine.SimControlInput) -> Unit,
  onStartSimIfIdle: () -> Unit,
  onSetFlightHoldAxis: (yawRateCmd: Float?, pitchCmd: Float?, rollCmd: Float?) -> Unit,
  onAdjustThrottle: (Float) -> Unit,
  onNudgeSimControl: (yawRateCmd: Float?, pitchCmd: Float?, rollCmd: Float?) -> Unit,
  onToggleSimRunning: () -> Unit,
  onStepSimOnce: () -> Unit,
  onResetSim: () -> Unit,
  onVerifyReplay: () -> Unit,
  onSelectScenario: (String?) -> Unit,
) {
  val statusColors = LocalSemanticStatusColors.current
  val s = uiState.simState
  val inp = uiState.simInput

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("flight_sim_list"),
  ) {
    item {
      Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(12.dp),
        ) {
          Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "محاكاة تعليمية مبسطة بوحدات SI (Δt = 0.02 s) غير مطابقة للواقع ولا تتصل بأي طائرة حقيقية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }
    }

    // Guided scenarios selector (Phase 6)
    item {
      Text(text = "السيناريوهات التدريبية الموجهة:", style = MaterialTheme.typography.titleSmall)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
      ) {
        FilterChip(
          selected = uiState.activeScenarioId == null,
          onClick = { onSelectScenario(null) },
          label = { Text("طيران افتراضي حر") },
          modifier = Modifier.minimumInteractiveComponentSize(),
        )
        uiState.scenarios.forEach { scen ->
          val done = scen.id in uiState.completedScenarioIds
          FilterChip(
            selected = uiState.activeScenarioId == scen.id,
            onClick = { onSelectScenario(scen.id) },
            label = { Text(scen.titleAr.substringAfter(": ")) },
            leadingIcon = if (done) {
              { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("scenario_chip_${scen.id}"),
          )
        }
      }

      val activeScenario = uiState.scenarios.find { it.id == uiState.activeScenarioId }
      if (activeScenario != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
          ) {
            Text(text = "${activeScenario.titleAr} (${activeScenario.difficultyAr})", style = MaterialTheme.typography.titleSmall)
            Text(text = activeScenario.goalSummaryAr, style = MaterialTheme.typography.bodySmall)
            uiState.scenarioEvaluation?.let { eval ->
              LinearProgressIndicator(
                progress = { eval.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
              )
              Text(
                text = eval.feedbackAr,
                style = MaterialTheme.typography.bodySmall,
                color = if (eval.completed) statusColors.success else if (eval.failedReasonAr != null) statusColors.error else MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    // Real-time 3D OpenGL ES 2.0 Outdoor Flight Simulator Viewport + Telemetry HUD
    item {
      val activeScenario = uiState.scenarios.find { it.id == uiState.activeScenarioId }
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "ميدان الطيران الافتراضي ثلاثي الأبعاد (OpenGL ES 2.0)",
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = onResetCamera,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("توسيط")
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
          ) {
            SimCameraMode.entries.forEach { mode ->
              FilterChip(
                selected = uiState.simCameraMode == mode,
                onClick = { onSelectCameraMode(mode) },
                label = { Text(mode.labelAr) },
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }

          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.25f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("sim_3d_flight_viewport"),
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                FlightSimulator3DView(
                  worldState = Sim3DWorldState(
                    xM = s.xM,
                    yM = s.yM,
                    altitudeM = s.altitudeM,
                    pitchRad = s.pitchRad,
                    rollRad = s.rollRad,
                    yawRad = s.yawRad,
                    thrustN = s.thrustN,
                    armed = s.armed,
                    windEnabled = inp.windEnabled,
                    targetAltMinM = activeScenario?.targetAltitudeMinM,
                    targetAltMaxM = activeScenario?.targetAltitudeMaxM,
                    gates = activeScenario?.gates?.map { SurveyGate3D(it.xM, it.yM, it.altitudeM, it.radiusM) }.orEmpty(),
                    activeGateIndex = uiState.scenarioClearedGates,
                    cameraMode = uiState.simCameraMode,
                    reducedMotion = uiState.displaySettings.reducedMotion,
                  ),
                  resetCameraSignal = uiState.simCameraResetSignal,
                  modifier = Modifier.fillMaxSize(),
                )
                if (uiState.simCameraMode == SimCameraMode.FPV_NOSE) {
                  Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.5f
                    val arm = 12.dp.toPx()
                    val osdColor = Color.White.copy(alpha = 0.85f)
                    drawLine(osdColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 2.dp.toPx())
                    drawLine(osdColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 2.dp.toPx())
                    drawCircle(osdColor, radius = 18.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.5.dp.toPx()))
                  }
                }
              }
            }
          }

          Text(
            text = "اسحب بالإصبع فوق المشهد لتدوير الكاميرا • اختر «منظور FPV» لمحاكاة الكاميرا الأمامية أو سيناريو البوابات 3D.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            MetricPill("الارتفاع", "${"%.2f".format(s.altitudeM)} m")
            MetricPill("السرعة Z", "${"%.2f".format(s.vzMs)} m/s")
            MetricPill("الموقع X/Y", "${"%.1f".format(s.xM)},${"%.1f".format(s.yM)}m")
            MetricPill("البطارية", "${"%.2f".format(s.batteryV)} V")
          }
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            MetricPill("Pitch", "${"%.2f".format(s.pitchRad)} rad")
            MetricPill("Roll", "${"%.2f".format(s.rollRad)} rad")
            MetricPill("Yaw", "${"%.2f".format(s.yawRad)} rad")
            MetricPill("الدفع/التيار", "${"%.1f".format(s.thrustN)}N/${"%.1f".format(s.currentA)}A")
          }

          s.safetyCutReasonAr?.let { cut ->
            Surface(
              color = statusColors.errorContainer,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = cut,
                style = MaterialTheme.typography.bodySmall,
                color = statusColors.error,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      }
    }

    // Tactile Flight Control & Yaw Rotation Pad (Left / Right Yaw + Throttle + Pitch / Roll)
    item {
      FlightPadControlsCard(
        simRunning = uiState.simRunning,
        throttlePercent = (inp.throttle * 100f).roundToInt(),
        onToggleSimRunning = onToggleSimRunning,
        onStartSimIfIdle = onStartSimIfIdle,
        onSetFlightHoldAxis = onSetFlightHoldAxis,
        onAdjustThrottle = onAdjustThrottle,
        onNudgeSimControl = onNudgeSimControl,
        onResetAxes = { onUpdateSimInput { it.copy(pitchCmd = 0f, rollCmd = 0f, yawRateCmd = 0f) } },
      )
    }

    // Controls
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "التسليح الافتراضي (Arm)", style = MaterialTheme.typography.titleSmall)
            Switch(
              checked = inp.armed,
              onCheckedChange = { armed -> onUpdateSimInput { it.copy(armed = armed, throttle = if (!armed) 0f else it.throttle) } },
              modifier = Modifier.testTag("sim_arm_switch"),
            )
          }

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "تفعيل الرياح الافتراضية (Seed=${uiState.simSeed})", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = inp.windEnabled,
              onCheckedChange = { w -> onUpdateSimInput { it.copy(windEnabled = w) } },
            )
          }

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "الخانق (Throttle): ${(inp.throttle * 100f).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(
                onClick = { onUpdateSimInput { it.copy(throttle = 0.49f) } },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Text("تحويم 49%")
              }
              OutlinedButton(
                onClick = { onUpdateSimInput { it.copy(pitchCmd = 0f, rollCmd = 0f, yawRateCmd = 0f) } },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Text("تصفير المحاور")
              }
            }
          }
          Slider(
            value = inp.throttle,
            onValueChange = { t -> onUpdateSimInput { it.copy(throttle = t) } },
            valueRange = 0f..1f,
            modifier = Modifier.testTag("sim_throttle_slider"),
          )

          Text(text = "الميل الأمامي/الخلفي (Pitch): ${"%.2f".format(inp.pitchCmd)}", style = MaterialTheme.typography.bodySmall)
          Slider(
            value = inp.pitchCmd,
            onValueChange = { p -> onUpdateSimInput { it.copy(pitchCmd = p) } },
            valueRange = -1f..1f,
          )

          Text(text = "التدحرج الجانبي (Roll): ${"%.2f".format(inp.rollCmd)}", style = MaterialTheme.typography.bodySmall)
          Slider(
            value = inp.rollCmd,
            onValueChange = { r -> onUpdateSimInput { it.copy(rollCmd = r) } },
            valueRange = -1f..1f,
          )

          Text(text = "الدوران الرأسي (Yaw Rate): ${"%.2f".format(inp.yawRateCmd)}", style = MaterialTheme.typography.bodySmall)
          Slider(
            value = inp.yawRateCmd,
            onValueChange = { y -> onUpdateSimInput { it.copy(yawRateCmd = y) } },
            valueRange = -1f..1f,
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = onToggleSimRunning,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("sim_play_pause_button"),
            ) {
              Icon(if (uiState.simRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (uiState.simRunning) "إيقاف" else "تشغيل")
            }
            OutlinedButton(
              onClick = onStepSimOnce,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sim_step_button"),
            ) {
              Icon(Icons.Default.SkipNext, contentDescription = null)
              Text("خطوة")
            }
            OutlinedButton(
              onClick = onResetSim,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("sim_reset_button"),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null)
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = { onUpdateSimInput { it.copy(armed = false, throttle = 0f) } },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Default.Stop, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("إيقاف آمن")
            }
            OutlinedButton(
              onClick = onVerifyReplay,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("sim_replay_button"),
            ) {
              Icon(Icons.Default.Replay, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("إعادة حتمية")
            }
          }

          uiState.simReplayStatusAr?.let { msg ->
            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // Equations & Validity limits
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Text(text = "المعادلات والافتراضات وحدود الصلاحية", style = MaterialTheme.typography.titleSmall)
          DronePhysicsEngine.equationsAndAssumptionsAr.forEach { eq ->
            Text(text = "• ${eq.titleAr}: ${eq.equationLtr}", style = MaterialTheme.typography.bodySmall)
            Text(text = "  ${eq.explanationAr}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisualProgrammingContent(
  uiState: LearningUiState,
  onAddProgramBlock: (VisualProgrammingSandbox.AstNode) -> Unit,
  onRemoveLastProgramBlock: () -> Unit,
  onLoadPresetProgram: (Boolean) -> Unit,
  onRunVisualProgram: () -> Unit,
) {
  val statusColors = LocalSemanticStatusColors.current

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("visual_code_list"),
  ) {
    item {
      Text(text = "مختبر البرمجة المرئية المعزولة (AST Sandbox)", style = MaterialTheme.typography.titleLarge)
      Text(
        text = "أوامر محصورة الأنواع للمستشعرات والمخرجات الافتراضية فقط (بحد 24 كتلة، عمق 4، و 250 خطوة محاكاة). لا يُنفّذ أي نص برمجي.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedButton(onClick = { onAddProgramBlock(VisualProgrammingSandbox.AstNode.SetVirtualThrottle(65)) }) {
          Text("+ خانق 65%")
        }
        OutlinedButton(onClick = { onAddProgramBlock(VisualProgrammingSandbox.AstNode.SetVirtualThrottle(49)) }) {
          Text("+ تحويم 49%")
        }
        OutlinedButton(onClick = { onAddProgramBlock(VisualProgrammingSandbox.AstNode.SetVirtualPitchDeg(12)) }) {
          Text("+ ميل 12°")
        }
        OutlinedButton(onClick = { onAddProgramBlock(VisualProgrammingSandbox.AstNode.WaitSteps(30)) }) {
          Text("+ انتظار 30 خطوة")
        }
        OutlinedButton(
          onClick = {
            onAddProgramBlock(
              VisualProgrammingSandbox.AstNode.IfVirtualSensor(
                sensor = VisualProgrammingSandbox.VirtualSensorType.ALTITUDE_M,
                op = VisualProgrammingSandbox.CompareOp.LESS_THAN,
                threshold = 4.0f,
                thenBody = listOf(VisualProgrammingSandbox.AstNode.SetVirtualThrottle(62)),
              ),
            )
          },
        ) {
          Text("+ شرط ارتفاع < 4m")
        }
        OutlinedButton(
          onClick = {
            onAddProgramBlock(
              VisualProgrammingSandbox.AstNode.RepeatBounded(
                times = 2,
                body = listOf(VisualProgrammingSandbox.AstNode.WaitSteps(15)),
              ),
            )
          },
        ) {
          Text("+ تكرار محدود 2×")
        }
        OutlinedButton(onClick = { onAddProgramBlock(VisualProgrammingSandbox.AstNode.SafeDisarm) }) {
          Text("+ إيقاف آمن")
        }
        OutlinedButton(onClick = onRemoveLastProgramBlock) {
          Text("حذف آخر كتلة")
        }
      }
    }

    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedButton(
          onClick = { onLoadPresetProgram(true) },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
        ) {
          Text("نموذج إقلاع آمن")
        }
        OutlinedButton(
          onClick = { onLoadPresetProgram(false) },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("load_unsafe_loop_preset"),
        ) {
          Text("اختبار حلقة محظورة")
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "شجرة الكتل الحالية (${uiState.programBlocks.size} كتلة):", style = MaterialTheme.typography.titleSmall)
          }
          uiState.programBlocks.forEachIndexed { idx, node ->
            Text(
              text = "${idx + 1}. ${describeAstNode(node)}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = onRunVisualProgram,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("run_sandbox_program_button"),
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("تشغيل البرنامج في البيئة المعزولة")
          }
        }
      }
    }

    item {
      val res = uiState.sandboxResult
      if (res != null) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (res.haltedBySafetyOrLimit) statusColors.errorContainer else statusColors.successContainer,
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("sandbox_result_card"),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp),
          ) {
            Text(
              text = res.statusLabelAr,
              style = MaterialTheme.typography.titleSmall,
              color = if (res.haltedBySafetyOrLimit) statusColors.error else statusColors.success,
            )
            Text(text = "الخطوات المنفّذة: ${res.executedSteps}", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            res.traceLogAr.take(10).forEach { line ->
              Text(text = "• $line", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun LogAnalyzerContent(
  uiState: LearningUiState,
  onAnalyzeCurrentLog: () -> Unit,
  onTestIncompleteLog: () -> Unit,
) {
  val statusColors = LocalSemanticStatusColors.current

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("log_analyzer_list"),
  ) {
    item {
      Text(text = "محلل السجلات الافتراضية ومقارنة الرحلات", style = MaterialTheme.typography.titleLarge)
      Text(
        text = "يعرض منحنى الارتفاع الافتراضي (m) وتقارير المقارنة، ويرفض السجلات الناقصة أو المتناقضة بعبارة «الأدلة غير كافية.».",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Button(
          onClick = onAnalyzeCurrentLog,
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("analyze_sim_log_button"),
        ) {
          Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("تحليل السجل المسجل (${uiState.simRecordedSamples.size})")
        }
        OutlinedButton(
          onClick = onTestIncompleteLog,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("test_incomplete_log_button"),
        ) {
          Text("اختبار سجل ناقص")
        }
      }
    }

    item {
      val samples = uiState.sandboxResult?.samples?.takeIf { it.isNotEmpty() } ?: uiState.simRecordedSamples
      if (samples.size >= 2) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp),
          ) {
            Text(text = "منحنى الارتفاع الزمني الافتراضي (m)", style = MaterialTheme.typography.titleSmall)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              val lineColor = MaterialTheme.colorScheme.primary
              val axisColor = MaterialTheme.colorScheme.outline
              Canvas(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(140.dp),
              ) {
                val w = size.width
                val h = size.height
                drawLine(axisColor, Offset(0f, h - 4f), Offset(w, h - 4f), strokeWidth = 1.5.dp.toPx())
                val maxAlt = (samples.maxOf { it.altitudeM }).coerceAtLeast(1f)
                val path = Path()
                samples.forEachIndexed { idx, sample ->
                  val x = (idx.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w
                  val y = (h - 8f) - ((sample.altitudeM / maxAlt) * (h - 20f))
                  if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
              }
            }
          }
        }
      }
    }

    item {
      val report = uiState.analyzedLogReport
      if (report != null) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (report.conclusive) statusColors.successContainer else statusColors.errorContainer,
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("log_analysis_report_card"),
        ) {
          Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp),
          ) {
            Text(
              text = report.headlineAr,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = if (report.conclusive) statusColors.success else statusColors.error,
            )
            if (report.conclusive) {
              Text(
                text = "المدة: ${"%.2f".format(report.durationSec)} s • أقصى ارتفاع: ${"%.2f".format(report.maxAltitudeM)} m • أدنى جهد: ${"%.2f".format(report.minBatteryV)} V",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
            report.notesAr.forEach { n ->
              Text(text = "• $n", style = MaterialTheme.typography.bodySmall)
            }
            uiState.comparedLogReport?.let { cmp ->
              HorizontalDivider()
              Text(text = cmp.summaryAr, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun FlightPadControlsCard(
  simRunning: Boolean,
  throttlePercent: Int,
  onToggleSimRunning: () -> Unit,
  onStartSimIfIdle: () -> Unit,
  onSetFlightHoldAxis: (yawRateCmd: Float?, pitchCmd: Float?, rollCmd: Float?) -> Unit,
  onAdjustThrottle: (Float) -> Unit,
  onNudgeSimControl: (yawRateCmd: Float?, pitchCmd: Float?, rollCmd: Float?) -> Unit,
  onResetAxes: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().testTag("sim_flight_pad_card"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.padding(14.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "لوحة التحكم بالطائرة والتدوير (أزرار فورية)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "نقرة قصيرة لخطوة دقيقة • اضغط مطولاً للطيران أو التدوير المستمر • الخانق: $throttlePercent%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
          onClick = onToggleSimRunning,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("sim_pad_toggle_run_button"),
        ) {
          Icon(
            imageVector = if (simRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (simRunning) "إيقاف" else "تشغيل",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (simRunning) "إيقاف" else "تشغيل", maxLines = 1)
        }
      }

      // Keep physical Left/Right directional layout identical regardless of RTL locale
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          // Left Pad: Throttle & Yaw (Rotate Left / Rotate Right)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(8.dp),
            ) {
              Text(
                text = "الارتفاع والدوران",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              HoldFlightButton(
                label = "صعود",
                icon = Icons.Default.KeyboardArrowUp,
                contentDesc = "زيادة الارتفاع (صعود)",
                testTag = "sim_btn_throttle_up",
                onTapNudge = { onAdjustThrottle(0.10f) },
                onHoldStep = {
                  onStartSimIfIdle()
                  while (true) {
                    onAdjustThrottle(0.06f)
                    delay(140L)
                  }
                },
                onHoldEnd = {},
                modifier = Modifier.fillMaxWidth(),
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                HoldFlightButton(
                  label = "تدوير يسار",
                  icon = Icons.AutoMirrored.Filled.RotateLeft,
                  contentDesc = "تدوير الطائرة يساراً",
                  testTag = "sim_btn_yaw_left",
                  onTapNudge = { onNudgeSimControl(0.85f, null, null) },
                  onHoldStep = {
                    onStartSimIfIdle()
                    onSetFlightHoldAxis(0.85f, null, null)
                    awaitCancellation()
                  },
                  onHoldEnd = { onSetFlightHoldAxis(0f, null, null) },
                  modifier = Modifier.weight(1f),
                )
                HoldFlightButton(
                  label = "تدوير يمين",
                  icon = Icons.AutoMirrored.Filled.RotateRight,
                  contentDesc = "تدوير الطائرة يميناً",
                  testTag = "sim_btn_yaw_right",
                  onTapNudge = { onNudgeSimControl(-0.85f, null, null) },
                  onHoldStep = {
                    onStartSimIfIdle()
                    onSetFlightHoldAxis(-0.85f, null, null)
                    awaitCancellation()
                  },
                  onHoldEnd = { onSetFlightHoldAxis(0f, null, null) },
                  modifier = Modifier.weight(1f),
                )
              }
              HoldFlightButton(
                label = "هبوط",
                icon = Icons.Default.KeyboardArrowDown,
                contentDesc = "تقليل الارتفاع (هبوط)",
                testTag = "sim_btn_throttle_down",
                onTapNudge = { onAdjustThrottle(-0.10f) },
                onHoldStep = {
                  onStartSimIfIdle()
                  while (true) {
                    onAdjustThrottle(-0.06f)
                    delay(140L)
                  }
                },
                onHoldEnd = {},
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          // Right Pad: Pitch (Forward / Backward) & Roll (Left / Right)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(8.dp),
            ) {
              Text(
                text = "الاتجاه الأفقي",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              HoldFlightButton(
                label = "أمام",
                icon = Icons.Default.KeyboardArrowUp,
                contentDesc = "تحريك الطائرة للأمام",
                testTag = "sim_btn_pitch_forward",
                onTapNudge = { onNudgeSimControl(null, 0.75f, null) },
                onHoldStep = {
                  onStartSimIfIdle()
                  onSetFlightHoldAxis(null, 0.75f, null)
                  awaitCancellation()
                },
                onHoldEnd = { onSetFlightHoldAxis(null, 0f, null) },
                modifier = Modifier.fillMaxWidth(),
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                HoldFlightButton(
                  label = "يسار",
                  icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                  contentDesc = "إمالة وحركة لليسار",
                  testTag = "sim_btn_roll_left",
                  onTapNudge = { onNudgeSimControl(null, null, -0.75f) },
                  onHoldStep = {
                    onStartSimIfIdle()
                    onSetFlightHoldAxis(null, null, -0.75f)
                    awaitCancellation()
                  },
                  onHoldEnd = { onSetFlightHoldAxis(null, null, 0f) },
                  modifier = Modifier.weight(1f),
                )
                HoldFlightButton(
                  label = "يمين",
                  icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDesc = "إمالة وحركة لليمين",
                  testTag = "sim_btn_roll_right",
                  onTapNudge = { onNudgeSimControl(null, null, 0.75f) },
                  onHoldStep = {
                    onStartSimIfIdle()
                    onSetFlightHoldAxis(null, null, 0.75f)
                    awaitCancellation()
                  },
                  onHoldEnd = { onSetFlightHoldAxis(null, null, 0f) },
                  modifier = Modifier.weight(1f),
                )
              }
              HoldFlightButton(
                label = "خلف",
                icon = Icons.Default.KeyboardArrowDown,
                contentDesc = "تحريك الطائرة للخلف",
                testTag = "sim_btn_pitch_backward",
                onTapNudge = { onNudgeSimControl(null, -0.75f, null) },
                onHoldStep = {
                  onStartSimIfIdle()
                  onSetFlightHoldAxis(null, -0.75f, null)
                  awaitCancellation()
                },
                onHoldEnd = { onSetFlightHoldAxis(null, 0f, null) },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }

      OutlinedButton(
        onClick = onResetAxes,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("sim_btn_center_axes"),
      ) {
        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("تثبيت وتصفير محاور التوجيه والدوران", maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
private fun HoldFlightButton(
  label: String,
  icon: ImageVector,
  contentDesc: String,
  testTag: String,
  onTapNudge: () -> Unit,
  onHoldStep: suspend () -> Unit,
  onHoldEnd: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  var heldActivated by remember { mutableStateOf(false) }

  LaunchedEffect(isPressed) {
    if (isPressed) {
      heldActivated = false
      try {
        delay(150L)
        heldActivated = true
        onHoldStep()
      } finally {
        if (heldActivated) {
          onHoldEnd()
        }
      }
    }
  }

  FilledTonalButton(
    onClick = {
      if (!heldActivated) {
        onTapNudge()
      }
      heldActivated = false
    },
    interactionSource = interactionSource,
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
    modifier = modifier.minimumInteractiveComponentSize().testTag(testTag),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = contentDesc,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MetricPill(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
  }
}

private fun describeAstNode(node: VisualProgrammingSandbox.AstNode): String = when (node) {
  is VisualProgrammingSandbox.AstNode.SetVirtualThrottle -> "ضبط الخانق الافتراضي = ${node.percent}%"
  is VisualProgrammingSandbox.AstNode.SetVirtualPitchDeg -> "ضبط الميل الافتراضي = ${node.degrees}°"
  is VisualProgrammingSandbox.AstNode.WaitSteps -> "انتظار ${node.steps} خطوة (${"%.2f".format(node.steps * 0.02f)} s)"
  is VisualProgrammingSandbox.AstNode.RepeatBounded -> "كرّر ${node.times} مرة (${node.body.size} أوامر)"
  is VisualProgrammingSandbox.AstNode.IfVirtualSensor -> "إذا كان ${node.sensor.labelAr} ${node.op.symbol} ${node.threshold}"
  VisualProgrammingSandbox.AstNode.SafeDisarm -> "إيقاف آمن (SafeDisarm)"
}
