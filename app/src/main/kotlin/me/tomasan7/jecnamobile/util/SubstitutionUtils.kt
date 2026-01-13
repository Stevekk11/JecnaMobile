package me.tomasan7.jecnamobile.util

import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.Lesson

internal data class SubstitutionOverrides(
    val subjectFull: String? = null,
    val classroom: String? = null,
    val teacherFull: String? = null,
    val teacherTag: String? = null,
)

internal fun String.cleanSubstitutionToken(): String =
    trim()
        .trimEnd(',', ';', '.', '+')

internal fun String.extractGroupSubstitutionForLesson(lesson: Lesson): String?
{
    val raw = this
    val markers = listOf("1/2", "2/2", "1/3", "2/3", "3/3")
    val foundInText = markers.filter { raw.contains(it) }.sortedBy { raw.indexOf(it) }
    
    if (foundInText.isEmpty())
        return raw
    
    val lessonGroup = lesson.group ?: return null
    
    val myMarker = foundInText.find { m ->
        lessonGroup == m || lessonGroup == m.substringBefore('/')
    } ?: return null

    val myMarkerIndex = foundInText.indexOf(myMarker)
    val start = raw.indexOf(myMarker)
    val end = if (myMarkerIndex + 1 < foundInText.size) raw.indexOf(foundInText[myMarkerIndex + 1]) else raw.length

    return raw.substring(start, end).trim()
}

internal fun String.extractSubstitutionOverridesForLesson(lesson: Lesson): SubstitutionOverrides? {
    val skipKeywords = listOf("odpadá", "0", "oběd")
    if (skipKeywords.any { this.contains(it, ignoreCase = true) }) {
        return null
    }

    val tail = if (lesson.group == null) {
        this.trim()
    } else {
        val markers = listOf("1/2", "2/2", "1/3", "2/3", "3/3")
        val foundMarker = markers.find { this.contains(it) } ?: return null

        val idx = indexOf(foundMarker)
        substring(idx + foundMarker.length).trim()
    }

    if (tail.isBlank()) return null

    val rawParts = tail.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (rawParts.size < 2) return null

    val subjectShort = rawParts.getOrNull(0)?.cleanSubstitutionToken()
    val classroom = rawParts.getOrNull(1)?.cleanSubstitutionToken() // Will capture "D2,D6"

    val teacherToken = rawParts.getOrNull(2)
    val teacherTag = teacherToken?.substringBefore('(')?.cleanSubstitutionToken()

    return SubstitutionOverrides(
        subjectFull = subjectShort,
        classroom = classroom,
        teacherTag = teacherTag
    )
}

/**
 * Resolves the full subject name from a subject short, using lessons already present in the timetable.
 * Falls back to returning the short name unchanged when it can't be resolved.
 */
internal fun resolveSubjectFullNameFromLessons(subjectShort: String, allLessons: List<Lesson>): String {
    return allLessons.firstOrNull { it.subjectName.short?.equals(subjectShort, ignoreCase = true) == true }
        ?.subjectName
        ?.full
        ?: subjectShort
}

/**
 * Resolves a [TeacherReference] for a substitution teacher tag.
 *
 * Order (keeps current Timetable.kt behavior):
 * 1) Match from provided [teacherReferences]
 * 2) Otherwise, try to infer from [allLessons] by matching teacher short tags
 * 3) Otherwise, return null
 */
internal fun resolveTeacherReferenceForTag(
    tag: String,
    teacherReferences: Set<TeacherReference>?,
    allLessons: List<Lesson>
): TeacherReference? {
    return teacherReferences
        ?.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
        ?: run {
            val name = allLessons.firstOrNull { it.teacherName?.short?.equals(tag, ignoreCase = true) == true }
                ?.teacherName
            if (name?.short != null) TeacherReference(name.full, name.short!!) else null
        }
}

/**
 * Builds a resolved [SubstitutionOverrides] by filling in full names (subject + teacher) where possible.
 * This is a pure helper extracted from Timetable UI.
 */
internal fun resolveSubstitutionOverrides(
    rawOverrides: SubstitutionOverrides?,
    teacherReferences: Set<TeacherReference>?,
    allLessons: List<Lesson>
): SubstitutionOverrides? {
    val subjectShort = rawOverrides?.subjectFull
    val resolvedSubjectFull = subjectShort?.let { resolveSubjectFullNameFromLessons(it, allLessons) }

    val resolvedTeacherRef: TeacherReference? = rawOverrides?.teacherTag?.let { tag ->
        resolveTeacherReferenceForTag(tag, teacherReferences, allLessons)
    }

    return rawOverrides?.copy(
        subjectFull = resolvedSubjectFull,
        teacherFull = resolvedTeacherRef?.fullName,
        teacherTag = resolvedTeacherRef?.tag ?: rawOverrides.teacherTag
    )
}

/**
 * Extracts "Subbing | Missing" text from a substitution string containing the "Missing(Subbing)" pattern.
 * Returns null when the pattern isn't present.
 */
internal fun String.getSpojTeacherText(): String? {
    val regex = Regex("(\\w+)\\(([^)]+)\\)")
    val match = regex.find(this) ?: return null
    return "${match.groupValues[2]} | ${match.groupValues[1]}"
}
