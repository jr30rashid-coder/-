package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalProgressStore
import com.example.domain.ContentOrigin
import com.example.domain.ContentPackSecurity
import com.example.domain.ContentReviewStatus
import com.example.ui.DisplaySettings
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches Arabic Drones 3D title`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("مُسيّرات 3D", appName)
  }

  @Test
  fun `merged manifest contains zero network or hardware permissions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_PERMISSIONS,
    )
    val requested = packageInfo.requestedPermissions?.toSet().orEmpty()

    assertFalse("INTERNET permission must not be present", Manifest.permission.INTERNET in requested)
    assertFalse(
      "ACCESS_NETWORK_STATE permission must not be present",
      Manifest.permission.ACCESS_NETWORK_STATE in requested,
    )
    assertFalse("BLUETOOTH permission must not be present", Manifest.permission.BLUETOOTH in requested)
    assertFalse(
      "ACCESS_FINE_LOCATION permission must not be present",
      Manifest.permission.ACCESS_FINE_LOCATION in requested,
    )
    assertFalse("CAMERA permission must not be present", Manifest.permission.CAMERA in requested)
    assertTrue(
      "Application supportsRtl must be enabled",
      (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SUPPORTS_RTL) != 0,
    )
  }

  @Test
  fun `content pack security validates safe json, blocks scripts, and stages safe zips`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val validJson = """
      {
        "schemaVersion": 1,
        "packId": "extra_pack_1",
        "titleAr": "حزمة إضافية",
        "license": "CC BY-SA 4.0",
        "lessons": [{
          "id": "extra_l1",
          "trackId": "track_1",
          "titleAr": "درس مستورد تجريبي",
          "summaryAr": "ملخص الدرس المستورد",
          "sourceNoteAr": "مصدر محلي",
          "sections": [{"titleAr": "مقدمة", "bodyAr": "نص تعليمي آمن"}]
        }]
      }
    """.trimIndent().toByteArray(Charsets.UTF_8)

    val validRes = ContentPackSecurity.validateJsonPack(validJson)
    assertTrue(validRes is ContentPackSecurity.ImportValidationResult.Valid)
    val pack = (validRes as ContentPackSecurity.ImportValidationResult.Valid).pack
    assertEquals(ContentOrigin.IMPORTED, pack.lessons.first().origin)
    assertEquals(ContentReviewStatus.REVIEW_REQUIRED, pack.lessons.first().reviewStatus)

    val scriptJson = """{"schemaVersion":1,"packId":"bad","titleAr":"<script>alert(1)</script>","license":"CC0"}"""
      .toByteArray(Charsets.UTF_8)
    val scriptRes = ContentPackSecurity.validateJsonPack(scriptJson)
    assertTrue(scriptRes is ContentPackSecurity.ImportValidationResult.Rejected)

    // ZIP path traversal & forbidden .dex rejections
    val traversalZip = buildZip(listOf("../evil.json" to validJson))
    val traversalRes = ContentPackSecurity.inspectAndStageZipPack(traversalZip)
    assertTrue(traversalRes is ContentPackSecurity.ImportValidationResult.Rejected)
    assertEquals("ZIP_PATH_TRAVERSAL", (traversalRes as ContentPackSecurity.ImportValidationResult.Rejected).code)

    val dexZip = buildZip(listOf("pack.json" to validJson, "classes.dex" to byteArrayOf(1, 2, 3)))
    val dexRes = ContentPackSecurity.inspectAndStageZipPack(dexZip)
    assertTrue(dexRes is ContentPackSecurity.ImportValidationResult.Rejected)
    assertEquals("ZIP_FORBIDDEN_EXTENSION", (dexRes as ContentPackSecurity.ImportValidationResult.Rejected).code)

    val stagingDir = java.io.File(context.cacheDir, "test_staging")
    val finalDir = java.io.File(context.filesDir, "test_packs")
    val safeZip = buildZip(listOf("pack.json" to validJson))
    val safeZipRes = ContentPackSecurity.inspectAndStageZipPack(safeZip, stagingDir, finalDir)
    assertTrue(safeZipRes is ContentPackSecurity.ImportValidationResult.Valid)
    assertTrue(java.io.File(finalDir, "extra_pack_1/pack.json").exists())
  }

  @Test
  fun `local progress store round-trip succeeds and rejects tampered or incompatible backups`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val progress = LocalProgressStore.PersistedProgress(
      completedLessonIds = setOf("t1_l1", "t1_l2"),
      quizBestPercentages = mapOf("t1_l1" to 100),
      completedScenarioIds = mapOf("scen_hover_5m" to true).keys,
      displaySettings = DisplaySettings(fontScale = 1.25f, highContrast = true, reducedMotion = true),
    )

    assertTrue(LocalProgressStore.saveToAppPrivateStorage(context, progress))
    val loaded = LocalProgressStore.loadFromAppPrivateStorage(context)
    assertEquals(progress, loaded)

    val backupJson = LocalProgressStore.serializeToBackupEnvelopeJson(progress)
    val restored = LocalProgressStore.parseAndValidateBackupJson(backupJson.toByteArray(Charsets.UTF_8))
    assertTrue(restored is LocalProgressStore.RestoreResult.Success)
    assertEquals(progress, (restored as LocalProgressStore.RestoreResult.Success).progress)

    // Tampered payload fails SHA-256 check
    val tampered = backupJson.replace("\"t1_l1\"", "\"t9_hacked\"")
    val tamperedRes = LocalProgressStore.parseAndValidateBackupJson(tampered.toByteArray(Charsets.UTF_8))
    assertTrue(tamperedRes is LocalProgressStore.RestoreResult.Failure)
    assertEquals("CHECKSUM_MISMATCH", (tamperedRes as LocalProgressStore.RestoreResult.Failure).code)

    // Unsupported version rejected
    val futureVersion = backupJson.replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
    val versionRes = LocalProgressStore.parseAndValidateBackupJson(futureVersion.toByteArray(Charsets.UTF_8))
    assertTrue(versionRes is LocalProgressStore.RestoreResult.Failure)
    assertEquals("UNSUPPORTED_VERSION", (versionRes as LocalProgressStore.RestoreResult.Failure).code)
  }

  private fun buildZip(entries: List<Pair<String, ByteArray>>): ByteArray {
    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { zos ->
      entries.forEach { (name, data) ->
        zos.putNextEntry(ZipEntry(name))
        zos.write(data)
        zos.closeEntry()
      }
    }
    return bos.toByteArray()
  }
}
