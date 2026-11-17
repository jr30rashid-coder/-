package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.ContentOrigin
import com.example.domain.LearningEvaluator
import com.example.domain.LessonUnit
import com.example.domain.SkillLevel
import com.example.ui.AcademyTab
import com.example.ui.LearningUiState
import com.example.ui.ModuleStep
import com.example.ui.components.ConceptMcqSection
import com.example.ui.components.PartVisualAssets
import com.example.ui.components.QuadrotorDiagramExercise
import com.example.ui.components.SafetyCheckCardsSection
import com.example.ui.components.SectionReviewQuiz
import com.example.ui.theme.LocalSemanticStatusColors

@Composable
fun AcademyScreen(
  uiState: LearningUiState,
  onSelectAcademyTab: (AcademyTab) -> Unit,
  onSearchWikiChange: (String) -> Unit,
  onToggleCoachClue: (String) -> Unit,
  onToggleCoachDerivation: (String) -> Unit,
  onSelectLevelFilter: (SkillLevel?) -> Unit,
  onOpenLesson: (String) -> Unit,
  onCloseLesson: () -> Unit,
  onSelectStep: (ModuleStep) -> Unit,
  onSelectChipForTap: (String) -> Unit,
  onPlaceComponent: (slotId: String, chipId: String) -> Unit,
  onRemovePlacedComponent: (slotId: String) -> Unit,
  onResetDragAndDrop: () -> Unit,
  onAnswerSafetyCard: (cardId: String, answerIsTrue: Boolean) -> Unit,
  onAnswerConceptMcq: (questionId: String, optionId: String) -> Unit,
  onSelectReviewQuizOption: (questionId: String, optionId: String) -> Unit,
  onSubmitReviewQuiz: () -> Unit,
  onRetryReviewQuiz: () -> Unit,
  onMarkLessonCompleted: (String) -> Unit,
  onImportPackBytes: (bytes: ByteArray, isZip: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeLesson = uiState.activeLesson
  if (activeLesson != null) {
    BackHandler { onCloseLesson() }
    LessonDetailView(
      lesson = activeLesson,
      uiState = uiState,
      onBackToCatalog = onCloseLesson,
      onSelectStep = onSelectStep,
      onSelectChipForTap = onSelectChipForTap,
      onPlaceComponent = onPlaceComponent,
      onRemovePlacedComponent = onRemovePlacedComponent,
      onResetDragAndDrop = onResetDragAndDrop,
      onAnswerSafetyCard = onAnswerSafetyCard,
      onAnswerConceptMcq = onAnswerConceptMcq,
      onSelectReviewQuizOption = onSelectReviewQuizOption,
      onSubmitReviewQuiz = onSubmitReviewQuiz,
      onRetryReviewQuiz = onRetryReviewQuiz,
      onMarkLessonCompleted = onMarkLessonCompleted,
      modifier = modifier,
    )
    return
  }

  Column(modifier = modifier) {
    TabRow(selectedTabIndex = uiState.academyTab.ordinal) {
      val lessonCount = uiState.tracks.sumOf { it.lessons.size }
      AcademyTab.entries.forEach { tab ->
        val label = if (tab == AcademyTab.COURSES) "المسارات ($lessonCount)" else tab.titleAr
        Tab(
          selected = uiState.academyTab == tab,
          onClick = { onSelectAcademyTab(tab) },
          text = { Text(label, maxLines = 1) },
          modifier = Modifier.testTag("academy_tab_${tab.name}"),
        )
      }
    }

    when (uiState.academyTab) {
      AcademyTab.WIKI_AND_CALCULATORS -> {
        OfflineWikiAndCalculatorsView(
          uiState = uiState,
          onSearchQueryChange = onSearchWikiChange,
          onOpenLesson = onOpenLesson,
        )
      }
      AcademyTab.STUDY_COACH -> {
        OfflineSocraticCoachView(
          uiState = uiState,
          onToggleClue = onToggleCoachClue,
          onToggleDerivation = onToggleCoachDerivation,
          onOpenRecommendedNext = onOpenLesson,
        )
      }
      AcademyTab.COURSES -> {
        val context = LocalContext.current
        val statusColors = LocalSemanticStatusColors.current

        val packPicker = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
          if (uri != null) {
            val name = uri.lastPathSegment.orEmpty().lowercase()
            val isZip = name.endsWith(".zip")
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
              onImportPackBytes(bytes, isZip)
            }
          }
        }

        val filteredTracks = if (uiState.selectedLevelFilter == null) {
          uiState.tracks
        } else {
          uiState.tracks.filter { track ->
            track.targetLevel == uiState.selectedLevelFilter ||
              track.lessons.any { it.level == uiState.selectedLevelFilter }
          }
        }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          item {
            Text(
              text = "المدرسة التعليمية (8 مسارات / ${uiState.tracks.sumOf { it.lessons.size }} درساً)",
              style = MaterialTheme.typography.headlineMedium,
            )
            Text(
              text = "منهج علمي متكامل من المبادئ الفيزيائية والأرصاد والموجات حتى تحليل السجلات والبرمجة المعزولة.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
      ) {
        FilterChip(
          selected = uiState.selectedLevelFilter == null,
          onClick = { onSelectLevelFilter(null) },
          label = { Text("الكل") },
          modifier = Modifier.minimumInteractiveComponentSize(),
        )
        SkillLevel.entries.forEach { level ->
          FilterChip(
            selected = uiState.selectedLevelFilter == level,
            onClick = { onSelectLevelFilter(level) },
            label = { Text(level.labelAr) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
    }

    items(filteredTracks, key = { it.id }) { track ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = "المستوى: ${track.targetLevel.labelAr}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
            val doneInTrack = track.lessons.count { it.id in uiState.completedLessonIds }
            Text(
              text = "$doneInTrack/${track.lessons.size} مكتمل",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(text = track.titleAr, style = MaterialTheme.typography.titleMedium)
          Text(
            text = track.subtitleAr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider()
          track.lessons.forEach { lesson ->
            val unlocked = LearningEvaluator.isLessonUnlocked(lesson, uiState.completedLessonIds)
            val completed = lesson.id in uiState.completedLessonIds
            val bestScore = uiState.quizBestPercentages[lesson.id]

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (unlocked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
              },
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("lesson_card_${lesson.id}")
                .clickable(enabled = unlocked) { onOpenLesson(lesson.id) },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp),
              ) {
                Icon(
                  imageVector = when {
                    completed -> Icons.Default.CheckCircle
                    unlocked -> Icons.Default.PlayArrow
                    else -> Icons.Default.Lock
                  },
                  contentDescription = null,
                  tint = when {
                    completed -> statusColors.success
                    unlocked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                  },
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = lesson.titleAr, style = MaterialTheme.typography.titleSmall)
                  val originLabel = if (lesson.origin == ContentOrigin.IMPORTED) " • ${lesson.origin.labelAr}" else ""
                  val scoreLabel = if (bestScore != null) " • أفضل نتيجة: $bestScore%" else ""
                  val prereqText = if (!unlocked) " • يتطلب إكمال الدرس السابق" else ""
                  Text(
                    text = "${lesson.estimatedMinutes} د • ${lesson.reviewStatus.labelAr}$originLabel$scoreLabel$prereqText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Phase 2: Safe Local Content Pack Import card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(16.dp),
        ) {
          Text(text = "استيراد حزم المحتوى المحلي الآمن (JSON / ZIP)", style = MaterialTheme.typography.titleSmall)
          Text(
            text = "يفحص المستورد المسارات (../) والامتدادات التنفيذية وبصمة SHA-256 وحدود الحجم دون تشغيل أي كود. المحتوى المستورد يبقى منفصلاً تحت «يتطلب مراجعة بشرية».",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedButton(
            onClick = { packPicker.launch(arrayOf("application/json", "application/zip")) },
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("import_content_pack_button"),
          ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("اختيار حزمة JSON أو ZIP محلية")
          }
          uiState.lastImportMessageAr?.let { msg ->
            Text(
              text = msg,
              style = MaterialTheme.typography.bodySmall,
              color = if (uiState.lastImportWasError) statusColors.error else statusColors.success,
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
      }
    }
  }
}

@Composable
private fun LessonDetailView(
  lesson: LessonUnit,
  uiState: LearningUiState,
  onBackToCatalog: () -> Unit,
  onSelectStep: (ModuleStep) -> Unit,
  onSelectChipForTap: (String) -> Unit,
  onPlaceComponent: (slotId: String, chipId: String) -> Unit,
  onRemovePlacedComponent: (slotId: String) -> Unit,
  onResetDragAndDrop: () -> Unit,
  onAnswerSafetyCard: (cardId: String, answerIsTrue: Boolean) -> Unit,
  onAnswerConceptMcq: (questionId: String, optionId: String) -> Unit,
  onSelectReviewQuizOption: (questionId: String, optionId: String) -> Unit,
  onSubmitReviewQuiz: () -> Unit,
  onRetryReviewQuiz: () -> Unit,
  onMarkLessonCompleted: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val availableSteps = ModuleStep.entries.filter { step ->
    when (step) {
      ModuleStep.DRAG_AND_DROP -> lesson.includeComponentDiagram
      ModuleStep.SAFETY_CARDS -> lesson.safetyCards.isNotEmpty()
      ModuleStep.CONCEPT_MCQS -> lesson.conceptMcqs.isNotEmpty()
      ModuleStep.REVIEW_QUIZ -> lesson.reviewQuiz.isNotEmpty()
      ModuleStep.THEORY_AND_FORMULAS -> true
    }
  }

  val activeStep = if (uiState.currentStep in availableSteps) uiState.currentStep else ModuleStep.THEORY_AND_FORMULAS

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedButton(
          onClick = onBackToCatalog,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("قائمة المسارات")
        }
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer,
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = "${lesson.reviewStatus.labelAr} • ${lesson.origin.labelAr}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }

    item {
      Text(text = lesson.titleAr, style = MaterialTheme.typography.titleLarge)
      Text(
        text = lesson.sourceNoteAr,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    item {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
      ) {
        availableSteps.forEachIndexed { idx, step ->
          FilterChip(
            selected = activeStep == step,
            onClick = { onSelectStep(step) },
            label = { Text("${idx + 1}. ${step.titleAr}") },
            modifier = Modifier.minimumInteractiveComponentSize(),
          )
        }
      }
    }

    item {
      when (activeStep) {
        ModuleStep.THEORY_AND_FORMULAS -> {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(14.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = "أهداف الدرس", style = MaterialTheme.typography.titleSmall)
                }
                lesson.objectivesAr.forEach { obj ->
                  Text(text = "• $obj", style = MaterialTheme.typography.bodyMedium)
                }
              }
            }

            lesson.sections.forEach { section ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(16.dp),
                ) {
                  Text(text = section.titleAr, style = MaterialTheme.typography.titleMedium)
                  val drawableId = PartVisualAssets.resolveSectionDrawable(section.visualDrawableKey)
                  if (drawableId != null) {
                    Image(
                      painter = painterResource(drawableId),
                      contentDescription = section.visualCaptionAr ?: section.titleAr,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(12.dp)),
                    )
                    section.visualCaptionAr?.let { caption ->
                      Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                  Text(text = section.bodyAr, style = MaterialTheme.typography.bodyLarge)
                  section.practicalExampleAr?.let { example ->
                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(10.dp),
                      ) {
                        Text(
                          text = "مثال تطبيقي محلول:",
                          style = MaterialTheme.typography.labelLarge,
                          color = MaterialTheme.colorScheme.primary,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(text = example, style = MaterialTheme.typography.bodyMedium)
                      }
                    }
                  }
                  section.commonMistakeAr?.let { mistake ->
                    Surface(
                      color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(10.dp),
                      ) {
                        Text(
                          text = "انتبه — خطأ شائع وكيف تتجنبه:",
                          style = MaterialTheme.typography.labelLarge,
                          color = MaterialTheme.colorScheme.error,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(text = mistake, style = MaterialTheme.typography.bodyMedium)
                      }
                    }
                  }
                  Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text = "الخلاصة الهندسية: ${section.keyTakeawayAr}",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.padding(10.dp),
                    )
                  }
                }
              }
            }

            if (lesson.equations.isNotEmpty()) {
              Text(text = "المعادلات والوحدات القياسية (SI)", style = MaterialTheme.typography.titleMedium)
              lesson.equations.forEach { eq ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(14.dp),
                  ) {
                    Text(text = eq.titleAr, style = MaterialTheme.typography.titleSmall)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                      Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Text(
                          text = eq.equationLtr,
                          fontFamily = FontFamily.Monospace,
                          style = MaterialTheme.typography.bodyMedium,
                          modifier = Modifier.padding(10.dp),
                        )
                      }
                    }
                    Text(text = eq.unitsNoteAr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = eq.explanationAr, style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }

            if (lesson.glossary.isNotEmpty()) {
              Text(text = "قاموس المصطلحات", style = MaterialTheme.typography.titleMedium)
              lesson.glossary.forEach { term ->
                Surface(
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                      text = "${term.termAr} (${term.symbolEn})",
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = term.definitionAr, style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }

            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "ملخص الدرس: ${lesson.summaryAr}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
              )
            }

            if (lesson.reviewQuiz.isEmpty()) {
              Button(
                onClick = { onMarkLessonCompleted(lesson.id) },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
              ) {
                Text("تسجيل إتمام قراءة الدرس محلياً")
              }
            }
          }
        }

        ModuleStep.DRAG_AND_DROP -> {
          QuadrotorDiagramExercise(
            promptAr = uiState.moduleData.diagramPromptAr,
            slots = uiState.moduleData.slots,
            chips = uiState.moduleData.chips,
            placedChipsBySlotId = uiState.placedChipsBySlotId,
            selectedChipForTapId = uiState.selectedChipForTapId,
            lastFeedback = uiState.lastPlacementFeedback,
            reducedMotion = uiState.displaySettings.reducedMotion,
            onSelectChipForTap = onSelectChipForTap,
            onPlaceComponent = onPlaceComponent,
            onRemovePlacedComponent = onRemovePlacedComponent,
            onReset = onResetDragAndDrop,
          )
        }

        ModuleStep.SAFETY_CARDS -> {
          SafetyCheckCardsSection(
            cards = lesson.safetyCards,
            answeredCards = uiState.answeredSafetyCards,
            onAnswerCard = onAnswerSafetyCard,
          )
        }

        ModuleStep.CONCEPT_MCQS -> {
          ConceptMcqSection(
            questions = lesson.conceptMcqs,
            answeredOptions = uiState.answeredConceptMcqs,
            onSelectOption = onAnswerConceptMcq,
          )
        }

        ModuleStep.REVIEW_QUIZ -> {
          SectionReviewQuiz(
            questions = lesson.reviewQuiz,
            selections = uiState.reviewQuizSelections,
            summary = uiState.reviewQuizSummary,
            onSelectOption = onSelectReviewQuizOption,
            onSubmitQuiz = onSubmitReviewQuiz,
            onRetryQuiz = onRetryReviewQuiz,
          )
        }
      }
    }

    item {
      val idx = availableSteps.indexOf(activeStep)
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (idx > 0) {
          OutlinedButton(
            onClick = { onSelectStep(availableSteps[idx - 1]) },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("السابق")
          }
        }
        if (idx < availableSteps.lastIndex) {
          Button(
            onClick = { onSelectStep(availableSteps[idx + 1]) },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
          ) {
            Text("التالي: ${availableSteps[idx + 1].titleAr}")
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
