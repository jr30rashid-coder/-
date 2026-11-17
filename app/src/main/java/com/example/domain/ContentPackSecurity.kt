package com.example.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 2: Safe Local Content Schema & ZIP/JSON Import Guard.
 * Enforces data-only content, strict bounds, path-traversal/symlink/executable rejection,
 * and atomic staging with rollback.
 */
object ContentPackSecurity {

  const val SUPPORTED_SCHEMA_VERSION = 1
  const val MAX_JSON_BYTES = 512 * 1024
  const val MAX_ZIP_ENTRIES = 30
  const val MAX_ZIP_ENTRY_BYTES = 256 * 1024
  const val MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES = 1024 * 1024
  const val MAX_PATH_DEPTH = 4
  const val MAX_LESSONS_PER_PACK = 20

  private val ID_REGEX = Regex("^[a-z0-9_\\-]{3,48}$")
  private val FORBIDDEN_EXTENSIONS = setOf(
    "apk", "dex", "jar", "so", "sh", "bash", "bat", "cmd", "exe", "dll",
    "js", "mjs", "py", "rb", "php", "wasm", "class", "elf", "bin",
  )
  private val ALLOWED_EXTENSIONS = setOf("json", "md", "txt")
  private val FORBIDDEN_PAYLOAD_PATTERNS = listOf(
    "<script", "javascript:", "eval(", "Function(", "Runtime.getRuntime", "ProcessBuilder", "System.load",
  )

  data class ImportedContentPack(
    val packId: String,
    val schemaVersion: Int,
    val titleAr: String,
    val license: String,
    val sha256: String,
    val lessons: List<LessonUnit>,
  )

  sealed class ImportValidationResult {
    data class Valid(val pack: ImportedContentPack) : ImportValidationResult()
    data class Rejected(val reasonAr: String, val code: String) : ImportValidationResult()
  }

  fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun validateJsonPack(
    rawBytes: ByteArray,
    expectedSha256: String? = null,
  ): ImportValidationResult {
    if (rawBytes.isEmpty()) {
      return ImportValidationResult.Rejected("ملف الحزمة فارغ.", "EMPTY_PACK")
    }
    if (rawBytes.size > MAX_JSON_BYTES) {
      return ImportValidationResult.Rejected(
        "حجم ملف JSON يتجاوز الحد المسموح (${MAX_JSON_BYTES / 1024} KB).",
        "SIZE_LIMIT_EXCEEDED",
      )
    }

    val actualSha256 = sha256Hex(rawBytes)
    if (!expectedSha256.isNullOrBlank() && !actualSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
      return ImportValidationResult.Rejected("فشل التحقق من بصمة SHA-256 للحزمة.", "SHA256_MISMATCH")
    }

    val text = rawBytes.toString(Charsets.UTF_8)
    FORBIDDEN_PAYLOAD_PATTERNS.forEach { pattern ->
      if (text.contains(pattern, ignoreCase = true)) {
        return ImportValidationResult.Rejected(
          "تم رفض الحزمة لاحتوائها على نمط غير آمن أو قابل للتنفيذ ($pattern).",
          "FORBIDDEN_EXECUTABLE_PATTERN",
        )
      }
    }

    val root = try {
      JSONObject(text)
    } catch (e: Exception) {
      return ImportValidationResult.Rejected("صيغة JSON غير صالحة.", "INVALID_JSON")
    }

    val schemaVersion = root.optInt("schemaVersion", -1)
    if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
      return ImportValidationResult.Rejected(
        "إصدار المخطط ($schemaVersion) غير مدعوم؛ المتوقع $SUPPORTED_SCHEMA_VERSION.",
        "UNSUPPORTED_SCHEMA_VERSION",
      )
    }

    val packId = root.optString("packId", "")
    if (!ID_REGEX.matches(packId)) {
      return ImportValidationResult.Rejected("معرّف الحزمة غير صالح (يجب أن يكون حروفاً إنجليزية صغيرة وأرقاماً).", "INVALID_PACK_ID")
    }

    val titleAr = root.optString("titleAr", "").trim()
    val license = root.optString("license", "").trim()
    if (titleAr.isEmpty() || license.isEmpty()) {
      return ImportValidationResult.Rejected("عنوان الحزمة والترخيص حقول إلزامية.", "MISSING_METADATA")
    }

    val lessonsArray = root.optJSONArray("lessons") ?: JSONArray()
    if (lessonsArray.length() == 0 || lessonsArray.length() > MAX_LESSONS_PER_PACK) {
      return ImportValidationResult.Rejected(
        "عدد الدروس في الحزمة يجب أن يكون بين 1 و $MAX_LESSONS_PER_PACK.",
        "INVALID_LESSON_COUNT",
      )
    }

    val lessons = mutableListOf<LessonUnit>()
    val seenIds = mutableSetOf<String>()
    for (i in 0 until lessonsArray.length()) {
      val obj = lessonsArray.optJSONObject(i)
        ?: return ImportValidationResult.Rejected("عنصر درس غير صالح عند الفهرس $i.", "INVALID_LESSON_OBJECT")
      val lessonId = obj.optString("id", "")
      val trackId = obj.optString("trackId", "")
      if (!ID_REGEX.matches(lessonId) || !ID_REGEX.matches(trackId) || !seenIds.add(lessonId)) {
        return ImportValidationResult.Rejected("معرّف درس أو مسار غير صالح أو مكرر ($lessonId).", "INVALID_OR_DUPLICATE_ID")
      }
      val lessonTitle = obj.optString("titleAr", "").trim()
      val summary = obj.optString("summaryAr", "").trim()
      val sourceNote = obj.optString("sourceNoteAr", "").trim()
      if (lessonTitle.isEmpty() || summary.isEmpty() || sourceNote.isEmpty()) {
        return ImportValidationResult.Rejected("بيانات الدرس ($lessonId) غير مكتملة.", "INCOMPLETE_LESSON_FIELDS")
      }

      val sectionsArr = obj.optJSONArray("sections") ?: JSONArray()
      if (sectionsArr.length() == 0) {
        return ImportValidationResult.Rejected("يجب أن يحتوي الدرس ($lessonId) على قسم شرح واحد على الأقل.", "EMPTY_SECTIONS")
      }
      val sections = buildList {
        for (s in 0 until sectionsArr.length()) {
          val sec = sectionsArr.optJSONObject(s)
            ?: return ImportValidationResult.Rejected("قسم غير صالح في الدرس ($lessonId).", "INVALID_SECTION")
          val sTitle = sec.optString("titleAr", "").trim()
          val sBody = sec.optString("bodyAr", "").trim()
          val sTakeaway = sec.optString("keyTakeawayAr", "").trim()
          if (sTitle.isEmpty() || sBody.isEmpty()) {
            return ImportValidationResult.Rejected("قسم ناقص في الدرس ($lessonId).", "INCOMPLETE_SECTION")
          }
          add(LessonSection(sTitle, sBody, sTakeaway.ifEmpty { sTitle }))
        }
      }

      val level = when (obj.optString("level", "BEGINNER")) {
        "INTERMEDIATE" -> SkillLevel.INTERMEDIATE
        "ADVANCED" -> SkillLevel.ADVANCED
        "SPECIALIST" -> SkillLevel.SPECIALIST
        else -> SkillLevel.BEGINNER
      }

      // Imported packs NEVER auto-promote themselves to APPROVED; always require local review.
      val importedStatus = ContentReviewStatus.REVIEW_REQUIRED

      val prereqs = mutableListOf<String>()
      val prereqArr = obj.optJSONArray("prerequisiteLessonIds") ?: JSONArray()
      for (p in 0 until prereqArr.length()) {
        val pid = prereqArr.optString(p, "")
        if (!ID_REGEX.matches(pid)) {
          return ImportValidationResult.Rejected("مرجع متطلب سابق غير صالح ($pid).", "INVALID_PREREQ_REF")
        }
        prereqs.add(pid)
      }

      lessons.add(
        LessonUnit(
          id = "${packId}__${lessonId}",
          trackId = trackId,
          titleAr = lessonTitle,
          level = level,
          estimatedMinutes = obj.optInt("estimatedMinutes", 10).coerceIn(3, 90),
          prerequisiteLessonIds = prereqs,
          reviewStatus = importedStatus,
          origin = ContentOrigin.IMPORTED,
          sourceNoteAr = "$sourceNote (مستورد محلياً — الترخيص: $license)",
          objectivesAr = listOf("محتوى مستورد محلياً يخضع للمراجعة البشرية قبل الاعتماد."),
          sections = sections,
          equations = emptyList(),
          glossary = emptyList(),
          includeComponentDiagram = false,
          safetyCards = emptyList(),
          conceptMcqs = emptyList(),
          reviewQuiz = emptyList(),
          summaryAr = summary,
        ),
      )
    }

    return ImportValidationResult.Valid(
      ImportedContentPack(
        packId = packId,
        schemaVersion = schemaVersion,
        titleAr = titleAr,
        license = license,
        sha256 = actualSha256,
        lessons = lessons,
      ),
    )
  }

  /**
   * Validates a ZIP content pack before writing any file, using staging + atomic promotion
   * when a target directory is provided, and rejecting traversal, symlinks, executables, and zip bombs.
   */
  fun inspectAndStageZipPack(
    zipBytes: ByteArray,
    stagingRootDir: File? = null,
    finalTargetDir: File? = null,
  ): ImportValidationResult {
    if (zipBytes.isEmpty()) {
      return ImportValidationResult.Rejected("ملف ZIP فارغ.", "EMPTY_ZIP")
    }

    var entryCount = 0
    var totalUncompressed = 0L
    var manifestJsonBytes: ByteArray? = null
    val validatedFiles = linkedMapOf<String, ByteArray>()

    try {
      ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
        while (true) {
          val entry: ZipEntry = zis.nextEntry ?: break
          entryCount++
          if (entryCount > MAX_ZIP_ENTRIES) {
            return ImportValidationResult.Rejected("تجاوزت الحزمة الحد الأقصى لعدد الملفات ($MAX_ZIP_ENTRIES).", "ZIP_TOO_MANY_ENTRIES")
          }

          val rawName = entry.name
          if (rawName.startsWith("/") || rawName.startsWith("\\") || rawName.contains(":")) {
            return ImportValidationResult.Rejected("مسار مطلق مرفوض داخل الأرشيف: $rawName", "ZIP_ABSOLUTE_PATH")
          }
          if (rawName.contains("..") || rawName.contains("\\")) {
            return ImportValidationResult.Rejected("مسار اختراق (Path Traversal) مرفوض: $rawName", "ZIP_PATH_TRAVERSAL")
          }
          // Reject potential symlink / unix mode indicators stored in extra or comment hints
          if (entry.comment?.contains("symlink", ignoreCase = true) == true) {
            return ImportValidationResult.Rejected("الروابط الرمزية مرفوضة داخل الأرشيف.", "ZIP_SYMLINK_REJECTED")
          }

          val segments = rawName.trimEnd('/').split('/').filter { it.isNotEmpty() }
          if (segments.size > MAX_PATH_DEPTH) {
            return ImportValidationResult.Rejected("عمق المجلدات يتجاوز الحد المسموح ($MAX_PATH_DEPTH).", "ZIP_MAX_DEPTH_EXCEEDED")
          }

          if (!entry.isDirectory) {
            val ext = rawName.substringAfterLast('.', "").lowercase()
            if (ext in FORBIDDEN_EXTENSIONS || ext !in ALLOWED_EXTENSIONS) {
              return ImportValidationResult.Rejected("امتداد ملف مرفوض أو غير مصرح به: .$ext", "ZIP_FORBIDDEN_EXTENSION")
            }

            val buffer = ByteArray(4096)
            val out = ByteArrayOutputStream()
            var entryBytes = 0
            while (true) {
              val read = zis.read(buffer)
              if (read <= 0) break
              entryBytes += read
              totalUncompressed += read
              if (entryBytes > MAX_ZIP_ENTRY_BYTES) {
                return ImportValidationResult.Rejected("ملف مفرد داخل ZIP يتجاوز الحد المسموح.", "ZIP_ENTRY_TOO_LARGE")
              }
              if (totalUncompressed > MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES) {
                return ImportValidationResult.Rejected("الحجم الإجمالي بعد الفك يتجاوز 1 MB (حماية من قنبلة ZIP).", "ZIP_BOMB_LIMIT")
              }
              out.write(buffer, 0, read)
            }
            val fileBytes = out.toByteArray()
            validatedFiles[rawName] = fileBytes
            if (rawName == "pack.json") {
              manifestJsonBytes = fileBytes
            }
          }
          zis.closeEntry()
        }
      }
    } catch (e: Exception) {
      return ImportValidationResult.Rejected("تعذر قراءة ملف ZIP بأمان.", "ZIP_CORRUPT")
    }

    val packJson = manifestJsonBytes
      ?: return ImportValidationResult.Rejected("يفتقد الأرشيف إلى ملف pack.json في الجذر.", "ZIP_MISSING_PACK_JSON")

    val jsonValidation = validateJsonPack(packJson)
    if (jsonValidation !is ImportValidationResult.Valid) {
      return jsonValidation
    }

    // Staging + safe atomic promotion if directories are supplied
    if (stagingRootDir != null && finalTargetDir != null) {
      val stagingDir = File(stagingRootDir, "stage_${jsonValidation.pack.packId}")
      val backupDir = File(stagingRootDir, "backup_${jsonValidation.pack.packId}")
      try {
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()
        val canonicalRoot = stagingDir.canonicalPath
        for ((relativePath, bytes) in validatedFiles) {
          val targetFile = File(stagingDir, relativePath)
          if (!targetFile.canonicalPath.startsWith(canonicalRoot + File.separator)) {
            throw SecurityException("Canonical path escape blocked")
          }
          targetFile.parentFile?.mkdirs()
          targetFile.writeBytes(bytes)
        }
        finalTargetDir.mkdirs()
        val destPackDir = File(finalTargetDir, jsonValidation.pack.packId)
        if (destPackDir.exists()) {
          if (backupDir.exists()) backupDir.deleteRecursively()
          destPackDir.renameTo(backupDir)
        }
        if (!stagingDir.renameTo(destPackDir)) {
          // Rollback if move fails
          if (backupDir.exists() && !destPackDir.exists()) {
            backupDir.renameTo(destPackDir)
          }
          return ImportValidationResult.Rejected("فشل نقل الحزمة من منطقة التجهيز الآمنة.", "STAGING_PROMOTION_FAILED")
        }
        if (backupDir.exists()) backupDir.deleteRecursively()
      } catch (e: Exception) {
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        return ImportValidationResult.Rejected("تم التراجع عن الاستيراد لحماية السلامة المحلية.", "STAGING_ROLLBACK")
      }
    }

    return jsonValidation
  }
}
