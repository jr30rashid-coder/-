package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.RegulationsAndWorkshopData
import com.example.domain.SafetyWorkshopAndLogs
import com.example.ui.GovernanceTab
import com.example.ui.LearningUiState
import com.example.ui.components.PartVisualAssets
import com.example.ui.theme.LocalSemanticStatusColors

@Composable
fun WorkshopAndGovernanceScreen(
  uiState: LearningUiState,
  onSelectTab: (GovernanceTab) -> Unit,
  onSelectWorkshopParts: (motorId: String?, escId: String?, batteryId: String?) -> Unit,
  onRunDiagnostic: (SafetyWorkshopAndLogs.DiagnosticEvidenceInput) -> Unit,
  onFilterRegulations: (String?) -> Unit,
  onExportBackupJson: () -> String,
  onImportBackupBytes: (ByteArray) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    TabRow(selectedTabIndex = uiState.governanceTab.ordinal) {
      GovernanceTab.entries.forEach { tab ->
        Tab(
          selected = uiState.governanceTab == tab,
          onClick = { onSelectTab(tab) },
          text = { Text(tab.titleAr) },
          modifier = Modifier.testTag("gov_tab_${tab.name}"),
        )
      }
    }

    when (uiState.governanceTab) {
      GovernanceTab.WORKSHOP -> WorkshopContent(
        uiState = uiState,
        onSelectWorkshopParts = onSelectWorkshopParts,
        onRunDiagnostic = onRunDiagnostic,
      )

      GovernanceTab.REGULATIONS -> RegulationsContent(
        uiState = uiState,
        onFilterRegulations = onFilterRegulations,
      )

      GovernanceTab.BACKUP_AND_LIMITS -> BackupAndLimitsContent(
        uiState = uiState,
        onExportBackupJson = onExportBackupJson,
        onImportBackupBytes = onImportBackupBytes,
      )
    }
  }
}

@Composable
private fun WorkshopContent(
  uiState: LearningUiState,
  onSelectWorkshopParts: (motorId: String?, escId: String?, batteryId: String?) -> Unit,
  onRunDiagnostic: (SafetyWorkshopAndLogs.DiagnosticEvidenceInput) -> Unit,
) {
  val statusColors = LocalSemanticStatusColors.current
  val motors = RegulationsAndWorkshopData.getMotors()
  val escs = RegulationsAndWorkshopData.getEscs()
  val batteries = RegulationsAndWorkshopData.getBatteries()

  val selectedMotor = motors.first { it.id == uiState.selectedMotorId }
  val selectedEsc = escs.first { it.id == uiState.selectedEscId }
  val selectedBattery = batteries.first { it.id == uiState.selectedBatteryId }

  val report = SafetyWorkshopAndLogs.checkBuildCompatibility(
    frameAndAvionicsMassKg = 0.42f,
    motor = selectedMotor,
    esc = selectedEsc,
    battery = selectedBattery,
  )

  var propInspected by remember { mutableStateOf(true) }
  var motorBalanced by remember { mutableStateOf(true) }
  var lowVoltageSample by remember { mutableStateOf(false) }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("workshop_list"),
  ) {
    item {
      Text(text = "الورشة التعليمية وحاسبة التوافق", style = MaterialTheme.typography.titleLarge)
      Text(
        text = "تحقق من توافق الجهد (S) والتيار (A) ونسبة الدفع إلى الوزن (TWR) دون الارتباط بأي عتاد حقيقي.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      var expandedAssetTitle by remember { mutableStateOf<String?>(null) }
      Text(text = "دليل القطع والمواد المصوّر (7 مكونات حقيقية):", style = MaterialTheme.typography.titleSmall)
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        items(PartVisualAssets.workshopGallery()) { asset ->
          val isExpanded = expandedAssetTitle == asset.titleAr
          Card(
            onClick = { expandedAssetTitle = if (isExpanded) null else asset.titleAr },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
              1.dp,
              if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.width(230.dp),
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(10.dp),
            ) {
              Image(
                painter = painterResource(asset.drawableRes),
                contentDescription = asset.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxWidth()
                  .aspectRatio(4f / 3f)
                  .clip(RoundedCornerShape(10.dp)),
              )
              Text(text = asset.titleAr, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Text(
                text = asset.typicalSpecAr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = asset.captionAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 8 else 3,
              )
              if (isExpanded) {
                asset.inspectionPointsAr.forEach { pt ->
                  Text(text = "• $pt", style = MaterialTheme.typography.bodySmall)
                }
              } else {
                Text(
                  text = "اضغط لعرض نقاط الفحص الهندسي",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
            }
          }
        }
      }
    }

    item {
      Text(text = "1. اختر المحركات:", style = MaterialTheme.typography.titleSmall)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
      ) {
        motors.forEach { m ->
          FilterChip(
            selected = m.id == uiState.selectedMotorId,
            onClick = { onSelectWorkshopParts(m.id, null, null) },
            label = { Text(m.nameAr) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = "2. اختر منظم السرعة (ESC):", style = MaterialTheme.typography.titleSmall)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
      ) {
        escs.forEach { e ->
          FilterChip(
            selected = e.id == uiState.selectedEscId,
            onClick = { onSelectWorkshopParts(null, e.id, null) },
            label = { Text(e.nameAr) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = "3. اختر البطارية:", style = MaterialTheme.typography.titleSmall)
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
      ) {
        batteries.forEach { b ->
          FilterChip(
            selected = b.id == uiState.selectedBatteryId,
            onClick = { onSelectWorkshopParts(null, null, b.id) },
            label = { Text(b.nameAr) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (report.isCompatible) statusColors.successContainer else statusColors.errorContainer,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("compatibility_report_card"),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (report.isCompatible) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
              contentDescription = null,
              tint = if (report.isCompatible) statusColors.success else statusColors.error,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (report.isCompatible) "التكوين الافتراضي متوافق" else "تحذير: تعارض هندسي في التكوين",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
          }
          Text(
            text = "الكتلة الكلية: ${"%.2f".format(report.totalMassKg)} kg • أقصى دفع: ${"%.1f".format(report.totalMaxThrustN)} N • نسبة TWR: ${"%.2f".format(report.thrustToWeightRatio)}",
            style = MaterialTheme.typography.bodySmall,
          )
          report.findingsAr.forEach { f ->
            Text(text = "• $f", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }

    // Evidence-based virtual diagnostic tool
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "المُشخّص الافتراضي المبني على الأدلة", style = MaterialTheme.typography.titleSmall)
          }
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("تم فحص المراوح والأذرع", style = MaterialTheme.typography.bodySmall)
            Switch(checked = propInspected, onCheckedChange = { propInspected = it })
          }
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("توازن دوران المحركات سليم", style = MaterialTheme.typography.bodySmall)
            Switch(checked = motorBalanced, onCheckedChange = { motorBalanced = it })
          }
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("محاكاة قراءة جهد منخفض (12.9 V)", style = MaterialTheme.typography.bodySmall)
            Switch(checked = lowVoltageSample, onCheckedChange = { lowVoltageSample = it })
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = {
                onRunDiagnostic(
                  SafetyWorkshopAndLogs.DiagnosticEvidenceInput(
                    symptomAr = "اهتزاز أو ضعف دفع افتراضي",
                    batteryVoltageMeasuredV = if (lowVoltageSample) 12.9f else 15.6f,
                    propInspectionDone = propInspected,
                    motorSpinBalanced = motorBalanced,
                  ),
                )
              },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("run_diagnostic_button"),
            ) {
              Text("تحليل الأدلة")
            }
            OutlinedButton(
              onClick = {
                onRunDiagnostic(
                  SafetyWorkshopAndLogs.DiagnosticEvidenceInput(
                    symptomAr = "عَرَض غير مكتمل المعطيات",
                    batteryVoltageMeasuredV = null,
                    propInspectionDone = false,
                    motorSpinBalanced = null,
                  ),
                )
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("run_insufficient_diagnostic_button"),
            ) {
              Text("أدلة ناقصة")
            }
          }

          uiState.diagnosticResult?.let { verdict ->
            Surface(
              color = if (verdict.conclusive) MaterialTheme.colorScheme.secondaryContainer else statusColors.errorContainer,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("diagnostic_verdict_card"),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(text = verdict.headlineAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = verdict.detailsAr, style = MaterialTheme.typography.bodySmall)
                Text(text = verdict.disclaimerAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun RegulationsContent(
  uiState: LearningUiState,
  onFilterRegulations: (String?) -> Unit,
) {
  val rules = RegulationsAndWorkshopData.getRegulatoryRules()
  val countries = rules.map { it.countryAr }.distinct()
  val filtered = if (uiState.regulatoryCountryFilter == null) rules else rules.filter { it.countryAr == uiState.regulatoryCountryFilter }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    item {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          modifier = Modifier.padding(12.dp),
        ) {
          Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "تنبيه الحوكمة: المواد التنظيمية أدناه غير معتمدة قانونياً وتُعرض بحالة «يتطلب مراجعة بشرية» أو «مسودة» مع تحذير تقادم واختلاف الدول.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
    }

    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
      ) {
        FilterChip(
          selected = uiState.regulatoryCountryFilter == null,
          onClick = { onFilterRegulations(null) },
          label = { Text("جميع الجهات") },
          modifier = Modifier.minimumInteractiveComponentSize(),
        )
        countries.forEach { c ->
          FilterChip(
            selected = uiState.regulatoryCountryFilter == c,
            onClick = { onFilterRegulations(c) },
            label = { Text(c) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
    }

    items(filtered, key = { it.id }) { rule ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "${rule.reviewStatus.labelAr} • غير معتمد قانونياً",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Text(text = "${rule.countryAr} — ${rule.authorityAr}", style = MaterialTheme.typography.titleMedium)
          Text(text = "الإصدار: ${rule.versionLabel} • السريان: ${rule.effectiveDate}", style = MaterialTheme.typography.bodySmall)
          Text(text = "آخر مراجعة مسجلة: ${rule.lastReviewedDate}", style = MaterialTheme.typography.bodySmall)
          Text(text = rule.summaryAr, style = MaterialTheme.typography.bodyMedium)
          HorizontalDivider()
          Text(
            text = "${rule.officialReferenceLabelAr}. ${rule.stalenessWarningAr}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@Composable
private fun BackupAndLimitsContent(
  uiState: LearningUiState,
  onExportBackupJson: () -> String,
  onImportBackupBytes: (ByteArray) -> Unit,
) {
  val context = LocalContext.current

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri: Uri? ->
    if (uri != null) {
      val json = onExportBackupJson()
      context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
      if (bytes != null) onImportBackupBytes(bytes)
    }
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
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
          Text(text = "النسخ الاحتياطي المحلي للتقدم (بدون أذونات تخزين)", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "تُحفظ أقل بيانات لازمة في مساحة التطبيق الخاصة مع بصمة SHA-256 ورفض النسخ التالفة أو غير المتوافقة بالإصدار.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = { exportLauncher.launch("drones3d_progress_backup.json") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("export_backup_button"),
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("تصدير نسخة")
            }
            OutlinedButton(
              onClick = { importLauncher.launch(arrayOf("application/json")) },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("import_backup_button"),
            ) {
              Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("استعادة نسخة")
            }
          }
          uiState.lastBackupStatusAr?.let { status ->
            Text(text = status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "الخصوصية وعدم الاتصال والتراخيص", style = MaterialTheme.typography.titleSmall)
          }
          Text(
            text = "• الشيفرة: GPL-3.0 | المحتوى التعليمي: CC BY-SA 4.0\n• معرّف التطبيق: ${BuildConfig.APPLICATION_ID}\n• الإصدار: ${BuildConfig.VERSION_NAME}\n• أذونات الشبكة والعتاد: 0 (لا يوجد INTERNET أو Firebase أو إعلانات).",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "المرحلة 14 — دراسة مستقبلية متقدمة (غير منفّذة تلقائياً)", style = MaterialTheme.typography.titleSmall)
          }
          Text(
            text = "أي توسيع مستقبلي نحو محاكاة ديناميكيات هوائية متقدمة (CFD) أو نماذج ثلاثية أبعاد عالية التفصيل أو حزم صوتية محلية يخضع لدراسة مستقلة للتراخيص والعمل دون إنترنت واستهلاك الذاكرة وموافقة صريحة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
