package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FullCurriculumCatalog
import com.example.data.LocalCurriculumRepository
import com.example.data.LocalProgressStore
import com.example.data.RegulationsAndWorkshopData
import com.example.domain.ContentPackSecurity
import com.example.domain.CurriculumTrack
import com.example.domain.DroneComponentChip
import com.example.domain.DroneComponentId
import com.example.domain.DronePhysicsEngine
import com.example.domain.InteractiveModuleData
import com.example.domain.LearningEvaluator
import com.example.domain.LessonUnit
import com.example.domain.PlacementResult
import com.example.domain.QuizSummary
import com.example.domain.SafetyWorkshopAndLogs
import com.example.domain.SkillLevel
import com.example.domain.VisualProgrammingSandbox
import com.example.ui.gl.GlRenderMode
import com.example.ui.gl.SimCameraMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LearningDestination {
  FOUNDATION,
  ACADEMY,
  DRONE_3D_LAB,
  SIMULATOR_HUB,
  WORKSHOP_GOVERNANCE,
}

enum class ModuleStep(val stepNumber: Int, val titleAr: String) {
  THEORY_AND_FORMULAS(1, "الشرح والمعادلات"),
  DRAG_AND_DROP(2, "تحديد المكونات"),
  SAFETY_CARDS(3, "بطاقات السلامة"),
  CONCEPT_MCQS(4, "أسئلة الفهم"),
  REVIEW_QUIZ(5, "اختبار القسم"),
}

enum class SimulatorTab(val titleAr: String) {
  FLIGHT_SIM("المحاكي والتدريب"),
  VISUAL_CODE("البرمجة المرئية"),
  LOG_ANALYZER("تحليل السجلات"),
}

enum class GovernanceTab(val titleAr: String) {
  WORKSHOP("الورشة والتشخيص"),
  REGULATIONS("السلامة واللوائح"),
  BACKUP_AND_LIMITS("النسخ والحدود"),
}

enum class AcademyTab(val titleAr: String) {
  COURSES("المسارات (15)"),
  WIKI_AND_CALCULATORS("الموسوعة والحاسبات"),
  STUDY_COACH("المدرب المحلي"),
}

data class DisplaySettings(
  val fontScale: Float = 1.0f,
  val highContrast: Boolean = false,
  val reducedMotion: Boolean = false,
)

data class LearningUiState(
  val destination: LearningDestination = LearningDestination.FOUNDATION,
  val academyTab: AcademyTab = AcademyTab.COURSES,
  val wikiSearchQuery: String = "",
  val coachRevealedClues: Set<String> = emptySet(),
  val coachRevealedDerivations: Set<String> = emptySet(),
  val tracks: List<CurriculumTrack> = FullCurriculumCatalog.getBundledTracks(),
  val selectedTrackId: String? = null,
  val selectedLevelFilter: SkillLevel? = null,
  val activeLessonId: String? = null,
  val moduleData: InteractiveModuleData = LocalCurriculumRepository.getPrototypeModule(),
  val currentStep: ModuleStep = ModuleStep.THEORY_AND_FORMULAS,
  val completedLessonIds: Set<String> = emptySet(),
  val quizBestPercentages: Map<String, Int> = emptyMap(),
  val importedPacks: List<ContentPackSecurity.ImportedContentPack> = emptyList(),
  val lastImportMessageAr: String? = null,
  val lastImportWasError: Boolean = false,
  // Interactive lesson exercises
  val placedChipsBySlotId: Map<String, DroneComponentChip> = emptyMap(),
  val selectedChipForTapId: String? = null,
  val lastPlacementFeedback: PlacementResult? = null,
  val answeredSafetyCards: Map<String, Boolean> = emptyMap(),
  val answeredConceptMcqs: Map<String, String> = emptyMap(),
  val reviewQuizSelections: Map<String, String> = emptyMap(),
  val reviewQuizSummary: QuizSummary? = null,
  // Phase 4: 3D Lab
  val glExplodeFactor: Float = 0.60f,
  val glZoomFactor: Float = 1.0f,
  val glSelectedPart: DroneComponentId? = null,
  val glPartSheetOpen: Boolean = false,
  val glHiddenParts: Set<DroneComponentId> = emptySet(),
  val glSpinPropellers: Boolean = true,
  val glRenderMode: GlRenderMode = GlRenderMode.SHADED,
  val glResetSignal: Int = 0,
  // Phase 5 & 6: Simulator & Scenarios
  val simulatorTab: SimulatorTab = SimulatorTab.FLIGHT_SIM,
  val simRunning: Boolean = false,
  val simSeed: Long = 2026L,
  val simInput: DronePhysicsEngine.SimControlInput = DronePhysicsEngine.SimControlInput(),
  val simState: DronePhysicsEngine.SimState = DronePhysicsEngine.SimState(),
  val simRecordedInputs: List<DronePhysicsEngine.SimControlInput> = emptyList(),
  val simRecordedSamples: List<DronePhysicsEngine.SimTelemetrySample> = emptyList(),
  val simReplayStatusAr: String? = null,
  val scenarios: List<DronePhysicsEngine.TrainingScenario> = DronePhysicsEngine.getTrainingScenarios(),
  val activeScenarioId: String? = null,
  val scenarioHoldSec: Float = 0f,
  val scenarioClearedGates: Int = 0,
  val scenarioEvaluation: DronePhysicsEngine.ScenarioEvaluation? = null,
  val completedScenarioIds: Set<String> = emptySet(),
  val simCameraMode: SimCameraMode = SimCameraMode.CHASE_3D,
  val simCameraResetSignal: Int = 0,
  // Phase 9: Visual Programming Sandbox
  val programBlocks: List<VisualProgrammingSandbox.AstNode> = VisualProgrammingSandbox.samplePresetTakeoffAndHover(),
  val sandboxResult: VisualProgrammingSandbox.SandboxExecutionResult? = null,
  // Phase 10: Telemetry Log Analyzer
  val analyzedLogReport: SafetyWorkshopAndLogs.LogAnalysisReport? = null,
  val comparedLogReport: SafetyWorkshopAndLogs.LogComparisonReport? = null,
  // Phase 7 & 8: Workshop & Regulations
  val governanceTab: GovernanceTab = GovernanceTab.WORKSHOP,
  val selectedMotorId: String = RegulationsAndWorkshopData.getMotors().first().id,
  val selectedEscId: String = RegulationsAndWorkshopData.getEscs().first().id,
  val selectedBatteryId: String = RegulationsAndWorkshopData.getBatteries().first().id,
  val diagnosticResult: SafetyWorkshopAndLogs.DiagnosticVerdict? = null,
  val regulatoryCountryFilter: String? = null,
  // Phase 11: Backup & Accessibility
  val displaySettings: DisplaySettings = DisplaySettings(),
  val isSettingsSheetOpen: Boolean = false,
  val lastBackupStatusAr: String? = null,
) {
  val activeLesson: LessonUnit?
    get() = activeLessonId?.let { FullCurriculumCatalog.findLesson(tracks, it) }
}

class LearningViewModel(
  private val appContext: Context? = null,
) : ViewModel() {

  private val _uiState = MutableStateFlow(loadInitialState())
  val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

  private var simLoopJob: Job? = null

  private fun loadInitialState(): LearningUiState {
    val persisted = appContext?.let { LocalProgressStore.loadFromAppPrivateStorage(it) }
    val base = LearningUiState()
    return if (persisted != null) {
      base.copy(
        completedLessonIds = persisted.completedLessonIds,
        quizBestPercentages = persisted.quizBestPercentages,
        completedScenarioIds = persisted.completedScenarioIds,
        displaySettings = persisted.displaySettings,
      )
    } else {
      base
    }
  }

  private fun persistState(state: LearningUiState) {
    val ctx = appContext ?: return
    LocalProgressStore.saveToAppPrivateStorage(
      ctx,
      LocalProgressStore.PersistedProgress(
        completedLessonIds = state.completedLessonIds,
        quizBestPercentages = state.quizBestPercentages,
        completedScenarioIds = state.completedScenarioIds,
        displaySettings = state.displaySettings,
      ),
    )
  }

  fun selectDestination(destination: LearningDestination) {
    if (destination != LearningDestination.SIMULATOR_HUB && _uiState.value.simRunning) {
      stopSimulationLoop()
    }
    _uiState.update { it.copy(destination = destination) }
  }

  fun selectLevelFilter(level: SkillLevel?) {
    _uiState.update { it.copy(selectedLevelFilter = level) }
  }

  fun selectAcademyTab(tab: AcademyTab) {
    _uiState.update { it.copy(academyTab = tab) }
  }

  fun updateWikiSearchQuery(query: String) {
    _uiState.update { it.copy(wikiSearchQuery = query) }
  }

  fun toggleCoachClue(topicId: String) {
    _uiState.update { current ->
      val next = if (topicId in current.coachRevealedClues) {
        current.coachRevealedClues - topicId
      } else {
        current.coachRevealedClues + topicId
      }
      current.copy(coachRevealedClues = next)
    }
  }

  fun toggleCoachDerivation(topicId: String) {
    _uiState.update { current ->
      val next = if (topicId in current.coachRevealedDerivations) {
        current.coachRevealedDerivations - topicId
      } else {
        current.coachRevealedDerivations + topicId
      }
      current.copy(coachRevealedDerivations = next)
    }
  }

  fun openLesson(lessonId: String) {
    val lesson = FullCurriculumCatalog.findLesson(_uiState.value.tracks, lessonId) ?: return
    _uiState.update {
      it.copy(
        destination = LearningDestination.ACADEMY,
        activeLessonId = lesson.id,
        currentStep = ModuleStep.THEORY_AND_FORMULAS,
        placedChipsBySlotId = emptyMap(),
        selectedChipForTapId = null,
        lastPlacementFeedback = null,
        answeredSafetyCards = emptyMap(),
        answeredConceptMcqs = emptyMap(),
        reviewQuizSelections = emptyMap(),
        reviewQuizSummary = null,
      )
    }
  }

  fun closeActiveLesson() {
    _uiState.update { it.copy(activeLessonId = null) }
  }

  fun selectModuleStep(step: ModuleStep) {
    _uiState.update { it.copy(currentStep = step) }
  }

  fun markLessonCompleted(lessonId: String) {
    _uiState.update { current ->
      val next = current.copy(completedLessonIds = current.completedLessonIds + lessonId)
      persistState(next)
      next
    }
  }

  // Drag-and-drop component exercise
  fun selectChipForTap(chipId: String) {
    _uiState.update { current ->
      current.copy(selectedChipForTapId = if (current.selectedChipForTapId == chipId) null else chipId)
    }
  }

  fun placeChipOnSlot(slotId: String, chipId: String) {
    _uiState.update { current ->
      val slot = current.moduleData.slots.find { it.id == slotId } ?: return@update current
      val chip = current.moduleData.chips.find { it.id == chipId } ?: return@update current
      val result = LearningEvaluator.validateComponentPlacement(slot, chip)
      val updated = if (result.isCorrect) {
        current.placedChipsBySlotId.filterValues { it.id != chip.id } + (slot.id to chip)
      } else {
        current.placedChipsBySlotId
      }
      current.copy(
        placedChipsBySlotId = updated,
        selectedChipForTapId = null,
        lastPlacementFeedback = result,
      )
    }
  }

  fun removePlacedComponent(slotId: String) {
    _uiState.update {
      it.copy(placedChipsBySlotId = it.placedChipsBySlotId - slotId, lastPlacementFeedback = null)
    }
  }

  fun resetDragAndDrop() {
    _uiState.update {
      it.copy(placedChipsBySlotId = emptyMap(), selectedChipForTapId = null, lastPlacementFeedback = null)
    }
  }

  fun answerSafetyCard(cardId: String, answerIsTrue: Boolean) {
    _uiState.update { it.copy(answeredSafetyCards = it.answeredSafetyCards + (cardId to answerIsTrue)) }
  }

  fun answerConceptMcq(questionId: String, optionId: String) {
    _uiState.update { it.copy(answeredConceptMcqs = it.answeredConceptMcqs + (questionId to optionId)) }
  }

  fun selectReviewQuizOption(questionId: String, optionId: String) {
    _uiState.update { current ->
      if (current.reviewQuizSummary != null) return@update current
      current.copy(reviewQuizSelections = current.reviewQuizSelections + (questionId to optionId))
    }
  }

  fun submitReviewQuiz() {
    _uiState.update { current ->
      val questions = current.activeLesson?.reviewQuiz ?: current.moduleData.reviewQuiz
      val lessonId = current.activeLesson?.id ?: current.moduleData.moduleId
      val summary = LearningEvaluator.gradeSectionQuiz(questions, current.reviewQuizSelections)
      val prevBest = current.quizBestPercentages[lessonId] ?: 0
      val nextBest = maxOf(prevBest, summary.percentage)
      val nextCompleted = if (summary.percentage >= 60) {
        current.completedLessonIds + lessonId
      } else {
        current.completedLessonIds
      }
      val next = current.copy(
        reviewQuizSummary = summary,
        quizBestPercentages = current.quizBestPercentages + (lessonId to nextBest),
        completedLessonIds = nextCompleted,
      )
      persistState(next)
      next
    }
  }

  fun retryReviewQuiz() {
    _uiState.update { it.copy(reviewQuizSelections = emptyMap(), reviewQuizSummary = null) }
  }

  // Phase 2: Safe Content Pack Import
  fun importContentPackBytes(rawBytes: ByteArray, isZip: Boolean) {
    val stagingDir = appContext?.let { java.io.File(it.cacheDir, "pack_staging") }
    val finalDir = appContext?.let { java.io.File(it.filesDir, "imported_packs") }
    val result = if (isZip) {
      ContentPackSecurity.inspectAndStageZipPack(rawBytes, stagingDir, finalDir)
    } else {
      ContentPackSecurity.validateJsonPack(rawBytes)
    }

    _uiState.update { current ->
      when (result) {
        is ContentPackSecurity.ImportValidationResult.Valid -> {
          val updatedPacks = current.importedPacks.filterNot { it.packId == result.pack.packId } + result.pack
          val mergedTracks = FullCurriculumCatalog.mergeTracksWithImportedPacks(
            FullCurriculumCatalog.getBundledTracks(),
            updatedPacks,
          )
          current.copy(
            importedPacks = updatedPacks,
            tracks = mergedTracks,
            lastImportWasError = false,
            lastImportMessageAr = "تم استيراد الحزمة «${result.pack.titleAr}» بأمان (${result.pack.lessons.size} درس — حالة المراجعة: يتطلب مراجعة بشرية). SHA-256: ${result.pack.sha256.take(12)}…",
          )
        }
        is ContentPackSecurity.ImportValidationResult.Rejected -> {
          current.copy(
            lastImportWasError = true,
            lastImportMessageAr = "تم حظر الحزمة [${result.code}]: ${result.reasonAr}",
          )
        }
      }
    }
  }

  // Phase 4: 3D Lab
  fun open3DLabWithPart(part: DroneComponentId?) {
    stopSimulationLoop()
    _uiState.update { current ->
      current.copy(
        destination = LearningDestination.DRONE_3D_LAB,
        activeLessonId = null,
        glExplodeFactor = if (current.glExplodeFactor < 0.45f) 0.60f else current.glExplodeFactor,
        glSelectedPart = part,
        glPartSheetOpen = part != null,
        glHiddenParts = if (part != null) current.glHiddenParts - part else current.glHiddenParts,
      )
    }
  }

  fun updateGlExplode(value: Float) {
    _uiState.update { it.copy(glExplodeFactor = value.coerceIn(0f, 1f)) }
  }

  fun updateGlZoom(value: Float) {
    _uiState.update { it.copy(glZoomFactor = value.coerceIn(0.6f, 1.8f)) }
  }

  fun selectGlPart(part: DroneComponentId?) {
    _uiState.update { current ->
      if (part == null) {
        current.copy(glSelectedPart = null, glPartSheetOpen = false)
      } else {
        current.copy(
          glSelectedPart = part,
          glPartSheetOpen = true,
          glHiddenParts = current.glHiddenParts - part,
        )
      }
    }
  }

  fun openGlPartSheet(part: DroneComponentId) {
    _uiState.update { current ->
      current.copy(
        glSelectedPart = part,
        glPartSheetOpen = true,
        glHiddenParts = current.glHiddenParts - part,
      )
    }
  }

  fun dismissGlPartSheet() {
    _uiState.update { it.copy(glPartSheetOpen = false) }
  }

  fun toggleGlPartVisibility(part: DroneComponentId) {
    _uiState.update { current ->
      val next = if (part in current.glHiddenParts) {
        current.glHiddenParts - part
      } else {
        current.glHiddenParts + part
      }
      current.copy(glHiddenParts = next)
    }
  }

  fun toggleIsolateGlPart(part: DroneComponentId) {
    _uiState.update { current ->
      val others = DroneComponentId.entries.filter { it != part }.toSet()
      val alreadyIsolated = current.glHiddenParts == others && current.glSelectedPart == part
      if (alreadyIsolated) {
        current.copy(glHiddenParts = emptySet(), glSelectedPart = part)
      } else {
        current.copy(glHiddenParts = others, glSelectedPart = part)
      }
    }
  }

  fun showAllGlParts() {
    _uiState.update { it.copy(glHiddenParts = emptySet()) }
  }

  fun toggleGlSpinPropellers(enabled: Boolean) {
    _uiState.update { it.copy(glSpinPropellers = enabled) }
  }

  fun toggleGlRenderMode() {
    _uiState.update { current ->
      val nextMode = if (current.glRenderMode == GlRenderMode.SHADED) {
        GlRenderMode.WIREFRAME
      } else {
        GlRenderMode.SHADED
      }
      current.copy(glRenderMode = nextMode)
    }
  }

  fun resetGlCameraOnly() {
    _uiState.update {
      it.copy(
        glZoomFactor = 1f,
        glResetSignal = it.glResetSignal + 1,
      )
    }
  }

  fun resetGlView() {
    _uiState.update {
      it.copy(
        glExplodeFactor = 0.60f,
        glZoomFactor = 1f,
        glSelectedPart = null,
        glPartSheetOpen = false,
        glHiddenParts = emptySet(),
        glRenderMode = GlRenderMode.SHADED,
        glResetSignal = it.glResetSignal + 1,
      )
    }
  }

  // Phase 5 & 6: Simulator & Scenarios
  fun selectSimulatorTab(tab: SimulatorTab) {
    _uiState.update { it.copy(simulatorTab = tab) }
  }

  fun selectSimCameraMode(mode: SimCameraMode) {
    _uiState.update { it.copy(simCameraMode = mode, simCameraResetSignal = it.simCameraResetSignal + 1) }
  }

  fun resetSimCamera() {
    _uiState.update { it.copy(simCameraResetSignal = it.simCameraResetSignal + 1) }
  }

  fun updateSimInput(transform: (DronePhysicsEngine.SimControlInput) -> DronePhysicsEngine.SimControlInput) {
    _uiState.update { it.copy(simInput = transform(it.simInput)) }
  }

  fun toggleSimulationRunning() {
    if (_uiState.value.simRunning) {
      stopSimulationLoop()
    } else {
      startSimulationLoop()
    }
  }

  fun startSimulationIfIdle() {
    _uiState.update { current ->
      current.copy(simInput = current.simInput.copy(armed = true))
    }
    if (!_uiState.value.simRunning) {
      startSimulationLoop()
    }
  }

  fun setFlightHoldAxis(
    yawRateCmd: Float? = null,
    pitchCmd: Float? = null,
    rollCmd: Float? = null,
  ) {
    _uiState.update { current ->
      val prev = current.simInput
      current.copy(
        simInput = prev.copy(
          armed = true,
          yawRateCmd = yawRateCmd ?: prev.yawRateCmd,
          pitchCmd = pitchCmd ?: prev.pitchCmd,
          rollCmd = rollCmd ?: prev.rollCmd,
        ),
      )
    }
  }

  fun adjustThrottleBy(delta: Float) {
    _uiState.update { current ->
      val prev = current.simInput
      val nextThrottle = if (delta > 0f && prev.throttle < 0.45f) {
        0.55f
      } else {
        (prev.throttle + delta).coerceIn(0f, 1f)
      }
      current.copy(
        simInput = prev.copy(
          armed = if (delta > 0f) true else prev.armed,
          throttle = nextThrottle,
        ),
      )
    }
    if (!_uiState.value.simRunning) {
      repeat(5) { stepSimulationOnce() }
    }
  }

  fun nudgeSimControl(
    yawRateCmd: Float? = null,
    pitchCmd: Float? = null,
    rollCmd: Float? = null,
  ) {
    _uiState.update { current ->
      val prev = current.simInput
      current.copy(
        simInput = prev.copy(
          armed = true,
          yawRateCmd = yawRateCmd ?: prev.yawRateCmd,
          pitchCmd = pitchCmd ?: prev.pitchCmd,
          rollCmd = rollCmd ?: prev.rollCmd,
        ),
      )
    }
    repeat(8) { stepSimulationOnce() }
    _uiState.update { current ->
      val prev = current.simInput
      current.copy(
        simInput = prev.copy(
          yawRateCmd = if (yawRateCmd != null) 0f else prev.yawRateCmd,
          pitchCmd = if (pitchCmd != null) 0f else prev.pitchCmd,
          rollCmd = if (rollCmd != null) 0f else prev.rollCmd,
        ),
      )
    }
  }

  fun stepSimulationOnce() {
    _uiState.update { current ->
      val nextState = DronePhysicsEngine.step(current.simState, current.simInput, current.simSeed)
      val sample = DronePhysicsEngine.toSample(nextState)
      val activeScenario = current.scenarios.find { it.id == current.activeScenarioId }
      var nextHold = current.scenarioHoldSec
      var nextClearedGates = current.scenarioClearedGates
      var nextEval = current.scenarioEvaluation
      var nextCompleted = current.completedScenarioIds

      if (activeScenario != null) {
        val (h, eval) = DronePhysicsEngine.evaluateScenarioProgress(
          scenario = activeScenario,
          state = nextState,
          accumulatedHoldSec = current.scenarioHoldSec,
          clearedGateCount = current.scenarioClearedGates,
        )
        nextHold = h
        nextClearedGates = eval.clearedGateCount
        nextEval = eval
        if (eval.completed) nextCompleted = nextCompleted + activeScenario.id
      }

      val next = current.copy(
        simState = nextState,
        simInput = if (nextState.safetyCutReasonAr != null) current.simInput.copy(armed = false, throttle = 0f) else current.simInput,
        simRecordedInputs = current.simRecordedInputs + current.simInput,
        simRecordedSamples = current.simRecordedSamples + sample,
        scenarioHoldSec = nextHold,
        scenarioClearedGates = nextClearedGates,
        scenarioEvaluation = nextEval,
        completedScenarioIds = nextCompleted,
        simReplayStatusAr = null,
      )
      if (nextCompleted != current.completedScenarioIds) persistState(next)
      next
    }
  }

  private fun startSimulationLoop() {
    simLoopJob?.cancel()
    _uiState.update { it.copy(simRunning = true) }
    simLoopJob = viewModelScope.launch {
      while (isActive && _uiState.value.simRunning) {
        stepSimulationOnce()
        delay(20L)
      }
    }
  }

  private fun stopSimulationLoop() {
    simLoopJob?.cancel()
    simLoopJob = null
    _uiState.update { it.copy(simRunning = false) }
  }

  fun resetSimulation() {
    stopSimulationLoop()
    _uiState.update { current ->
      val activeScenario = current.scenarios.find { it.id == current.activeScenarioId }
      current.copy(
        simState = DronePhysicsEngine.SimState(),
        simInput = DronePhysicsEngine.SimControlInput(
          armed = false,
          throttle = 0f,
          windEnabled = activeScenario?.windEnabledByDefault ?: false,
        ),
        simRecordedInputs = emptyList(),
        simRecordedSamples = emptyList(),
        simReplayStatusAr = null,
        scenarioHoldSec = 0f,
        scenarioClearedGates = 0,
        scenarioEvaluation = null,
      )
    }
  }

  fun verifyDeterministicReplay() {
    _uiState.update { current ->
      if (current.simRecordedInputs.isEmpty()) {
        return@update current.copy(simReplayStatusAr = "شغّل المحاكي لعدة خطوات أولاً لتسجيل مدخلات قابلة للإعادة.")
      }
      val replayed = DronePhysicsEngine.replaySession(current.simRecordedInputs, current.simSeed)
      val exactMatch = replayed == current.simRecordedSamples
      current.copy(
        simReplayStatusAr = if (exactMatch) {
          "نجحت إعادة التشغيل الحتمية (${replayed.size} خطوة متطابقة تماماً بنفس البذرة والمدخلات)."
        } else {
          "اختلفت نتائج الإعادة عن الجلسة المسجلة."
        },
        analyzedLogReport = SafetyWorkshopAndLogs.analyzeFlightLog(replayed),
      )
    }
  }

  fun selectTrainingScenario(scenarioId: String?) {
    stopSimulationLoop()
    _uiState.update { current ->
      val scenario = current.scenarios.find { it.id == scenarioId }
      current.copy(
        activeScenarioId = scenarioId,
        simState = DronePhysicsEngine.SimState(),
        simInput = DronePhysicsEngine.SimControlInput(
          armed = false,
          throttle = 0f,
          windEnabled = scenario?.windEnabledByDefault ?: false,
        ),
        simRecordedInputs = emptyList(),
        simRecordedSamples = emptyList(),
        scenarioHoldSec = 0f,
        scenarioClearedGates = 0,
        scenarioEvaluation = null,
      )
    }
  }

  // Phase 9: Visual Programming Sandbox
  fun addProgramBlock(node: VisualProgrammingSandbox.AstNode) {
    _uiState.update { it.copy(programBlocks = it.programBlocks + node, sandboxResult = null) }
  }

  fun removeLastProgramBlock() {
    _uiState.update { current ->
      if (current.programBlocks.isEmpty()) current
      else current.copy(programBlocks = current.programBlocks.dropLast(1), sandboxResult = null)
    }
  }

  fun loadPresetProgram(safe: Boolean) {
    val preset = if (safe) {
      VisualProgrammingSandbox.samplePresetTakeoffAndHover()
    } else {
      // Excessive loop count test preset demonstrating safety rejection
      listOf(
        VisualProgrammingSandbox.AstNode.RepeatBounded(
          times = 99,
          body = listOf(VisualProgrammingSandbox.AstNode.WaitSteps(5)),
        ),
      )
    }
    _uiState.update { it.copy(programBlocks = preset, sandboxResult = null) }
  }

  fun runVisualProgram() {
    _uiState.update { current ->
      val result = VisualProgrammingSandbox.execute(current.programBlocks, current.simSeed)
      val analysis = if (result.samples.isNotEmpty()) {
        SafetyWorkshopAndLogs.analyzeFlightLog(result.samples)
      } else {
        current.analyzedLogReport
      }
      current.copy(
        sandboxResult = result,
        analyzedLogReport = analysis,
      )
    }
  }

  // Phase 10: Log Analyzer
  fun analyzeCurrentSimLog() {
    _uiState.update { current ->
      val report = SafetyWorkshopAndLogs.analyzeFlightLog(current.simRecordedSamples)
      val baseline = RegulationsAndWorkshopData.getBaselineComparisonRun()
      val compare = SafetyWorkshopAndLogs.compareFlightLogs(baseline, current.simRecordedSamples)
      current.copy(analyzedLogReport = report, comparedLogReport = compare)
    }
  }

  fun testIncompleteLogAnalysis() {
    val brokenSamples = listOf(
      DronePhysicsEngine.SimTelemetrySample(1, 0.02f, 1.0f, 0f, 0f, 13f, 0f, 0f, 15.8f, 10f),
      DronePhysicsEngine.SimTelemetrySample(2, 0.04f, 1.2f, 0f, 0f, 13f, 0f, 0f, 15.7f, 10f),
    )
    _uiState.update { current ->
      current.copy(
        analyzedLogReport = SafetyWorkshopAndLogs.analyzeFlightLog(brokenSamples),
        comparedLogReport = null,
      )
    }
  }

  // Phase 7 & 8: Workshop & Governance
  fun selectGovernanceTab(tab: GovernanceTab) {
    _uiState.update { it.copy(governanceTab = tab) }
  }

  fun selectWorkshopParts(motorId: String? = null, escId: String? = null, batteryId: String? = null) {
    _uiState.update { current ->
      current.copy(
        selectedMotorId = motorId ?: current.selectedMotorId,
        selectedEscId = escId ?: current.selectedEscId,
        selectedBatteryId = batteryId ?: current.selectedBatteryId,
      )
    }
  }

  fun runDiagnosticEvaluation(input: SafetyWorkshopAndLogs.DiagnosticEvidenceInput) {
    _uiState.update { it.copy(diagnosticResult = SafetyWorkshopAndLogs.evaluateDiagnosticEvidence(input)) }
  }

  fun filterRegulationsByCountry(country: String?) {
    _uiState.update { it.copy(regulatoryCountryFilter = country) }
  }

  // Phase 11: Backup Export / Import & Display Settings
  fun exportProgressBackupJson(): String {
    val s = _uiState.value
    return LocalProgressStore.serializeToBackupEnvelopeJson(
      LocalProgressStore.PersistedProgress(
        completedLessonIds = s.completedLessonIds,
        quizBestPercentages = s.quizBestPercentages,
        completedScenarioIds = s.completedScenarioIds,
        displaySettings = s.displaySettings,
      ),
    )
  }

  fun importProgressBackupBytes(rawBytes: ByteArray) {
    _uiState.update { current ->
      when (val res = LocalProgressStore.parseAndValidateBackupJson(rawBytes)) {
        is LocalProgressStore.RestoreResult.Success -> {
          val next = current.copy(
            completedLessonIds = res.progress.completedLessonIds,
            quizBestPercentages = res.progress.quizBestPercentages,
            completedScenarioIds = res.progress.completedScenarioIds,
            displaySettings = res.progress.displaySettings,
            lastBackupStatusAr = "تمت استعادة التقدم المحلي والتحقق من البصمة (${res.sha256.take(12)}…).",
          )
          persistState(next)
          next
        }
        is LocalProgressStore.RestoreResult.Failure -> {
          current.copy(lastBackupStatusAr = "فشلت الاستعادة [${res.code}]: ${res.reasonAr}")
        }
      }
    }
  }

  fun updateFontScale(rawScale: Float) {
    _uiState.update { current ->
      val next = current.copy(
        displaySettings = current.displaySettings.copy(fontScale = LearningEvaluator.clampFontScale(rawScale)),
      )
      persistState(next)
      next
    }
  }

  fun toggleHighContrast(enabled: Boolean) {
    _uiState.update { current ->
      val next = current.copy(displaySettings = current.displaySettings.copy(highContrast = enabled))
      persistState(next)
      next
    }
  }

  fun toggleReducedMotion(enabled: Boolean) {
    _uiState.update { current ->
      val next = current.copy(displaySettings = current.displaySettings.copy(reducedMotion = enabled))
      persistState(next)
      next
    }
  }

  fun setSettingsSheetOpen(open: Boolean) {
    _uiState.update { it.copy(isSettingsSheetOpen = open) }
  }

  override fun onCleared() {
    super.onCleared()
    simLoopJob?.cancel()
  }

  companion object {
    fun factory(appContext: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LearningViewModel(appContext.applicationContext) as T
      }
    }
  }
}
