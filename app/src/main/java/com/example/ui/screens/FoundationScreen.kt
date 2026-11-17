package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DroneComponentId
import com.example.ui.LearningDestination
import com.example.ui.LearningUiState
import com.example.ui.theme.LocalSemanticStatusColors

@Composable
fun FoundationScreen(
  uiState: LearningUiState,
  onNavigate: (LearningDestination) -> Unit,
  onOpen3DPart: (DroneComponentId) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalSemanticStatusColors.current
  val totalLessons = uiState.tracks.sumOf { it.lessons.size }
  val completedLessons = uiState.completedLessonIds.size
  val progressFraction = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons.toFloat()

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("foundation_list"),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(20.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Image(
              painter = painterResource(R.drawable.ic_drone_foreground_1794885829002),
              contentDescription = stringResource(R.string.cd_app_emblem),
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "أكاديمية عربية متكاملة لعلوم منظومات الطيران المسيّر المدنية",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
              )
            }
          }

          AssistChip(
            onClick = {},
            label = { Text("${uiState.tracks.sumOf { it.lessons.size }} درساً • موسوعة وحاسبات • محاكي 3D/FPV") },
            leadingIcon = {
              Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
            },
          )

          Button(
            onClick = { onNavigate(LearningDestination.ACADEMY) },
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("open_learning_module_button"),
          ) {
            Text("الدخول إلى المدرسة والمسارات (8 مسارات / ${uiState.tracks.sumOf { it.lessons.size }} درساً)")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            OutlinedButton(
              onClick = { onNavigate(LearningDestination.DRONE_3D_LAB) },
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("quick_3d_lab_button"),
            ) {
              Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("مختبر 3D")
            }
            OutlinedButton(
              onClick = { onNavigate(LearningDestination.SIMULATOR_HUB) },
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("quick_sim_button"),
            ) {
              Icon(Icons.Default.FlightTakeoff, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("المحاكي")
            }
          }

          OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("open_accessibility_settings_button"),
          ) {
            Icon(Icons.Default.SettingsAccessibility, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.cta_accessibility_settings))
          }

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "استكشاف الأجزاء المنفصلة في النموذج 3D:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              listOf(
                DroneComponentId.MOTORS to "المحركات",
                DroneComponentId.BATTERY to "البطارية",
                DroneComponentId.PROPELLERS to "المراوح",
              ).forEach { (part, label) ->
                AssistChip(
                  onClick = { onOpen3DPart(part) },
                  label = { Text(label) },
                  modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("home_3d_part_${part.name}"),
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
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(text = "تقدّمك المحلي في المدرسة", style = MaterialTheme.typography.titleMedium)
            Text(
              text = "$completedLessons / $totalLessons درس",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
          )
          Text(
            text = "السيناريوهات التدريبية المنجزة: ${uiState.completedScenarioIds.size} / ${uiState.scenarios.size} • يحفظ محلياً فقط دون حسابات.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("civilian_safety_disclaimer_card"),
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          modifier = Modifier.padding(16.dp),
        ) {
          Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = stringResource(R.string.cd_safety_warning_icon),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = stringResource(R.string.civilian_notice_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
              text = stringResource(R.string.civilian_notice_body),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(14.dp),
        ) {
          Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = statusColors.success,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "يعمل دون إنترنت (0 أذونات شبكة) • كل المحتوى التعليمي واللوائح موسومة بالحالة الحقيقية للمراجعة.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
