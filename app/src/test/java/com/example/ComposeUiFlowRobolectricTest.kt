package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxxhdpi")
class ComposeUiFlowRobolectricTest {

  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `home screen shows civilian disclaimer and opens academy wiki and coach tabs`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("foundation_list")
      .performScrollToNode(hasTestTag("civilian_safety_disclaimer_card"))
    composeRule.onNodeWithTag("civilian_safety_disclaimer_card").performScrollTo().assertIsDisplayed()

    composeRule.onNodeWithTag("foundation_list")
      .performScrollToNode(hasTestTag("open_learning_module_button"))
    composeRule.onNodeWithTag("open_learning_module_button").performScrollTo().performClick()
    composeRule.waitForIdle()

    // Courses tab is active initially
    composeRule.onNodeWithTag("academy_tab_COURSES").assertIsDisplayed()

    // Switch to Offline Wiki & Calculators
    composeRule.onNodeWithTag("academy_tab_WIKI_AND_CALCULATORS").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("wiki_list")
      .performScrollToNode(hasTestTag("wiki_search_field"))
    composeRule.onNodeWithTag("wiki_search_field").performScrollTo().performTextInput("فرينل")
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("wiki_list")
      .performScrollToNode(hasTestTag("wiki_calc_powertrain"))
    composeRule.onNodeWithTag("wiki_calc_powertrain").performScrollTo().assertIsDisplayed()

    // Switch to Offline Socratic Coach
    composeRule.onNodeWithTag("academy_tab_STUDY_COACH").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("coach_recommendation_card").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `simulator hub executes visual sandbox and flags insufficient evidence logs`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("foundation_list")
      .performScrollToNode(hasTestTag("quick_sim_button"))
    composeRule.onNodeWithTag("quick_sim_button").performScrollTo().performClick()
    composeRule.waitForIdle()

    // Verify 3D survey gates scenario chip exists in Flight Sim horizontal bar
    composeRule.onNodeWithTag("scenario_chip_scen_survey_gates").performScrollTo().assertIsDisplayed()

    // Switch to Visual Programming Sandbox & run preset program
    composeRule.onNodeWithTag("sim_tab_VISUAL_CODE").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("visual_code_list")
      .performScrollToNode(hasTestTag("run_sandbox_program_button"))
    composeRule.onNodeWithTag("run_sandbox_program_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("visual_code_list")
      .performScrollToNode(hasTestTag("sandbox_result_card"))
    composeRule.onNodeWithTag("sandbox_result_card").performScrollTo().assertIsDisplayed()

    // Switch to Log Analyzer and verify incomplete log reports literal insufficient evidence
    composeRule.onNodeWithTag("sim_tab_LOG_ANALYZER").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("log_analyzer_list")
      .performScrollToNode(hasTestTag("test_incomplete_log_button"))
    composeRule.onNodeWithTag("test_incomplete_log_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("log_analyzer_list")
      .performScrollToNode(hasTestTag("log_analysis_report_card"))
    composeRule.onNodeWithTag("log_analysis_report_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("الأدلة غير كافية.").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `workshop diagnostics reports literal insufficient evidence on missing data`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("nav_bar_WORKSHOP_GOVERNANCE").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("workshop_list")
      .performScrollToNode(hasTestTag("run_insufficient_diagnostic_button"))
    composeRule.onNodeWithTag("run_insufficient_diagnostic_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("workshop_list")
      .performScrollToNode(hasTestTag("diagnostic_verdict_card"))
    composeRule.onNodeWithTag("diagnostic_verdict_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("الأدلة غير كافية.").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `drone 3d lab renders gl viewport and opens educational bottom sheet on part selection`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("foundation_list")
      .performScrollToNode(hasTestTag("home_3d_part_BATTERY"))
    composeRule.onNodeWithTag("home_3d_part_BATTERY").performScrollTo().performClick()
    composeRule.waitForIdle()

    // BottomSheet opens immediately for the selected Battery part
    composeRule.onNodeWithTag("part_educational_bottom_sheet").assertIsDisplayed()
    composeRule.onNodeWithTag("sheet_part_title").assertIsDisplayed()
    composeRule.onNodeWithTag("close_part_sheet_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("part_educational_bottom_sheet").assertDoesNotExist()

    // Verify GLSurfaceView viewport, floating HUD controls (wireframe toggle + camera reset), and separated part cards
    composeRule.onNodeWithTag("gl_quadrotor_viewport").assertIsDisplayed()
    composeRule.onNodeWithTag("floating_gl_controls_bar").assertIsDisplayed()
    composeRule.onNodeWithTag("floating_toggle_render_mode_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("سلكي • 60%").assertIsDisplayed()
    composeRule.onNodeWithTag("floating_reset_camera_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("floating_toggle_render_mode_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("ملون • 60%").assertIsDisplayed()

    composeRule.onNodeWithTag("drone_3d_lab_list")
      .performScrollToNode(hasTestTag("primary_part_card_MOTORS"))
    composeRule.onNodeWithTag("primary_part_card_MOTORS").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("primary_part_card_PROPELLERS").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("primary_part_card_BATTERY").performScrollTo().assertIsDisplayed()

    // Tap Motors primary card -> opens BottomSheet for Motors, isolate inside sheet, then close
    composeRule.onNodeWithTag("primary_part_card_MOTORS").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("part_educational_bottom_sheet").assertIsDisplayed()
    composeRule.onNodeWithTag("isolate_from_sheet_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("close_part_sheet_button").performScrollTo().performClick()
    composeRule.waitForIdle()

    // Restore all parts from the 3D viewport HUD, verify highlight chip, and clear glow highlight
    composeRule.onNodeWithTag("gl_highlight_halo_chip").assertIsDisplayed()
    composeRule.onNodeWithTag("drone_3d_lab_list")
      .performScrollToNode(hasTestTag("show_all_3d_parts_button"))
    composeRule.onNodeWithTag("show_all_3d_parts_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("floating_clear_highlight_button").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("floating_clear_highlight_button").assertDoesNotExist()
  }

  @Test
  fun `simulator displays flight pad buttons and rotates drone right and left on tap`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("foundation_list")
      .performScrollToNode(hasTestTag("quick_sim_button"))
    composeRule.onNodeWithTag("quick_sim_button").performScrollTo().performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("flight_sim_list")
      .performScrollToNode(hasTestTag("sim_flight_pad_card"))
    composeRule.onNodeWithTag("sim_flight_pad_card").performScrollTo().assertIsDisplayed()

    composeRule.onNodeWithTag("sim_btn_throttle_up").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("sim_btn_yaw_right").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("-0.19 rad").assertIsDisplayed()

    composeRule.onNodeWithTag("sim_btn_yaw_left").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("sim_btn_yaw_left").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("0.19 rad").assertIsDisplayed()

    composeRule.onNodeWithTag("sim_btn_pitch_forward").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("sim_btn_roll_right").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("sim_btn_roll_left").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("sim_btn_pitch_backward").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("sim_btn_throttle_down").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("sim_btn_center_axes").performScrollTo().performClick()
    composeRule.waitForIdle()
  }
}
