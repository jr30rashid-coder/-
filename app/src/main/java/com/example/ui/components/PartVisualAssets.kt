package com.example.ui.components

import androidx.annotation.DrawableRes
import com.example.R
import com.example.domain.DroneComponentId

data class PartVisualAsset(
  @param:DrawableRes val drawableRes: Int,
  val titleAr: String,
  val captionAr: String,
  val typicalSpecAr: String,
  val inspectionPointsAr: List<String>,
  val workingPrincipleAr: String,
  val keyFormulaLtr: String,
  val commonMistakesAr: List<String>,
  val relatedTrackHintAr: String,
)

object PartVisualAssets {

  @DrawableRes
  fun resolveSectionDrawable(key: String?): Int? = when (key) {
    "frame_carbon" -> R.drawable.img_drone_frame_carbon_1794887206136
    "brushless_motor" -> R.drawable.img_brushless_motor_1794887943012
    "drone_propeller" -> R.drawable.img_drone_propeller_1794887942944
    "esc_module" -> R.drawable.img_esc_module_1794887943982
    "fc_imu" -> R.drawable.img_flight_controller_imu_1794887207403
    "gnss_imu_sensor" -> R.drawable.img_gnss_imu_module_1794887945974
    "lipo_battery" -> R.drawable.img_lipo_battery_pack_1794887207755
    "civilian_drone" -> R.drawable.img_civilian_drone_real_1794887944793
    else -> null
  }

  fun forComponent(component: DroneComponentId): PartVisualAsset = when (component) {
    DroneComponentId.FRAME -> PartVisualAsset(
      drawableRes = R.drawable.img_drone_frame_carbon_1794887206136,
      titleAr = "هيكل ألياف الكربون 3K وفواصل الألمنيوم",
      captionAr = "صفيحة علوية وسفلية من ألياف الكربون المنسوجة عالية الصلابة مع أربعة أذرع متناظرة وفواصل معدنية لحمل المكونات وعزل الاهتزازات.",
      typicalSpecAr = "الكتلة الهيكلية: 0.11 – 0.35 kg | السماكة: 2 – 5 mm",
      inspectionPointsAr = listOf(
        "فحص حواف الكربون بحثاً عن شقوق دقيقة أو انفصال طبقات (Delamination).",
        "التأكد من إحكام براغي الفواصل وعدم ملامسة أي لوحة إلكترونية للكربون الموصل للكهرباء.",
      ),
      workingPrincipleAr = "ينقل الهيكل عزوم المحركات وردود أفعال الدفع إلى مركز الكتلة، ويحدّ تردده الرنيني العالي من تشويش اهتزازات الأذرع على مستشعرات الجيروسكوب.",
      keyFormulaLtr = "CoG ≈ Σ(mᵢ · rᵢ) / Σmᵢ  |  f_resonance ∝ √(k / m)",
      commonMistakesAr = listOf(
        "تلامس أسلاك الطاقة المكشوفة مع ألياف الكربون (الكربون موصل جيد وقد يسبب قصر دائرة).",
        "إغفال شد البراغي بمثبت خفيف أو ترك ذراع مرنة تهتز أثناء الطيران.",
      ),
      relatedTrackHintAr = "المسار 1 (تشريح المسيرة الهندسي) — الدرس 1.1 و 1.2",
    )

    DroneComponentId.MOTORS -> PartVisualAsset(
      drawableRes = R.drawable.img_brushless_motor_1794887943012,
      titleAr = "محرك كهربائي عديم الفرش (BLDC Outrunner)",
      captionAr = "الجرس المعدني الخارجي يدور بمغناطيسات دائمة حول ملفات النحاس الثابتة؛ يخرج من قاعدته ثلاثة أسلاك طور تتصل بمنظم السرعة، وفي قمته محور مسنن بصامولة قفل.",
      typicalSpecAr = "ثابت السرعة: 900 – 2400 KV | الجهد: 3S – 6S",
      inspectionPointsAr = listOf(
        "التأكد من سلاسة دوران الجرس باليد وعدم وجود احتكاك أو رمل داخل الملفات.",
        "فحص سلامة عزل الأسلاك النحاسية وعدم ارتخاء صامولة تثبيت المروحة.",
      ),
      workingPrincipleAr = "يتبدل التيار ثلاثي الطور في ملفات العضو الساكن لإنتاج مجال مغناطيسي دوار يجذب مغناطيسات الجرس الخارجي، فتتحول القدرة الكهربائية إلى عزم دوراني وسرعة زاوية تتناسب طردياً مع الجهد وقيمة KV.",
      keyFormulaLtr = "RPM_no_load ≈ KV · V_bat  |  P_mech = τ · ω",
      commonMistakesAr = listOf(
        "استخدام براغي تثبيت طويلة تلامس ملفات النحاس أسفل المحرك فتتلف العزل وتحرق الملف.",
        "جمع محرك ذي KV مرتفع مع مروحة بقطر كبير مما يسبب سحب تيار مفرط وسخونة عالية.",
      ),
      relatedTrackHintAr = "المسار 4 (منظومة الدفع والبطارية) — الدرس 4.1",
    )

    DroneComponentId.PROPELLERS -> PartVisualAsset(
      drawableRes = R.drawable.img_drone_propeller_1794887942944,
      titleAr = "مروحة دفع مركّبة من ألياف الكربون/النايلون",
      captionAr = "شفرات انسيابية ملتوية الهندسة لتوليد فرق ضغط ورفع عمودي متجانس من الجذر حتى الطرف، مع محور تثبيت مركزي مثقوب بدقة.",
      typicalSpecAr = "الأقطار الشائعة: 5 – 15 in (0.127 – 0.38 m) | أزواج CW و CCW",
      inspectionPointsAr = listOf(
        "التحقق من خلو حافة الهجوم من أي خدوش أو انبعاجات تسبب اهتزازاً عالي التردد.",
        "مطابقة اتجاه الشفرة (CW / CCW) مع اتجاه دوران المحرك قبل أي تشغيل.",
      ),
      workingPrincipleAr = "تعمل كل شفرة كجناح دوار يسرّع الهواء للأسفل (التدفق المستحث) ليولد قوة رفع صاعدة وفق قانون نيوتن الثالث، وتتعادل عزوم المقاومة بدوران زوجين متعاكسين (CW و CCW).",
      keyFormulaLtr = "T = C_T · ρ · n² · D⁴  |  T_hover = m · g",
      commonMistakesAr = listOf(
        "تركيب مروحة CW على محرك يدور CCW مما يدفع الطائرة للأسفل أو يقلبها لحظياً.",
        "تشغيل مراوح مشروخة أو غير متزنة مما يرفع حرارة المحركات ويشوّه قراءات الـ IMU.",
      ),
      relatedTrackHintAr = "المسار 2 (الديناميكا الهوائية) — الدرس 2.1 و 2.3",
    )

    DroneComponentId.ESC -> PartVisualAsset(
      drawableRes = R.drawable.img_esc_module_1794887943982,
      titleAr = "منظم السرعة الإلكتروني (ESC PCB)",
      captionAr = "لوحة قدرة تحتوي على ترانزستورات MOSFET لتبديل التيار ثلاثي الطور، ومكثف إلكتروليتي لامتصاص نبضات الجهد، وثلاث منصات نحاسية للحام أسلاك المحرك.",
      typicalSpecAr = "التيار المستمر: 20 – 60 A | جهد الدخل: 3S – 6S",
      inspectionPointsAr = listOf(
        "فحص نقاط اللحام (M1/M2/M3 و BAT+/BAT-) للتأكد من لمعانها وخلوها من قصر كهربائي.",
        "التأكد من سلامة المكثف الأسود وعدم انتفاخه أو تلف غلافه العازل.",
      ),
      workingPrincipleAr = "يستقبل منظم السرعة إشارة رقمية (مثل DShot أو PWM) من وحدة التحكم، ثم يبدّل ترانزستورات MOSFET بسرعة فائقة لتكوين أطوار الجهد الثلاثة وتغيير سرعة المحرك خلال أجزاء من الثانية.",
      keyFormulaLtr = "P_loss ≈ I² · R_ds(on)  |  I_esc_rated ≥ 1.25 · I_peak",
      commonMistakesAr = listOf(
        "اختيار ESC بتيار أقصى أقل من سحب المحركات عند الخانق الكامل.",
        "عكس قطبية كابلات البطارية (+) و (-) ولو للحظة واحدة مما يتلف دائرة القدرة فوراً.",
      ),
      relatedTrackHintAr = "المسار 4 (منظومة الدفع والقدرة) — الدرس 4.1",
    )

    DroneComponentId.FLIGHT_CONTROLLER -> PartVisualAsset(
      drawableRes = R.drawable.img_flight_controller_imu_1794887207403,
      titleAr = "لوحة وحدة التحكم بالطيران (Flight Controller PCB)",
      captionAr = "لوحة إلكترونية متعددة الطبقات يتوسطها المعالج الدقيق (MCU)، وبجانبه شريحة القصور الذاتي ومقياس الضغط الجوي ذو الثقب الدقيق، مع أطراف التوصيل المعيارية.",
      typicalSpecAr = "تردد الحلقة: 50 – 4000 Hz | الجهد المنظم: 5 V / 3.3 V",
      inspectionPointsAr = listOf(
        "تركيب اللوحة على حلقات مطاطية ممتصة للاهتزازات مع توجيه السهم المرجعي للأمام.",
        "حماية ثقب مقياس الضغط الجوي من تيارات المراوح المباشرة باستخدام إسفنجة نافذة للهواء.",
      ),
      workingPrincipleAr = "تدمج وحدة التحكم قراءات الجيروسكوب ومقياس التسارع والضغط الجوي عبر مرشحات رقمية، وتحسب التصحيح المطلوب لكل محرك باستخدام حلقة تحكم PID لعزل الاضطرابات والحفاظ على الاتزان.",
      keyFormulaLtr = "u(t) = K_p·e(t) + K_i·∫e(τ)dτ + K_d·(de/dt)",
      commonMistakesAr = listOf(
        "تركيب اللوحة باتجاه مائل أو معكوس دون ضبط زاوية التوجيه في البرمجيات.",
        "تعريض مقياس الضغط الجوي لتيار هواء المراوح مباشرة مما يسبب قفزات وهمية في الارتفاع.",
      ),
      relatedTrackHintAr = "المسار 5 (وحدة التحكم وحلقة PID) — الدرس 5.1 و 5.2",
    )

    DroneComponentId.SENSORS -> PartVisualAsset(
      drawableRes = R.drawable.img_gnss_imu_module_1794887945974,
      titleAr = "وحدة الملاحة والمستشعرات (GNSS / Compass Module)",
      captionAr = "هوائي خزفي مربع لاستقبال إشارات الملاحة الفضائية مثبت أعلى لوحة خضراء تحوي درعاً معدنياً ضد التداخل الكهرومغناطيسي وموصل إشارات متعدد الأطراف.",
      typicalSpecAr = "التغذية: 5 V | الواجهة الافتراضية: UART + I2C",
      inspectionPointsAr = listOf(
        "تثبيت الهوائي موجّهاً للأعلى وبعيداً عن أسلاك التيار العالي لتقليل تشويش البوصلة.",
        "التحقق من إحكام القابس متعدد الأسلاك وعدم وجود كسر أو شد زائد على الموصل.",
      ),
      workingPrincipleAr = "يقيس مستشعر GNSS زمن وصول الإشارات من عدة أقمار لحساب الموقع والسرعة الأفقية، بينما يقرأ المغناطيسوميتر اتجاه الشمال المغناطيسي لتصحيح الانحراف التراكمي للبوصلة الداخلية.",
      keyFormulaLtr = "d = c · Δt  |  P_rx = P_tx − FSPL(f, d)",
      commonMistakesAr = listOf(
        "تثبيت البوصلة ملاصقة لكابلات البطارية حيث يولّد التيار العالي مجالاً مغناطيسياً يضلّل الاتجاه.",
        "عكس توصيل سلكي TX و RX في منفذ UART الخاص بوحدة الملاحة.",
      ),
      relatedTrackHintAr = "المسار 3 (المستشعرات وفيزياء الراديو) — الدرس 3.1 و 3.3",
    )

    DroneComponentId.BATTERY -> PartVisualAsset(
      drawableRes = R.drawable.img_lipo_battery_pack_1794887207755,
      titleAr = "حزمة بطارية ليثيوم بوليمر (LiPo 4S)",
      captionAr = "أربع خلايا متصلة على التوالي داخل غلاف عازل، يخرج منها كبلان سميكان بموصل XT60 الأصفر للقدرة العالية وموصل أبيض رفيع لموازنة جهد كل خلية.",
      typicalSpecAr = "الجهد الاسمي: 14.8 V | جهد الامتلاء: 16.8 V | التوازن: 3.85 V/خلية للتخزين",
      inspectionPointsAr = listOf(
        "رفض أي بطارية منتفخة أو مثقوبة الغلاف فوراً وعدم شحنها.",
        "فحص تساوي جهود الخلايا عبر قابس الموازنة قبل التشغيل أو التخزين.",
      ),
      workingPrincipleAr = "توفر خلايا الليثيوم الموصلة على التوالي (S) فرق الجهد الكلي، بينما تحدد السعة (mAh) ومعامل التفريغ (C) كمية الطاقة المخزنة وأقصى تيار آمن دون هبوط حاد في الجهد (Voltage Sag).",
      keyFormulaLtr = "E (Wh) = V_nom × Ah  |  I_max_safe = Ah × C_rating",
      commonMistakesAr = listOf(
        "تفريغ الخلية دون 3.3 V تحت الحمل أو ترك البطارية مشحونة بالكامل لأيام طويلة (تُخزّن عند 3.85 V).",
        "تثبيت البطارية بعيداً عن مركز الثقل مما يجبر المحركات الخلفية أو الأمامية على جهد غير متماثل.",
      ),
      relatedTrackHintAr = "المسار 4 (البطاريات وميزانية القدرة) — الدرس 4.2",
    )
  }

  fun workshopGallery(): List<PartVisualAsset> = listOf(
    forComponent(DroneComponentId.FRAME),
    forComponent(DroneComponentId.MOTORS),
    forComponent(DroneComponentId.PROPELLERS),
    forComponent(DroneComponentId.ESC),
    forComponent(DroneComponentId.FLIGHT_CONTROLLER),
    forComponent(DroneComponentId.SENSORS),
    forComponent(DroneComponentId.BATTERY),
  )
}
