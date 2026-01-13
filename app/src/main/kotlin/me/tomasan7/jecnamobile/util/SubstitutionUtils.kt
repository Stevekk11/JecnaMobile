package me.tomasan7.jecnamobile.util

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

/**
 * Extracts the part of a substitution text that belongs to the given [lesson] when the lesson spot is split by group.
 *
 * Rules:
 * - If the text contains both "1/2" and "2/2", it will be split at the first occurrence of the second marker,
 *   and each chunk is routed to its respective group.
 * - If it contains only one marker, it will be shown only on the matching group.
 * - If it contains no markers (or group is unknown), we fallback to showing it on the first lesson (legacy behavior).
 */
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

/**
 * From substitution text, extract the substitution subject(short) + classroom + substituting teacher(tag)
 * for the given lesson.
 *
 * Examples:
 *  - "2/2 CIT 19c Ku(Ka)+" -> subjectShort=CIT, classroom=19c, teacherTag=Ku
 *  - "1/2 TP 27 (Ms) odpadá..., 2/2 TP 23 Ma(Pe)+" -> for group 2: subjectShort=TP, classroom=23, teacherTag=Ma
 *
 * Rules (per requirement):
 *  - word right after group marker is subject short
 *  - next word is classroom
 *  - next word is substituting teacher (ignore anything in parentheses, which is the missing teacher)
 */
internal fun String.extractSubstitutionOverridesForLesson(lesson: Lesson): SubstitutionOverrides? {
    val tail = if (lesson.group == null) {
        // For non-split lessons, use the entire string
        if(this.contains("odpadá") || this.contains("0")) {
            return null
        }
        this.trim()
    } else {
        val marker = when {
            lesson.group == "1/2" || lesson.group == "1" -> if (contains("1/3")) "1/3" else "1/2"
            lesson.group == "2/2" || lesson.group == "2" -> if (contains("2/3")) "2/3" else "2/2"
            lesson.group == "3/3" || lesson.group == "3" -> "3/3"
            else -> return null
        }
        val idx = indexOf(marker)
        if (idx == -1) return null
        substring(idx + marker.length).trim()
    }

    if (tail.isBlank()) return null

    val rawParts = tail.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (rawParts.isEmpty()) return null

    val subjectShort = rawParts.getOrNull(0)?.cleanSubstitutionToken()
    val classroom = rawParts.getOrNull(1)?.cleanSubstitutionToken()

    val teacherToken = rawParts.getOrNull(2)?.cleanSubstitutionToken()
    val teacherTag = teacherToken?.substringBefore('(')?.cleanSubstitutionToken()

    if (subjectShort.isNullOrBlank() && classroom.isNullOrBlank() && teacherTag.isNullOrBlank()) return null

    return SubstitutionOverrides(
        subjectFull = subjectShort,
        classroom = classroom,
        teacherTag = teacherTag
    )
}
