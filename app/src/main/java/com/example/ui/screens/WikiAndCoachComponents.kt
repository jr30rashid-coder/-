package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.EngineeringWikiAndCoach
import com.example.domain.LearningEvaluator
import com.example.ui.LearningUiState
import com.example.ui.theme.LocalSemanticStatusColors
import kotlin.math.roundToInt

@Composable
fun OfflineWikiAndCalculatorsView(
  uiState: LearningUiState,
  onSearchQueryChange: (String) -> Unit,
  onOpenLesson: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val statusColors = LocalSemanticStatusColors.current
  val q = uiState.wikiSearchQuery.trim()

  val allLessons = remember(uiState.tracks) { uiState.tracks.flatMap { it.lessons } }
  val allGlossary = remember(allLessons) {
    allLessons.flatMap { lesson -> lesson.glossary.map { it to lesson.titleAr } }.distinctBy { it.first.termAr }
  }
  val wikiFormulas = remember { EngineeringWikiAndCoach.getWikiReferenceEntries() }

  val filteredFormulas = remember(q, wikiFormulas) {
    if (q.isEmpty()) wikiFormulas else wikiFormulas.filter {
      it.titleAr.contains(q, ignoreCase = true) ||
        it.categoryAr.contains(q, ignoreCase = true) ||
        it.equationLtr.contains(q, ignoreCase = true) ||
        it.explanationAr.contains(q, ignoreCase = true)
    }
  }
  val filteredGlossary = remember(q, allGlossary) {
    if (q.isEmpty()) allGlossary.take(8) else allGlossary.filter { (term, _) ->
      term.termAr.contains(q, ignoreCase = true) ||
        term.symbolEn.contains(q, ignoreCase = true) ||
        term.definitionAr.contains(q, ignoreCase = true)
    }
  }
  val filteredLessons = remember(q, allLessons) {
    if (q.isEmpty()) emptyList() else allLessons.filter { lesson ->
      lesson.titleAr.contains(q, ignoreCase = true) ||
        lesson.sections.any { it.bodyAr.contains(q, ignoreCase = true) || it.titleAr.contains(q, ignoreCase = true) }
    }
  }

  // Calculator 1: Powertrain TWR & Hover Time
  var massKg by remember { mutableFloatStateOf(1.40f) }
  var thrustPerMotorN by remember { mutableFloatStateOf(7.0f) }
  var cellsS by remember { mutableIntStateOf(4) }
  var capacityMah by remember { mutableIntStateOf(3000) }
  var hoverCurrentA by remember { mutableFloatStateOf(14.0f) }
  val powertrainResult = EngineeringWikiAndCoach.calculateHoverAndPowertrainBudget(
    totalMassKg = massKg,
    maxThrustPerMotorN = thrustPerMotorN,
    batteryCellsS = cellsS,
    batteryCapacityMah = capacityMah,
    hoverCurrentA = hoverCurrentA,
  )

  // Calculator 2: Radio Link Budget & Fresnel Zone
  var selectedBand by remember { mutableStateOf(EngineeringWikiAndCoach.RadioBandOption.BAND_2400) }
  var distanceM by remember { mutableFloatStateOf(500f) }
  var txPowerMw by remember { mutableFloatStateOf(100f) }
  val linkResult = EngineeringWikiAndCoach.calculateLinkBudgetAndFresnel(
    band = selectedBand,
    distanceM = distanceM,
    txPowerMw = txPowerMw,
  )

  // Calculator 3: ISA Air Density & Elevation
  var elevationM by remember { mutableFloatStateOf(300f) }
  var ambientTempC by remember { mutableFloatStateOf(22f) }
  val densityResult = EngineeringWikiAndCoach.calculateAirDensityAndThrustRatio(
    elevationM = elevationM,
    temperatureC = ambientTempC,
  )

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("wiki_list"),
  ) {
    item {
      Text(text = "الموسوعة الهندسية والحاسبات (دون إنترنت)", style = MaterialTheme.typography.headlineSmall)
      Text(
        text = "مرجع علمي مفتوح داخل التطبيق: ابحث في المعادلات والمصطلحات أو اختبر حسابات الدفع والطاقة والموجات وكثافة الهواء.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Search box
    item {
      OutlinedTextField(
        value = uiState.wikiSearchQuery,
        onValueChange = onSearchQueryChange,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (uiState.wikiSearchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
            }
          }
        },
        placeholder = { Text("ابحث: PID، فرينل، كثافة الهواء، ESC، الجيروسكوب...") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("wiki_search_field"),
      )
    }

    // Search results for lessons (when query active)
    if (filteredLessons.isNotEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(14.dp)) {
            Text(text = "دروس مطابقة (${filteredLessons.size})", style = MaterialTheme.typography.titleSmall)
            filteredLessons.forEach { lesson ->
              val unlocked = LearningEvaluator.isLessonUnlocked(lesson, uiState.completedLessonIds)
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                  .fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .clickable(enabled = unlocked) { onOpenLesson(lesson.id) },
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(text = lesson.titleAr, style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = if (unlocked) "اضغط لفتح الدرس" else "مقفل: يتطلب إكمال الدرس السابق",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Calculator 1: Powertrain TWR & Hover Time
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("wiki_calc_powertrain"),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "حاسبة ميزان الطائرة (TWR) وزمن التحويم", style = MaterialTheme.typography.titleMedium)
          }

          Text("الكتلة الإجمالية: ${"%.2f".format(massKg)} kg (الوزن: ${"%.2f".format(powertrainResult.weightN)} N)", style = MaterialTheme.typography.bodySmall)
          Slider(value = massKg, onValueChange = { massKg = it }, valueRange = 0.4f..6.0f)

          Text("أقصى دفع للمحرك الواحد: ${"%.1f".format(thrustPerMotorN)} N (الإجمالي: ${"%.1f".format(powertrainResult.totalThrustN)} N)", style = MaterialTheme.typography.bodySmall)
          Slider(value = thrustPerMotorN, onValueChange = { thrustPerMotorN = it }, valueRange = 2.0f..25.0f)

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(3, 4, 6).forEach { sCount ->
              FilterChip(
                selected = cellsS == sCount,
                onClick = { cellsS = sCount },
                label = { Text("${sCount}S (${"%.1f".format(sCount * 3.7f)}V)") },
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }

          Text("سعة البطارية: $capacityMah mAh • تيار التحويم: ${"%.1f".format(hoverCurrentA)} A", style = MaterialTheme.typography.bodySmall)
          Slider(value = capacityMah.toFloat(), onValueChange = { capacityMah = it.roundToInt() }, valueRange = 1000f..10000f)
          Slider(value = hoverCurrentA, onValueChange = { hoverCurrentA = it }, valueRange = 4f..45f)

          Surface(
            color = if (powertrainResult.twr < 1.2f) statusColors.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
              Text(
                text = "نسبة الدفع للوزن TWR = ${"%.2f".format(powertrainResult.twr)} • خانق التحويم ≈ ${"%.0f".format(powertrainResult.hoverThrottlePercent)}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "الطاقة = ${"%.1f".format(powertrainResult.energyWh)} Wh • زمن التحويم التقديري (بهامش 20%) ≈ ${"%.1f".format(powertrainResult.estimatedHoverMin)} دقيقة",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(text = powertrainResult.verdictAr, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    // Calculator 2: Radio Link Budget & Fresnel Zone
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("wiki_calc_radio"),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
          Text(text = "حاسبة الرابط اللاسلكي ومنطقة فرينل (FSPL)", style = MaterialTheme.typography.titleMedium)
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          ) {
            EngineeringWikiAndCoach.RadioBandOption.entries.forEach { band ->
              FilterChip(
                selected = selectedBand == band,
                onClick = { selectedBand = band },
                label = { Text(band.labelAr) },
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }

          Text("المسافة (خط البصر): ${distanceM.roundToInt()} m • قدرة البث الافتراضية: ${txPowerMw.roundToInt()} mW", style = MaterialTheme.typography.bodySmall)
          Slider(value = distanceM, onValueChange = { distanceM = it }, valueRange = 20f..3000f)
          Slider(value = txPowerMw, onValueChange = { txPowerMw = it }, valueRange = 25f..500f)

          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
              Text(
                text = "الطول الموجي λ = ${"%.3f".format(linkResult.wavelengthM)} m • الفقد FSPL = ${"%.1f".format(linkResult.fsplDb)} dB",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "نصف قطر فرينل r₁ = ${"%.2f".format(linkResult.fresnelRadiusM)} m • الإشارة النظرية = ${"%.1f".format(linkResult.estimatedRxDbm)} dBm",
                style = MaterialTheme.typography.bodySmall,
              )
              Text(text = linkResult.guidanceAr, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    // Calculator 3: ISA Air Density & Elevation
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("wiki_calc_density"),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
          Text(text = "حاسبة كثافة الهواء (ISA) والارتفاع", style = MaterialTheme.typography.titleMedium)
          Text("الارتفاع عن سطح البحر: ${elevationM.roundToInt()} m • درجة الحرارة: ${ambientTempC.roundToInt()} °C", style = MaterialTheme.typography.bodySmall)
          Slider(value = elevationM, onValueChange = { elevationM = it }, valueRange = 0f..3500f)
          Slider(value = ambientTempC, onValueChange = { ambientTempC = it }, valueRange = -10f..50f)

          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
              Text(
                text = "كثافة الهواء ρ = ${"%.3f".format(densityResult.densityKgM3)} kg/m³ (${"%.1f".format(densityResult.relativeRatioPercent)}% من القياسي)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(text = densityResult.noteAr, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    // Wiring topology reference
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "مخطط التوصيل المرجعي (مسار الطاقة مقابل الإشارة)", style = MaterialTheme.typography.titleSmall)
          }
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
              Text(
                text = """
                  [LiPo Battery (XT60 + Balance)]
                     ├──(High Current VBat)──> [4-in-1 ESC + Capacitor] ══> [M1, M2, M3, M4]
                     └──(Regulated BEC 5V/9V)─> [Flight Controller (MCU + IMU)]
                                                  ├── I2C/UART ──> [Barometer + GNSS + Compass]
                                                  └── DShot/PWM ─> [ESC Signal Lines (S1..S4)]
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
          Text(
            text = "ملاحظة أمان: خطوط القدرة العالية (VBat) تُفصل فيزياوياً وكهرومغناطيسياً عن أسلاك البوصلة وخطوط الإشارة.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Searchable Formulas & Glossary Reference
    item {
      Text(text = "مرجع المعادلات السريع (${filteredFormulas.size})", style = MaterialTheme.typography.titleMedium)
    }

    items(filteredFormulas.size) { index ->
      val f = filteredFormulas[index]
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(12.dp)) {
          Text(text = "${f.categoryAr} • ${f.titleAr}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
              Text(text = f.equationLtr, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            }
          }
          Text(text = f.explanationAr, style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    if (filteredGlossary.isNotEmpty()) {
      item {
        Text(text = "القاموس التقني (${filteredGlossary.size})", style = MaterialTheme.typography.titleMedium)
      }
      items(filteredGlossary.size) { idx ->
        val (term, lessonTitle) = filteredGlossary[idx]
        Surface(
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "${term.termAr} (${term.symbolEn})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(text = term.definitionAr, style = MaterialTheme.typography.bodySmall)
            Text(text = "المصدر: $lessonTitle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
fun OfflineSocraticCoachView(
  uiState: LearningUiState,
  onToggleClue: (String) -> Unit,
  onToggleDerivation: (String) -> Unit,
  onOpenRecommendedNext: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val totalLessons = uiState.tracks.sumOf { it.lessons.size }
  val recommendation = EngineeringWikiAndCoach.recommendNextStudyStepAr(
    completedLessons = uiState.completedLessonIds,
    totalLessons = totalLessons,
    completedScenarios = uiState.completedScenarioIds,
  )
  val nextUnlockedLesson = uiState.tracks.flatMap { it.lessons }.firstOrNull { lesson ->
    lesson.id !in uiState.completedLessonIds && LearningEvaluator.isLessonUnlocked(lesson, uiState.completedLessonIds)
  }
  val topics = remember { EngineeringWikiAndCoach.getSocraticTopics() }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    item {
      Text(text = "المدرب العلمي المحلي (منهج سقراطي دون إنترنت)", style = MaterialTheme.typography.headlineSmall)
      Text(
        text = "مرشد تعليمي مبني على القواعد الفيزيائية الحتمية: يوجّهك بتلميح استنتاجي أولاً قبل عرض الاشتقاق الكامل، دون أي اتصال بالإنترنت.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Next-step advisor card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("coach_recommendation_card"),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "خطة التعلم المقترحة حسب تقدمك", style = MaterialTheme.typography.titleMedium)
          }
          Text(text = recommendation, style = MaterialTheme.typography.bodyMedium)
          if (nextUnlockedLesson != null) {
            OutlinedButton(
              onClick = { onOpenRecommendedNext(nextUnlockedLesson.id) },
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
            ) {
              Text("متابعة: ${nextUnlockedLesson.titleAr}")
            }
          }
        }
      }
    }

    // Socratic topic cards
    items(topics.size) { index ->
      val topic = topics[index]
      val clueVisible = topic.id in uiState.coachRevealedClues
      val derivationVisible = topic.id in uiState.coachRevealedDerivations

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("coach_topic_${topic.id}"),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
          Text(text = topic.trackHintAr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Text(text = topic.questionAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = { onToggleClue(topic.id) },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (clueVisible) "إخفاء التلميح" else "تلميح استنتاجي")
            }
            OutlinedButton(
              onClick = { onToggleDerivation(topic.id) },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Text(if (derivationVisible) "إخفاء الاشتقاق" else "عرض الاشتقاق")
            }
          }

          AnimatedVisibility(visible = clueVisible) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
                Text(text = "تلميح للتفكير:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(text = topic.guidingClueAr, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }

          AnimatedVisibility(visible = derivationVisible) {
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
                HorizontalDivider()
                Text(text = "الاشتقاق الفيزيائي الكامل:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(text = topic.detailedDerivationAr, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}
