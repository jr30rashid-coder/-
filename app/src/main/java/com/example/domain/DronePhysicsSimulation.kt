package com.example.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Phase 5 & 6: Fixed-step SI physics simulation (educational simplified quadrotor model)
 * and progressive guided virtual training scenarios.
 * Strictly decoupled from UI and real hardware.
 */
object DronePhysicsEngine {

  const val FIXED_DT_SEC = 0.02f // 50 Hz fixed step
  const val GRAVITY_MS2 = 9.81f
  const val MASS_KG = 1.40f
  const val MAX_TOTAL_THRUST_N = 28.0f
  const val LINEAR_DRAG_COEFF = 0.55f
  const val MAX_TILT_RAD = 0.45f // ~25.8 deg safe educational tilt cap
  const val HARD_SAFETY_TILT_RAD = 0.70f // ~40 deg cutoff envelope
  const val MAX_ALTITUDE_M = 120.0f
  const val MIN_BATTERY_CUTOFF_V = 13.0f
  const val NOMINAL_BATTERY_V = 16.4f

  data class SimControlInput(
    val armed: Boolean = false,
    val throttle: Float = 0f, // 0.0 .. 1.0
    val pitchCmd: Float = 0f, // -1.0 .. 1.0
    val rollCmd: Float = 0f, // -1.0 .. 1.0
    val yawRateCmd: Float = 0f, // -1.0 .. 1.0
    val windEnabled: Boolean = false,
  )

  data class SimState(
    val stepIndex: Int = 0,
    val timeSec: Float = 0f,
    val xM: Float = 0f,
    val yM: Float = 0f,
    val altitudeM: Float = 0f,
    val vxMs: Float = 0f,
    val vyMs: Float = 0f,
    val vzMs: Float = 0f,
    val pitchRad: Float = 0f,
    val rollRad: Float = 0f,
    val yawRad: Float = 0f,
    val thrustN: Float = 0f,
    val batteryV: Float = NOMINAL_BATTERY_V,
    val currentA: Float = 0.3f,
    val armed: Boolean = false,
    val groundContact: Boolean = true,
    val safetyCutReasonAr: String? = null,
  )

  data class SimTelemetrySample(
    val stepIndex: Int,
    val timeSec: Float,
    val altitudeM: Float,
    val vzMs: Float,
    val horizontalSpeedMs: Float,
    val thrustN: Float,
    val pitchRad: Float,
    val rollRad: Float,
    val batteryV: Float,
    val currentA: Float,
  ) {
    fun toStateSnapshot(): String =
      "t=${"%.2f".format(timeSec)}s z=${"%.2f".format(altitudeM)}m vz=${"%.2f".format(vzMs)}m/s V=${"%.2f".format(batteryV)}V"
  }

  data class SurveyGateWaypoint(
    val id: String,
    val labelAr: String,
    val xM: Float,
    val yM: Float,
    val altitudeM: Float,
    val radiusM: Float = 2.0f,
  )

  data class TrainingScenario(
    val id: String,
    val titleAr: String,
    val difficultyAr: String,
    val goalSummaryAr: String,
    val instructionsAr: List<String>,
    val constraintsAr: List<String>,
    val targetAltitudeMinM: Float,
    val targetAltitudeMaxM: Float,
    val requiredHoldSec: Float,
    val maxAllowedTiltRad: Float,
    val windEnabledByDefault: Boolean,
    val gates: List<SurveyGateWaypoint> = emptyList(),
  )

  data class ScenarioEvaluation(
    val scenarioId: String,
    val progressFraction: Float,
    val completed: Boolean,
    val failedReasonAr: String?,
    val feedbackAr: String,
    val clearedGateCount: Int = 0,
    val totalGateCount: Int = 0,
  )

  val equationsAndAssumptionsAr: List<PhysicsFormula> = listOf(
    PhysicsFormula(
      titleAr = "معادلة الدفع الرأسي مع تأثير الأرض (Ground Effect)",
      equationLtr = "a_z = (T * k_ge(z) * cos(θ) * cos(φ) - m * g - c_d * v_z) / m",
      unitsNoteAr = "T بالنيوتن (N)، k_ge يصل إلى 1.12 قرب الأرض (z < 0.35 m)، m = 1.40 kg",
      explanationAr = "الكتلة ثابتة 1.40 kg؛ يتلاشى تأثير الأرض تلقائياً فوق ارتفاع 0.35 m.",
    ),
    PhysicsFormula(
      titleAr = "تدوير الدفع الأفقي ثلاثي الأبعاد حسب الانعراج (Yaw)",
      equationLtr = "F_world = R_z(ψ) * [T*sin(θ), T*sin(φ)]^T - c_d * v_xy + F_wind",
      unitsNoteAr = "a_x, a_y بوحدة m/s²، ψ زاوية الانعراج بالراديان (rad)",
      explanationAr = "اتجاه الميل للأمام يتحرك فعلياً وفق اتجاه مقدمة الطائرة في الفضاء ثلاثي الأبعاد.",
    ),
    PhysicsFormula(
      titleAr = "التكامل الزمني الثابت (Fixed-Step Euler)",
      equationLtr = "v(t + Δt) = v(t) + a(t) * Δt ,   Δt = 0.02 s (50 Hz)",
      unitsNoteAr = "خطوة زمنية ثابتة مستقلة عن معدل إطارات الشاشة لضمان الحتمية (Determinism)",
      explanationAr = "نفس الحالة الابتدائية والبذرة (Seed) والمدخلات تُنتج نفس المسار تماماً عند الإعادة.",
    ),
  )

  fun step(
    prev: SimState,
    rawInput: SimControlInput,
    seed: Long,
  ): SimState {
    val throttle = rawInput.throttle.coerceIn(0f, 1f)
    val pitchCmd = rawInput.pitchCmd.coerceIn(-1f, 1f)
    val rollCmd = rawInput.rollCmd.coerceIn(-1f, 1f)
    val yawRateCmd = rawInput.yawRateCmd.coerceIn(-1f, 1f)

    // Safety cutoff checks
    if (!rawInput.armed) {
      val restingAltitude = if (prev.altitudeM <= 0.01f) 0f else (prev.altitudeM + prev.vzMs * FIXED_DT_SEC).coerceAtLeast(0f)
      val nextVz = if (restingAltitude <= 0f) 0f else prev.vzMs - GRAVITY_MS2 * FIXED_DT_SEC
      return prev.copy(
        stepIndex = prev.stepIndex + 1,
        timeSec = prev.timeSec + FIXED_DT_SEC,
        altitudeM = restingAltitude,
        vxMs = if (restingAltitude <= 0f) 0f else prev.vxMs * 0.96f,
        vyMs = if (restingAltitude <= 0f) 0f else prev.vyMs * 0.96f,
        vzMs = nextVz,
        pitchRad = if (restingAltitude <= 0f) 0f else prev.pitchRad * 0.9f,
        rollRad = if (restingAltitude <= 0f) 0f else prev.rollRad * 0.9f,
        thrustN = 0f,
        currentA = 0.3f,
        armed = false,
        groundContact = restingAltitude <= 0.001f,
      )
    }

    if (prev.batteryV <= MIN_BATTERY_CUTOFF_V) {
      return prev.copy(
        stepIndex = prev.stepIndex + 1,
        timeSec = prev.timeSec + FIXED_DT_SEC,
        armed = false,
        thrustN = 0f,
        currentA = 0.3f,
        safetyCutReasonAr = "إيقاف آمن تلقائي: هبوط جهد البطارية الافتراضية دون الحد الأدنى (${MIN_BATTERY_CUTOFF_V} V).",
      )
    }

    if (prev.altitudeM >= MAX_ALTITUDE_M && throttle > 0.52f) {
      // Soft altitude ceiling limiter
      return prev.copy(
        stepIndex = prev.stepIndex + 1,
        timeSec = prev.timeSec + FIXED_DT_SEC,
        altitudeM = MAX_ALTITUDE_M,
        vzMs = prev.vzMs.coerceAtMost(0f),
        safetyCutReasonAr = "تنبيه حد الارتفاع الافتراضي (${MAX_ALTITUDE_M.toInt()} m): تم تقييد الصعود التلقائي.",
      )
    }

    // First-order attitude tracking
    val targetPitch = pitchCmd * MAX_TILT_RAD
    val targetRoll = rollCmd * MAX_TILT_RAD
    val alphaAttitude = 0.18f
    val nextPitch = prev.pitchRad + (targetPitch - prev.pitchRad) * alphaAttitude
    val nextRoll = prev.rollRad + (targetRoll - prev.rollRad) * alphaAttitude
    val nextYaw = ((prev.yawRad + yawRateCmd * 1.4f * FIXED_DT_SEC + PI.toFloat()) % (2f * PI.toFloat())) - PI.toFloat()

    if (kotlin.math.abs(nextPitch) > HARD_SAFETY_TILT_RAD || kotlin.math.abs(nextRoll) > HARD_SAFETY_TILT_RAD) {
      return prev.copy(
        stepIndex = prev.stepIndex + 1,
        timeSec = prev.timeSec + FIXED_DT_SEC,
        armed = false,
        thrustN = 0f,
        safetyCutReasonAr = "إيقاف آمن تلقائي: تجاوزت زاوية الميل الحد الآمن.",
      )
    }

    val rawThrustN = throttle * MAX_TOTAL_THRUST_N
    val groundEffectBoost = if (prev.altitudeM < 0.35f) {
      1f + 0.12f * (1f - (prev.altitudeM / 0.35f).coerceIn(0f, 1f))
    } else {
      1f
    }
    val thrustN = rawThrustN * groundEffectBoost
    val verticalThrust = thrustN * cos(nextPitch) * cos(nextRoll)

    // Deterministic seeded wind disturbance (zero when windEnabled == false)
    val (windFx, windFy) = if (rawInput.windEnabled && prev.altitudeM > 0.2f) {
      val rng = Random(seed xor (prev.stepIndex.toLong() * 0x5E3779B97F4A7C15L))
      val gustX = (sin(prev.timeSec * 1.3f) * 1.4f) + ((rng.nextFloat() - 0.5f) * 0.9f)
      val gustY = (cos(prev.timeSec * 0.9f) * 1.1f) + ((rng.nextFloat() - 0.5f) * 0.9f)
      gustX to gustY
    } else {
      0f to 0f
    }

    val bodyFwdThrust = -rawThrustN * sin(nextPitch)
    val bodySideThrust = rawThrustN * sin(nextRoll)
    val worldThrustX = bodyFwdThrust * sin(nextYaw) + bodySideThrust * cos(nextYaw)
    val worldThrustY = bodyFwdThrust * cos(nextYaw) - bodySideThrust * sin(nextYaw)

    val az = (verticalThrust - MASS_KG * GRAVITY_MS2 - LINEAR_DRAG_COEFF * prev.vzMs) / MASS_KG
    val ax = (worldThrustX - LINEAR_DRAG_COEFF * prev.vxMs + windFx) / MASS_KG
    val ay = (worldThrustY - LINEAR_DRAG_COEFF * prev.vyMs + windFy) / MASS_KG

    var nextVz = prev.vzMs + az * FIXED_DT_SEC
    var nextVx = prev.vxMs + ax * FIXED_DT_SEC
    var nextVy = prev.vyMs + ay * FIXED_DT_SEC

    var nextAlt = prev.altitudeM + nextVz * FIXED_DT_SEC
    var groundContact = false
    if (nextAlt <= 0f) {
      nextAlt = 0f
      if (nextVz < 0f) nextVz = 0f
      nextVx *= 0.8f
      nextVy *= 0.8f
      groundContact = true
    }

    val nextX = (prev.xM + nextVx * FIXED_DT_SEC).coerceIn(-25f, 25f)
    val nextY = (prev.yM + nextVy * FIXED_DT_SEC).coerceIn(-25f, 25f)

    val currentA = 0.6f + (throttle * throttle * 26.0f)
    val sagV = currentA * 0.018f
    val depletionRateVPerSec = currentA * 0.0022f
    val baseNoLoadV = (prev.batteryV + (prev.currentA * 0.018f) - depletionRateVPerSec * FIXED_DT_SEC)
      .coerceIn(12.6f, NOMINAL_BATTERY_V)
    val nextBatteryV = (baseNoLoadV - sagV).coerceAtLeast(12.4f)

    return SimState(
      stepIndex = prev.stepIndex + 1,
      timeSec = prev.timeSec + FIXED_DT_SEC,
      xM = nextX,
      yM = nextY,
      altitudeM = nextAlt,
      vxMs = nextVx,
      vyMs = nextVy,
      vzMs = nextVz,
      pitchRad = nextPitch,
      rollRad = nextRoll,
      yawRad = nextYaw,
      thrustN = thrustN,
      batteryV = nextBatteryV,
      currentA = currentA,
      armed = true,
      groundContact = groundContact,
      safetyCutReasonAr = null,
    )
  }

  fun toSample(state: SimState): SimTelemetrySample = SimTelemetrySample(
    stepIndex = state.stepIndex,
    timeSec = state.timeSec,
    altitudeM = state.altitudeM,
    vzMs = state.vzMs,
    horizontalSpeedMs = sqrt(state.vxMs * state.vxMs + state.vyMs * state.vyMs),
    thrustN = state.thrustN,
    pitchRad = state.pitchRad,
    rollRad = state.rollRad,
    batteryV = state.batteryV,
    currentA = state.currentA,
  )

  /**
   * Replays an exact recorded control sequence deterministically from initial state & seed.
   */
  fun replaySession(
    inputs: List<SimControlInput>,
    seed: Long,
    initialState: SimState = SimState(),
  ): List<SimTelemetrySample> {
    var state = initialState
    val out = ArrayList<SimTelemetrySample>(inputs.size)
    for (input in inputs) {
      state = step(state, input, seed)
      out.add(toSample(state))
    }
    return out
  }

  fun getTrainingScenarios(): List<TrainingScenario> = listOf(
    TrainingScenario(
      id = "scen_hover_5m",
      titleAr = "السيناريو 1: الإقلاع والتحويم المستقر عند 5 أمتار",
      difficultyAr = "مبتدئ",
      goalSummaryAr = "ارفع الخانق تدريجياً (نحو 49%–53%) للتحويم بين 4.0 m و 6.5 m لمدة 3 ثوانٍ.",
      instructionsAr = listOf(
        "فعّل التسليح الافتراضي (Arm) قبل زيادة الخانق.",
        "وازن الدفع مع الوزن (~13.73 N عند 49% خانق تقريباً) ثم ثبّت الارتفاع.",
      ),
      constraintsAr = listOf("ابقِ الميل أقل من 0.25 rad", "لا تصعد فوق 15 m"),
      targetAltitudeMinM = 4.0f,
      targetAltitudeMaxM = 6.5f,
      requiredHoldSec = 3.0f,
      maxAllowedTiltRad = 0.25f,
      windEnabledByDefault = false,
    ),
    TrainingScenario(
      id = "scen_altitude_12m",
      titleAr = "السيناريو 2: الصعود المضبوط إلى 12 متراً",
      difficultyAr = "متوسط",
      goalSummaryAr = "اصعد بسلاسة وحافظ على الارتفاع بين 10.5 m و 13.5 m لمدة 3 ثوانٍ.",
      instructionsAr = listOf(
        "زد الخانق مؤقتاً للصعود، ثم أعده قرب نقطة الاتزان عند الاقتراب من 12 m.",
      ),
      constraintsAr = listOf("تجنب التذبذب الحاد في السرعة العمودية", "حافظ على ميل أقل من 0.30 rad"),
      targetAltitudeMinM = 10.5f,
      targetAltitudeMaxM = 13.5f,
      requiredHoldSec = 3.0f,
      maxAllowedTiltRad = 0.30f,
      windEnabledByDefault = false,
    ),
    TrainingScenario(
      id = "scen_wind_hover",
      titleAr = "السيناريو 3: التحويم تحت اضطراب الرياح الافتراضية",
      difficultyAr = "متقدم",
      goalSummaryAr = "فعّل الرياح الافتراضية وحافظ على ارتفاع بين 6.0 m و 9.0 m لمدة 4 ثوانٍ.",
      instructionsAr = listOf(
        "استخدم أوامر Pitch و Roll المحدودة لتعويض الانحراف دون فقدان الارتفاع.",
      ),
      constraintsAr = listOf("الميل المسموح <= 0.35 rad", "تذكّر أن تميل المركبة يزيد الحاجة للدفع الرأسي"),
      targetAltitudeMinM = 6.0f,
      targetAltitudeMaxM = 9.0f,
      requiredHoldSec = 4.0f,
      maxAllowedTiltRad = 0.35f,
      windEnabledByDefault = true,
    ),
    TrainingScenario(
      id = "scen_safe_landing",
      titleAr = "السيناريو 4: الهبوط الآمن وإلغاء التسليح",
      difficultyAr = "متوسط",
      goalSummaryAr = "اهبط بنعومة إلى ارتفاع <= 0.2 m بسرعة هبوط لطيفة ثم أطفئ التسليح.",
      instructionsAr = listOf(
        "خفّض الخانق تدريجياً تحت 48% مع بقاء الميل صفراً، وعند ملامسة الأرض أوقف التسليح.",
      ),
      constraintsAr = listOf("لا توقف التسليح على ارتفاع عالٍ لتفادي السقوط الحر"),
      targetAltitudeMinM = 0.0f,
      targetAltitudeMaxM = 0.3f,
      requiredHoldSec = 1.0f,
      maxAllowedTiltRad = 0.20f,
      windEnabledByDefault = false,
    ),
    TrainingScenario(
      id = "scen_survey_gates",
      titleAr = "السيناريو 5: اجتياز بوابات المسح الجوي الثلاثية (Polygon 3D)",
      difficultyAr = "متقدم",
      goalSummaryAr = "حلّق عبر البوابات الثلاث المضاءة بالترتيب باستخدام الخانق والميل في الميدان 3D أو منظور FPV.",
      instructionsAr = listOf(
        "أقلع أولاً للوصول إلى ارتفاع نحو 3.2 m ثم أمِل للأمام (Pitch+) باتجاه البوابة الأولى.",
        "استعن بـ «منظور FPV» أو «تتبّع 3D» لمتابعة البوابة النشطة باللون الأزرق الفاتح.",
      ),
      constraintsAr = listOf("تجنب تجاوز زاوية الميل 0.42 rad", "ارفع الخانق قليلاً أثناء الميل للأمام للحفاظ على الارتفاع"),
      targetAltitudeMinM = 2.0f,
      targetAltitudeMaxM = 6.0f,
      requiredHoldSec = 0f,
      maxAllowedTiltRad = 0.42f,
      windEnabledByDefault = false,
      gates = listOf(
        SurveyGateWaypoint("gate_1", "البوابة 1 (أمام المنصة)", xM = 0.0f, yM = -4.0f, altitudeM = 3.2f, radiusM = 2.0f),
        SurveyGateWaypoint("gate_2", "البوابة 2 (مسار مائل يميناً)", xM = 2.6f, yM = -7.2f, altitudeM = 4.0f, radiusM = 2.2f),
        SurveyGateWaypoint("gate_3", "البوابة 3 (نهاية الممر المدني)", xM = -2.0f, yM = -10.0f, altitudeM = 3.6f, radiusM = 2.2f),
      ),
    ),
  )

  fun evaluateScenarioProgress(
    scenario: TrainingScenario,
    state: SimState,
    accumulatedHoldSec: Float,
    clearedGateCount: Int = 0,
  ): Pair<Float, ScenarioEvaluation> {
    if (state.safetyCutReasonAr != null) {
      return 0f to ScenarioEvaluation(
        scenarioId = scenario.id,
        progressFraction = 0f,
        completed = false,
        failedReasonAr = state.safetyCutReasonAr,
        feedbackAr = "فشلت المحاولة بسبب تفعيل الإيقاف الآمن. أعد ضبط السيناريو وحاول مجدداً.",
        clearedGateCount = clearedGateCount,
        totalGateCount = scenario.gates.size,
      )
    }

    val maxTilt = maxOf(kotlin.math.abs(state.pitchRad), kotlin.math.abs(state.rollRad))
    if (maxTilt > scenario.maxAllowedTiltRad) {
      return 0f to ScenarioEvaluation(
        scenarioId = scenario.id,
        progressFraction = (accumulatedHoldSec / scenario.requiredHoldSec.coerceAtLeast(1f)).coerceIn(0f, 1f),
        completed = false,
        failedReasonAr = "تجاوزت زاوية الميل الحد (${"%.2f".format(scenario.maxAllowedTiltRad)} rad).",
        feedbackAr = "قلّل أوامر الإمالة لإبقاء الطائرة متزنة ضمن قيود السيناريو.",
        clearedGateCount = clearedGateCount,
        totalGateCount = scenario.gates.size,
      )
    }

    // Waypoint gates scenario progression
    if (scenario.gates.isNotEmpty()) {
      var nextCleared = clearedGateCount.coerceIn(0, scenario.gates.size)
      if (nextCleared < scenario.gates.size && state.armed) {
        val gate = scenario.gates[nextCleared]
        val dx = state.xM - gate.xM
        val dy = state.yM - gate.yM
        val dz = state.altitudeM - gate.altitudeM
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        if (dist <= gate.radiusM) {
          nextCleared += 1
        }
      }
      val fraction = nextCleared.toFloat() / scenario.gates.size.toFloat()
      val completed = nextCleared >= scenario.gates.size
      val feedback = if (completed) {
        "رائع! اجتزت جميع بوابات المسح الجوي الثلاثية (${scenario.gates.size}/${scenario.gates.size}) بنجاح (تدريب افتراضي)."
      } else {
        val nextGate = scenario.gates[nextCleared]
        val dx = nextGate.xM - state.xM
        val dy = nextGate.yM - state.yM
        val dz = nextGate.altitudeM - state.altitudeM
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        "اجتزت $nextCleared/${scenario.gates.size} بوابات • التالي: ${nextGate.labelAr} (على بعد ${"%.1f".format(dist)} m، ارتفاع الهدف ${"%.1f".format(nextGate.altitudeM)} m)."
      }
      return 0f to ScenarioEvaluation(
        scenarioId = scenario.id,
        progressFraction = fraction,
        completed = completed,
        failedReasonAr = null,
        feedbackAr = feedback,
        clearedGateCount = nextCleared,
        totalGateCount = scenario.gates.size,
      )
    }

    val inAltBand = state.altitudeM in scenario.targetAltitudeMinM..scenario.targetAltitudeMaxM
    val conditionMet = if (scenario.id == "scen_safe_landing") {
      inAltBand && !state.armed && state.groundContact
    } else {
      inAltBand && state.armed
    }

    val nextHold = if (conditionMet) accumulatedHoldSec + FIXED_DT_SEC else 0f
    val fraction = (nextHold / scenario.requiredHoldSec).coerceIn(0f, 1f)
    val completed = nextHold >= scenario.requiredHoldSec

    val feedback = when {
      completed -> "أتممت هدف السيناريو الافتراضي بنجاح! (تدريب افتراضي تعليمي غير مطابق للواقع)."
      conditionMet -> "ممتاز! حافظ على الثبات داخل النطاق المستهدف (${"%.1f".format(nextHold)} / ${"%.1f".format(scenario.requiredHoldSec)} s)."
      state.altitudeM < scenario.targetAltitudeMinM -> "الارتفاع الحالي (${"%.1f".format(state.altitudeM)} m) أقل من النطاق المطلوب."
      state.altitudeM > scenario.targetAltitudeMaxM -> "الارتفاع الحالي (${"%.1f".format(state.altitudeM)} m) أعلى من النطاق المطلوب."
      else -> "اضبط التسليح والخانق للوصول إلى النطاق المستهدف."
    }

    return nextHold to ScenarioEvaluation(
      scenarioId = scenario.id,
      progressFraction = fraction,
      completed = completed,
      failedReasonAr = null,
      feedbackAr = feedback,
    )
  }
}
