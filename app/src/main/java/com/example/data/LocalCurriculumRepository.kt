package com.example.data

import com.example.domain.ContentReviewStatus
import com.example.domain.DroneComponentChip
import com.example.domain.DroneComponentId
import com.example.domain.DronePartSlot
import com.example.domain.InteractiveModuleData
import com.example.domain.McqOption
import com.example.domain.MultipleChoiceQuestion
import com.example.domain.SafetyCheckCard

/**
 * Bundled, strictly offline educational prototype module.
 * Marked DRAFT / REVIEW_REQUIRED until qualified human curriculum & safety review is recorded.
 */
object LocalCurriculumRepository {

  fun getPrototypeModule(): InteractiveModuleData = InteractiveModuleData(
    moduleId = "track1_lesson0_proto",
    trackTitleAr = "المسار 1: أساسيات المنظومات الجوية المسيّرة المدنية",
    lessonTitleAr = "مكونات الطائرة رباعية المراوح وقواعد السلامة الأولية",
    levelLabelAr = "المستوى: مبتدئ (وحدة تجريبية)",
    reviewStatus = ContentReviewStatus.DRAFT,
    sourceNoteAr =
      "محتوى تعليمي أولي بوحدات النظام الدولي (SI) للاستخدام المدني فقط؛ لم تُستكمل المراجعة البشرية الرسمية بعد.",
    learningObjectivesAr = listOf(
      "تمييز المكونات الرئيسة الستة في منظومة طائرة رباعية المراوح افتراضية.",
      "التحقق من مبادئ السلامة المدنية والحدود النظامية الأساسية (خط البصر VLOS).",
      "الربط بين الطاقة الكهربائية (V / A) والدفع الميكانيكي (N) دون ربط بأي جهاز فعلي.",
    ),
    diagramPromptAr =
      "اسحب كل بطاقة مكوّن إلى موضعه الصحيح على المخطط الهندسي العلوي (أو اضغط على البطاقة ثم الموضع لإسقاطها):",
    slots = listOf(
      DronePartSlot(
        id = "slot_propeller",
        targetComponent = DroneComponentId.PROPELLERS,
        titleAr = "الشفرة الدوّارة (أعلى اليسار)",
        xFraction = 0.21f,
        yFraction = 0.20f,
      ),
      DronePartSlot(
        id = "slot_motor",
        targetComponent = DroneComponentId.MOTORS,
        titleAr = "محور الدفع (أعلى اليمين)",
        xFraction = 0.79f,
        yFraction = 0.20f,
      ),
      DronePartSlot(
        id = "slot_fc",
        targetComponent = DroneComponentId.FLIGHT_CONTROLLER,
        titleAr = "مركز المنظومة",
        xFraction = 0.50f,
        yFraction = 0.44f,
      ),
      DronePartSlot(
        id = "slot_esc",
        targetComponent = DroneComponentId.ESC,
        titleAr = "ذراع التغذية (أسفل اليسار)",
        xFraction = 0.27f,
        yFraction = 0.72f,
      ),
      DronePartSlot(
        id = "slot_frame",
        targetComponent = DroneComponentId.FRAME,
        titleAr = "الذراع الهيكلي (أسفل اليمين)",
        xFraction = 0.73f,
        yFraction = 0.72f,
      ),
      DronePartSlot(
        id = "slot_battery",
        targetComponent = DroneComponentId.BATTERY,
        titleAr = "حجرة الطاقة (الذيل)",
        xFraction = 0.50f,
        yFraction = 0.85f,
      ),
    ),
    chips = listOf(
      DroneComponentChip(
        id = "chip_fc",
        component = DroneComponentId.FLIGHT_CONTROLLER,
        labelAr = "وحدة التحكم (FC)",
        shortSpecAr = "حساب الاتزان بالراديان (rad)",
        roleExplanationAr = "وحدة التحكم تستقر في مركز الهيكل لتقرأ المستشعرات الافتراضية وتوازن المحاور.",
      ),
      DronePartSlotChipFactory.propellers(),
      DronePartSlotChipFactory.motors(),
      DronePartSlotChipFactory.esc(),
      DronePartSlotChipFactory.frame(),
      DronePartSlotChipFactory.battery(),
    ),
    safetyCards = listOf(
      SafetyCheckCard(
        id = "safe_1",
        statementAr = "في التشغيل المدني الأساسي، يجب إبقاء الطائرة المسيّرة ضمن خط البصر المباشر (VLOS) طوال الوقت.",
        expectedIsTrue = true,
        explanationAr = "صحيح: الطيران ضمن خط البصر المباشر يتيح للمشغل مراقبة العوائق والمجال الجوي المحيط بأمان.",
        reviewStatus = ContentReviewStatus.REVIEW_REQUIRED,
      ),
      SafetyCheckCard(
        id = "safe_2",
        statementAr = "اجتياز اختبار داخل تطبيق تعليمي محلي يكفي للحصول على تصريح طيران قرب المطارات.",
        expectedIsTrue = false,
        explanationAr = "خطأ: التطبيق تعليمي افتراضي فقط، والطيران قرب المطارات محظور أو خاضع لتصاريح الجهات التنظيمية المختصة.",
        reviewStatus = ContentReviewStatus.REVIEW_REQUIRED,
      ),
      SafetyCheckCard(
        id = "safe_3",
        statementAr = "يجب أن يتجاوز مجموع قوى الرفع للمراوح الأربع وزن الطائرة (m · g بالنيوتن N) حتى تبدأ بالصعود.",
        expectedIsTrue = true,
        explanationAr = "صحيح: وفق قانون نيوتن الثاني، يلزم أن يتغلب الدفع الصاعد على قوة الجاذبية المؤثرة على الكتلة (kg).",
        reviewStatus = ContentReviewStatus.DRAFT,
      ),
      SafetyCheckCard(
        id = "safe_4",
        statementAr = "يتصل تطبيق «مُسيّرات 3D» لاسلكياً بالمحركات الفعلية لاختبار سرعة الدوران.",
        expectedIsTrue = false,
        explanationAr = "خطأ: التطبيق يعمل دون اتصال بالشبكة أو بأجهزة حقيقية، وكل المكونات والحركات فيه افتراضية للتعليم فقط.",
        reviewStatus = ContentReviewStatus.DRAFT,
      ),
    ),
    conceptMcqs = listOf(
      MultipleChoiceQuestion(
        id = "mcq_1",
        promptAr = "ما الوظيفة الأساسية لمنظّم السرعة الإلكتروني (ESC) في المنظومة الرباعية؟",
        options = listOf(
          McqOption("a", "تحويل أوامر وحدة التحكم إلى طاقة ثلاثية الطور لضبط سرعة المحرك"),
          McqOption("b", "تخزين خرائط التضاريس عالية الدقة عبر الإنترنت"),
          McqOption("c", "توليد الطاقة الكهربائية من حركة الهواء أثناء الهبوط"),
        ),
        correctOptionId = "a",
        explanationAr = "منظم السرعة (ESC) يستقبل إشارة التحكم الرقمية ويغذّي المحرك عديم الفرش بالتيار المناسب لتغيير سرعة الدوران.",
      ),
      MultipleChoiceQuestion(
        id = "mcq_2",
        promptAr = "لماذا تدور مروحتان باتجاه عقارب الساعة (CW) ومروحتان بعكسها (CCW)؟",
        options = listOf(
          McqOption("a", "لتقليل استهلاك البطارية إلى النصف في وضع الإيقاف"),
          McqOption("b", "لمعادلة عزم الدوران حول محور الانعراج (Yaw) ومنع التفاف الهيكل"),
          McqOption("c", "لزيادة الجهد الكهربائي المقاس بالفولت (V)"),
        ),
        correctOptionId = "b",
        explanationAr = "تعاكس اتجاه الدوران بين الأزواج القطرية يلغي عزم رد الفعل الناتج عن دوران المراوح عند التحويم المستقر.",
      ),
      MultipleChoiceQuestion(
        id = "mcq_3",
        promptAr = "أي وحدة قياس في النظام الدولي (SI) تُستخدم للتعبير عن القوة والوزن في نماذج الطيران؟",
        options = listOf(
          McqOption("a", "النيوتن (N)"),
          McqOption("b", "الأمبير في الساعة (Ah)"),
          McqOption("c", "الراديان (rad)"),
        ),
        correctOptionId = "a",
        explanationAr = "القوة والوزن يُقاسان بالنيوتن (N = kg·m/s²)، بينما يقيس الراديان الزوايا والأمبير-ساعة سعة البطارية.",
      ),
    ),
    reviewQuiz = listOf(
      MultipleChoiceQuestion(
        id = "quiz_1",
        promptAr = "1. أي مكوّن يُعد «عقل» التوازن الذي يدمج قراءات المستشعرات الافتراضية؟",
        options = listOf(
          McqOption("a", "وحدة التحكم بالطيران (FC)"),
          McqOption("b", "إطار الألياف الكربونية"),
          McqOption("c", "موصل البطارية الرئيسي"),
        ),
        correctOptionId = "a",
        explanationAr = "وحدة التحكم بالطيران (FC) تحسب أوامر التصحيح لكل منظم سرعة للحفاظ على استقرار المحاور.",
      ),
      MultipleChoiceQuestion(
        id = "quiz_2",
        promptAr = "2. إذا كانت كتلة طائرة افتراضية 1.5 kg وتسارع الجاذبية 9.81 m/s²، فكم تبلغ قوة وزنها تقريباً؟",
        options = listOf(
          McqOption("a", "1.5 N"),
          McqOption("b", "14.7 N"),
          McqOption("c", "98.1 N"),
        ),
        correctOptionId = "b",
        explanationAr = "الوزن = الكتلة × تسارع الجاذبية = 1.5 × 9.81 ≈ 14.7 N.",
      ),
      MultipleChoiceQuestion(
        id = "quiz_3",
        promptAr = "3. عند الرغبة في التحليق المدني، ما المرجع الملزم لتحديد المناطق المسموح بها؟",
        options = listOf(
          McqOption("a", "اللوائح الرسمية الصادرة عن هيئة الطيران المدني في البلد المعني"),
          McqOption("b", "التخمين الشخصي أو نتائج الاختبارات التعليمية داخل التطبيق"),
          McqOption("c", "إعدادات سرعة المراوح فقط"),
        ),
        correctOptionId = "a",
        explanationAr = "التشغيل الحقيقي يخضع حصراً للأنظمة والجهات الرسمية في كل دولة ولا يُستعاض عنها بأي تطبيق تعليمي.",
      ),
      MultipleChoiceQuestion(
        id = "quiz_4",
        promptAr = "4. أي وصف يعبّر بدقة عن طبيعة تطبيق «مُسيّرات 3D» في هذه المرحلة؟",
        options = listOf(
          McqOption("a", "منصة تعليمية عربية تعمل دون إنترنت وببيانات افتراضية آمنة"),
          McqOption("b", "نظام قيادة سحابي متصل بالإنترنت للتحكم بطائرات حقيقية"),
          McqOption("c", "أداة لتجاوز قيود الارتفاع الجوي"),
        ),
        correctOptionId = "a",
        explanationAr = "التطبيق تعليمي مدني مفتوح المصدر يعمل دون إنترنت ولا يتصل بأي عتاد حقيقي.",
      ),
      MultipleChoiceQuestion(
        id = "quiz_5",
        promptAr = "5. ما الدور المباشر للبطارية في المنظومة الكهربائية للطائرة؟",
        options = listOf(
          McqOption("a", "تزويد منظمات السرعة ووحدة التحكم بالطاقة الكهربائية (V / A)"),
          McqOption("b", "توليد الرفع الهوائي مباشرة دون مراوح"),
          McqOption("c", "توجيه الإشارة اللاسلكية عبر الأقمار الصناعية"),
        ),
        correctOptionId = "a",
        explanationAr = "البطارية تمدّ منظومة الدفع والتحكم بالتيار والجهد اللازمين لتشغيل الدوائر والمحركات.",
      ),
    ),
  )
}

private object DronePartSlotChipFactory {
  fun propellers() = DroneComponentChip(
    id = "chip_propeller",
    component = DroneComponentId.PROPELLERS,
    labelAr = "المراوح (Propellers)",
    shortSpecAr = "توليد قوة الرفع (N)",
    roleExplanationAr = "المراوح تُثبت فوق المحركات عند أطراف الأذرع لتحويل العزم الدوراني إلى دفع هوائي.",
  )

  fun motors() = DroneComponentChip(
    id = "chip_motor",
    component = DroneComponentId.MOTORS,
    labelAr = "المحرك عديم الفرش",
    shortSpecAr = "تحويل كهربائي إلى دوران",
    roleExplanationAr = "المحرك يرتكز في نهاية كل ذراع ويدير المروحة المتصلة بمحوره.",
  )

  fun esc() = DroneComponentChip(
    id = "chip_esc",
    component = DroneComponentId.ESC,
    labelAr = "منظّم السرعة (ESC)",
    shortSpecAr = "تنظيم التيار (A)",
    roleExplanationAr = "منظم السرعة يوضع على الذراع بين وحدة التحكم والمحرك لضبط سرعة الدوران.",
  )

  fun frame() = DroneComponentChip(
    id = "chip_frame",
    component = DroneComponentId.FRAME,
    labelAr = "الهيكل والأذرع (Frame)",
    shortSpecAr = "الكتلة والصلابة (kg)",
    roleExplanationAr = "الهيكل يربط الأذرع الأربعة بالمركز ويتحمّل قوى الطيران والاهتزازات.",
  )

  fun battery() = DroneComponentChip(
    id = "chip_battery",
    component = DroneComponentId.BATTERY,
    labelAr = "البطارية (LiPo)",
    shortSpecAr = "الجهد الكهربائي (V)",
    roleExplanationAr = "البطارية تُثبت في حجرة الطاقة المتوازنة في جسم الطائرة لتغذية المنظومة.",
  )
}
