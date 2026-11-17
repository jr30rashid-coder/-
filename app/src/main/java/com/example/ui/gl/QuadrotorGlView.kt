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
import com.example.domain.DroneComponentId
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class GlRenderMode(val labelAr: String) {
  SHADED("ملون"),
  WIREFRAME("سلكي"),
}

data class GlSceneState(
  val explodeFactor: Float = 0.60f,
  val zoomFactor: Float = 1f,
  val selectedPart: DroneComponentId? = null,
  val hiddenParts: Set<DroneComponentId> = emptySet(),
  val spinPropellers: Boolean = true,
  val renderMode: GlRenderMode = GlRenderMode.SHADED,
  val glowPulseEnabled: Boolean = true,
  val backgroundRgb: Triple<Float, Float, Float> = Triple(0.05f, 0.09f, 0.14f),
)

data class HaloMetrics(
  val innerScaleMultiplier: Float,
  val outerScaleMultiplier: Float,
  val innerAlpha: Float,
  val outerAlpha: Float,
  val ringScaleBoost: Float,
  val contourLineWidth: Float,
)

object GlHighlightHaloSpec {
  fun haloColorRgb(part: DroneComponentId): Triple<Float, Float, Float> = when (part) {
    DroneComponentId.BATTERY -> Triple(0.24f, 0.90f, 1.00f)
    DroneComponentId.MOTORS -> Triple(1.00f, 0.78f, 0.24f)
    DroneComponentId.PROPELLERS -> Triple(0.25f, 1.00f, 0.84f)
    DroneComponentId.FLIGHT_CONTROLLER -> Triple(0.32f, 0.96f, 0.62f)
    DroneComponentId.ESC -> Triple(1.00f, 0.56f, 0.30f)
    DroneComponentId.SENSORS -> Triple(1.00f, 0.86f, 0.34f)
    DroneComponentId.FRAME -> Triple(0.60f, 0.88f, 1.00f)
  }

  fun computeHaloMetrics(pulse01: Float, burst01: Float): HaloMetrics {
    val p = pulse01.coerceIn(0f, 1f)
    val b = burst01.coerceIn(0f, 1f)
    return HaloMetrics(
      innerScaleMultiplier = 1.09f + 0.04f * p + 0.05f * b,
      outerScaleMultiplier = 1.22f + 0.07f * p + 0.10f * b,
      innerAlpha = (0.36f + 0.16f * p + 0.18f * b).coerceIn(0f, 0.92f),
      outerAlpha = (0.16f + 0.10f * p + 0.12f * b).coerceIn(0f, 0.75f),
      ringScaleBoost = 1.18f + 0.10f * p + 0.16f * b,
      contourLineWidth = 3.2f + 1.0f * p + 1.4f * b,
    )
  }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun QuadrotorGlView(
  sceneState: GlSceneState,
  resetSignal: Int,
  modifier: Modifier = Modifier,
  onPartTapped: ((DroneComponentId?) -> Unit)? = null,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val renderer = remember { QuadrotorGlRenderer() }

  renderer.updateScene(sceneState)

  val glSurfaceViewRef = remember { arrayOfNulls<GLSurfaceView>(1) }

  DisposableEffect(resetSignal) {
    renderer.resetCamera()
    onDispose {}
  }

  DisposableEffect(sceneState.selectedPart) {
    renderer.suggestFocusTiltFor(sceneState.selectedPart)
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
      glSurfaceViewRef[0]?.onPause()
      glSurfaceViewRef[0] = null
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx: Context ->
      GLSurfaceView(ctx).apply {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        var downX = 0f
        var downY = 0f
        var prevX = 0f
        var prevY = 0f
        var totalMovePx = 0f
        var downTimeMs = 0L

        setOnTouchListener { v, event ->
          when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
              downX = event.x
              downY = event.y
              prevX = event.x
              prevY = event.y
              totalMovePx = 0f
              downTimeMs = System.currentTimeMillis()
              renderer.previewTouchAt(event.x, event.y)
              v.parent?.requestDisallowInterceptTouchEvent(true)
              true
            }
            MotionEvent.ACTION_MOVE -> {
              val dx = event.x - prevX
              val dy = event.y - prevY
              prevX = event.x
              prevY = event.y
              totalMovePx += sqrt((dx * dx + dy * dy).toDouble()).toFloat()
              if (totalMovePx > 6f) {
                renderer.clearHoverPreview()
                renderer.orbitBy(dx * 0.45f, dy * 0.45f)
              }
              true
            }
            MotionEvent.ACTION_UP -> {
              v.parent?.requestDisallowInterceptTouchEvent(false)
              renderer.clearHoverPreview()
              val elapsedMs = System.currentTimeMillis() - downTimeMs
              if (totalMovePx < 18f && elapsedMs < 350L && onPartTapped != null) {
                val hit = renderer.pickPartAt(event.x, event.y)
                if (hit != null) {
                  v.post { onPartTapped.invoke(hit) }
                } else if (abs(event.x - downX) < 18f && abs(event.y - downY) < 18f) {
                  v.post { onPartTapped.invoke(null) }
                }
              }
              true
            }
            MotionEvent.ACTION_CANCEL -> {
              renderer.clearHoverPreview()
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
      renderer.updateScene(sceneState)
    },
  )
}

private class QuadrotorGlRenderer : GLSurfaceView.Renderer {

  private data class PartAabb(
    val part: DroneComponentId,
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float,
  )

  private data class HighlightPrimitive(
    val isBox: Boolean,
    val x: Float,
    val y: Float,
    val z: Float,
    val sx: Float,
    val sy: Float,
    val sz: Float,
    val rotYDeg: Float,
  )

  private val pickLock = Any()
  private val invVpMatrixSnapshot = FloatArray(16)
  private var hasValidPickMatrix = false
  private var viewportWidth = 1
  private var viewportHeight = 1
  private var partBoundsSnapshot: List<PartAabb> = emptyList()

  @Volatile private var sceneState = GlSceneState()
  @Volatile private var hoveredPart: DroneComponentId? = null
  @Volatile private var yawDeg = 38f
  @Volatile private var pitchDeg = 24f
  @Volatile private var targetPitchDeg: Float? = null
  private var propAngleDeg = 0f
  private var glowPhaseRad = 0f
  private var selectionBurstSec = 0f
  private var lastHighlightedPart: DroneComponentId? = null
  private var lastFrameMs = System.currentTimeMillis()

  private var programId = 0
  private var posHandle = 0
  private var normHandle = 0
  private var mvpHandle = 0
  private var modelHandle = 0
  private var colorHandle = 0
  private var lightHandle = 0
  private var unlitHandle = 0
  private var eyePosHandle = 0
  private var rimPowerHandle = 0

  private lateinit var boxMesh: MeshBuffer
  private lateinit var cylinderMesh: MeshBuffer
  private lateinit var boxWireMesh: MeshBuffer
  private lateinit var cylinderWireMesh: MeshBuffer
  private lateinit var ringLoopMesh: MeshBuffer
  private lateinit var gridMesh: MeshBuffer
  private var activeWireframeMode = false
  private var activeLineWidth = 2.2f

  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val tmpInvVp = FloatArray(16)
  private val highlightPrimitives = ArrayList<HighlightPrimitive>(24)

  fun updateScene(state: GlSceneState) {
    sceneState = state
  }

  fun previewTouchAt(touchX: Float, touchY: Float) {
    hoveredPart = pickPartAt(touchX, touchY)
  }

  fun clearHoverPreview() {
    hoveredPart = null
  }

  fun orbitBy(deltaYaw: Float, deltaPitch: Float) {
    targetPitchDeg = null
    yawDeg = (yawDeg + deltaYaw) % 360f
    pitchDeg = (pitchDeg + deltaPitch).coerceIn(-80f, 80f)
  }

  fun resetCamera() {
    targetPitchDeg = null
    yawDeg = 38f
    pitchDeg = 24f
  }

  fun suggestFocusTiltFor(part: DroneComponentId?) {
    targetPitchDeg = when (part) {
      DroneComponentId.BATTERY -> -22f
      DroneComponentId.PROPELLERS -> 32f
      DroneComponentId.MOTORS -> 20f
      DroneComponentId.FLIGHT_CONTROLLER, DroneComponentId.SENSORS -> 36f
      DroneComponentId.ESC -> -14f
      else -> null
    }
  }

  fun pickPartAt(touchX: Float, touchY: Float): DroneComponentId? {
    val inv = FloatArray(16)
    val boxes: List<PartAabb>
    val w: Int
    val h: Int
    synchronized(pickLock) {
      if (!hasValidPickMatrix || viewportWidth <= 0 || viewportHeight <= 0) return null
      System.arraycopy(invVpMatrixSnapshot, 0, inv, 0, 16)
      boxes = partBoundsSnapshot
      w = viewportWidth
      h = viewportHeight
    }

    val ndcX = (2f * touchX / w.toFloat()) - 1f
    val ndcY = 1f - (2f * touchY / h.toFloat())
    val nearNdc = floatArrayOf(ndcX, ndcY, -1f, 1f)
    val farNdc = floatArrayOf(ndcX, ndcY, 1f, 1f)
    val nearWorld = FloatArray(4)
    val farWorld = FloatArray(4)

    Matrix.multiplyMV(nearWorld, 0, inv, 0, nearNdc, 0)
    Matrix.multiplyMV(farWorld, 0, inv, 0, farNdc, 0)
    if (abs(nearWorld[3]) < 1e-5f || abs(farWorld[3]) < 1e-5f) return null

    val ox = nearWorld[0] / nearWorld[3]
    val oy = nearWorld[1] / nearWorld[3]
    val oz = nearWorld[2] / nearWorld[3]
    var dx = (farWorld[0] / farWorld[3]) - ox
    var dy = (farWorld[1] / farWorld[3]) - oy
    var dz = (farWorld[2] / farWorld[3]) - oz
    val len = sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    if (len < 1e-5f) return null
    dx /= len
    dy /= len
    dz /= len

    var bestPart: DroneComponentId? = null
    var bestT = Float.POSITIVE_INFINITY
    val pad = 0.09f
    for (box in boxes) {
      val t = intersectRayAabb(
        ox, oy, oz, dx, dy, dz,
        box.minX - pad, box.minY - pad, box.minZ - pad,
        box.maxX + pad, box.maxY + pad, box.maxZ + pad,
      )
      if (t != null && t < bestT) {
        bestT = t
        bestPart = box.part
      }
    }
    return bestPart
  }

  private fun intersectRayAabb(
    ox: Float, oy: Float, oz: Float,
    dx: Float, dy: Float, dz: Float,
    minX: Float, minY: Float, minZ: Float,
    maxX: Float, maxY: Float, maxZ: Float,
  ): Float? {
    var tMin = 0f
    var tMax = 100f

    fun slab(origin: Float, dir: Float, lo: Float, hi: Float): Boolean {
      if (abs(dir) < 1e-6f) {
        return origin in lo..hi
      }
      var t1 = (lo - origin) / dir
      var t2 = (hi - origin) / dir
      if (t1 > t2) {
        val tmp = t1
        t1 = t2
        t2 = tmp
      }
      tMin = max(tMin, t1)
      tMax = min(tMax, t2)
      return tMin <= tMax
    }

    if (!slab(ox, dx, minX, maxX)) return null
    if (!slab(oy, dy, minY, maxY)) return null
    if (!slab(oz, dz, minZ, maxZ)) return null
    return tMin
  }

  override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    programId = buildShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    posHandle = GLES20.glGetAttribLocation(programId, "aPosition")
    normHandle = GLES20.glGetAttribLocation(programId, "aNormal")
    mvpHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
    modelHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix")
    colorHandle = GLES20.glGetUniformLocation(programId, "uColor")
    lightHandle = GLES20.glGetUniformLocation(programId, "uLightDir")
    unlitHandle = GLES20.glGetUniformLocation(programId, "uUnlit")
    eyePosHandle = GLES20.glGetUniformLocation(programId, "uEyePos")
    rimPowerHandle = GLES20.glGetUniformLocation(programId, "uRimPower")

    boxMesh = createUnitBoxMesh()
    cylinderMesh = createUnitCylinderMesh(segments = 20)
    boxWireMesh = createUnitBoxWireframeMesh()
    cylinderWireMesh = createUnitCylinderWireframeMesh(segments = 20)
    ringLoopMesh = createUnitRingLoopMesh(segments = 36)
    gridMesh = createReferenceGridMesh(halfExtent = 3.0f, step = 0.75f, y = -1.48f)
    lastFrameMs = System.currentTimeMillis()
  }

  override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
    viewportWidth = width.coerceAtLeast(1)
    viewportHeight = height.coerceAtLeast(1)
    GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
    Matrix.perspectiveM(projectionMatrix, 0, 42f, aspect, 0.5f, 32f)
  }

  override fun onDrawFrame(unused: GL10?) {
    val state = sceneState
    val nowMs = System.currentTimeMillis()
    val dtSec = ((nowMs - lastFrameMs).coerceIn(0L, 100L)) / 1000f
    lastFrameMs = nowMs
    if (state.spinPropellers) {
      propAngleDeg = (propAngleDeg + dtSec * 360f) % 360f
    }

    val activeHighlight = hoveredPart ?: state.selectedPart
    if (activeHighlight != lastHighlightedPart) {
      lastHighlightedPart = activeHighlight
      selectionBurstSec = if (activeHighlight != null) 0.70f else 0f
    } else if (selectionBurstSec > 0f) {
      selectionBurstSec = (selectionBurstSec - dtSec).coerceAtLeast(0f)
    }

    if (state.glowPulseEnabled) {
      glowPhaseRad = ((glowPhaseRad + dtSec * 4.4f) % (2.0 * Math.PI).toFloat())
    }
    val pulse01 = if (state.glowPulseEnabled) 0.5f + 0.5f * sin(glowPhaseRad.toDouble()).toFloat() else 0.75f
    val burst01 = (selectionBurstSec / 0.70f).coerceIn(0f, 1f)

    targetPitchDeg?.let { target ->
      val diff = target - pitchDeg
      if (abs(diff) < 0.4f) {
        pitchDeg = target
        targetPitchDeg = null
      } else {
        pitchDeg += diff * min(1f, dtSec * 7f)
      }
    }

    activeWireframeMode = state.renderMode == GlRenderMode.WIREFRAME
    val (bgR, bgG, bgB) = if (activeWireframeMode) Triple(0.03f, 0.06f, 0.11f) else state.backgroundRgb
    GLES20.glClearColor(bgR, bgG, bgB, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    val dist = 5.5f / state.zoomFactor.coerceIn(0.6f, 1.8f)
    val yawRad = Math.toRadians(yawDeg.toDouble())
    val pitchRad = Math.toRadians(pitchDeg.toDouble())
    val eyeX = (dist * cos(pitchRad) * sin(yawRad)).toFloat()
    val eyeY = (dist * sin(pitchRad)).toFloat()
    val eyeZ = (dist * cos(pitchRad) * cos(yawRad)).toFloat()

    Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0f, -0.05f, 0f, 0f, 1f, 0f)
    Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    GLES20.glUseProgram(programId)
    GLES20.glUniform3f(lightHandle, 0.52f, 0.86f, 0.48f)
    GLES20.glUniform3f(eyePosHandle, eyeX, eyeY, eyeZ)
    GLES20.glUniform1f(rimPowerHandle, 0f)

    // Subtle ground reference grid
    Matrix.setIdentityM(modelMatrix, 0)
    GLES20.glUniform1f(unlitHandle, 1f)
    drawLinesMesh(gridMesh, floatArrayOf(0.14f, 0.24f, 0.34f, 1f), 1.4f)

    val explode = state.explodeFactor.coerceIn(0f, 1f)
    val armPos = listOf(
      Triple(1.1f, 1.1f, 45f),
      Triple(-1.1f, 1.1f, -45f),
      Triple(-1.1f, -1.1f, 45f),
      Triple(1.1f, -1.1f, -45f),
    )
    val currentBounds = ArrayList<PartAabb>(16)
    highlightPrimitives.clear()

    // 1. Frame & 4 Arms
    if (DroneComponentId.FRAME !in state.hiddenParts) {
      val color = partColor(DroneComponentId.FRAME, activeHighlight, floatArrayOf(0.24f, 0.34f, 0.44f, 1f))
      val record = activeHighlight == DroneComponentId.FRAME
      drawBox(0f, 0f, 0f, 0.84f, 0.13f, 1.16f, 0f, color, record)
      armPos.forEach { (ax, az, rotY) ->
        drawBox(ax * 0.5f, 0f, az * 0.5f, 0.13f, 0.085f, 1.58f, rotY, color, record)
        drawCylinder(ax, 0.01f, az, 0.16f, 0.05f, color, record)
      }
      currentBounds += PartAabb(DroneComponentId.FRAME, -0.45f, -0.09f, -0.60f, 0.45f, 0.09f, 0.60f)
    }

    // 2. Battery (LiPo cell pack + strap + XT60 connector, separates cleanly downward)
    if (DroneComponentId.BATTERY !in state.hiddenParts) {
      val batY = -0.25f - explode * 0.82f
      val color = partColor(DroneComponentId.BATTERY, activeHighlight, floatArrayOf(0.15f, 0.64f, 0.88f, 1f))
      val strapColor = partColor(DroneComponentId.BATTERY, activeHighlight, floatArrayOf(0.14f, 0.20f, 0.27f, 1f))
      val plugColor = partColor(DroneComponentId.BATTERY, activeHighlight, floatArrayOf(0.95f, 0.72f, 0.16f, 1f))
      val record = activeHighlight == DroneComponentId.BATTERY
      drawBox(0f, batY, 0f, 0.58f, 0.24f, 0.94f, 0f, color, record)
      drawBox(0f, batY, 0.12f, 0.61f, 0.26f, 0.10f, 0f, strapColor, record)
      drawBox(0f, batY + 0.15f, -0.34f, 0.14f, 0.09f, 0.16f, 0f, plugColor, record)
      if (explode > 0.05f) {
        drawGuideStem(0f, -0.07f, 0f, 0f, batY + 0.13f, 0f)
      }
      currentBounds += PartAabb(DroneComponentId.BATTERY, -0.32f, batY - 0.15f, -0.49f, 0.32f, batY + 0.20f, 0.49f)
    }

    // 3. Flight Controller PCB (center top, separates upward)
    if (DroneComponentId.FLIGHT_CONTROLLER !in state.hiddenParts) {
      val fcY = 0.13f + explode * 0.48f
      val color = partColor(DroneComponentId.FLIGHT_CONTROLLER, activeHighlight, floatArrayOf(0.12f, 0.74f, 0.56f, 1f))
      val chipColor = partColor(DroneComponentId.FLIGHT_CONTROLLER, activeHighlight, floatArrayOf(0.16f, 0.22f, 0.28f, 1f))
      val record = activeHighlight == DroneComponentId.FLIGHT_CONTROLLER
      drawBox(0f, fcY, 0f, 0.46f, 0.06f, 0.46f, 0f, color, record)
      drawBox(0f, fcY + 0.04f, 0f, 0.16f, 0.03f, 0.16f, 0f, chipColor, record)
      if (explode > 0.05f) {
        drawGuideStem(0f, 0.07f, 0f, 0f, fcY - 0.03f, 0f)
      }
      currentBounds += PartAabb(DroneComponentId.FLIGHT_CONTROLLER, -0.25f, fcY - 0.05f, -0.25f, 0.25f, fcY + 0.07f, 0.25f)
    }

    // 4. Virtual Sensors Dome (GNSS puck + mast)
    if (DroneComponentId.SENSORS !in state.hiddenParts) {
      val sensY = 0.27f + explode * 0.96f
      val color = partColor(DroneComponentId.SENSORS, activeHighlight, floatArrayOf(0.95f, 0.66f, 0.18f, 1f))
      val record = activeHighlight == DroneComponentId.SENSORS
      drawCylinder(0f, sensY, 0.12f, 0.17f, 0.12f, color, record)
      drawCylinder(0f, sensY - 0.08f, 0.12f, 0.035f, 0.10f, color, record)
      if (explode > 0.05f) {
        drawGuideStem(0f, 0.15f + explode * 0.48f, 0.12f, 0f, sensY - 0.12f, 0.12f)
      }
      currentBounds += PartAabb(DroneComponentId.SENSORS, -0.18f, sensY - 0.13f, -0.06f, 0.18f, sensY + 0.08f, 0.30f)
    }

    // 5. ESC Modules on arms
    if (DroneComponentId.ESC !in state.hiddenParts) {
      val escY = -0.09f - explode * 0.38f
      val color = partColor(DroneComponentId.ESC, activeHighlight, floatArrayOf(0.84f, 0.38f, 0.28f, 1f))
      val record = activeHighlight == DroneComponentId.ESC
      armPos.forEach { (ax, az, rotY) ->
        val ex = ax * 0.52f
        val ez = az * 0.52f
        drawBox(ex, escY, ez, 0.18f, 0.06f, 0.30f, rotY, color, record)
        if (explode > 0.05f) {
          drawGuideStem(ex, -0.04f, ez, ex, escY + 0.03f, ez)
        }
        currentBounds += PartAabb(DroneComponentId.ESC, ex - 0.16f, escY - 0.05f, ez - 0.16f, ex + 0.16f, escY + 0.05f, ez + 0.16f)
      }
    }

    // 6. Brushless Motors (base flange + copper stator + rotor bell + shaft, radially & vertically separated)
    if (DroneComponentId.MOTORS !in state.hiddenParts) {
      val bellColor = partColor(DroneComponentId.MOTORS, activeHighlight, floatArrayOf(0.78f, 0.84f, 0.90f, 1f))
      val copperColor = partColor(DroneComponentId.MOTORS, activeHighlight, floatArrayOf(0.86f, 0.48f, 0.22f, 1f))
      val record = activeHighlight == DroneComponentId.MOTORS
      val radialShift = 1f + explode * 0.18f
      val motY = 0.13f + explode * 0.34f
      armPos.forEach { (ax, az, _) ->
        val mx = ax * radialShift
        val mz = az * radialShift
        drawCylinder(mx, motY - 0.05f, mz, 0.155f, 0.06f, copperColor, record)
        drawCylinder(mx, motY + 0.03f, mz, 0.16f, 0.12f, bellColor, record)
        drawCylinder(mx, motY + 0.12f, mz, 0.035f, 0.08f, bellColor, record)
        if (explode > 0.05f) {
          drawGuideStem(ax, 0.03f, az, mx, motY - 0.08f, mz)
        }
        currentBounds += PartAabb(DroneComponentId.MOTORS, mx - 0.18f, motY - 0.09f, mz - 0.18f, mx + 0.18f, motY + 0.16f, mz + 0.18f)
      }
    }

    // 7. Spinning Propellers (hub cone + CW/CCW dual blades, separated cleanly above motors)
    if (DroneComponentId.PROPELLERS !in state.hiddenParts) {
      val bladeColor = partColor(DroneComponentId.PROPELLERS, activeHighlight, floatArrayOf(0.20f, 0.88f, 0.80f, 1f))
      val hubColor = partColor(DroneComponentId.PROPELLERS, activeHighlight, floatArrayOf(0.92f, 0.95f, 0.98f, 1f))
      val record = activeHighlight == DroneComponentId.PROPELLERS
      val radialShift = 1f + explode * 0.18f
      val propY = 0.31f + explode * 0.94f
      val motTopY = 0.25f + explode * 0.34f
      armPos.forEachIndexed { idx, (ax, az, _) ->
        val px = ax * radialShift
        val pz = az * radialShift
        val dir = if (idx % 2 == 0) 1f else -1f
        val rot = propAngleDeg * dir + idx * 35f
        drawCylinder(px, propY + 0.015f, pz, 0.065f, 0.055f, hubColor, record)
        drawBox(px, propY, pz, 0.92f, 0.025f, 0.11f, rot, bladeColor, record)
        drawBox(px, propY, pz, 0.11f, 0.025f, 0.92f, rot, bladeColor, record)
        if (explode > 0.05f) {
          drawGuideStem(px, motTopY, pz, px, propY - 0.02f, pz)
        }
        currentBounds += PartAabb(DroneComponentId.PROPELLERS, px - 0.46f, propY - 0.04f, pz - 0.46f, px + 0.46f, propY + 0.06f, pz + 0.46f)
      }
    }

    // Selection Glow Halo Pass (additive volumetric Fresnel aura + contour + orbital beacon rings)
    if (activeHighlight != null && activeHighlight !in state.hiddenParts && highlightPrimitives.isNotEmpty()) {
      drawSelectionGlowHaloPass(
        part = activeHighlight,
        bounds = currentBounds.filter { it.part == activeHighlight },
        pulse01 = pulse01,
        burst01 = burst01,
      )
    }

    val invertedOk = Matrix.invertM(tmpInvVp, 0, vpMatrix, 0)
    synchronized(pickLock) {
      partBoundsSnapshot = currentBounds
      if (invertedOk) {
        System.arraycopy(tmpInvVp, 0, invVpMatrixSnapshot, 0, 16)
        hasValidPickMatrix = true
      } else {
        hasValidPickMatrix = false
      }
    }
  }

  private fun drawSelectionGlowHaloPass(
    part: DroneComponentId,
    bounds: List<PartAabb>,
    pulse01: Float,
    burst01: Float,
  ) {
    val (hr, hg, hb) = GlHighlightHaloSpec.haloColorRgb(part)
    val metrics = GlHighlightHaloSpec.computeHaloMetrics(pulse01, burst01)

    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
    GLES20.glDepthMask(false)

    val outerColor = floatArrayOf(hr, hg, hb, metrics.outerAlpha)
    val innerColor = floatArrayOf(min(1f, hr + 0.15f), min(1f, hg + 0.15f), min(1f, hb + 0.15f), metrics.innerAlpha)
    val contourColor = floatArrayOf(1f, 0.96f, 0.72f, (0.72f + 0.25f * pulse01).coerceIn(0f, 1f))
    val ringColor = floatArrayOf(hr, hg, hb, (0.65f + 0.30f * pulse01).coerceIn(0f, 1f))

    // Volumetric outer + inner Fresnel glow shells on every primitive of the selected part
    GLES20.glUniform1f(unlitHandle, 1f)
    for (prim in highlightPrimitives) {
      drawHaloShell(prim, metrics.outerScaleMultiplier, outerColor, rimPower = 1.8f)
      drawHaloShell(prim, metrics.innerScaleMultiplier, innerColor, rimPower = 1.35f)
    }

    // Crisp luminous wireframe contour overlay
    GLES20.glUniform1f(rimPowerHandle, 0f)
    for (prim in highlightPrimitives) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, prim.x, prim.y, prim.z)
      if (prim.rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, prim.rotYDeg, 0f, 1f, 0f)
      val cScale = 1.035f + 0.015f * pulse01
      Matrix.scaleM(modelMatrix, 0, prim.sx * cScale, prim.sy * cScale, prim.sz * cScale)
      drawLinesMesh(if (prim.isBox) boxWireMesh else cylinderWireMesh, contourColor, metrics.contourLineWidth)
    }

    // Luminous orbital focus rings framing each selected assembly bounding box
    bounds.forEach { box ->
      val cx = (box.minX + box.maxX) * 0.5f
      val cy = (box.minY + box.maxY) * 0.5f
      val cz = (box.minZ + box.maxZ) * 0.5f
      val radius = max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.55f * metrics.ringScaleBoost
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, cx, cy, cz)
      Matrix.scaleM(modelMatrix, 0, radius, 1f, radius)
      drawLineLoopMesh(ringLoopMesh, ringColor, 2.8f + pulse01 * 1.2f)
    }

    GLES20.glUniform1f(rimPowerHandle, 0f)
    GLES20.glDepthMask(true)
    GLES20.glDisable(GLES20.GL_BLEND)
  }

  private fun drawHaloShell(
    prim: HighlightPrimitive,
    scaleMul: Float,
    color: FloatArray,
    rimPower: Float,
  ) {
    val sx = prim.sx * scaleMul + (scaleMul - 1f) * 0.08f
    val sy = prim.sy * scaleMul + (scaleMul - 1f) * 0.08f
    val sz = prim.sz * scaleMul + (scaleMul - 1f) * 0.08f
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, prim.x, prim.y, prim.z)
    if (prim.rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, prim.rotYDeg, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

    val mesh = if (prim.isBox) boxMesh else cylinderMesh
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniform1f(rimPowerHandle, rimPower)
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

  private fun partColor(
    part: DroneComponentId,
    selected: DroneComponentId?,
    base: FloatArray,
  ): FloatArray {
    activeLineWidth = if (selected == part) 3.4f else 2.2f
    val wireBoost = if (activeWireframeMode) 1.18f else 1.0f
    return when {
      selected == null -> floatArrayOf(
        min(1f, base[0] * wireBoost),
        min(1f, base[1] * wireBoost),
        min(1f, base[2] * wireBoost),
        1f,
      )
      selected == part -> {
        val (hr, hg, hb) = GlHighlightHaloSpec.haloColorRgb(part)
        floatArrayOf(
          min(1f, base[0] * 0.65f + hr * 0.45f),
          min(1f, base[1] * 0.65f + hg * 0.45f),
          min(1f, base[2] * 0.65f + hb * 0.45f),
          1f,
        )
      }
      else -> floatArrayOf(base[0] * 0.54f, base[1] * 0.54f, base[2] * 0.54f, 1f)
    }
  }

  private fun drawGuideStem(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
    val lineData = floatArrayOf(x0, y0, z0, x1, y1, z1)
    val normData = floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f)
    val pBuf = toFloatBuffer(lineData)
    val nBuf = toFloatBuffer(normData)
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniform1f(unlitHandle, 1f)
    GLES20.glUniform1f(rimPowerHandle, 0f)
    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4fv(colorHandle, 1, floatArrayOf(0.45f, 0.68f, 0.86f, 1f), 0)
    GLES20.glEnableVertexAttribArray(posHandle)
    GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, pBuf)
    GLES20.glEnableVertexAttribArray(normHandle)
    GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, 0, nBuf)
    GLES20.glLineWidth(2f)
    GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
    GLES20.glDisableVertexAttribArray(posHandle)
    GLES20.glDisableVertexAttribArray(normHandle)
  }

  private fun drawLinesMesh(mesh: MeshBuffer, color: FloatArray, lineWidth: Float = activeLineWidth) {
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniform1f(unlitHandle, 1f)
    GLES20.glUniform1f(rimPowerHandle, 0f)
    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4fv(colorHandle, 1, color, 0)
    GLES20.glEnableVertexAttribArray(posHandle)
    GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.positions)
    GLES20.glEnableVertexAttribArray(normHandle)
    GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.normals)
    GLES20.glLineWidth(lineWidth)
    GLES20.glDrawArrays(GLES20.GL_LINES, 0, mesh.vertexCount)
    GLES20.glDisableVertexAttribArray(posHandle)
    GLES20.glDisableVertexAttribArray(normHandle)
  }

  private fun drawLineLoopMesh(mesh: MeshBuffer, color: FloatArray, lineWidth: Float) {
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniform1f(unlitHandle, 1f)
    GLES20.glUniform1f(rimPowerHandle, 0f)
    GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4fv(colorHandle, 1, color, 0)
    GLES20.glEnableVertexAttribArray(posHandle)
    GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.positions)
    GLES20.glEnableVertexAttribArray(normHandle)
    GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, 0, mesh.normals)
    GLES20.glLineWidth(lineWidth)
    GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, mesh.vertexCount)
    GLES20.glDisableVertexAttribArray(posHandle)
    GLES20.glDisableVertexAttribArray(normHandle)
  }

  private fun drawBox(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    rotYDeg: Float,
    color: FloatArray,
    recordForHalo: Boolean = false,
  ) {
    if (recordForHalo) {
      highlightPrimitives += HighlightPrimitive(true, x, y, z, sx, sy, sz, rotYDeg)
    }
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (rotYDeg != 0f) Matrix.rotateM(modelMatrix, 0, rotYDeg, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    if (activeWireframeMode) {
      drawLinesMesh(boxWireMesh, color)
    } else {
      drawMesh(boxMesh, color)
    }
  }

  private fun drawCylinder(
    x: Float,
    y: Float,
    z: Float,
    radius: Float,
    height: Float,
    color: FloatArray,
    recordForHalo: Boolean = false,
  ) {
    if (recordForHalo) {
      highlightPrimitives += HighlightPrimitive(false, x, y, z, radius, height, radius, 0f)
    }
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, radius, height, radius)
    if (activeWireframeMode) {
      drawLinesMesh(cylinderWireMesh, color)
    } else {
      drawMesh(cylinderMesh, color)
    }
  }

  private fun drawMesh(mesh: MeshBuffer, color: FloatArray) {
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniform1f(unlitHandle, 0f)
    GLES20.glUniform1f(rimPowerHandle, 0f)
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

  private fun createUnitRingLoopMesh(segments: Int): MeshBuffer {
    val p = FloatArray(segments * 3)
    val n = FloatArray(segments * 3)
    for (i in 0 until segments) {
      val a = (2.0 * Math.PI * i) / segments
      p[i * 3] = cos(a).toFloat()
      p[i * 3 + 1] = 0f
      p[i * 3 + 2] = sin(a).toFloat()
      n[i * 3] = 0f
      n[i * 3 + 1] = 1f
      n[i * 3 + 2] = 0f
    }
    return MeshBuffer(toFloatBuffer(p), toFloatBuffer(n), segments)
  }

  private fun createReferenceGridMesh(halfExtent: Float, step: Float, y: Float): MeshBuffer {
    val p = mutableListOf<Float>()
    val n = mutableListOf<Float>()
    var v = -halfExtent
    while (v <= halfExtent + 0.001f) {
      p.addAll(listOf(-halfExtent, y, v, halfExtent, y, v))
      p.addAll(listOf(v, y, -halfExtent, v, y, halfExtent))
      repeat(4) { n.addAll(listOf(0f, 1f, 0f)) }
      v += step
    }
    return MeshBuffer(toFloatBuffer(p.toFloatArray()), toFloatBuffer(n.toFloatArray()), p.size / 3)
  }

  private fun createUnitBoxMesh(): MeshBuffer {
    val p = mutableListOf<Float>()
    val n = mutableListOf<Float>()
    fun addQuad(
      v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray,
      nx: Float, ny: Float, nz: Float,
    ) {
      val tris = listOf(v0, v1, v2, v0, v2, v3)
      tris.forEach { v ->
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

  private fun createUnitBoxWireframeMesh(): MeshBuffer {
    val h = 0.5f
    val edges = floatArrayOf(
      -h, -h, -h,  h, -h, -h,
       h, -h, -h,  h, -h,  h,
       h, -h,  h, -h, -h,  h,
      -h, -h,  h, -h, -h, -h,
      -h,  h, -h,  h,  h, -h,
       h,  h, -h,  h,  h,  h,
       h,  h,  h, -h,  h,  h,
      -h,  h,  h, -h,  h, -h,
      -h, -h, -h, -h,  h, -h,
       h, -h, -h,  h,  h, -h,
       h, -h,  h,  h,  h,  h,
      -h, -h,  h, -h,  h,  h,
    )
    val norms = FloatArray(edges.size) { if (it % 3 == 1) 1f else 0f }
    return MeshBuffer(toFloatBuffer(edges), toFloatBuffer(norms), edges.size / 3)
  }

  private fun createUnitCylinderWireframeMesh(segments: Int): MeshBuffer {
    val p = mutableListOf<Float>()
    val halfH = 0.5f
    for (i in 0 until segments) {
      val a0 = (2.0 * Math.PI * i) / segments
      val a1 = (2.0 * Math.PI * (i + 1)) / segments
      val x0 = cos(a0).toFloat()
      val z0 = sin(a0).toFloat()
      val x1 = cos(a1).toFloat()
      val z1 = sin(a1).toFloat()
      p.addAll(listOf(x0, halfH, z0, x1, halfH, z1))
      p.addAll(listOf(x0, -halfH, z0, x1, -halfH, z1))
      if (i % 2 == 0) {
        p.addAll(listOf(x0, -halfH, z0, x0, halfH, z0))
      }
    }
    val norms = FloatArray(p.size) { if (it % 3 == 1) 1f else 0f }
    return MeshBuffer(toFloatBuffer(p.toFloatArray()), toFloatBuffer(norms), p.size / 3)
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
      varying vec3 vWorldPos;
      void main() {
        gl_Position = uMvpMatrix * aPosition;
        vec4 wp = uModelMatrix * aPosition;
        vWorldPos = wp.xyz;
        vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
      }
    """

    private const val FRAGMENT_SHADER = """
      precision mediump float;
      uniform vec4 uColor;
      uniform vec3 uLightDir;
      uniform float uUnlit;
      uniform vec3 uEyePos;
      uniform float uRimPower;
      varying vec3 vWorldNormal;
      varying vec3 vWorldPos;
      void main() {
        vec3 n = normalize(vWorldNormal);
        if (uRimPower > 0.0) {
          vec3 v = normalize(uEyePos - vWorldPos);
          float rim = pow(clamp(1.0 - abs(dot(n, v)), 0.0, 1.0), uRimPower);
          float alpha = clamp(uColor.a * (0.28 + 0.72 * rim), 0.0, 1.0);
          gl_FragColor = vec4(uColor.rgb * (0.70 + 0.85 * rim), alpha);
        } else {
          float diff = max(dot(n, normalize(uLightDir)), 0.0);
          float shaded = 0.42 + 0.58 * diff;
          float light = mix(shaded, 1.0, uUnlit);
          gl_FragColor = vec4(uColor.rgb * light, uColor.a);
        }
      }
    """
  }
}
