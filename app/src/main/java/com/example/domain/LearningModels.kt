package com.example.domain

enum class ContentReviewStatus(val labelAr: String) {
  DRAFT("مسودة أولية"),
  REVIEW_REQUIRED("يتطلب مراجعة بشرية"),
  APPROVED("معتمد بعد المراجعة"),
  DEPRECATED("متقادم"),
}

enum class SkillLevel(val labelAr: String) {
  BEGINNER("مبتدئ"),
  INTERMEDIATE("متوسط"),
  ADVANCED("متقدم"),
  SPECIALIST("متخصص"),
}

enum class ContentOrigin(val labelAr: String) {
  BUNDLED("محتوى مدمج"),
  IMPORTED("حزمة مستوردة محلياً"),
}

enum class DroneComponentId(val labelAr: String) {
  FRAME("الهيكل والأذرع"),
  MOTORS("المحركات عديمة الفرش"),
  PROPELLERS("المراوح"),
  FLIGHT_CONTROLLER("وحدة التحكم بالطيران (FC)"),
  ESC("منظمات السرعة (ESC)"),
  BATTERY("البطارية (LiPo)"),
  SENSORS("وحدة المستشعرات الافتراضية"),
}

data class DronePartSlot(
  val id: String,
  val targetComponent: DroneComponentId,
  val titleAr: String,
  val xFraction: Float,
  val yFraction: Float,
)

data class DroneComponentChip(
  val id: String,
  val component: DroneComponentId,
  val labelAr: String,
  val shortSpecAr: String,
  val roleExplanationAr: String,
)

data class PlacementResult(
  val isCorrect: Boolean,
  val messageAr: String,
)

data class SafetyCheckCard(
  val id: String,
  val statementAr: String,
  val expectedIsTrue: Boolean,
  val explanationAr: String,
  val reviewStatus: ContentReviewStatus = ContentReviewStatus.REVIEW_REQUIRED,
)

data class McqOption(
  val id: String,
  val textAr: String,
)

data class MultipleChoiceQuestion(
  val id: String,
  val promptAr: String,
  val options: List<McqOption>,
  val correctOptionId: String,
  val explanationAr: String,
  val reviewStatus: ContentReviewStatus = ContentReviewStatus.DRAFT,
)

data class QuizQuestionResult(
  val question: MultipleChoiceQuestion,
  val selectedOptionId: String?,
  val isCorrect: Boolean,
)

data class QuizSummary(
  val totalQuestions: Int,
  val correctCount: Int,
  val percentage: Int,
  val results: List<QuizQuestionResult>,
  val nonCertificationNoticeAr: String,
)

data class LessonSection(
  val titleAr: String,
  val bodyAr: String,
  val keyTakeawayAr: String,
  val visualDrawableKey: String? = null,
  val visualCaptionAr: String? = null,
  val practicalExampleAr: String? = null,
  val commonMistakeAr: String? = null,
)

data class GlossaryTerm(
  val termAr: String,
  val symbolEn: String,
  val definitionAr: String,
)

data class PhysicsFormula(
  val titleAr: String,
  val equationLtr: String,
  val unitsNoteAr: String,
  val explanationAr: String,
)

data class LessonUnit(
  val id: String,
  val trackId: String,
  val titleAr: String,
  val level: SkillLevel,
  val estimatedMinutes: Int,
  val prerequisiteLessonIds: List<String>,
  val reviewStatus: ContentReviewStatus,
  val origin: ContentOrigin = ContentOrigin.BUNDLED,
  val sourceNoteAr: String,
  val objectivesAr: List<String>,
  val sections: List<LessonSection>,
  val equations: List<PhysicsFormula>,
  val glossary: List<GlossaryTerm>,
  val includeComponentDiagram: Boolean = false,
  val safetyCards: List<SafetyCheckCard>,
  val conceptMcqs: List<MultipleChoiceQuestion>,
  val reviewQuiz: List<MultipleChoiceQuestion>,
  val summaryAr: String,
)

data class CurriculumTrack(
  val id: String,
  val number: Int,
  val titleAr: String,
  val subtitleAr: String,
  val targetLevel: SkillLevel,
  val lessons: List<LessonUnit>,
)

data class InteractiveModuleData(
  val moduleId: String,
  val trackTitleAr: String,
  val lessonTitleAr: String,
  val levelLabelAr: String,
  val reviewStatus: ContentReviewStatus,
  val sourceNoteAr: String,
  val learningObjectivesAr: List<String>,
  val diagramPromptAr: String,
  val slots: List<DronePartSlot>,
  val chips: List<DroneComponentChip>,
  val safetyCards: List<SafetyCheckCard>,
  val conceptMcqs: List<MultipleChoiceQuestion>,
  val reviewQuiz: List<MultipleChoiceQuestion>,
)

object LearningEvaluator {
  fun clampFontScale(raw: Float): Float = raw.coerceIn(0.8f, 1.5f)

  fun isLessonUnlocked(
    lesson: LessonUnit,
    completedLessonIds: Set<String>,
  ): Boolean = lesson.prerequisiteLessonIds.all { it in completedLessonIds }

  fun validateComponentPlacement(
    slot: DronePartSlot,
    chip: DroneComponentChip,
  ): PlacementResult {
    return if (slot.targetComponent == chip.component) {
      PlacementResult(
        isCorrect = true,
        messageAr = "أحسنت! تم تثبيت «${chip.labelAr}» في موضع «${slot.titleAr}». ${chip.roleExplanationAr}",
      )
    } else {
      PlacementResult(
        isCorrect = false,
        messageAr = "الموضع غير مطابق: «${chip.labelAr}» لا يُركّب في «${slot.titleAr}». تلميح: ${chip.roleExplanationAr}",
      )
    }
  }

  fun gradeSectionQuiz(
    questions: List<MultipleChoiceQuestion>,
    selectedAnswers: Map<String, String>,
  ): QuizSummary {
    val results = questions.map { question ->
      val chosen = selectedAnswers[question.id]
      QuizQuestionResult(
        question = question,
        selectedOptionId = chosen,
        isCorrect = chosen == question.correctOptionId,
      )
    }
    val correctCount = results.count { it.isCorrect }
    val percentage = if (questions.isEmpty()) 0 else (correctCount * 100) / questions.size
    return QuizSummary(
      totalQuestions = questions.size,
      correctCount = correctCount,
      percentage = percentage,
      results = results,
      nonCertificationNoticeAr =
        "تنبيه: هذه النتيجة للتقييم الذاتي التعليمي فقط ولا تمثل كفاءة تشغيلية أو رخصة طيران معتمدة.",
    )
  }
}
