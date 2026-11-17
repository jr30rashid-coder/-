package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LearningDestination
import com.example.ui.LearningUiState
import com.example.ui.LearningViewModel
import com.example.ui.components.AccessibilitySettingsSheet
import com.example.ui.screens.AcademyScreen
import com.example.ui.screens.Drone3DLabScreen
import com.example.ui.screens.FoundationScreen
import com.example.ui.screens.SimulatorAndCodeScreen
import com.example.ui.screens.WorkshopAndGovernanceScreen
import com.example.ui.theme.Drones3DTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val learningViewModel: LearningViewModel = viewModel(
        factory = LearningViewModel.factory(applicationContext),
      )
      val uiState by learningViewModel.uiState.collectAsStateWithLifecycle()

      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Drones3DTheme(
          highContrast = uiState.displaySettings.highContrast,
          fontScale = uiState.displaySettings.fontScale,
        ) {
          Drones3DAppShell(
            uiState = uiState,
            viewModel = learningViewModel,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Drones3DAppShell(
  uiState: LearningUiState,
  viewModel: LearningViewModel,
) {
  if (uiState.destination != LearningDestination.FOUNDATION && uiState.activeLesson == null) {
    BackHandler { viewModel.selectDestination(LearningDestination.FOUNDATION) }
  }

  val navItems = listOf(
    Triple(LearningDestination.FOUNDATION, "الرئيسية", Icons.Filled.Home to Icons.Outlined.Home),
    Triple(LearningDestination.ACADEMY, "المدرسة", Icons.Filled.School to Icons.Outlined.School),
    Triple(LearningDestination.DRONE_3D_LAB, "3D", Icons.Filled.ViewInAr to Icons.Outlined.ViewInAr),
    Triple(LearningDestination.SIMULATOR_HUB, "المحاكي", Icons.Filled.FlightTakeoff to Icons.Outlined.FlightTakeoff),
    Triple(LearningDestination.WORKSHOP_GOVERNANCE, "الورشة", Icons.Filled.Build to Icons.Outlined.Build),
  )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        CenterAlignedTopAppBar(
          title = { Text(stringResource(R.string.app_name)) },
          actions = {
            IconButton(
              onClick = { viewModel.setSettingsSheetOpen(true) },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("top_accessibility_button"),
            ) {
              Icon(
                imageVector = Icons.Default.SettingsAccessibility,
                contentDescription = stringResource(R.string.cd_accessibility_button),
              )
            }
          },
        )
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar {
            navItems.forEach { (destination, label, icons) ->
              val selected = uiState.destination == destination
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectDestination(destination) },
                icon = { Icon(if (selected) icons.first else icons.second, contentDescription = null) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("nav_bar_${destination.name}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavigationRail) {
          NavigationRail {
            navItems.forEach { (destination, label, icons) ->
              val selected = uiState.destination == destination
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectDestination(destination) },
                icon = { Icon(if (selected) icons.first else icons.second, contentDescription = null) },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_rail_${destination.name}"),
              )
            }
          }
        }

        Box(
          contentAlignment = Alignment.TopCenter,
          modifier = Modifier.fillMaxSize(),
        ) {
          val contentModifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)

          when (uiState.destination) {
            LearningDestination.FOUNDATION -> {
              FoundationScreen(
                uiState = uiState,
                onNavigate = viewModel::selectDestination,
                onOpen3DPart = viewModel::open3DLabWithPart,
                onOpenSettings = { viewModel.setSettingsSheetOpen(true) },
                modifier = contentModifier,
              )
            }

            LearningDestination.ACADEMY -> {
              AcademyScreen(
                uiState = uiState,
                onSelectAcademyTab = viewModel::selectAcademyTab,
                onSearchWikiChange = viewModel::updateWikiSearchQuery,
                onToggleCoachClue = viewModel::toggleCoachClue,
                onToggleCoachDerivation = viewModel::toggleCoachDerivation,
                onSelectLevelFilter = viewModel::selectLevelFilter,
                onOpenLesson = viewModel::openLesson,
                onCloseLesson = viewModel::closeActiveLesson,
                onSelectStep = viewModel::selectModuleStep,
                onSelectChipForTap = viewModel::selectChipForTap,
                onPlaceComponent = viewModel::placeChipOnSlot,
                onRemovePlacedComponent = viewModel::removePlacedComponent,
                onResetDragAndDrop = viewModel::resetDragAndDrop,
                onAnswerSafetyCard = viewModel::answerSafetyCard,
                onAnswerConceptMcq = viewModel::answerConceptMcq,
                onSelectReviewQuizOption = viewModel::selectReviewQuizOption,
                onSubmitReviewQuiz = viewModel::submitReviewQuiz,
                onRetryReviewQuiz = viewModel::retryReviewQuiz,
                onMarkLessonCompleted = viewModel::markLessonCompleted,
                onImportPackBytes = viewModel::importContentPackBytes,
                modifier = contentModifier,
              )
            }

            LearningDestination.DRONE_3D_LAB -> {
              Drone3DLabScreen(
                uiState = uiState,
                onExplodeChange = viewModel::updateGlExplode,
                onZoomChange = viewModel::updateGlZoom,
                onSelectPart = viewModel::selectGlPart,
                onOpenPartSheet = viewModel::openGlPartSheet,
                onDismissPartSheet = viewModel::dismissGlPartSheet,
                onToggleVisibility = viewModel::toggleGlPartVisibility,
                onToggleIsolatePart = viewModel::toggleIsolateGlPart,
                onShowAllParts = viewModel::showAllGlParts,
                onToggleSpinPropellers = viewModel::toggleGlSpinPropellers,
                onToggleRenderMode = viewModel::toggleGlRenderMode,
                onResetCamera = viewModel::resetGlCameraOnly,
                onResetView = viewModel::resetGlView,
                modifier = contentModifier,
              )
            }

            LearningDestination.SIMULATOR_HUB -> {
              SimulatorAndCodeScreen(
                uiState = uiState,
                onSelectTab = viewModel::selectSimulatorTab,
                onSelectCameraMode = viewModel::selectSimCameraMode,
                onResetCamera = viewModel::resetSimCamera,
                onUpdateSimInput = viewModel::updateSimInput,
                onStartSimIfIdle = viewModel::startSimulationIfIdle,
                onSetFlightHoldAxis = viewModel::setFlightHoldAxis,
                onAdjustThrottle = viewModel::adjustThrottleBy,
                onNudgeSimControl = viewModel::nudgeSimControl,
                onToggleSimRunning = viewModel::toggleSimulationRunning,
                onStepSimOnce = viewModel::stepSimulationOnce,
                onResetSim = viewModel::resetSimulation,
                onVerifyReplay = viewModel::verifyDeterministicReplay,
                onSelectScenario = viewModel::selectTrainingScenario,
                onAddProgramBlock = viewModel::addProgramBlock,
                onRemoveLastProgramBlock = viewModel::removeLastProgramBlock,
                onLoadPresetProgram = viewModel::loadPresetProgram,
                onRunVisualProgram = viewModel::runVisualProgram,
                onAnalyzeCurrentLog = viewModel::analyzeCurrentSimLog,
                onTestIncompleteLog = viewModel::testIncompleteLogAnalysis,
                modifier = contentModifier,
              )
            }

            LearningDestination.WORKSHOP_GOVERNANCE -> {
              WorkshopAndGovernanceScreen(
                uiState = uiState,
                onSelectTab = viewModel::selectGovernanceTab,
                onSelectWorkshopParts = viewModel::selectWorkshopParts,
                onRunDiagnostic = viewModel::runDiagnosticEvaluation,
                onFilterRegulations = viewModel::filterRegulationsByCountry,
                onExportBackupJson = viewModel::exportProgressBackupJson,
                onImportBackupBytes = viewModel::importProgressBackupBytes,
                modifier = contentModifier,
              )
            }
          }
        }
      }
    }
  }

  if (uiState.isSettingsSheetOpen) {
    AccessibilitySettingsSheet(
      settings = uiState.displaySettings,
      onUpdateFontScale = viewModel::updateFontScale,
      onToggleHighContrast = viewModel::toggleHighContrast,
      onToggleReducedMotion = viewModel::toggleReducedMotion,
      onDismiss = { viewModel.setSettingsSheetOpen(false) },
    )
  }
}
