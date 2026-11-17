package com.example.domain

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Offline Scientific Calculators, Searchable Engineering Wiki helpers,
 * and Deterministic Socratic Study Coach (inspired by structured technical academies,
 * strictly offline with zero internet, telemetry, or external LLM calls).
 */
object EngineeringWikiAndCoach {

  enum class RadioBandOption(val labelAr: String, val freqMHz: Float) {
    BAND_915("915 MHz (مدى طويل)", 915f),
    BAND_2400("2.4 GHz (تحكم قياسي)", 2400f),
    BAND_5800("5.8 GHz (نقل بصري)", 5800f),
  }

  data class LinkBudgetResult(
    val wavelengthM: Float,
    val fsplDb: Float,
    val fresnelRadiusM: Float,
    val txDbm: Float,
    val estimatedRxDbm: Float,
    val guidanceAr: String,
  )

  data class AirDensityResult(
    val densityKgM3: Float,
    val relativeRatioPercent: Float,
    val noteAr: String,
  )

  data class PowertrainBudgetResult(
    val weightN: Float,
    val totalThrustN: Float,
    val twr: Float,
    val hoverThrottlePercent: Float,
    val energyWh: Float,
    val estimatedHoverMin: Float,
    val verdictAr: String,
  )

  data class SocraticCoachTopic(
    val id: String,
    val questionAr: String,
    val trackHintAr: String,
    val guidingClueAr: String,
    val detailedDerivationAr: String,
  )

  data class WikiFormulaEntry(
    val id: String,
    val categoryAr: String,
    val titleAr: String,
    val equationLtr: String,
    val explanationAr: String,
  )

  /**
   * Free-space path loss & 1st Fresnel zone radius in pure SI / engineering units.
   * Assumptions: Line-of-Sight (LOS), omnidirectional 0 dBi antennas, isotropic propagation.
   */
  fun calculateLinkBudgetAndFresnel(
    band: RadioBandOption,
    distanceM: Float,
    txPowerMw: Float,
  ): LinkBudgetResult {
    val d = distanceM.coerceIn(5f, 10_000f)
    val freqHz = band.freqMHz * 1_000_000.0
    val wavelengthM = (299_792_458.0 / freqHz).toFloat()
    val fsplDb = (20.0 * log10(d.toDouble()) + 20.0 * log10(band.freqMHz.toDouble()) - 27.55).toFloat()
    // Max radius at midpoint: r = 0.5 * sqrt(lambda * d)
    val fresnelRadiusM = (0.5 * sqrt(wavelengthM.toDouble() * d.toDouble())).toFloat()
    val safeMw = txPowerMw.coerceIn(10f, 1000f)
    val txDbm = (10.0 * log10(safeMw.toDouble())).toFloat()
    val estimatedRxDbm = txDbm - fsplDb

    val guidance = when {
      estimatedRxDbm > -75f -> "هامش الإشارة في الفضاء الحر قوي نظرياً (مع افتراض خلو العوائق ومنطقة فرينل بنسبة 60% على الأقل)."
      estimatedRxDbm > -95f -> "الإشارة مقبولة نظرياً في خط البصر المفتوح؛ أي عائق شجري أو مبنى داخل نصف قطر فرينل (${"%.1f".format(fresnelRadiusM)} m) سيسبب توهيناً حاداً."
      else -> "التوهين في الفضاء الحر مرتفع عند هذه المسافة؛ يلزم خفض المسافة أو رفع الكسب الهوائي في المحاكاة التعليمية."
    }

    return LinkBudgetResult(
      wavelengthM = wavelengthM,
      fsplDb = fsplDb,
      fresnelRadiusM = fresnelRadiusM,
      txDbm = txDbm,
      estimatedRxDbm = estimatedRxDbm,
      guidanceAr = guidance,
    )
  }

  /**
   * ISA-based air density estimation from elevation (m) and ambient temperature (°C).
   */
  fun calculateAirDensityAndThrustRatio(
    elevationM: Float,
    temperatureC: Float,
  ): AirDensityResult {
    val h = elevationM.coerceIn(0f, 5000f)
    val tempK = (temperatureC.coerceIn(-20f, 55f) + 273.15f).toDouble()
    // Barometric approximation for tropospheric pressure (Pa)
    val pressurePa = 101325.0 * (1.0 - 2.25577e-5 * h.toDouble()).pow(5.25588)
    val rSpecific = 287.058
    val rho = (pressurePa / (rSpecific * tempK)).toFloat()
    val ratioPct = (rho / 1.225f) * 100f

    val note = when {
      ratioPct >= 95f -> "كثافة الهواء قريبة من المستوى القياسي لسطح البحر (1.225 kg/m³)؛ أداء الدفع في النطاق الاعتيادي."
      ratioPct >= 82f -> "انخفاض ملحوظ في الكثافة: ستحتاج المراوح لسرعة دوران أكبر لتحقيق نفس الدفع، مما يزيد استهلاك البطارية."
      else -> "كثافة هواء منخفضة بشدة (ارتفاع عالٍ أو حرارة مرتفعة): ينخفض هامش الدفع بشكل حساس ويتراجع زمن التحويم."
    }

    return AirDensityResult(
      densityKgM3 = rho,
      relativeRatioPercent = ratioPct,
      noteAr = note,
    )
  }

  /**
   * Quick SI powertrain and hover endurance estimator.
   */
  fun calculateHoverAndPowertrainBudget(
    totalMassKg: Float,
    maxThrustPerMotorN: Float,
    batteryCellsS: Int,
    batteryCapacityMah: Int,
    hoverCurrentA: Float,
  ): PowertrainBudgetResult {
    val m = totalMassKg.coerceIn(0.2f, 25f)
    val weightN = m * DronePhysicsEngine.GRAVITY_MS2
    val totalThrustN = maxThrustPerMotorN.coerceIn(1f, 60f) * 4f
    val twr = totalThrustN / weightN
    val hoverThrottlePct = ((weightN / totalThrustN) * 100f).coerceIn(5f, 100f)
    val nominalV = batteryCellsS.coerceIn(2, 8) * 3.7f
    val capAh = batteryCapacityMah.coerceIn(300, 30_000) / 1000f
    val energyWh = nominalV * capAh
    // Reserve 20% battery buffer for safe LiPo preservation
    val usableAh = capAh * 0.80f
    val safeI = hoverCurrentA.coerceIn(1f, 120f)
    val estimatedHoverMin = (usableAh / safeI) * 60f

    val verdict = when {
      twr < 1.2f -> "تحذير: نسبة الدفع للوزن (${"%.2f".format(twr)}) منخفضة جداً ولا تمنح هامش صعود آمناً."
      twr in 1.2f..1.79f -> "هامش دفع متوسط؛ مناسب فقط للتحويم الهادئ دون رياح أو حمولات إضافية."
      twr in 1.8f..2.8f -> "نسبة دفع متوازنة ومثالية للتدريب المدني المستقر (نحو 40%–55% خانق للتحويم)."
      else -> "نسبة دفع عالية جداً: انتبه لسحب التيار الأقصى وتصنيف منظم السرعة والبطارية."
    }

    return PowertrainBudgetResult(
      weightN = weightN,
      totalThrustN = totalThrustN,
      twr = twr,
      hoverThrottlePercent = hoverThrottlePct,
      energyWh = energyWh,
      estimatedHoverMin = estimatedHoverMin,
      verdictAr = verdict,
    )
  }

  fun recommendNextStudyStepAr(
    completedLessons: Set<String>,
    totalLessons: Int,
    completedScenarios: Set<String>,
  ): String = when {
    completedLessons.isEmpty() ->
      "نقطة البداية الموصى بها: ابدأ بالدرس 1.1 في المسار الأول لتتعرف على المكونات الستة بصورها الحقيقية ثم أكمل تمرين السحب والإفلات."
    "t1_l2" !in completedLessons ->
      "الخطوة التالية: أكمل الدرس 1.2 لفهم وحدات النظام الدولي SI وتصنيفات الطائرات المدنية."
    "t2_l1" !in completedLessons ->
      "أحسنت! انتقل الآن إلى المسار 2 (القوى والمحاور ومصفوفة الخلط) لربط الدفع بالاتزان قبل تجربة المحاكي 3D."
    completedScenarios.isEmpty() ->
      "حان وقت التطبيق العملي: افتح تبويب «المحاكي» وجرّب السيناريو 1 للتحويم المستقر عند 5 أمتار في الميدان ثلاثي الأبعاد."
    completedLessons.size < totalLessons ->
      "تقدم ممتاز (${completedLessons.size}/$totalLessons درس)! تابع الدروس المتقدمة في المسارات 3–8 واستخدم الحاسبات العلمية للتحقق من المعادلات."
    else ->
      "أتممت جميع الدروس المدمجة ($totalLessons/$totalLessons)! اختبر مهاراتك في سيناريو البوابات 3D والبرمجة المعزولة ومحلل السجلات."
  }

  fun getSocraticTopics(): List<SocraticCoachTopic> = listOf(
    SocraticCoachTopic(
      id = "coach_tilt_altitude",
      questionAr = "لماذا تهبط الطائرة عندما أميلها للأمام في المحاكي رغم بقاء الخانق ثابتاً؟",
      trackHintAr = "المسار 2 • ميكانيكا القوى",
      guidingClueAr = "فكّر في اتجاه متجه الدفع الكلي T: عندما تميل الطائرة بزاوية θ عن الرأسي، هل تبقى المركّبة الصاعدة مساويةً لكامل T؟ تذكّر دالة جيب التمام cos(θ).",
      detailedDerivationAr = "المركّبة الرأسية المقاومة للوزن تصبح T·cos(θ). بما أن cos(θ) < 1 لأي زاوية ميل غير صفرية، يقل الدفع العمودي عن الوزن m·g فتتسارع الطائرة للأسفل ما لم ترفع الخانق قليلاً.",
    ),
    SocraticCoachTopic(
      id = "coach_esc_battery",
      questionAr = "كيف أعرف أن البطارية ومنظّم السرعة (ESC) لن يسخنا أو يفشلا في التصنيع التعليمي؟",
      trackHintAr = "المسار 3 و 6 • الطاقة والتوافق",
      guidingClueAr = "قارن بين ثلاثة أرقام: جهد البطارية (عدد الخلايا S)، وتيار الذروة للمحركات الأربعة، وتصنيف التيار المستمر لكل ESC بهامش +25%.",
      detailedDerivationAr = "1) يجب أن يطابق عدد خلايا البطارية (S) نطاق جهد الـ ESC. 2) تيار الـ ESC المستمر ≥ 1.25 × أقصى تيار للمحرك الواحد. 3) تيار البطارية المستمر (Ah × C) ≥ مجموع تيارات المحركات الأربعة.",
    ),
    SocraticCoachTopic(
      id = "coach_pid_oscillation",
      questionAr = "متى تسبب حلقة التحكم PID اهتزازاً سريعاً وكيف أميّز سببه؟",
      trackHintAr = "المسار 3 • نظرية التحكم",
      guidingClueAr = "تأمّل دور الحد التناسبي P والحد التفاضلي D: أيهما يدفع بقوة كبيرة عند خطأ صغير، وأيهما يضخّم ضوضاء الاهتزازات الميكانيكية؟",
      detailedDerivationAr = "الاهتزاز البطيء/العنيف ينتج غالباً عن فرط قيمة Kp، بينما التذبذب عالي التردد مع سخونة المحركات ينتج عن تضخيم Kd لضوضاء مروحة غير متوازنة أو هيكل غير معزول.",
    ),
    SocraticCoachTopic(
      id = "coach_radio_fresnel",
      questionAr = "لماذا ينقطع الرابط اللاسلكي أحياناً رغم وجود خط نظر ظاهري فوق الأشجار؟",
      trackHintAr = "المسار 3 (الدرس 3.3) • الفيزياء الراديوية",
      guidingClueAr = "الموجة الراديوية لا تسير في خط رفيع كالليزر! ابحث في مفهوم «منطقة فرينل الأولى» (Fresnel Zone) ونصف قطرها r عند منتصف المسافة.",
      detailedDerivationAr = "تنتشر الطاقة داخل إهليلج ثلاثي الأبعاد حول الخط المستقيم؛ إذا حجب عائق أكثر من 40% من نصف قطر فرينل r = 0.5·√(λ·d)، تتداخل الموجات المنعكسة هدامياً ويضعف الرابط بشدة.",
    ),
    SocraticCoachTopic(
      id = "coach_evidence_diag",
      questionAr = "كيف أتصرف كمهندس عندما تأتي قراءات التشخيص أو السجل متناقضة أو ناقصة؟",
      trackHintAr = "المسار 6 و 8 • المنهج العلمي",
      guidingClueAr = "هل يُسمح في الصيانة بتخمين عطل وحدة التحكم إذا لم يُقَس جهد البطارية تحت الحمل ولم تُفحص المراوح؟",
      detailedDerivationAr = "أبداً! القاعدة الصارمة هي التصريح «الأدلة غير كافية.» وطلب القياس المفقود أو إعادة الفحص ببيانات متصاعدة زمنياً وخالية من القيم الشاذة.",
    ),
  )

  fun getWikiReferenceEntries(): List<WikiFormulaEntry> = listOf(
    WikiFormulaEntry("wf_weight", "الميكانيكا", "الوزن بالنيوتن (SI)", "W = m * 9.81", "تحويل الكتلة بالكيلوغرام إلى قوة جذب أرضية بالنيوتن."),
    WikiFormulaEntry("wf_tilt", "الطيران", "الدفع المطلوب أثناء الميل", "T = (m * g) / (cos(θ) * cos(φ))", "تفسير فقدان الرفع عند الإمالة ووجوب تعويض الخانق."),
    WikiFormulaEntry("wf_lipo", "الطاقة", "التيار الأقصى والهبوط الجهدي", "I_max = Ah * C  |  ΔV = I * R_int", "حساب أقصى سحب آمن للبطارية وهبوط الجهد تحت الحمل."),
    WikiFormulaEntry("wf_pid", "التحكم", "معادلة المتحكم PID", "u(t) = Kp·e + Ki·∫e dt + Kd·(de/dt)", "موازنة سرعة الاستجابة (P)، إزالة الانحراف (I)، والتخميد (D)."),
    WikiFormulaEntry("wf_fspl", "الراديو", "توهين الفضاء الحر (FSPL)", "FSPL(dB) = 20·log10(d_m) + 20·log10(f_MHz) - 27.55", "حساب فقدان الإشارة الهندسي في خط البصر المفتوح."),
    WikiFormulaEntry("wf_fresnel", "الراديو", "نصف قطر منطقة فرينل", "r_1 = 0.5 * sqrt(λ * d)", "نصف القطر الواجب إخلاؤه من العوائق بنسبة ≥ 60% عند منتصف المسار."),
    WikiFormulaEntry("wf_density", "الأرصاد", "كثافة الهواء من الغاز المثالي", "ρ = P / (287.058 * T_Kelvin)", "تراجع الكثافة مع الارتفاع والحرارة يقلل دفع المراوح طردياً."),
  )
}
