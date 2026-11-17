package com.example.data

import com.example.domain.ContentReviewStatus
import com.example.domain.DronePhysicsEngine
import com.example.domain.SafetyWorkshopAndLogs.RegulatoryRule
import com.example.domain.SafetyWorkshopAndLogs.WorkshopBattery
import com.example.domain.SafetyWorkshopAndLogs.WorkshopEsc
import com.example.domain.SafetyWorkshopAndLogs.WorkshopMotor

object RegulationsAndWorkshopData {

  /**
   * Phase 7: Regulatory awareness items.
   * Explicitly tagged REVIEW_REQUIRED / DRAFT — never posed as certified legal advice.
   */
  fun getRegulatoryRules(): List<RegulatoryRule> = listOf(
    RegulatoryRule(
      id = "reg_sa_gaca",
      countryAr = "المملكة العربية السعودية",
      authorityAr = "الهيئة العامة للطيران المدني (GACA)",
      versionLabel = "مرجع إرشادي غير مراجَع بشرياً",
      effectiveDate = "غير مؤكد داخل التطبيق — راجع البوابة الرسمية",
      lastReviewedDate = "2026-11-16 (مسودة تقنية)",
      officialReferenceLabelAr = "راجع اللوائح الرسمية المنشورة لدى GACA قبل أي تشغيل",
      summaryAr = "تشمل الضوابط العامة في معظم الدول تسجيل المركبات وفق فئة الوزن، والالتزام بالمجال الجوي المسموح، والحصول على التصاريح اللازمة للمهام المدنية والتجارية.",
      stalenessWarningAr = "تنبيه: اللوائح الوطنية تتغير دورياً؛ هذا النص مسودة تعليمية لا يُعتد بها كمرجع قانوني دون مراجعة رسمية محدثة.",
      reviewStatus = ContentReviewStatus.REVIEW_REQUIRED,
    ),
    RegulatoryRule(
      id = "reg_ae_gcaa",
      countryAr = "الإمارات العربية المتحدة",
      authorityAr = "الهيئة العامة للطيران المدني (GCAA)",
      versionLabel = "مسودة تعليمية عامة",
      effectiveDate = "يُتحقق من النشرة الرسمية السارية",
      lastReviewedDate = "2026-11-16 (مسودة تقنية)",
      officialReferenceLabelAr = "راجع بوابة GCAA والجهات المحلية المختصة",
      summaryAr = "تتطلب العمليات المدنية الالتزام بمناطق الطيران المصرح بها وقيود الخصوصية والارتفاع والبعد عن المطارات.",
      stalenessWarningAr = "تنبيه تقادم واختلاف الاختصاص: قد تختلف اشتراطات كل إمارة أو جهة محلية؛ تحقّق رسمياً قبل أي نشاط.",
      reviewStatus = ContentReviewStatus.REVIEW_REQUIRED,
    ),
    RegulatoryRule(
      id = "reg_intl_general",
      countryAr = "مبادئ إرشادية دولية عامة (ICAO)",
      authorityAr = "إرشادات عامة للسلامة المدنية",
      versionLabel = "إطار تعليمي أولي",
      effectiveDate = "غير ملزم قانونياً بحد ذاته",
      lastReviewedDate = "2026-11-16 (مسودة)",
      officialReferenceLabelAr = "المواد الإرشادية لسلامة منظومات الطائرات بدون طيار",
      summaryAr = "الأولوية القصوى لسلامة الأفراد والممتلكات في الجو وعلى الأرض، مع الحفاظ على خط البصر المباشر وتجنب الحشود والمطارات.",
      stalenessWarningAr = "القانون الملزم هو التشريع الوطني الساري في بلد التشغيل الفعلي فقط.",
      reviewStatus = ContentReviewStatus.DRAFT,
    ),
  )

  // Phase 8: Workshop components
  fun getMotors(): List<WorkshopMotor> = listOf(
    WorkshopMotor("m_2207_1950", "محرك تعليمي 2207 - 1950KV", kvRating = 1950, maxCurrentA = 28f, maxThrustPerMotorN = 10.5f, massKg = 0.034f),
    WorkshopMotor("m_2306_2450", "محرك تعليمي 2306 - 2450KV", kvRating = 2450, maxCurrentA = 36f, maxThrustPerMotorN = 12.2f, massKg = 0.033f),
    WorkshopMotor("m_2807_1300", "محرك حمولة خفيفة 2807 - 1300KV", kvRating = 1300, maxCurrentA = 32f, maxThrustPerMotorN = 15.0f, massKg = 0.052f),
    WorkshopMotor("m_1404_3800", "محرك خفيف 1404 - 3800KV", kvRating = 3800, maxCurrentA = 14f, maxThrustPerMotorN = 4.2f, massKg = 0.011f),
  )

  fun getEscs(): List<WorkshopEsc> = listOf(
    WorkshopEsc("esc_45a", "منظم سرعة 45A (3S–6S)", continuousCurrentA = 45f, minCellsS = 3, maxCellsS = 6, massKg = 0.015f),
    WorkshopEsc("esc_30a", "منظم سرعة 30A (2S–4S)", continuousCurrentA = 30f, minCellsS = 2, maxCellsS = 4, massKg = 0.010f),
    WorkshopEsc("esc_20a", "منظم سرعة خفيف 20A (2S–4S)", continuousCurrentA = 20f, minCellsS = 2, maxCellsS = 4, massKg = 0.007f),
  )

  fun getBatteries(): List<WorkshopBattery> = listOf(
    WorkshopBattery("b_4s_2200", "بطارية 4S 2200mAh 45C", cellsS = 4, nominalVoltageV = 14.8f, capacityMah = 2200, maxDischargeC = 45, massKg = 0.240f),
    WorkshopBattery("b_6s_1800", "بطارية 6S 1800mAh 70C", cellsS = 6, nominalVoltageV = 22.2f, capacityMah = 1800, maxDischargeC = 70, massKg = 0.295f),
    WorkshopBattery("b_3s_1500", "بطارية 3S 1500mAh 25C", cellsS = 3, nominalVoltageV = 11.1f, capacityMah = 1500, maxDischargeC = 25, massKg = 0.135f),
  )

  fun getBaselineComparisonRun(): List<DronePhysicsEngine.SimTelemetrySample> {
    val inputs = buildList {
      repeat(50) { add(DronePhysicsEngine.SimControlInput(armed = true, throttle = 0.65f)) }
      repeat(60) { add(DronePhysicsEngine.SimControlInput(armed = true, throttle = 0.49f)) }
      repeat(40) { add(DronePhysicsEngine.SimControlInput(armed = true, throttle = 0.35f)) }
    }
    return DronePhysicsEngine.replaySession(inputs, seed = 101L)
  }
}
