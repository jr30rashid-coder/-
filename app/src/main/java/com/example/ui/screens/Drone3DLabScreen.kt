package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.DroneComponentId
import com.example.ui.LearningUiState
import com.example.ui.components.PartEducationalBottomSheet
import com.example.ui.components.PartVisualAssets
import com.example.ui.gl.GlRenderMode
import com.example.ui.gl.GlSceneState
import com.example.ui.gl.QuadrotorGlView
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Drone3DLabScreen(
  uiState: LearningUiState,
  onExplodeChange: (Float) -> Unit,
  onZoomChange: (Float) -> Unit,
  onSelectPart: (DroneComponentId?) -> Unit,
  onOpenPartSheet: (DroneComponentId) -> Unit,
  onDismissPartSheet: () -> Unit,
  onToggleVisibility: (DroneComponentId) -> Unit,
  onToggleIsolatePart: (DroneComponentId) -> Unit,
  onShowAllParts: () -> Unit,
  onToggleSpinPropellers: (Boolean) -> Unit,
  onToggleRenderMode: () -> Unit,
  onResetCamera: () -> Unit,
  onResetView: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val animatedExplode by animateFloatAsState(
    targetValue = uiState.glExplodeFactor,
    animationSpec = if (uiState.displaySettings.reducedMotion) snap() else spring(stiffness = Spring.StiffnessMediumLow),
    label = "glExplodeAnim",
  )
  val animatedZoom by animateFloatAsState(
    targetValue = uiState.glZoomFactor,
    animationSpec = if (uiState.displaySettings.reducedMotion) snap() else spring(stiffness = Spring.StiffnessMediumLow),
    label = "glZoomAnim",
  )

  val partSpecs = mapOf(
    DroneComponentId.FRAME to "الهيكل والأذرع: يحمل المنظومة ويحدد التناظر الهندسي والصلابة البنيوية (كتلة هيكلية بالـ kg).",
    DroneComponentId.MOTORS to "المحركات عديمة الفرش: تحوّل الطاقة الكهربائية ثلاثية الطور إلى عزم دوراني عند أطراف الأذرع.",
    DroneComponentId.PROPELLERS to "المراوح المتعاكسة (CW/CCW): تولّد الدفع الصاعد (N) مع إلغاء عزم الانعراج الصافي عند التحويم.",
    DroneComponentId.FLIGHT_CONTROLLER to "وحدة التحكم بالطيران (FC): مركز المعالجة الذي يدمج المستشعرات الافتراضية ويوزّع أوامر الاستقرار.",
    DroneComponentId.ESC to "منظمات السرعة (ESC): تضبط سرعة كل محرك بدقة بناءً على إشارة وحدة التحكم وتتحمل تيار الذروة (A).",
    DroneComponentId.BATTERY to "البطارية (LiPo): تزوّد المنظومة بالجهد (V) والتيار اللازمين وتؤثر كتلتها في مركز الثقل.",
    DroneComponentId.SENSORS to "حزمة المستشعرات الافتراضية: نموذج تعليمي للجيروسكوب ومقياس التسارع ومقياس الضغط الجوي.",
  )

  val primaryFocusParts = listOf(
    Triple(DroneComponentId.MOTORS, "المحركات", Icons.Default.ElectricBolt to "دفع ثلاثي الطور"),
    Triple(DroneComponentId.BATTERY, "البطارية", Icons.Default.BatteryChargingFull to "طاقة LiPo ومركز الثقل"),
    Triple(DroneComponentId.PROPELLERS, "المراوح", Icons.Default.Toys to "توليد الرفع CW/CCW"),
  )

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("drone_3d_lab_list"),
  ) {
    item {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "المختبر ثلاثي الأبعاد (OpenGL ES 2.0)", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "نموذج رباعية تعليمي مبسّط عبر GLSurfaceView — اسحب للتدوير والمس أي جزء لاستكشافه.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedButton(
          onClick = onResetView,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("reset_3d_view_button"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ضبط")
        }
      }
    }

    item {
      // Lock 3D viewport layout direction to LTR so OpenGL coordinate handedness never flips
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.12f)
            .clip(RoundedCornerShape(20.dp))
            .testTag("gl_quadrotor_viewport"),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            QuadrotorGlView(
              sceneState = GlSceneState(
                explodeFactor = animatedExplode,
                zoomFactor = animatedZoom,
                selectedPart = uiState.glSelectedPart,
                hiddenParts = uiState.glHiddenParts,
                spinPropellers = uiState.glSpinPropellers && !uiState.displaySettings.reducedMotion,
                renderMode = uiState.glRenderMode,
                glowPulseEnabled = !uiState.displaySettings.reducedMotion,
              ),
              resetSignal = uiState.glResetSignal,
              onPartTapped = { tapped ->
                if (tapped != null) onSelectPart(tapped) else onSelectPart(null)
              },
              modifier = Modifier.fillMaxSize(),
            )

            // Arabic RTL HUD + Floating Control Dock over the GLSurfaceView
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .align(Alignment.TopCenter)
                  .padding(10.dp),
              ) {
                val activePart = uiState.glSelectedPart
                val haloRgb = activePart?.let { com.example.ui.gl.GlHighlightHaloSpec.haloColorRgb(it) }
                val haloAccent = haloRgb?.let { Color(it.first, it.second, it.third, 1f) }
                  ?: Color(0xFF8BE9FD)
                Surface(
                  onClick = { activePart?.let(onOpenPartSheet) },
                  enabled = activePart != null,
                  color = Color(0xCC0D1B2A),
                  border = if (activePart != null) BorderStroke(1.5.dp, haloAccent) else null,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("gl_highlight_halo_chip"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(
                      imageVector = if (activePart != null) Icons.Default.CenterFocusStrong else Icons.Default.TouchApp,
                      contentDescription = null,
                      tint = haloAccent,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = activePart?.let { "توهج مُختار: ${it.labelAr}" }
                        ?: "المس أي جزء في النموذج 3D",
                      color = Color(0xFFF3F8FF),
                      style = MaterialTheme.typography.labelMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                Surface(
                  color = Color(0xCC12354A),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Text(
                    text = "${uiState.glRenderMode.labelAr} • ${(uiState.glExplodeFactor * 100f).roundToInt()}%",
                    color = Color(0xFF8BE9FD),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                      .padding(horizontal = 10.dp, vertical = 6.dp)
                      .testTag("gl_mode_status_badge"),
                  )
                }
              }

              // Floating Bottom Control Dock over GLSurfaceView
              Surface(
                color = Color(0xDE0B1622),
                border = BorderStroke(1.dp, Color(0x558BE9FD)),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .padding(horizontal = 10.dp, vertical = 10.dp)
                  .testTag("floating_gl_controls_bar"),
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                  val isWireframe = uiState.glRenderMode == GlRenderMode.WIREFRAME
                  FilledTonalButton(
                    onClick = onToggleRenderMode,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                      containerColor = if (isWireframe) Color(0xFF174E6B) else Color(0xFF1A2B3C),
                      contentColor = Color(0xFFF3F8FF),
                    ),
                    modifier = Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("floating_toggle_render_mode_button"),
                  ) {
                    Icon(
                      imageVector = if (isWireframe) Icons.Default.Layers else Icons.Default.GridOn,
                      contentDescription = "تبديل نمط العرض ثلاثي الأبعاد",
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (isWireframe) "عرض ملون" else "هيكل سلكي",
                      maxLines = 1,
                    )
                  }

                  FilledTonalButton(
                    onClick = onResetCamera,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                      containerColor = Color(0xFF1A2B3C),
                      contentColor = Color(0xFFF3F8FF),
                    ),
                    modifier = Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("floating_reset_camera_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.RestartAlt,
                      contentDescription = "إعادة ضبط الكاميرا",
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "ضبط الكاميرا",
                      maxLines = 1,
                    )
                  }

                  if (uiState.glSelectedPart != null) {
                    FilledTonalButton(
                      onClick = { onSelectPart(null) },
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF28394B),
                        contentColor = Color(0xFFFFE082),
                      ),
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("floating_clear_highlight_button"),
                    ) {
                      Text("مسح التوهج", maxLines = 1)
                    }
                  }

                  if (uiState.glHiddenParts.isNotEmpty()) {
                    Button(
                      onClick = onShowAllParts,
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("show_all_3d_parts_button"),
                    ) {
                      Text("إظهار الكل", maxLines = 1)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Highlighted Focus Row for the 3 Primary Subassemblies: Motors, Battery, Propellers
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "الأجزاء الرئيسية المنفصلة (اضغط للاستكشاف المباشر):",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          primaryFocusParts.forEach { (partId, shortTitle, iconAndSub) ->
            val isSelected = uiState.glSelectedPart == partId
            Card(
              onClick = { onSelectPart(partId) },
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
              ),
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("primary_part_card_${partId.name}"),
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 6.dp, vertical = 10.dp),
              ) {
                Icon(
                  imageVector = iconAndSub.first,
                  contentDescription = shortTitle,
                  tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp),
                )
                Text(
                  text = shortTitle,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                )
                Text(
                  text = iconAndSub.second,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
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
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Text(text = "وضعيات التفكيك المعماري:", style = MaterialTheme.typography.titleSmall)
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            FilterChip(
              selected = uiState.glExplodeFactor < 0.1f,
              onClick = { onExplodeChange(0f) },
              label = { Text("مجمّع") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("preset_explode_assembled_button"),
            )
            FilterChip(
              selected = uiState.glExplodeFactor in 0.45f..0.75f,
              onClick = { onExplodeChange(0.60f) },
              label = { Text("منفصل 60%") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("preset_explode_separated_button"),
            )
            FilterChip(
              selected = uiState.glExplodeFactor > 0.85f,
              onClick = { onExplodeChange(1.0f) },
              label = { Text("كامل 100%") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("preset_explode_full_button"),
            )
          }

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "مسافة فصل الأجزاء (Exploded View)", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${(uiState.glExplodeFactor * 100f).roundToInt()}%",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = uiState.glExplodeFactor,
            onValueChange = onExplodeChange,
            valueRange = 0f..1f,
            modifier = Modifier.testTag("gl_explode_slider"),
          )

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "التقريب (Zoom)", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "${"%.1f".format(uiState.glZoomFactor)}x",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = uiState.glZoomFactor,
            onValueChange = onZoomChange,
            valueRange = 0.6f..1.8f,
            modifier = Modifier.testTag("gl_zoom_slider"),
          )

          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "تدوير المراوح في النموذج", style = MaterialTheme.typography.titleSmall)
            Switch(
              checked = uiState.glSpinPropellers,
              onCheckedChange = onToggleSpinPropellers,
            )
          }
        }
      }
    }

    item {
      Text(text = "جميع المكونات (اختر للتمييز أو الإخفاء):", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        DroneComponentId.entries.forEach { part ->
          val isSelected = uiState.glSelectedPart == part
          val isHidden = part in uiState.glHiddenParts
          FilterChip(
            selected = isSelected,
            onClick = { onSelectPart(part) },
            label = { Text(part.labelAr) },
            trailingIcon = {
              Icon(
                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isHidden) "إظهار ${part.labelAr}" else "إخفاء ${part.labelAr}",
                modifier = Modifier
                  .size(18.dp)
                  .testTag("toggle_vis_${part.name}"),
              )
            },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("select_3d_part_${part.name}"),
          )
        }
      }
    }

    item {
      val selected = uiState.glSelectedPart
      if (selected != null) {
        val visual = PartVisualAssets.forComponent(selected)
        val isIsolated = uiState.glHiddenParts.size == DroneComponentId.entries.size - 1 &&
          selected !in uiState.glHiddenParts
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("part_detail_card"),
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
              Text(
                text = selected.labelAr,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onToggleIsolatePart(selected) },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("isolate_selected_part_button"),
                ) {
                  Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isIsolated) "عرض الكل" else "عزل الجزء")
                }
                OutlinedButton(
                  onClick = { onToggleVisibility(selected) },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Text(if (selected in uiState.glHiddenParts) "إظهار" else "إخفاء")
                }
              }
            }
            Image(
              painter = painterResource(visual.drawableRes),
              contentDescription = visual.titleAr,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(12.dp)),
            )
            Text(
              text = visual.titleAr,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = visual.captionAr,
              style = MaterialTheme.typography.bodyMedium,
            )
            Surface(
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "المواصفات الهندسية النموذجية: ${visual.typicalSpecAr}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp),
              )
            }
            Text(
              text = partSpecs[selected].orEmpty(),
              style = MaterialTheme.typography.bodySmall,
            )
            Text(
              text = "نقاط الفحص البصري والصيانة:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
            visual.inspectionPointsAr.forEach { point ->
              Text(text = "• $point", style = MaterialTheme.typography.bodySmall)
            }
            Button(
              onClick = { onOpenPartSheet(selected) },
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("reopen_part_sheet_button"),
            ) {
              Text("فتح اللوحة التعليمية التفصيلية")
            }
          }
        }
      } else {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "اختر «المحركات» أو «البطارية» أو «المراوح» من الأعلى أو المس أي مكوّن داخل نافذة العرض ثلاثية الأبعاد لعرض صورته الحقيقية ومواصفاته.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  val sheetPart = uiState.glSelectedPart
  if (uiState.glPartSheetOpen && sheetPart != null) {
    val isIsolated = uiState.glHiddenParts.size == DroneComponentId.entries.size - 1 &&
      sheetPart !in uiState.glHiddenParts
    PartEducationalBottomSheet(
      part = sheetPart,
      engineeringSummaryAr = partSpecs[sheetPart].orEmpty(),
      isHidden = sheetPart in uiState.glHiddenParts,
      isIsolated = isIsolated,
      onToggleIsolate = { onToggleIsolatePart(sheetPart) },
      onToggleVisibility = { onToggleVisibility(sheetPart) },
      onDismiss = onDismissPartSheet,
    )
  }
}
