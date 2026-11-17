package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.domain.DroneComponentChip
import com.example.domain.DronePartSlot
import com.example.domain.PlacementResult
import com.example.ui.theme.LocalSemanticStatusColors
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuadrotorDiagramExercise(
  promptAr: String,
  slots: List<DronePartSlot>,
  chips: List<DroneComponentChip>,
  placedChipsBySlotId: Map<String, DroneComponentChip>,
  selectedChipForTapId: String?,
  lastFeedback: PlacementResult?,
  reducedMotion: Boolean,
  onSelectChipForTap: (String) -> Unit,
  onPlaceComponent: (slotId: String, chipId: String) -> Unit,
  onRemovePlacedComponent: (slotId: String) -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val statusColors = LocalSemanticStatusColors.current
  val snapRadiusPx = with(density) { 64.dp.toPx() }

  var rootWindowOrigin by remember { mutableStateOf(Offset.Zero) }
  val slotWindowCenters = remember { mutableStateMapOf<String, Offset>() }
  var activeDragChip by remember { mutableStateOf<DroneComponentChip?>(null) }
  var activeDragWindowPos by remember { mutableStateOf(Offset.Zero) }

  val hoveredSlotId = remember(activeDragChip, activeDragWindowPos, slotWindowCenters.size) {
    if (activeDragChip == null) null
    else {
      slotWindowCenters.entries
        .map { (id, center) -> id to hypot(center.x - activeDragWindowPos.x, center.y - activeDragWindowPos.y) }
        .filter { it.second <= snapRadiusPx }
        .minByOrNull { it.second }
        ?.first
    }
  }

  val placedChipIds = remember(placedChipsBySlotId) { placedChipsBySlotId.values.map { it.id }.toSet() }
  val availableChips = remember(chips, placedChipIds) { chips.filterNot { it.id in placedChipIds } }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .onGloballyPositioned { rootWindowOrigin = it.positionInWindow() },
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "التمرين 1: تحديد مكونات المسيّرة (${placedChipsBySlotId.size}/${slots.size})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
          onClick = onReset,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("reset_drag_drop_button"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("إعادة")
        }
      }

      Text(
        text = promptAr,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Technical schematic container: strictly LTR so physical drone geometry is never mirrored in RTL.
      val diagramDescription = stringResource(R.string.cd_quadrotor_diagram)
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = diagramDescription },
        ) {
          BoxWithConstraints(
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(1.08f)
              .padding(12.dp),
          ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val outlineColor = MaterialTheme.colorScheme.outline
            val surfaceColor = MaterialTheme.colorScheme.surface

            Canvas(modifier = Modifier.fillMaxSize()) {
              val w = size.width
              val h = size.height
              val center = Offset(w * 0.5f, h * 0.48f)

              // Reference engineering grid
              val gridStep = w / 8f
              for (i in 1..7) {
                drawLine(
                  color = outlineColor.copy(alpha = 0.14f),
                  start = Offset(gridStep * i, 0f),
                  end = Offset(gridStep * i, h),
                  strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                  color = outlineColor.copy(alpha = 0.14f),
                  start = Offset(0f, gridStep * i),
                  end = Offset(w, gridStep * i),
                  strokeWidth = 1.dp.toPx(),
                )
              }

              // Diagonal arms (top-left, top-right, bottom-left, bottom-right)
              val rotorCenters = listOf(
                Offset(w * 0.21f, h * 0.21f),
                Offset(w * 0.79f, h * 0.21f),
                Offset(w * 0.24f, h * 0.74f),
                Offset(w * 0.76f, h * 0.74f),
              )
              rotorCenters.forEach { rc ->
                drawLine(
                  color = primaryColor.copy(alpha = 0.72f),
                  start = center,
                  end = rc,
                  strokeWidth = 10.dp.toPx(),
                  cap = StrokeCap.Round,
                )
                // Rotor guard ring
                drawCircle(
                  color = secondaryColor.copy(alpha = 0.22f),
                  radius = w * 0.135f,
                  center = rc,
                )
                drawCircle(
                  color = secondaryColor,
                  radius = w * 0.135f,
                  center = rc,
                  style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                  ),
                )
                // Motor hub
                drawCircle(
                  color = primaryColor,
                  radius = w * 0.036f,
                  center = rc,
                )
              }

              // Central fuselage body
              val bodyW = w * 0.26f
              val bodyH = h * 0.34f
              drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(center.x - bodyW / 2f, center.y - bodyH / 2f),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
              )
              drawRoundRect(
                color = primaryColor,
                topLeft = Offset(center.x - bodyW / 2f, center.y - bodyH / 2f),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx()),
              )

              // Forward heading chevron (top)
              drawCircle(
                color = secondaryColor,
                radius = 5.dp.toPx(),
                center = Offset(center.x, center.y - bodyH * 0.5f - 10.dp.toPx()),
              )
            }

            // Drop target nodes positioned with fixed LTR geometry
            val boxW = maxWidth
            val boxH = maxHeight
            slots.forEach { slot ->
              val placed = placedChipsBySlotId[slot.id]
              val isHovered = hoveredSlotId == slot.id
              val canTapPlace = selectedChipForTapId != null && placed == null
              val targetWidth = 104.dp
              val targetHeight = 52.dp
              val xOffset = (boxW * slot.xFraction) - (targetWidth / 2f)
              val yOffset = (boxH * slot.yFraction) - (targetHeight / 2f)

              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = when {
                    placed != null -> statusColors.successContainer
                    isHovered -> MaterialTheme.colorScheme.primaryContainer
                    canTapPlace -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                  },
                  border = BorderStroke(
                    width = if (isHovered || canTapPlace) 2.dp else 1.2.dp,
                    color = when {
                      placed != null -> statusColors.success
                      isHovered -> MaterialTheme.colorScheme.primary
                      canTapPlace -> MaterialTheme.colorScheme.secondary
                      else -> MaterialTheme.colorScheme.outline
                    },
                  ),
                  tonalElevation = if (isHovered) 6.dp else 2.dp,
                  modifier = Modifier
                    .offset(x = xOffset.coerceIn(0.dp, boxW - targetWidth), y = yOffset.coerceIn(0.dp, boxH - targetHeight))
                    .size(width = targetWidth, height = targetHeight)
                    .minimumInteractiveComponentSize()
                    .testTag("drop_slot_${slot.id}")
                    .onGloballyPositioned { coords ->
                      val pos = coords.positionInWindow()
                      slotWindowCenters[slot.id] = Offset(
                        x = pos.x + coords.size.width / 2f,
                        y = pos.y + coords.size.height / 2f,
                      )
                    }
                    .clickable {
                      if (placed != null) {
                        onRemovePlacedComponent(slot.id)
                      } else if (selectedChipForTapId != null) {
                        onPlaceComponent(slot.id, selectedChipForTapId)
                      }
                    },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(horizontal = 6.dp, vertical = 4.dp),
                  ) {
                    if (placed != null) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.cd_correct_badge),
                        tint = statusColors.success,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = placed.labelAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                      )
                    } else {
                      Text(
                        text = slot.titleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Draggable & Tap-selectable Component Chips
      Text(
        text = if (availableChips.isEmpty()) {
          "تم تثبيت جميع المكونات بنجاح!"
        } else if (selectedChipForTapId != null) {
          "اضغط الآن على الموضع المستهدف في المخطط لإسقاط المكوّن المحدد:"
        } else {
          "اسحب البطاقة إلى الموضع المناسب أو اضغط عليها لتحديدها:"
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        availableChips.forEach { chip ->
          val isSelectedForTap = selectedChipForTapId == chip.id
          var chipOriginInWindow by remember(chip.id) { mutableStateOf(Offset.Zero) }
          var chipSize by remember(chip.id) { mutableStateOf(Size.Zero) }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelectedForTap) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
              width = if (isSelectedForTap) 2.dp else 1.dp,
              color = if (isSelectedForTap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
            tonalElevation = 2.dp,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("component_chip_${chip.id}")
              .onGloballyPositioned { coords ->
                chipOriginInWindow = coords.positionInWindow()
                chipSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
              }
              .pointerInput(chip.id) {
                detectDragGestures(
                  onDragStart = { startOffset ->
                    activeDragChip = chip
                    activeDragWindowPos = chipOriginInWindow + startOffset
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    activeDragWindowPos += dragAmount
                  },
                  onDragEnd = {
                    val dragged = activeDragChip
                    val targetSlot = hoveredSlotId
                    activeDragChip = null
                    if (dragged != null && targetSlot != null) {
                      onPlaceComponent(targetSlot, dragged.id)
                    }
                  },
                  onDragCancel = {
                    activeDragChip = null
                  },
                )
              }
              .clickable { onSelectChipForTap(chip.id) },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
              Icon(
                imageVector = if (isSelectedForTap) Icons.Default.TouchApp else Icons.Default.DragIndicator,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = chip.labelAr,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = chip.shortSpecAr,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      // Immediate feedback banner
      val showFeedback = lastFeedback != null
      if (reducedMotion) {
        if (lastFeedback != null) {
          PlacementFeedbackBanner(lastFeedback)
        }
      } else {
        AnimatedVisibility(visible = showFeedback) {
          lastFeedback?.let { PlacementFeedbackBanner(it) }
        }
      }
    }

    // Floating dragged chip preview overlay
    val dragged = activeDragChip
    if (dragged != null) {
      val localOffset = activeDragWindowPos - rootWindowOrigin
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp,
        modifier = Modifier
          .zIndex(10f)
          .offset {
            IntOffset(
              x = (localOffset.x - 70.dp.toPx()).roundToInt(),
              y = (localOffset.y - 24.dp.toPx()).roundToInt(),
            )
          }
          .clip(RoundedCornerShape(14.dp)),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = dragged.labelAr, style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
private fun PlacementFeedbackBanner(feedback: PlacementResult) {
  val statusColors = LocalSemanticStatusColors.current
  val container = if (feedback.isCorrect) statusColors.successContainer else statusColors.errorContainer
  val accent = if (feedback.isCorrect) statusColors.success else statusColors.error

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = container,
    border = BorderStroke(1.dp, accent),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("drag_drop_feedback_banner"),
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.padding(12.dp),
    ) {
      Icon(
        imageVector = if (feedback.isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
        contentDescription = if (feedback.isCorrect) {
          stringResource(R.string.cd_correct_badge)
        } else {
          stringResource(R.string.cd_incorrect_badge)
        },
        tint = accent,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = feedback.messageAr,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
