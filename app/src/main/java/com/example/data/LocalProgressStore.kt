package com.example.data

import android.content.Context
import com.example.domain.ContentPackSecurity
import com.example.domain.LearningEvaluator
import com.example.ui.DisplaySettings
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 11: Minimal app-private local progress persistence & checksummed backup/restore.
 * Zero network, zero accounts, zero storage permissions (uses filesDir & SAF URIs).
 */
object LocalProgressStore {

  const val SCHEMA_VERSION = 1
  const val MAX_BACKUP_BYTES = 128 * 1024
  private const val PROGRESS_FILENAME = "drones3d_progress_v1.json"

  data class PersistedProgress(
    val schemaVersion: Int = SCHEMA_VERSION,
    val completedLessonIds: Set<String> = emptySet(),
    val quizBestPercentages: Map<String, Int> = emptyMap(),
    val completedScenarioIds: Set<String> = emptySet(),
    val displaySettings: DisplaySettings = DisplaySettings(),
  )

  sealed class RestoreResult {
    data class Success(val progress: PersistedProgress, val sha256: String) : RestoreResult()
    data class Failure(val reasonAr: String, val code: String) : RestoreResult()
  }

  fun saveToAppPrivateStorage(context: Context, progress: PersistedProgress): Boolean {
    return try {
      val json = serializeToBackupEnvelopeJson(progress)
      val target = File(context.filesDir, PROGRESS_FILENAME)
      val temp = File(context.filesDir, "$PROGRESS_FILENAME.tmp")
      temp.writeText(json, Charsets.UTF_8)
      if (!temp.renameTo(target)) {
        target.writeText(json, Charsets.UTF_8)
        temp.delete()
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  fun loadFromAppPrivateStorage(context: Context): PersistedProgress {
    val file = File(context.filesDir, PROGRESS_FILENAME)
    if (!file.exists()) return PersistedProgress()
    return when (val res = parseAndValidateBackupJson(file.readBytes())) {
      is RestoreResult.Success -> res.progress
      is RestoreResult.Failure -> PersistedProgress()
    }
  }

  fun serializeToBackupEnvelopeJson(progress: PersistedProgress): String {
    val payload = JSONObject().apply {
      put("schemaVersion", SCHEMA_VERSION)
      put("completedLessonIds", JSONArray(progress.completedLessonIds.sorted()))
      put("completedScenarioIds", JSONArray(progress.completedScenarioIds.sorted()))
      val scoresObj = JSONObject()
      progress.quizBestPercentages.toSortedMap().forEach { (k, v) ->
        scoresObj.put(k, v.coerceIn(0, 100))
      }
      put("quizBestPercentages", scoresObj)
      put(
        "displaySettings",
        JSONObject().apply {
          put("fontScale", LearningEvaluator.clampFontScale(progress.displaySettings.fontScale).toDouble())
          put("highContrast", progress.displaySettings.highContrast)
          put("reducedMotion", progress.displaySettings.reducedMotion)
        },
      )
    }
    val payloadString = payload.toString()
    val payloadSha = ContentPackSecurity.sha256Hex(payloadString.toByteArray(Charsets.UTF_8))

    return JSONObject().apply {
      put("format", "drones3d_local_backup")
      put("schemaVersion", SCHEMA_VERSION)
      put("payloadSha256", payloadSha)
      put("payload", payload)
    }.toString(2)
  }

  fun parseAndValidateBackupJson(rawBytes: ByteArray): RestoreResult {
    if (rawBytes.isEmpty()) {
      return RestoreResult.Failure("ملف النسخة الاحتياطية فارغ.", "EMPTY_BACKUP")
    }
    if (rawBytes.size > MAX_BACKUP_BYTES) {
      return RestoreResult.Failure("تجاوزت النسخة الاحتياطية الحد الأقصى للحجم (${MAX_BACKUP_BYTES / 1024} KB).", "OVERSIZED_BACKUP")
    }

    val envelope = try {
      JSONObject(rawBytes.toString(Charsets.UTF_8))
    } catch (_: Exception) {
      return RestoreResult.Failure("تعذر قراءة ملف النسخة الاحتياطية (JSON تالف).", "CORRUPT_JSON")
    }

    if (envelope.optString("format", "") != "drones3d_local_backup") {
      return RestoreResult.Failure("صيغة الملف لا تنتمي لنسخ مُسيّرات 3D الاحتياطية.", "INVALID_FORMAT")
    }

    val version = envelope.optInt("schemaVersion", -1)
    if (version != SCHEMA_VERSION) {
      return RestoreResult.Failure("إصدار النسخة الاحتياطية ($version) غير متوافق مع الإصدار الحالي ($SCHEMA_VERSION).", "UNSUPPORTED_VERSION")
    }

    val expectedSha = envelope.optString("payloadSha256", "")
    val payload = envelope.optJSONObject("payload")
      ?: return RestoreResult.Failure("الحزمة الداخلية مفقودة في النسخة الاحتياطية.", "MISSING_PAYLOAD")

    val actualSha = ContentPackSecurity.sha256Hex(payload.toString().toByteArray(Charsets.UTF_8))
    if (expectedSha.isBlank() || !expectedSha.equals(actualSha, ignoreCase = true)) {
      return RestoreResult.Failure("فشل التحقق من بصمة التكامل SHA-256 (الملف معدّل أو تالف).", "CHECKSUM_MISMATCH")
    }

    val completedLessons = mutableSetOf<String>()
    val lessonsArr = payload.optJSONArray("completedLessonIds") ?: JSONArray()
    if (lessonsArr.length() > 200) {
      return RestoreResult.Failure("تجاوزت قائمة الدروس الحد الأقصى المسموح.", "TOO_MANY_RECORDS")
    }
    for (i in 0 until lessonsArr.length()) {
      val id = lessonsArr.optString(i, "").trim()
      if (id.isNotEmpty() && id.length <= 64) completedLessons.add(id)
    }

    val completedScenarios = mutableSetOf<String>()
    val scenArr = payload.optJSONArray("completedScenarioIds") ?: JSONArray()
    if (scenArr.length() > 100) {
      return RestoreResult.Failure("تجاوزت قائمة السيناريوهات الحد الأقصى المسموح.", "TOO_MANY_RECORDS")
    }
    for (i in 0 until scenArr.length()) {
      val id = scenArr.optString(i, "").trim()
      if (id.isNotEmpty() && id.length <= 64) completedScenarios.add(id)
    }

    val scores = mutableMapOf<String, Int>()
    val scoresObj = payload.optJSONObject("quizBestPercentages") ?: JSONObject()
    val keys = scoresObj.keys()
    while (keys.hasNext()) {
      val k = keys.next()
      val v = scoresObj.optInt(k, -1)
      if (v !in 0..100) {
        return RestoreResult.Failure("تم رصد درجة اختبار خارج النطاق 0..100.", "INVALID_SCORE")
      }
      scores[k] = v
    }

    val settingsObj = payload.optJSONObject("displaySettings")
    val settings = if (settingsObj != null) {
      DisplaySettings(
        fontScale = LearningEvaluator.clampFontScale(settingsObj.optDouble("fontScale", 1.0).toFloat()),
        highContrast = settingsObj.optBoolean("highContrast", false),
        reducedMotion = settingsObj.optBoolean("reducedMotion", false),
      )
    } else {
      DisplaySettings()
    }

    return RestoreResult.Success(
      progress = PersistedProgress(
        schemaVersion = version,
        completedLessonIds = completedLessons,
        quizBestPercentages = scores,
        completedScenarioIds = completedScenarios,
        displaySettings = settings,
      ),
      sha256 = actualSha,
    )
  }
}
