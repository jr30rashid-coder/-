package com.example.ui.gl

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

enum class SimCameraMode(val labelAr: String) {
  CHASE_3D("تتبّع 3D"),
  FPV_NOSE("منظور FPV"),
  GROUND_OBSERVER("منظور الطيار (VLOS)"),
  TOP_DOWN("منظور مساحي علوي"),
}

data class SurveyGate3D(
  val xM: Float,
  val yM: Float,
  val altitudeM: Float,
  val radiusM: Float,
)

data class Sim3DWorldState(
  val xM: Float = 0f,
  val yM: Float = 0f,
  val altitudeM: Float = 0f,
  val pitchRad: Float = 0f,
  val rollRad: Float = 0f,
  val yawRad: Float = 0f,
  val thrustN: Float = 0f,
  val armed: Boolean = false,
  val windEnabled: Boolean = false,
  val targetAltMinM: Float? = null,
  val targetAltMaxM: Float? = null,
  val gates: List<SurveyGate3D> = emptyList(),
  val activeGateIndex: Int = 0,
  val cameraMode: SimCameraMode = SimCameraMode.CHASE_3D,
  val reducedMotion: Boolean = false,
)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun FlightSimulator3DView(
  worldState: Sim3DWorldState,
  resetCameraSignal: Int,
  modifier: Modifier = Modifier,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val renderer = remember { FlightSimGlRenderer() }

  renderer.updateWorld(worldState)
  val glSurfaceViewRef = remember { arrayOfNulls<GLSurfaceView>(1) }

  DisposableEffect(resetCameraSignal) {
    renderer.resetOrbit()
    onDispose {}
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> glSurfaceViewRef[0]?.onResume()
        Lifecycle.Event.ON_PAUSE -> glSurfaceViewRef[0]?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx: Context ->
      GLSurfaceView(ctx).apply {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        var prevX = 0f
        var prevY = 0f
        setOnTouchListener { v, event ->
          when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
              prevX = event.x
              prevY = event.y
              v.parent?.requestDisallowInterceptTouchEvent(true)
              true
            }
            MotionEvent.ACTION_MOVE -> {
              val dx = event.x - prevX
              val dy = event.y - prevY
              prevX = event.x
              prevY = event.y
              renderer.orbitBy(dx * 0.40f, dy * 0.35f)
              true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
              v.parent?.requestDisallowInterceptTouchEvent(false)
              true
            }
            else -> false
          }
        }
        glSurfaceViewRef[0] = this
      }
    },
    update = {
      renderer.updateWorld(worldState)
    },
  )
}

private class FlightSimGlRenderer : GLSurfaceView.Renderer {

  @Volatile private var world = Sim3DWorldState()
  @Volatile private var orbitYawDeg = 28f
  @Volatile private var orbitPitchDeg = 22f
  private var propAngleDeg = 0f
  private var animPhaseSec = 0f
  private var lastFrameMs = System.currentTimeMillis()

  private var programId = 0
  private var posHandle = 0
  private var normHandle = 0
  private var mvpHandle = 0
  private var modelHandle = 0
  private var colorHandle = 0
  private var lightHandle = 0

  private lateinit var boxMesh: MeshBuffer
  private lateinit var cylinderMesh: MeshBuffer
  private lateinit var coneMesh: MeshBuffer

  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val fpvPoseMatrix = FloatArray(16)
  private val fpvEyeVec = FloatArray(4)
  private val fpvCenterVec = FloatArray(4)
  private val fpvUpVec = FloatArray(4)

  fun updateWorld(state: Sim3DWorldState) {
    world = state
  }

  fun orbitBy(dxDeg: Float, dyDeg: Float) {
    orbitYawDeg = (orbitYawDeg + dxDeg) % 360f
    orbitPitchDeg = (orbitPitchDeg + dyDeg).coerceIn(5f, 82f)
  }

  fun resetOrbit() {
    orbitYawDeg = 28f
    orbitPitchDeg = 22f
  }

  override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    programId = buildShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    posHandle = GLES20.glGetAttribLocation(programId, "aPosition")
    normHandle = GLES20.glGetAttribLocation(programId, "aNormal")
    mvpHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
    modelHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix")
    colorHandle = GLES20.glGetUniformLocation(programId, "uColor")
    lightHandle = GLES20.glGetUniformLocation(programId, "uLightDir")

    boxMesh = createUnitBoxMesh()
    cylinderMesh = createUnitCylinderMesh(segments = 20)
    coneMesh = createUnitConeMesh(segments = 16)
    lastFrameMs = System.currentTimeMillis()
  }

  override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
    Matrix.perspectiveM(projectionMatrix, 0, 50f, aspect, 0.25f, 140f)
  }

  override fun onDrawFrame(unused: GL10?) {
    val w = world
    val nowMs = System.currentTimeMillis()
    val dtSec = ((nowMs - lastFrameMs).coerceIn(0L, 100L)) / 1000f
    lastFrameMs = nowMs
    animPhaseSec += dtSec

    if (!w.reducedMotion) {
      val spinRate = when {
        !w.armed -> 0f
        w.thrustN > 0.5f -> 420f + w.thrustN * 42f
        else -> 220f
      }
      propAngleDeg = (propAngleDeg + dtSec * spinRate) % 360f
    }

    // Crisp daytime sky color
    GLES20.glClearColor(0.56f, 0.77f, 0.94f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    // Drone world coordinates: xM -> X, altitudeM -> Y (up), yM -> Z
    val droneX = w.xM
    val droneY = w.altitudeM + 0.14f
    val droneZ = w.yM

    val yawRad = Math.toRadians(orbitYawDeg.toDouble())
    val pitchRad = Math.toRadians(orbitPitchDeg.toDouble())

    when (w.cameraMode) {
      SimCameraMode.CHASE_3D -> {
        val dist = 3.8f
        val yawSumRad = yawRad + w.yawRad.toDouble()
        val eyeX = droneX + (dist * cos(pitchRad) * sin(yawSumRad)).toFloat()
        val eyeY = (droneY + (dist * sin(pitchRad)).toFloat()).coerceAtLeast(0.45f)
        val eyeZ = droneZ + (dist * cos(pitchRad) * cos(yawSumRad)).toFloat()
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, droneX, droneY, droneZ, 0f, 1f, 0f)
      }
      SimCameraMode.FPV_NOSE -> {
        val dYawDeg = Math.toDegrees(w.yawRad.toDouble()).toFloat()
        val dPitchDeg = Math.toDegrees(w.pitchRad.toDouble()).toFloat()
        val dRollDeg = Math.toDegrees(w.rollRad.toDouble()).toFloat()
        Matrix.setIdentityM(fpvPoseMatrix, 0)
        Matrix.translateM(fpvPoseMatrix, 0, droneX, droneY, droneZ)
        Matrix.rotateM(fpvPoseMatrix, 0, dYawDeg, 0f, 1f, 0f)
        Matrix.rotateM(fpvPoseMatrix, 0, -dPitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(fpvPoseMatrix, 0, -dRollDeg, 0f, 0f, 1f)

        Matrix.multiplyMV(fpvEyeVec, 0, fpvPoseMatrix, 0, floatArrayOf(0f, 0.04f, -0.26f, 1f), 0)
        Matrix.multiplyMV(fpvCenterVec, 0, fpvPoseMatrix, 0, floatArrayOf(0f, -0.10f, -8.0f, 1f), 0)
        Matrix.multiplyMV(fpvUpVec, 0, fpvPoseMatrix, 0, floatArrayOf(0f, 1f, 0f, 0f), 0)
        Matrix.setLookAtM(
          viewMatrix, 0,
          fpvEyeVec[0], fpvEyeVec[1].coerceAtLeast(0.18f), fpvEyeVec[2],
          fpvCenterVec[0], fpvCenterVec[1], fpvCenterVec[2],
          fpvUpVec[0], fpvUpVec[1], fpvUpVec[2],
        )
      }
      SimCameraMode.GROUND_OBSERVER -> {
        val eyeX = (8.5f * sin(yawRad)).toFloat()
        val eyeY = 1.70f
        val eyeZ = (8.5f * cos(yawRad)).toFloat()
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, droneX, droneY.coerceAtLeast(0.6f), droneZ, 0f, 1f, 0f)
      }
      SimCameraMode.TOP_DOWN -> {
        val eyeX = droneX + 0.01f
        val eyeY = (droneY + 11.5f).coerceAtLeast(12f)
        val eyeZ = droneZ + 1.8f
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, droneX, 0f, droneZ, 0f, 1f, 0f)
      }
    }

    Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    GLES20.glUseProgram(programId)
    GLES20.glUniform3f(lightHandle, 0.45f, 0.90f, 0.35f)

    // 1. Survey field terrain tiles (2m x 2m checkerboard over -28m .. +28m)
    drawGroundTerrain()

    // 2. Distant horizon hills ring for scale & depth
    drawHorizonLandmarks()

    // 3. Helipad & orange safety cones at origin (0, 0, 0)
    drawHelipadAndCones()

    // 4. Active scenario 3D target altitude beacon ring or 3D survey waypoint gates
    if (w.gates.isNotEmpty()) {
      drawSurveyGates(w.gates, w.activeGateIndex)
    } else if (w.targetAltMinM != null && w.targetAltMaxM != null && w.targetAltMaxM > 0.5f) {
      val midAlt = (w.targetAltMinM + w.targetAltMaxM) * 0.5f
      val inZone = w.altitudeM in w.targetAltMinM..w.targetAltMaxM
      val ringColor = if (inZone) {
        floatArrayOf(0.10f, 0.85f, 0.42f, 0.72f)
      } else {
        floatArrayOf(0.08f, 0.72f, 0.90f, 0.48f)
      }
      drawTargetAltitudeMarkers(midAlt, ringColor)
    }

    // 5. Dynamic ground shadow directly below drone
    val shadowScale = (1.15f - (w.altitudeM * 0.035f)).coerceIn(0.45f, 1.15f)
    val shadowAlpha = (0.42f - (w.altitudeM * 0.012f)).coerceIn(0.14f, 0.42f)
    drawTransformedBox(
      x = droneX, y = 0.012f, z = droneZ,
      sx = 0.95f * shadowScale, sy = 0.01f, sz = 0.95f * shadowScale,
      yawDeg = Math.toDegrees(w.yawRad.toDouble()).toFloat(),
      pitchDeg = 0f, rollDeg = 0f,
      color = floatArrayOf(0.04f, 0.08f, 0.06f, shadowAlpha),
    )

    // 6. Optional wind indicator streamers around the field when wind is active
    if (w.windEnabled) {
      drawWindIndicators(droneX, droneY, droneZ)
    }

    // 7. Realistic 3D Quadrotor Aircraft (hidden in FPV nose view to prevent clipping)
    if (w.cameraMode != SimCameraMode.FPV_NOSE) {
      drawQuadrotor(
        x = droneX,
        y = droneY,
        z = droneZ,
        yawRad = w.yawRad,
        pitchRad = w.pitchRad,
        rollRad = w.rollRad,
        armed = w.armed,
      )
    }
  }

  private fun drawGroundTerrain() {
    val tileSize = 4f
    for (gx in -7..6) {
      for (gz in -7..6) {
        val isEven = (gx + gz) and 1 == 0
        val distSq = gx * gx + gz * gz
        val tint = if (distSq < 12) {
          if (isEven) floatArrayOf(0.24f, 0.46f, 0.27f, 1f) else floatArrayOf(0.21f, 0.41f, 0.24f, 1f)
        } else {
          if (isEven) floatArrayOf(0.28f, 0.45f, 0.26f, 1f) else floatArrayOf(0.26f, 0.42f, 0.24f, 1f)
        }
        drawTransformedBox(
          x = (gx + 0.5f) * tileSize,
          y = -0.05f,
          z = (gz + 0.5f) * tileSize,
          sx = tileSize,
          sy = 0.10f,
          sz = tileSize,
          yawDeg = 0f, pitchDeg = 0f, rollDeg = 0f,
          color = tint,
        )
      }
    }
    // Thin metric runway axis lines on X and Z
    drawTransformedBox(0f, 0.004f, 0f, 56f, 0.008f, 0.08f, 0f, 0f, 0f, floatArrayOf(0.82f, 0.86f, 0.82f, 0.45f))
    drawTransformedBox(0f, 0.004f, 0f, 0.08f, 0.008f, 56f, 0f, 0f, 0f, floatArrayOf(0.82f, 0.86f, 0.82f, 0.45f))
  }

  private fun drawHorizonLandmarks() {
    val hillColor = floatArrayOf(0.32f, 0.46f, 0.44f, 1f)
    val treeColor = floatArrayOf(0.14f, 0.34f, 0.18f, 1f)
    val trunkColor = floatArrayOf(0.36f, 0.24f, 0.14f, 1f)

    // Distant perimeter hills
    for (i in 0 until 12) {
      val angle = Math.toRadians(i * 30.0)
      val r = 34f
      val hx = (r * cos(angle)).toFloat()
      val hz = (r * sin(angle)).toFloat()
      drawTransformedBox(hx, 1.4f, hz, 12f, 3.0f + (i % 3) * 1.1f, 5f, (i * 30f), 0f, 0f, hillColor)
    }

    // Perimeter reference trees / posts at 14-18m
    val treeSpots = listOf(
      Pair(-12f, -10f), Pair(13f, -11f), Pair(-14f, 9f), Pair(12f, 12f),
      Pair(0f, -16f), Pair(-16f, 0f), Pair(16f, 1f),
    )
    treeSpots.forEach { (tx, tz) ->
      drawTransformedCylinder(tx, 0.7f, tz, 0.18f, 1.4f, trunkColor)
      drawTransformedCone(tx, 2.2f, tz, 0.95f, 2.2f, treeColor)
    }
  }

  private fun drawHelipadAndCones() {
    // Concrete pad disc
    drawTransformedCylinder(0f, 0.01f, 0f, 1.85f, 0.02f, floatArrayOf(0.32f, 0.35f, 0.39f, 1f))
    // Outer safety ring accent (yellow/amber bars)
    drawTransformedCylinder(0f, 0.014f, 0f, 1.55f, 0.018f, floatArrayOf(0.95f, 0.76f, 0.16f, 1f))
    drawTransformedCylinder(0f, 0.018f, 0f, 1.38f, 0.018f, floatArrayOf(0.28f, 0.31f, 0.35f, 1f))

    // White 'H' landing pad marking
    val white = floatArrayOf(0.96f, 0.97f, 0.98f, 1f)
    drawTransformedBox(-0.42f, 0.028f, 0f, 0.16f, 0.015f, 1.1f, 0f, 0f, 0f, white)
    drawTransformedBox(0.42f, 0.028f, 0f, 0.16f, 0.015f, 1.1f, 0f, 0f, 0f, white)
    drawTransformedBox(0f, 0.028f, 0f, 0.84f, 0.015f, 0.16f, 0f, 0f, 0f, white)

    // 4 safety pylons around helipad
    val orange = floatArrayOf(0.96f, 0.42f, 0.10f, 1f)
    listOf(Pair(3.2f, 3.2f), Pair(-3.2f, 3.2f), Pair(-3.2f, -3.2f), Pair(3.2f, -3.2f)).forEach { (cx, cz) ->
      drawTransformedCone(cx, 0.25f, cz, 0.22f, 0.50f, orange)
    }
  }

  private fun drawTargetAltitudeMarkers(altM: Float, color: FloatArray) {
    // Draw a glowing square gate frame at target altitude above the helipad
    val r = 2.0f
    val t = 0.07f
    drawTransformedBox(0f, altM, -r, r * 2f, t, t, 0f, 0f, 0f, color)
    drawTransformedBox(0f, altM, r, r * 2f, t, t, 0f, 0f, 0f, color)
    drawTransformedBox(-r, altM, 0f, t, t, r * 2f, 0f, 0f, 0f, color)
    drawTransformedBox(r, altM, 0f, t, t, r * 2f, 0f, 0f, 0f, color)
  }

  private fun drawSurveyGates(gates: List<SurveyGate3D>, activeGateIndex: Int) {
    val poleColor = floatArrayOf(0.45f, 0.48f, 0.52f, 0.75f)
    gates.forEachIndexed { idx, gate ->
      val gx = gate.xM
      val gy = gate.altitudeM
      val gz = gate.yM
      val halfW = gate.radiusM * 0.75f
      val halfH = gate.radiusM * 0.65f
      val barThickness = if (idx == activeGateIndex) 0.14f else 0.09f

      val gateColor = when {
        idx < activeGateIndex -> floatArrayOf(0.12f, 0.86f, 0.44f, 0.85f) // Cleared: emerald
        idx == activeGateIndex -> floatArrayOf(0.05f, 0.86f, 0.96f, 0.92f) // Active target: bright cyan
        else -> floatArrayOf(0.95f, 0.72f, 0.18f, 0.55f) // Upcoming: muted amber
      }

      // Vertical support poles down to ground
      drawTransformedCylinder(gx - halfW, gy * 0.5f, gz, 0.05f, gy, poleColor)
      drawTransformedCylinder(gx + halfW, gy * 0.5f, gz, 0.05f, gy, poleColor)

      // Gate frame (top, bottom, left, right beams)
      drawTransformedBox(gx, gy + halfH, gz, halfW * 2f + barThickness, barThickness, barThickness, 0f, 0f, 0f, gateColor)
      drawTransformedBox(gx, (gy - halfH).coerceAtLeast(0.2f), gz, halfW * 2f + barThickness, barThickness, barThickness, 0f, 0f, 0f, gateColor)
      drawTransformedBox(gx - halfW, gy, gz, barThickness, halfH * 2f, barThickness, 0f, 0f, 0f, gateColor)
      drawTransformedBox(gx + halfW, gy, gz, barThickness, halfH * 2f, barThickness, 0f, 0f, 0f, gateColor)
    }
  }

  private fun drawWindIndicators(dx: Float, dy: Float, dz: Float) {
    val shift = (animPhaseSec * 2.5f) % 3f
    val windColor = floatArrayOf(0.92f, 0.96f, 1.0f, 0.55f)
    drawTransformedBox(dx - 1.5f + shift, dy + 0.55f, dz - 0.9f, 0.65f, 0.03f, 0.03f, 0f, 0f, 0f, windColor)
    drawTransformedBox(dx - 2.2f + shift, dy + 0.20f, dz + 0.8f, 0.55f, 0.03f, 0.03f, 0f, 0f, 0f, windColor)
  }

  private fun drawQuadrotor(
    x: Float,
    y: Float,
    z: Float,
    yawRad: Float,
    pitchRad: Float,
    rollRad: Float,
    armed: Boolean,
  ) {
    val yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()
    val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
    val rollDeg = Math.toDegrees(rollRad.toDouble()).toFloat()

    // Helper builds drone-local matrix first, then transforms to world pose
    fun drawDronePartBox(
      lx: Float, ly: Float, lz: Float,
      sx: Float, sy: Float, sz: Float,
      localRotYDeg: Float,
      color: FloatArray,
    ) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, x, y, z)
      Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
      Matrix.rotateM(modelMatrix, 0, -pitchDeg, 1f, 0f, 0f)
      Matrix.rotateM(modelMatrix, 0, -rollDeg, 0f, 0f, 1f)
      Matrix.translateM(modelMatrix, 0, lx, ly, lz)
      if (localRotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, localRotYDeg, 0f, 1f, 0f)
      Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
      drawMesh(boxMesh, color)
    }

    fun drawDronePartCylinder(
      lx: Float, ly: Float, lz: Float,
      radius: Float, height: Float,
      color: FloatArray,
    ) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, x, y, z)
      Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
      Matrix.rotateM(modelMatrix, 0, -pitchDeg, 1f, 0f, 0f)
      Matrix.rotateM(modelMatrix, 0, -rollDeg, 0f, 0f, 1f)
      Matrix.translateM(modelMatrix, 0, lx, ly, lz)
      Matrix.scaleM(modelMatrix, 0, radius, height, radius)
      drawMesh(cylinderMesh, color)
    }

    val carbon = floatArrayOf(0.14f, 0.16f, 0.19f, 1f)
    val aluminum = floatArrayOf(0.75f, 0.79f, 0.84f, 1f)
    val batteryBlue = floatArrayOf(0.10f, 0.44f, 0.78f, 1f)
    val propColor = floatArrayOf(0.10f, 0.78f, 0.74f, 0.86f)
    val frontLed = if (armed) floatArrayOf(0.12f, 0.96f, 0.34f, 1f) else floatArrayOf(0.18f, 0.50f, 0.25f, 1f)
    val rearLed = if (armed) floatArrayOf(0.98f, 0.20f, 0.18f, 1f) else floatArrayOf(0.50f, 0.16f, 0.16f, 1f)

    // Central chassis + battery + GNSS dome + landing skids
    drawDronePartBox(0f, 0f, 0f, 0.28f, 0.06f, 0.42f, 0f, carbon)
    drawDronePartBox(0f, -0.06f, 0f, 0.18f, 0.07f, 0.28f, 0f, batteryBlue)
    drawDronePartCylinder(0f, 0.07f, 0.08f, 0.06f, 0.06f, floatArrayOf(0.92f, 0.88f, 0.74f, 1f))
    // Landing skids
    drawDronePartBox(-0.18f, -0.11f, 0f, 0.025f, 0.025f, 0.44f, 0f, carbon)
    drawDronePartBox(0.18f, -0.11f, 0f, 0.025f, 0.025f, 0.44f, 0f, carbon)
    drawDronePartBox(-0.14f, -0.06f, 0f, 0.02f, 0.10f, 0.03f, 0f, carbon)
    drawDronePartBox(0.14f, -0.06f, 0f, 0.02f, 0.10f, 0.03f, 0f, carbon)

    // 4 Arms, motors, LEDs & propellers (front is -Z, rear is +Z)
    val arms = listOf(
      Triple(0.36f, -0.36f, -45f),  // Front-right
      Triple(-0.36f, -0.36f, 45f),  // Front-left
      Triple(-0.36f, 0.36f, -45f),  // Rear-left
      Triple(0.36f, 0.36f, 45f),    // Rear-right
    )

    arms.forEachIndexed { idx, (ax, az, rotY) ->
      drawDronePartBox(ax * 0.52f, 0f, az * 0.52f, 0.045f, 0.03f, 0.52f, rotY, carbon)
      drawDronePartCylinder(ax, 0.04f, az, 0.055f, 0.065f, aluminum)
      val ledColor = if (az < 0f) frontLed else rearLed
      drawDronePartBox(ax, -0.02f, az, 0.04f, 0.025f, 0.04f, 0f, ledColor)

      val dir = if (idx % 2 == 0) 1f else -1f
      val bladeRot = propAngleDeg * dir + idx * 42f
      drawDronePartBox(ax, 0.085f, az, 0.36f, 0.012f, 0.042f, bladeRot, propColor)
      drawDronePartBox(ax, 0.085f, az, 0.042f, 0.012f, 0.36f, bladeRot, propColor)
    }
  }

  private fun drawTransformedBox(
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    yawDeg: Float, pitchDeg: Float, rollDeg: Float,
    color: FloatArray,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (yawDeg != 0f) Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
    if (pitchDeg != 0f) Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
    if (rollDeg != 0f) Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    drawMesh(boxMesh, color)
  }

  private fun drawTransformedCylinder(
    x: Float, y: Float, z: Float,
    radius: Float, height: Float,
    color: FloatArray,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, radius, height, radius)
    drawMesh(cylinderMesh, color)
  }

  private fun drawTransformedCone(
    x: Float, y: Float, z: Float,
    radius: Float, height: Float,
    color: FloatArray,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, radius, height, radius)
    drawMesh(coneMesh, color)
  }

  private fun drawMesh(mesh: MeshBuffer, color: FloatArray) {
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4fv(colorHandle, 1, color, 0)

    GLES20.glEnableVertexAttribArray(posHandle)
    GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.positions)
    GLES20.glEnableVertexAttribArray(normHandle)
    GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.normals)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)

    GLES20.glDisableVertexAttribArray(posHandle)
    GLES20.glDisableVertexAttribArray(normHandle)
  }

  private class MeshBuffer(
    val positions: FloatBuffer,
    val normals: FloatBuffer,
    val vertexCount: Int,
  )

  private fun toFloatBuffer(arr: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(arr)
      position(0)
    }

  private fun createUnitBoxMesh(): MeshBuffer {
    val p = mutableListOf<Float>()
    val n = mutableListOf<Float>()
    fun addQuad(
      v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray,
      nx: Float, ny: Float, nz: Float,
    ) {
      listOf(v0, v1, v2, v0, v2, v3).forEach { v ->
        p.add(v[0]); p.add(v[1]); p.add(v[2])
        n.add(nx); n.add(ny); n.add(nz)
      }
    }
    val h = 0.5f
    addQuad(floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h), 0f, 0f, 1f)
    addQuad(floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h), 0f, 0f, -1f)
    addQuad(floatArrayOf(-h, h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), floatArrayOf(-h, h, -h), 0f, 1f, 0f)
    addQuad(floatArrayOf(-h, -h, -h), floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(-h, -h, h), 0f, -1f, 0f)
    addQuad(floatArrayOf(h, -h, h), floatArrayOf(h, -h, -h), floatArrayOf(h, h, -h), floatArrayOf(h, h, h), 1f, 0f, 0f)
    addQuad(floatArrayOf(-h, -h, -h), floatArrayOf(-h, -h, h), floatArrayOf(-h, h, h), floatArrayOf(-h, h, -h), -1f, 0f, 0f)
    return MeshBuffer(toFloatBuffer(p.toFloatArray()), toFloatBuffer(n.toFloatArray()), p.size / 3)
  }

  private fun createUnitCylinderMesh(segments: Int): MeshBuffer {
    val p = mutableListOf<Float>()
    val n = mutableListOf<Float>()
    val halfH = 0.5f
    for (i in 0 until segments) {
      val a0 = (2.0 * Math.PI * i) / segments
      val a1 = (2.0 * Math.PI * (i + 1)) / segments
      val x0 = cos(a0).toFloat()
      val z0 = sin(a0).toFloat()
      val x1 = cos(a1).toFloat()
      val z1 = sin(a1).toFloat()
      p.addAll(listOf(x0, -halfH, z0, x1, -halfH, z1, x1, halfH, z1))
      n.addAll(listOf(x0, 0f, z0, x1, 0f, z1, x1, 0f, z1))
      p.addAll(listOf(x0, -halfH, z0, x1, halfH, z1, x0, halfH, z0))
      n.addAll(listOf(x0, 0f, z0, x1, 0f, z1, x0, 0f, z0))
      p.addAll(listOf(0f, halfH, 0f, x0, halfH, z0, x1, halfH, z1))
      n.addAll(listOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f))
      p.addAll(listOf(0f, -halfH, 0f, x1, -halfH, z1, x0, -halfH, z0))
      n.addAll(listOf(0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f))
    }
    return MeshBuffer(toFloatBuffer(p.toFloatArray()), toFloatBuffer(n.toFloatArray()), p.size / 3)
  }

  private fun createUnitConeMesh(segments: Int): MeshBuffer {
    val p = mutableListOf<Float>()
    val n = mutableListOf<Float>()
    val halfH = 0.5f
    for (i in 0 until segments) {
      val a0 = (2.0 * Math.PI * i) / segments
      val a1 = (2.0 * Math.PI * (i + 1)) / segments
      val x0 = cos(a0).toFloat()
      val z0 = sin(a0).toFloat()
      val x1 = cos(a1).toFloat()
      val z1 = sin(a1).toFloat()
      val nx = (x0 + x1) * 0.5f
      val nz = (z0 + z1) * 0.5f
      p.addAll(listOf(0f, halfH, 0f, x0, -halfH, z0, x1, -halfH, z1))
      n.addAll(listOf(nx, 0.45f, nz, nx, 0.45f, nz, nx, 0.45f, nz))
      p.addAll(listOf(0f, -halfH, 0f, x1, -halfH, z1, x0, -halfH, z0))
      n.addAll(listOf(0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f))
    }
    return MeshBuffer(toFloatBuffer(p.toFloatArray()), toFloatBuffer(n.toFloatArray()), p.size / 3)
  }

  private fun buildShaderProgram(vsSrc: String, fsSrc: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
    return GLES20.glCreateProgram().also { prog ->
      GLES20.glAttachShader(prog, vs)
      GLES20.glAttachShader(prog, fs)
      GLES20.glLinkProgram(prog)
    }
  }

  private fun compileShader(type: Int, src: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
      GLES20.glShaderSource(shader, src)
      GLES20.glCompileShader(shader)
    }
  }

  companion object {
    private const val VERTEX_SHADER = """
      uniform mat4 uMvpMatrix;
      uniform mat4 uModelMatrix;
      attribute vec4 aPosition;
      attribute vec3 aNormal;
      varying vec3 vWorldNormal;
      void main() {
        gl_Position = uMvpMatrix * aPosition;
        vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
      }
    """

    private const val FRAGMENT_SHADER = """
      precision mediump float;
      uniform vec4 uColor;
      uniform vec3 uLightDir;
      varying vec3 vWorldNormal;
      void main() {
        float diff = max(dot(normalize(vWorldNormal), normalize(uLightDir)), 0.0);
        float light = 0.46 + 0.54 * diff;
        gl_FragColor = vec4(uColor.rgb * light, uColor.a);
      }
    """
  }
}
