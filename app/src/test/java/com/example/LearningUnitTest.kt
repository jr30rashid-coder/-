package com.example

import com.example.data.FullCurriculumCatalog
import com.example.data.LocalCurriculumRepository
import com.example.data.RegulationsAndWorkshopData
import com.example.domain.DroneComponentId
import com.example.domain.DronePhysicsEngine
import com.example.domain.EngineeringWikiAndCoach
import com.example.domain.LearningEvaluator
import com.example.domain.SafetyWorkshopAndLogs
import com.example.domain.VisualProgrammingSandbox
import com.example.ui.AcademyTab
import com.example.ui.LearningViewModel
import com.example.ui.components.PartVisualAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningUnitTest {

  @Test
  fun `full 8-track curriculum exists and enforces prerequisites`() {
    val tracks = FullCurriculumCatalog.getBundledTracks()
    assertEquals(8, tracks.size)
    assertEquals(15, tracks.sumOf { it.lessons.size })
    val t1l1 = FullCurriculumCatalog.findLesson(tracks, "t1_l1")!!
    val t1l2 = FullCurriculumCatalog.findLesson(tracks, "t1_l2")!!
    val t2l3 = FullCurriculumCatalog.findLesson(tracks, "t2_l3")!!
    val t3l3 = FullCurriculumCatalog.findLesson(tracks, "t3_l3")!!
    assertTrue(LearningEvaluator.isLessonUnlocked(t1l1, emptySet()))
    assertFalse(LearningEvaluator.isLessonUnlocked(t1l2, emptySet()))
    assertTrue(LearningEvaluator.isLessonUnlocked(t1l2, setOf("t1_l1")))
    assertFalse(LearningEvaluator.isLessonUnlocked(t2l3, emptySet()))
    assertTrue(LearningEvaluator.isLessonUnlocked(t2l3, setOf("t2_l2")))
    assertFalse(LearningEvaluator.isLessonUnlocked(t3l3, emptySet()))
    assertTrue(LearningEvaluator.isLessonUnlocked(t3l3, setOf("t3_l2")))

    val drawables = DroneComponentId.entries.map { PartVisualAssets.forComponent(it).drawableRes }.toSet()
    assertEquals(7, drawables.size)
  }

  @Test
  fun `component placement validation accepts matching target and rejects mismatch`() {
    val module = LocalCurriculumRepository.getPrototypeModule()
    val fcSlot = module.slots.first { it.id == "slot_fc" }
    val fcChip = module.chips.first { it.id == "chip_fc" }
    val batteryChip = module.chips.first { it.id == "chip_battery" }

    assertTrue(LearningEvaluator.validateComponentPlacement(fcSlot, fcChip).isCorrect)
    assertFalse(LearningEvaluator.validateComponentPlacement(fcSlot, batteryChip).isCorrect)
  }

  @Test
  fun `font scale clamps strictly between 80 percent and 150 percent`() {
    assertEquals(0.8f, LearningEvaluator.clampFontScale(0.5f), 0.001f)
    assertEquals(1.2f, LearningEvaluator.clampFontScale(1.2f), 0.001f)
    assertEquals(1.5f, LearningEvaluator.clampFontScale(2.0f), 0.001f)
  }

  @Test
  fun `fixed-step simulation is deterministic with wind and evaluates scenario`() {
    val inputs = List(180) { step ->
      val throttle = if (step < 55) 0.72f else 0.49f
      DronePhysicsEngine.SimControlInput(armed = true, throttle = throttle, windEnabled = true)
    }
    val run1 = DronePhysicsEngine.replaySession(inputs, seed = 999L)
    val run2 = DronePhysicsEngine.replaySession(inputs, seed = 999L)
    assertEquals(run1, run2)
    assertTrue(run1.maxOf { it.altitudeM } > 2.0f)

    // Scenario hover test
    val scenario = DronePhysicsEngine.getTrainingScenarios().first { it.id == "scen_hover_5m" }
    var hold = 0f
    var completed = false
    val hoverState = DronePhysicsEngine.SimState(altitudeM = 5.0f, armed = true)
    repeat(160) {
      val (nextHold, eval) = DronePhysicsEngine.evaluateScenarioProgress(scenario, hoverState, hold)
      hold = nextHold
      if (eval.completed) completed = true
    }
    assertTrue(completed)
  }

  @Test
  fun `visual programming sandbox enforces bounds and runs safe preset`() {
    val safeResult = VisualProgrammingSandbox.execute(VisualProgrammingSandbox.samplePresetTakeoffAndHover())
    assertFalse(safeResult.haltedBySafetyOrLimit)
    assertTrue(safeResult.executedSteps > 50)
    assertTrue(safeResult.samples.isNotEmpty())

    val infiniteLoopAttempt = listOf(
      VisualProgrammingSandbox.AstNode.RepeatBounded(
        times = 99,
        body = listOf(VisualProgrammingSandbox.AstNode.WaitSteps(5)),
      ),
    )
    val validation = VisualProgrammingSandbox.validateProgram(infiniteLoopAttempt)
    assertFalse(validation.isValid)

    // Excessive nesting depth > 4
    val deepAst = listOf(
      VisualProgrammingSandbox.AstNode.RepeatBounded(
        times = 2,
        body = listOf(
          VisualProgrammingSandbox.AstNode.RepeatBounded(
            times = 2,
            body = listOf(
              VisualProgrammingSandbox.AstNode.RepeatBounded(
                times = 2,
                body = listOf(
                  VisualProgrammingSandbox.AstNode.RepeatBounded(
                    times = 2,
                    body = listOf(
                      VisualProgrammingSandbox.AstNode.RepeatBounded(
                        times = 2,
                        body = listOf(VisualProgrammingSandbox.AstNode.SafeDisarm),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
    assertFalse(VisualProgrammingSandbox.validateProgram(deepAst).isValid)
  }

  @Test
  fun `regulations are gated non-approved and diagnostics or logs report literal insufficient evidence`() {
    RegulationsAndWorkshopData.getRegulatoryRules().forEach { rule ->
      assertFalse(rule.isDisplayableAsCertified)
    }

    val diag = SafetyWorkshopAndLogs.evaluateDiagnosticEvidence(
      SafetyWorkshopAndLogs.DiagnosticEvidenceInput(
        symptomAr = "عَرَض ناقص",
        batteryVoltageMeasuredV = null,
        propInspectionDone = false,
        motorSpinBalanced = null,
      ),
    )
    assertFalse(diag.conclusive)
    assertEquals(SafetyWorkshopAndLogs.INSUFFICIENT_EVIDENCE_LITERAL, diag.headlineAr)

    val shortLog = SafetyWorkshopAndLogs.analyzeFlightLog(emptyList())
    assertFalse(shortLog.conclusive)
    assertEquals(SafetyWorkshopAndLogs.INSUFFICIENT_EVIDENCE_LITERAL, shortLog.headlineAr)
  }

  @Test
  fun `viewModel manages quiz lifecycle and progress updates`() {
    val vm = LearningViewModel()
    vm.openLesson("t1_l1")
    val lesson = vm.uiState.value.activeLesson!!
    lesson.reviewQuiz.forEach { q -> vm.selectReviewQuizOption(q.id, q.correctOptionId) }
    vm.submitReviewQuiz()

    val summary = vm.uiState.value.reviewQuizSummary
    assertNotNull(summary)
    assertEquals(100, summary?.percentage)
    assertTrue("t1_l1" in vm.uiState.value.completedLessonIds)

    vm.retryReviewQuiz()
    assertNull(vm.uiState.value.reviewQuizSummary)
  }

  @Test
  fun `offline wiki scientific calculators and socratic coach return valid engineering bounds`() {
    val link = EngineeringWikiAndCoach.calculateLinkBudgetAndFresnel(
      band = EngineeringWikiAndCoach.RadioBandOption.BAND_2400,
      distanceM = 500f,
      txPowerMw = 100f,
    )
    assertTrue(link.wavelengthM in 0.11f..0.14f)
    assertTrue(link.fsplDb in 90f..98f)
    assertTrue(link.fresnelRadiusM > 3.5f)
    assertEquals(20.0f, link.txDbm, 0.1f)

    val seaLevel = EngineeringWikiAndCoach.calculateAirDensityAndThrustRatio(0f, 15f)
    val highHot = EngineeringWikiAndCoach.calculateAirDensityAndThrustRatio(2000f, 40f)
    assertEquals(1.225f, seaLevel.densityKgM3, 0.01f)
    assertTrue(highHot.densityKgM3 < 0.95f)
    assertTrue(highHot.relativeRatioPercent < 80f)

    val powertrain = EngineeringWikiAndCoach.calculateHoverAndPowertrainBudget(
      totalMassKg = 1.4f,
      maxThrustPerMotorN = 7.0f,
      batteryCellsS = 4,
      batteryCapacityMah = 3000,
      hoverCurrentA = 14.0f,
    )
    assertEquals(2.04f, powertrain.twr, 0.05f)
    assertTrue(powertrain.estimatedHoverMin > 9f)

    assertTrue(EngineeringWikiAndCoach.getSocraticTopics().size >= 5)
    assertTrue(EngineeringWikiAndCoach.getWikiReferenceEntries().size >= 7)
    val nextHint = EngineeringWikiAndCoach.recommendNextStudyStepAr(emptySet(), 15, emptySet())
    assertTrue(nextHint.contains("1.1"))
  }

  @Test
  fun `3d survey waypoint gates scenario progresses sequentially to completion`() {
    val gatesScenario = DronePhysicsEngine.getTrainingScenarios().first { it.id == "scen_survey_gates" }
    assertEquals(3, gatesScenario.gates.size)

    var cleared = 0
    gatesScenario.gates.forEachIndexed { idx, gate ->
      val atGate = DronePhysicsEngine.SimState(
        xM = gate.xM,
        yM = gate.yM,
        altitudeM = gate.altitudeM,
        armed = true,
      )
      val (_, eval) = DronePhysicsEngine.evaluateScenarioProgress(gatesScenario, atGate, 0f, cleared)
      cleared = eval.clearedGateCount
      assertEquals(idx + 1, cleared)
      assertEquals(idx == gatesScenario.gates.lastIndex, eval.completed)
    }

    val vm = LearningViewModel()
    vm.selectAcademyTab(AcademyTab.STUDY_COACH)
    assertEquals(AcademyTab.STUDY_COACH, vm.uiState.value.academyTab)
    vm.toggleCoachClue("coach_tilt_altitude")
    assertTrue("coach_tilt_altitude" in vm.uiState.value.coachRevealedClues)
    vm.updateWikiSearchQuery("فرينل")
    assertEquals("فرينل", vm.uiState.value.wikiSearchQuery)
  }

  @Test
  fun `3d lab separates major assemblies and isolates motors battery or propellers`() {
    val vm = LearningViewModel()
    assertTrue(vm.uiState.value.glExplodeFactor >= 0.5f)
    assertFalse(vm.uiState.value.glPartSheetOpen)

    vm.open3DLabWithPart(DroneComponentId.BATTERY)
    assertEquals(com.example.ui.LearningDestination.DRONE_3D_LAB, vm.uiState.value.destination)
    assertEquals(DroneComponentId.BATTERY, vm.uiState.value.glSelectedPart)
    assertTrue(vm.uiState.value.glPartSheetOpen)

    vm.dismissGlPartSheet()
    assertFalse(vm.uiState.value.glPartSheetOpen)
    assertEquals(DroneComponentId.BATTERY, vm.uiState.value.glSelectedPart)

    vm.selectGlPart(DroneComponentId.PROPELLERS)
    assertEquals(DroneComponentId.PROPELLERS, vm.uiState.value.glSelectedPart)
    assertTrue(vm.uiState.value.glPartSheetOpen)

    vm.toggleIsolateGlPart(DroneComponentId.MOTORS)
    assertEquals(DroneComponentId.MOTORS, vm.uiState.value.glSelectedPart)
    assertFalse(DroneComponentId.MOTORS in vm.uiState.value.glHiddenParts)
    assertTrue(DroneComponentId.BATTERY in vm.uiState.value.glHiddenParts)
    assertTrue(DroneComponentId.PROPELLERS in vm.uiState.value.glHiddenParts)

    vm.showAllGlParts()
    assertTrue(vm.uiState.value.glHiddenParts.isEmpty())

    assertEquals(com.example.ui.gl.GlRenderMode.SHADED, vm.uiState.value.glRenderMode)
    vm.toggleGlRenderMode()
    assertEquals(com.example.ui.gl.GlRenderMode.WIREFRAME, vm.uiState.value.glRenderMode)
    vm.updateGlZoom(1.5f)
    val prevSignal = vm.uiState.value.glResetSignal
    vm.resetGlCameraOnly()
    assertEquals(1.0f, vm.uiState.value.glZoomFactor, 0.001f)
    assertEquals(prevSignal + 1, vm.uiState.value.glResetSignal)
    assertEquals(DroneComponentId.MOTORS, vm.uiState.value.glSelectedPart)
  }

  @Test
  fun `simulator control buttons rotate drone right and left and step climb or pitch`() {
    val vm = LearningViewModel()
    assertEquals(0f, vm.uiState.value.simState.yawRad, 0.0001f)

    // Rotate right (clockwise -> negative yawRad)
    vm.nudgeSimControl(yawRateCmd = -0.85f)
    val yawAfterRight = vm.uiState.value.simState.yawRad
    assertTrue(vm.uiState.value.simInput.armed)
    assertTrue("Expected yawRad < 0 after rotating right, got $yawAfterRight", yawAfterRight < -0.05f)
    assertEquals(0f, vm.uiState.value.simInput.yawRateCmd, 0.0001f)

    // Rotate left twice (counter-clockwise -> positive yawRad delta)
    vm.nudgeSimControl(yawRateCmd = 0.85f)
    vm.nudgeSimControl(yawRateCmd = 0.85f)
    val yawAfterLeft = vm.uiState.value.simState.yawRad
    assertTrue("Expected yawRad > 0 after rotating left twice, got $yawAfterLeft", yawAfterLeft > 0.05f)
    assertEquals(0f, vm.uiState.value.simInput.yawRateCmd, 0.0001f)

    // Climb throttle steps auto-arm and increase altitude cleanly
    vm.adjustThrottleBy(0.10f)
    assertTrue(vm.uiState.value.simInput.throttle >= 0.55f)
    vm.adjustThrottleBy(0.15f)
    assertTrue(vm.uiState.value.simState.altitudeM > 0f)

    // Pitch forward steps tilt and resets transient command
    vm.nudgeSimControl(pitchCmd = 0.75f)
    assertTrue(vm.uiState.value.simState.pitchRad > 0.01f)
    assertEquals(0f, vm.uiState.value.simInput.pitchCmd, 0.0001f)
  }

  @Test
  fun `gl selection highlight halo computes valid bounds and distinct component glow hues`() {
    val resting = com.example.ui.gl.GlHighlightHaloSpec.computeHaloMetrics(pulse01 = 0f, burst01 = 0f)
    val peaked = com.example.ui.gl.GlHighlightHaloSpec.computeHaloMetrics(pulse01 = 1f, burst01 = 1f)
    assertTrue(peaked.innerScaleMultiplier > resting.innerScaleMultiplier)
    assertTrue(peaked.outerScaleMultiplier > peaked.innerScaleMultiplier)
    assertTrue(peaked.innerAlpha in 0.35f..0.95f)
    assertTrue(peaked.outerAlpha in 0.15f..0.75f)
    assertTrue(peaked.contourLineWidth > resting.contourLineWidth)

    val motorsRgb = com.example.ui.gl.GlHighlightHaloSpec.haloColorRgb(DroneComponentId.MOTORS)
    val batteryRgb = com.example.ui.gl.GlHighlightHaloSpec.haloColorRgb(DroneComponentId.BATTERY)
    val propsRgb = com.example.ui.gl.GlHighlightHaloSpec.haloColorRgb(DroneComponentId.PROPELLERS)
    assertTrue(motorsRgb != batteryRgb && batteryRgb != propsRgb)
  }
}
