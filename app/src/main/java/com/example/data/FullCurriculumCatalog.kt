package com.example.data

import com.example.domain.ContentPackSecurity
import com.example.domain.CurriculumTrack
import com.example.domain.LessonUnit

object FullCurriculumCatalog {

  fun getBundledTracks(): List<CurriculumTrack> =
    CurriculumTracks1To4.buildTracks() + CurriculumTracks5To8.buildTracks()

  fun mergeTracksWithImportedPacks(
    bundled: List<CurriculumTrack>,
    importedPacks: List<ContentPackSecurity.ImportedContentPack>,
  ): List<CurriculumTrack> {
    if (importedPacks.isEmpty()) return bundled
    val importedLessons = importedPacks.flatMap { it.lessons }
    val existingIds = bundled.map { it.id }.toSet()
    val updatedBundled = bundled.map { track ->
      val extra = importedLessons.filter { it.trackId == track.id }
      if (extra.isEmpty()) track else track.copy(lessons = track.lessons + extra)
    }
    val unmatchedImported = importedLessons.filter { it.trackId !in existingIds }
    return if (unmatchedImported.isEmpty()) {
      updatedBundled
    } else {
      updatedBundled + CurriculumTrack(
        id = "track_imported",
        number = 9,
        titleAr = "حزم تعليمية مستوردة محلياً",
        subtitleAr = "محتوى إضافي مستورد تحت المراجعة المحلية",
        targetLevel = unmatchedImported.first().level,
        lessons = unmatchedImported,
      )
    }
  }

  fun findLesson(tracks: List<CurriculumTrack>, lessonId: String): LessonUnit? =
    tracks.asSequence().flatMap { it.lessons.asSequence() }.firstOrNull { it.id == lessonId }
}
