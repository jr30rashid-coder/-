package com.example.domain

/**
 * Phase 9: Sandboxed Typed-AST Visual Programming Engine.
 * Strictly supports virtual sensors and bounded virtual flight outputs.
 * No string evaluation, no script execution, no OS/network/hardware access.
 */
object VisualProgrammingSandbox {

  const val MAX_BLOCKS = 24
  const val MAX_NESTING_DEPTH = 4
  const val MAX_EXECUTION_STEPS = 250
  const val MAX_WALL_MILLIS = 200L

  enum class VirtualSensorType(val labelAr: String) {
    ALTITUDE_M("الارتفاع الافتراضي (m)"),
    BATTERY_V("جهد البطارية الافتراضي (V)"),
    VERTICAL_SPEED_MS("السرعة العمودية (m/s)"),
  }

  enum class CompareOp(val symbol: String) {
    LESS_THAN("<"),
    GREATER_THAN(">"),
  }

  sealed class AstNode {
    data class SetVirtualThrottle(val percent: Int) : AstNode()
    data class SetVirtualPitchDeg(val degrees: Int) : AstNode()
    data class WaitSteps(val steps: Int) : AstNode()
    data class RepeatBounded(val times: Int, val body: List<AstNode>) : AstNode()
    data class IfVirtualSensor(
      val sensor: VirtualSensorType,
      val op: CompareOp,
      val threshold: Float,
      val thenBody: List<AstNode>,
      val elseBody: List<AstNode> = emptyList(),
    ) : AstNode()
    data object SafeDisarm : AstNode()
  }

  data class ProgramValidationResult(
    val isValid: Boolean,
    val totalBlocks: Int,
    val maxDepth: Int,
    val errorAr: String? = null,
  )

  data class SandboxExecutionResult(
    val statusLabelAr: String,
    val haltedBySafetyOrLimit: Boolean,
    val executedSteps: Int,
    val traceLogAr: List<String>,
    val samples: List<DronePhysicsEngine.SimTelemetrySample>,
  )

  fun validateProgram(nodes: List<AstNode>): ProgramValidationResult {
    var count = 0
    var deepest = 0

    fun walk(list: List<AstNode>, depth: Int): String? {
      if (depth > MAX_NESTING_DEPTH) {
        return "تم رفض البرنامج: تجاوز عمق التداخل الحد المسموح ($MAX_NESTING_DEPTH)."
      }
      if (depth > deepest) deepest = depth
      for (node in list) {
        count++
        if (count > MAX_BLOCKS) {
          return "تم رفض البرنامج: تجاوز عدد الكتل الحد المسموح ($MAX_BLOCKS)."
        }
        when (node) {
          is AstNode.SetVirtualThrottle -> {
            if (node.percent !in 0..100) {
              return "تم رفض البرنامج: نسبة الخانق (${node.percent}%) خارج النطاق 0..100."
            }
          }
          is AstNode.SetVirtualPitchDeg -> {
            if (node.degrees !in -20..20) {
              return "تم رفض البرنامج: زاوية الميل (${node.degrees}°) خارج النطاق الآمن -20..20."
            }
          }
          is AstNode.WaitSteps -> {
            if (node.steps !in 1..100) {
              return "تم رفض البرنامج: عدد خطوات الانتظار (${node.steps}) خارج النطاق 1..100."
            }
          }
          is AstNode.RepeatBounded -> {
            if (node.times !in 1..20) {
              return "تم رفض البرنامج: عدد التكرار (${node.times}) خارج النطاق الآمن 1..20 (منع الحلقات غير المنتهية)."
            }
            walk(node.body, depth + 1)?.let { return it }
          }
          is AstNode.IfVirtualSensor -> {
            walk(node.thenBody, depth + 1)?.let { return it }
            walk(node.elseBody, depth + 1)?.let { return it }
          }
          AstNode.SafeDisarm -> Unit
        }
      }
      return null
    }

    if (nodes.isEmpty()) {
      return ProgramValidationResult(isValid = false, totalBlocks = 0, maxDepth = 0, errorAr = "البرنامج فارغ؛ أضف كتلة واحدة على الأقل.")
    }

    val err = walk(nodes, 1)
    return ProgramValidationResult(
      isValid = err == null,
      totalBlocks = count,
      maxDepth = deepest,
      errorAr = err,
    )
  }

  fun execute(
    nodes: List<AstNode>,
    seed: Long = 42L,
  ): SandboxExecutionResult {
    val validation = validateProgram(nodes)
    if (!validation.isValid) {
      return SandboxExecutionResult(
        statusLabelAr = validation.errorAr ?: "فشل التحقق من البرنامج.",
        haltedBySafetyOrLimit = true,
        executedSteps = 0,
        traceLogAr = listOf(validation.errorAr ?: "رفض التحقق"),
        samples = emptyList(),
      )
    }

    val startMs = System.currentTimeMillis()
    var simState = DronePhysicsEngine.SimState(armed = true)
    var currentInput = DronePhysicsEngine.SimControlInput(armed = true, throttle = 0f)
    val trace = mutableListOf<String>()
    val samples = mutableListOf<DronePhysicsEngine.SimTelemetrySample>()
    var stepsExecuted = 0
    var haltReason: String? = null

    fun evalCondition(cond: AstNode.IfVirtualSensor): Boolean {
      val reading = when (cond.sensor) {
        VirtualSensorType.ALTITUDE_M -> simState.altitudeM
        VirtualSensorType.BATTERY_V -> simState.batteryV
        VirtualSensorType.VERTICAL_SPEED_MS -> simState.vzMs
      }
      return when (cond.op) {
        CompareOp.LESS_THAN -> reading < cond.threshold
        CompareOp.GREATER_THAN -> reading > cond.threshold
      }
    }

    fun runBlock(list: List<AstNode>) {
      for (node in list) {
        if (haltReason != null) return
        if (System.currentTimeMillis() - startMs > MAX_WALL_MILLIS) {
          haltReason = "إيقاف آمن: تجاوز زمن التنفيذ المعزول (${MAX_WALL_MILLIS} ms)."
          return
        }
        when (node) {
          is AstNode.SetVirtualThrottle -> {
            currentInput = currentInput.copy(armed = true, throttle = node.percent / 100f)
            trace.add("ضبط الخانق الافتراضي إلى ${node.percent}%")
          }
          is AstNode.SetVirtualPitchDeg -> {
            currentInput = currentInput.copy(pitchCmd = node.degrees / 25f)
            trace.add("ضبط الميل الافتراضي إلى ${node.degrees}°")
          }
          is AstNode.WaitSteps -> {
            trace.add("تنفيذ ${node.steps} خطوة محاكاة (${"%.2f".format(node.steps * DronePhysicsEngine.FIXED_DT_SEC)} s)")
            repeat(node.steps) {
              if (stepsExecuted >= MAX_EXECUTION_STEPS) {
                haltReason = "إيقاف آمن: بلوغ الحد الأقصى للخطوات ($MAX_EXECUTION_STEPS خطوة)."
                return
              }
              simState = DronePhysicsEngine.step(simState, currentInput, seed)
              stepsExecuted++
              samples.add(DronePhysicsEngine.toSample(simState))
              if (simState.safetyCutReasonAr != null) {
                haltReason = simState.safetyCutReasonAr
                return
              }
            }
          }
          is AstNode.RepeatBounded -> {
            trace.add("تكرار محدود (${node.times} مرات)")
            repeat(node.times) {
              if (haltReason != null) return
              runBlock(node.body)
            }
          }
          is AstNode.IfVirtualSensor -> {
            val res = evalCondition(node)
            trace.add("شرط [${node.sensor.labelAr} ${node.op.symbol} ${node.threshold}] -> ${if (res) "تحقق" else "لم يتحقق"}")
            runBlock(if (res) node.thenBody else node.elseBody)
          }
          AstNode.SafeDisarm -> {
            currentInput = currentInput.copy(armed = false, throttle = 0f, pitchCmd = 0f, rollCmd = 0f)
            simState = DronePhysicsEngine.step(simState, currentInput, seed)
            stepsExecuted++
            samples.add(DronePhysicsEngine.toSample(simState))
            trace.add("إيقاف آمن للمحركات الافتراضية (SafeDisarm)")
          }
        }
      }
    }

    runBlock(nodes)

    val statusText = haltReason ?: "اكتمل التنفيذ المعزول بأمان خلال $stepsExecuted خطوة."
    return SandboxExecutionResult(
      statusLabelAr = statusText,
      haltedBySafetyOrLimit = haltReason != null,
      executedSteps = stepsExecuted,
      traceLogAr = trace,
      samples = samples,
    )
  }

  fun samplePresetTakeoffAndHover(): List<AstNode> = listOf(
    AstNode.SetVirtualThrottle(68),
    AstNode.WaitSteps(35),
    AstNode.IfVirtualSensor(
      sensor = VirtualSensorType.ALTITUDE_M,
      op = CompareOp.GREATER_THAN,
      threshold = 2.0f,
      thenBody = listOf(
        AstNode.SetVirtualThrottle(49),
        AstNode.WaitSteps(50),
      ),
      elseBody = listOf(
        AstNode.SetVirtualThrottle(60),
        AstNode.WaitSteps(30),
      ),
    ),
    AstNode.SetVirtualThrottle(35),
    AstNode.WaitSteps(40),
    AstNode.SafeDisarm,
  )
}
