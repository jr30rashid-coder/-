package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.DisplaySettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsSheet(
  settings: DisplaySettings,
  onUpdateFontScale: (Float) -> Unit,
  onToggleHighContrast: (Boolean) -> Unit,
  onToggleReducedMotion: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.titleLarge,
      )

      Column {
        val percent = (settings.fontScale * 100f).roundToInt()
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.settings_font_scale),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = settings.fontScale,
          onValueChange = onUpdateFontScale,
          valueRange = 0.8f..1.5f,
          steps = 6,
          modifier = Modifier.testTag("font_scale_slider"),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = stringResource(R.string.settings_high_contrast),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = stringResource(R.string.settings_high_contrast_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = settings.highContrast,
          onCheckedChange = onToggleHighContrast,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("high_contrast_switch"),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = stringResource(R.string.settings_reduced_motion),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = stringResource(R.string.settings_reduced_motion_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = settings.reducedMotion,
          onCheckedChange = onToggleReducedMotion,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("reduced_motion_switch"),
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("close_settings_button"),
      ) {
        Text(stringResource(R.string.settings_close))
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
