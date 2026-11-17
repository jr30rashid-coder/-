package com.example.domain

import kotlin.math.abs

/**
 * Phases 7, 8 & 10: Regulatory Governance, Workshop Compatibility/Evidence-Based Diagnostics,
 * and Telemetry Log Analysis.
 */
object SafetyWorkshopAndLogs {

  const val INSUFFICIENT_EVIDENCE_LITERAL = "الأدلة غير كافية."

  // Phase 7: Regulatory content with mandatory authority, jurisdiction, dates, and review gating
  data class RegulatoryRule(
    val id: String,
    val countryAr: String,
    val authorityAr: String,
    val versionLabel: String,
    val effectiveDate: String,
    val lastReviewedDate: String,
    val officialReferenceLabelAr: String,
    val summaryAr: String,
    val stalenessWarningAr: String,
    val reviewStatus: ContentReviewStatus,
  ) {
    val isDisplayableAsCertified: Boolean
      get() = reviewStatus == ContentReviewStatus.APPROVED
  }

  // Phase 8: Workshop components & diagnostics
  data class WorkshopMotor(
    val id: String,
    val nameAr: String,
    val kvRating: Int,
    val maxCurrentA: Float,
    val maxThrustPerMotorN: Float,
    val massKg: Float,
  )

  data class WorkshopEsc(
    val id: String,
    val nameAr: String,
    val continuousCurrentA: Float,
    val minCellsS: Int,
    val maxCellsS: Int,
    val massKg: Float,
  )

  data class WorkshopBattery(
    val id: String,
    val nameAr: String,
    val cellsS: Int,
    val nominalVoltageV: Float,
    val capacityMah: Int,
    val maxDischargeC: Int,
    val massKg: Float,
  )

  data class CompatibilityReport(
    val isCompatible: Boolean,
    val totalMassKg: Float,
    val totalMaxThrustN: Float,
    val thrustToWeightRatio: Float,
    val peakTotalCurrentA: Float,
    val batteryMaxContinuousA: Float,
    val findingsAr: List<String>,
  )

  data class DiagnosticEvidenceInput(
    val symptomAr: String,
    val batteryVoltageMeasuredV: Float?,
    val propInspectionDone: Boolean,
    val motorSpinBalanced: Boolean?,
    val hasConflictingReadings: Boolean = false,
  )

  data class DiagnosticVerdict(
    val conclusive: Boolean,
    val headlineAr: String,
    val detailsAr: String,
    val disclaimerAr: String = "تشخيص تعليمي افتراضي مبني على الأدلة المدخلة فقط؛ لا يغني عن فحص مهندس مؤهل ولا يفحص أجهزة حقيقية.",
  )

  // Phase 10: Flight log analysis
  data class LogAnalysisReport(
    val conclusive: Boolean,
    val headlineAr: String,
    val sampleCount: Int,
    val durationSec: Float,
    val maxAltitudeM: Float,
    val maxClimbMs: Float,
    val minBatteryV: Float,
    val maxTiltRad: Float,
    val notesAr: List<String>,
  )

  data class LogComparisonReport(
    val conclusive: Boolean,
    val summaryAr: String,
  )

  fun checkBuildCompatibility(
    frameAndAvionicsMassKg: Float,
    motor: WorkshopMotor,
    esc: WorkshopEsc,
    battery: WorkshopBattery,
  ): CompatibilityReport {
    val findings = mutableListOf<String>()
    var compatible = true

    if (battery.cellsS !in esc.minCellsS..esc.maxCellsS) {
      compatible = false
      findings.add(
        "عدم توافق الجهد: البطارية ${battery.cellsS}S (${battery.nominalVoltageV} V) خارج نطاق منظم السرعة (${esc.minCellsS}S–${esc.maxCellsS}S).",
      )
    }

    val requiredEscA = motor.maxCurrentA * 1.25f
    if (esc.continuousCurrentA < requiredEscA) {
      compatible = false
      findings.add(
        "هامش تيار غير كافٍ: تيار المحرك الأقصى (${motor.maxCurrentA} A) يتطلب منظم سرعة >= ${"%.1f".format(requiredEscA)} A.",
      )
    }

    val peakTotalCurrentA = motor.maxCurrentA * 4f
    val batteryMaxContinuousA = (battery.capacityMah / 1000f) * battery.maxDischargeC
    if (batteryMaxContinuousA < peakTotalCurrentA) {
      compatible = false
      findings.add(
        "تفريغ البطارية (${"%.1f".format(batteryMaxContinuousA)} A) أقل من ذروة سحب المحركات الأربع (${"%.1f".format(peakTotalCurrentA)} A).",
      )
    }

    val totalMassKg = frameAndAvionicsMassKg + (motor.massKg * 4f) + (esc.massKg * 4f) + battery.massKg
    val totalWeightN = totalMassKg * DronePhysicsEngine.GRAVITY_MS2
    val totalMaxThrustN = motor.maxThrustPerMotorN * 4f
    val twr = if (totalWeightN > 0f) totalMaxThrustN / totalWeightN else 0f

    if (twr < 1.8f) {
      compatible = false
      findings.add(
        "نسبة الدفع إلى الوزن (${"%.2f".format(twr)}) أقل من الهامش التعليمي الموصى به (1.80+) للتحويم المستقر والمناورة.",
      )
    } else {
      findings.add(
        "توافق سليم: نسبة الدفع إلى الوزن = ${"%.2f".format(twr)} وهامش التيار آمن افتراضياً.",
      )
    }

    return CompatibilityReport(
      isCompatible = compatible,
      totalMassKg = totalMassKg,
      totalMaxThrustN = totalMaxThrustN,
      thrustToWeightRatio = twr,
      peakTotalCurrentA = peakTotalCurrentA,
      batteryMaxContinuousA = batteryMaxContinuousA,
      findingsAr = findings,
    )
  }

  fun evaluateDiagnosticEvidence(input: DiagnosticEvidenceInput): DiagnosticVerdict {
    if (input.hasConflictingReadings || input.batteryVoltageMeasuredV == null || !input.propInspectionDone || input.motorSpinBalanced == null) {
      return DiagnosticVerdict(
        conclusive = false,
        headlineAr = INSUFFICIENT_EVIDENCE_LITERAL,
        detailsAr = "لإصدار فرضية تشخيص افتراضية موثوقة يجب توفر قراءة جهد صالحة وفحص المراوح وتوازن الدوران دون تناقض في المعطيات.",
      )
    }

    val v = input.batteryVoltageMeasuredV
    return when {
      v < 13.2f -> DiagnosticVerdict(
        conclusive = true,
        headlineAr = "فرضية افتراضية: هبوط جهد مصدر الطاقة (${"%.2f".format(v)} V)",
        detailsAr = "الجهد المقاس منخفض لنطاق 4S الافتراضي، ما يفسّر ضعف الدفع أو التوقف الوقائي المبكر.",
      )
      !input.motorSpinBalanced -> DiagnosticVerdict(
        conclusive = true,
        headlineAr = "فرضية افتراضية: عدم اتزان ميكانيكي في مجموعة الدفع",
        detailsAr = "عدم توازن دوران المحرك الافتراضي يرفع الاهتزاز ويشتت قراءات الجيروسكوب الافتراضي.",
      )
      else -> DiagnosticVerdict(
        conclusive = false,
        headlineAr = INSUFFICIENT_EVIDENCE_LITERAL,
        detailsAr = "القراءات الأساسية تبدو ضمن النطاق الطبيعي ولا تفسّر العَرَض المذكور؛ يلزم سجل قياس إضافي.",
      )
    }
  }

  fun analyzeFlightLog(samples: List<DronePhysicsEngine.SimTelemetrySample>): LogAnalysisReport {
    if (samples.size < 5) {
      return LogAnalysisReport(
        conclusive = false,
        headlineAr = INSUFFICIENT_EVIDENCE_LITERAL,
        sampleCount = samples.size,
        durationSec = 0f,
        maxAltitudeM = 0f,
        maxClimbMs = 0f,
        minBatteryV = 0f,
        maxTiltRad = 0f,
        notesAr = listOf("عدد العينات (${samples.size}) غير كافٍ لإجراء تحليل زمني موثوق (الحد الأدنى 5 عينات)."),
      )
    }

    var prevTime = -1f
    for (s in samples) {
      if (s.altitudeM.isNaN() || s.batteryV.isNaN() || s.timeSec.isNaN() ||
        s.altitudeM.isInfinite() || s.batteryV.isInfinite() || s.timeSec < prevTime ||
        s.altitudeM < -0.5f || s.batteryV <= 0f
      ) {
        return LogAnalysisReport(
          conclusive = false,
          headlineAr = INSUFFICIENT_EVIDENCE_LITERAL,
          sampleCount = samples.size,
          durationSec = 0f,
          maxAltitudeM = 0f,
          maxClimbMs = 0f,
          minBatteryV = 0f,
          maxTiltRad = 0f,
          notesAr = listOf("رُصدت بيانات غير متسقة أو زمن غير متصاعد أو قيم فيزيائية متناقضة في السجل."),
        )
      }
      prevTime = s.timeSec
    }

    val duration = samples.last().timeSec - samples.first().timeSec
    val maxAlt = samples.maxOf { it.altitudeM }
    val maxClimb = samples.maxOf { abs(it.vzMs) }
    val minBatt = samples.minOf { it.batteryV }
    val maxTilt = samples.maxOf { maxOf(abs(it.pitchRad), abs(it.rollRad)) }

    val notes = mutableListOf<String>()
    if (minBatt < 13.8f) {
      notes.add("لوحظ هبوط واضح في الجهد إلى ${"%.2f".format(minBatt)} V؛ راجع سحب التيار.")
    } else {
      notes.add("استقرار جهد البطارية ضمن النطاق الافتراضي الآمن.")
    }
    if (maxTilt > 0.35f) {
      notes.add("تم تسجيل زاوية ميل مرتفعة (${"%.2f".format(maxTilt)} rad).")
    }

    return LogAnalysisReport(
      conclusive = true,
      headlineAr = "تحليل مكتمل للسجل الافتراضي",
      sampleCount = samples.size,
      durationSec = duration,
      maxAltitudeM = maxAlt,
      maxClimbMs = maxClimb,
      minBatteryV = minBatt,
      maxTiltRad = maxTilt,
      notesAr = notes,
    )
  }

  fun compareFlightLogs(
    runA: List<DronePhysicsEngine.SimTelemetrySample>,
    runB: List<DronePhysicsEngine.SimTelemetrySample>,
  ): LogComparisonReport {
    val reportA = analyzeFlightLog(runA)
    val reportB = analyzeFlightLog(runB)
    if (!reportA.conclusive || !reportB.conclusive) {
      return LogComparisonReport(
        conclusive = false,
        summaryAr = INSUFFICIENT_EVIDENCE_LITERAL,
      )
    }
    val deltaAlt = reportB.maxAltitudeM - reportA.maxAltitudeM
    val deltaBatt = reportB.minBatteryV - reportA.minBatteryV
    return LogComparisonReport(
      conclusive = true,
      summaryAr = "مقارنة الرحلة B مقابل A: فرق أقصى ارتفاع = ${"%+.2f".format(deltaAlt)} m، وفرق أدنى جهد = ${"%+.2f".format(deltaBatt)} V.",
    )
  }
}
