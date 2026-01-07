package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.*
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.manipulate
import java.time.DayOfWeek

@Composable
fun Timetable(
    timetable: Timetable,
    modifier: Modifier = Modifier,
    hideClass: Boolean = false,
    showSubstitutions: Boolean = true,
    onTeacherClick: (TeacherReference) -> Unit = {},
    onClassroomClick: (ClassroomReference) -> Unit = { }
)
{
    val mostLessonsInLessonSpotInEachDay = remember(timetable) {
        timetable.run {
            val result = mutableMapOf<DayOfWeek, Int>()

            for (day in days)
            {
                var dayResult = 0

                for (lessonSpot in getLessonSpotsForDay(day)!!)
                    if (lessonSpot.size > dayResult)
                        dayResult = lessonSpot.size

                result[day] = dayResult
            }

            result
        }
    }

    val dialogState = rememberObjectDialogState<Lesson>()

    Box(modifier = modifier) {
        val breakWidth = 5.dp
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(breakWidth)
        ) {
            Row {
                timetable.lessonPeriods.forEachIndexed { i, lessonPeriod ->
                    if (i == 0)
                        HorizontalSpacer(30.dp)

                    HorizontalSpacer(breakWidth)

                    TimetableLessonPeriod(
                        modifier = Modifier.size(width = 100.dp, height = 50.dp),
                        lessonPeriod = lessonPeriod,
                        hourIndex = i + 1
                    )
                }
            }

            timetable.daysSorted.forEach { day ->
                val rowModifier = if (mostLessonsInLessonSpotInEachDay[day]!! <= 2)
                    Modifier.height(100.dp)
                else
                    Modifier.height(IntrinsicSize.Min)
                Row(rowModifier) {
                    DayLabel(
                        getWeekDayName(day).substring(0, 2), Modifier
                            .width(30.dp)
                            .fillMaxHeight()
                    )
                    HorizontalSpacer(breakWidth)
                    timetable.getLessonSpotsForDay(day)!!.forEach { lessonSpot ->
                        LessonSpot(
                            lessonSpot = lessonSpot,
                            onLessonClick = { dialogState.show(it) },
                            current = timetable.getCurrentLessonSpot() === lessonSpot,
                            next = timetable.getCurrentNextLessonSpot(takeEmpty = true) === lessonSpot,
                            hideClass = hideClass,
                            showSubstitutions = showSubstitutions,
                            breakWidth = breakWidth
                        )
                        HorizontalSpacer(breakWidth)
                    }
                }
            }
        }
        ObjectDialog(
            state = dialogState,
            onDismissRequest = { dialogState.hide() },
            content = { lesson ->
                LessonDialogContent(
                    lesson = lesson,
                    onCloseClick = { dialogState.hide() },
                    onTeacherClick = { onTeacherClick(it) },
                    onClassroomClick = { onClassroomClick(it) }
                )
            }
        )
    }
}

@Composable
private fun TimetableLessonPeriod(
    lessonPeriod: LessonPeriod,
    hourIndex: Int,
    modifier: Modifier = Modifier
)
{
    Surface(
        modifier = modifier,
        shadowElevation = ElevationLevel.level1,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).manipulate(1.5f),
        shape = RoundedCornerShape(5.dp),
    ) {
        Box(Modifier.padding(4.dp)) {
            Text(
                modifier = Modifier.align(Alignment.TopCenter),
                text = hourIndex.toString(),
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = lessonPeriod.toString(),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LessonSpot(
    lessonSpot: LessonSpot,
    onLessonClick: (Lesson) -> Unit = {},
    current: Boolean = false,
    next: Boolean = false,
    hideClass: Boolean = false,
    showSubstitutions: Boolean = true,
    breakWidth: Dp = 0.dp
)
{
    val totalWidth = lessonSpot.periodSpan * 100.dp + breakWidth * (lessonSpot.periodSpan - 1)
    var lessonSpotModifier = Modifier.width(totalWidth)

    if (lessonSpot.size <= 2)
        lessonSpotModifier = lessonSpotModifier.fillMaxHeight()

    Column(lessonSpotModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lessonSpot.forEachIndexed { index, lesson ->
            /* If there is < 2 lessons, they are stretched to  */
            var lessonModifier = Modifier.fillMaxWidth()
            lessonModifier = if (lessonSpot.size <= 2)
                lessonModifier.weight(1f)
            else
                lessonModifier.height(50.dp)

            val substitutionForLesson = remember(showSubstitutions, lessonSpot.substitution, lessonSpot.size, index, lesson.group) {
                if (!showSubstitutions) return@remember null
                val raw = lessonSpot.substitution ?: return@remember null

                // If the spot isn't split (or groups aren't known), keep original behavior.
                if (lessonSpot.size <= 1) return@remember if (index == 0) raw else null

                // For split lessons, try to route substitutions marked with 1/2 or 2/2 to the correct group.
                raw.extractGroupSubstitutionForLesson(lesson)
            }

            Lesson(
                modifier = lessonModifier,
                onClick = { onLessonClick(lesson) },
                lesson = lesson,
                current = current,
                next = next,
                hideClass = hideClass,
                substitution = substitutionForLesson
            )
        }
    }
}

/**
 * Extracts the part of a substitution text that belongs to the given [lesson] when the lesson spot is split by group.
 *
 * Rules:
 * - If the text contains both "1/2" and "2/2", it will be split at the first occurrence of the second marker,
 *   and each chunk is routed to its respective group.
 * - If it contains only one marker, it will be shown only on the matching group.
 * - If it contains no markers (or group is unknown), we fallback to showing it on the first lesson (legacy behavior).
 */
private fun String.extractGroupSubstitutionForLesson(lesson: Lesson): String?
{
    val raw = this
    val idx1 = raw.indexOf("1/2")
    val idx2 = raw.indexOf("2/2")

    // No group markers -> keep legacy behavior: show on the first lesson only.
    if (idx1 == -1 && idx2 == -1)
        return if (lesson.group == null || lesson.group == "0") raw else null

    // If we don't know the group's id, we can't route reliably.
    val lessonGroup = lesson.group

    fun matchesGroup(marker: String): Boolean
    {
        if (lessonGroup == null) return false
        // Be permissive: group in API seems to be a string number ("1", "2").
        // Some data might already include "1/2" style.
        return lessonGroup == marker || lessonGroup == marker.substringBefore('/')
    }

    // Both markers present: split into two chunks at the second marker boundary.
    if (idx1 != -1 && idx2 != -1)
    {
        val firstIs1 = idx1 < idx2
        val firstMarkerIndex = if (firstIs1) idx1 else idx2
        val secondMarkerIndex = if (firstIs1) idx2 else idx1

        val firstChunk = raw.substring(firstMarkerIndex, secondMarkerIndex).trim()
        val secondChunk = raw.substring(secondMarkerIndex).trim()

        return when {
            firstIs1 && matchesGroup("1/2") -> firstChunk
            firstIs1 && matchesGroup("2/2") -> secondChunk
            !firstIs1 && matchesGroup("2/2") -> firstChunk
            !firstIs1 && matchesGroup("1/2") -> secondChunk
            else -> null
        }
    }

    // Only one marker present.
    if (idx1 != -1)
        return if (matchesGroup("1/2")) raw.substring(idx1).trim() else null

    // idx2 != -1
    return if (matchesGroup("2/2")) raw.substring(idx2).trim() else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Lesson(
    lesson: Lesson,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    current: Boolean = false,
    next: Boolean = false,
    hideClass: Boolean = false,
    substitution: String? = null
)
{
    val shape = RoundedCornerShape(5.dp)
    val substitutionLower = substitution?.lowercase()
    val greenOutline = Color(0xFF4CAF50)
    val borderColor = when {
        substitutionLower == null -> if (next) MaterialTheme.colorScheme.inverseSurface else null
        substitutionLower.contains("0") || substitutionLower.contains("odpadá") -> greenOutline
        else -> MaterialTheme.colorScheme.error
    }
    val outlinedModifier = if (borderColor != null) modifier.border(1.dp, borderColor, shape) else modifier
    Surface(
        modifier = outlinedModifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = if (current) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        // Use a bit more padding and enforce bounded children to avoid visual overlap in split lessons.
        Box(Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {
            if (lesson.subjectName.short != null)
                Text(
                    lesson.subjectName.short!!,
                    Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (substitution != null) TextDecoration.LineThrough else null
                )
            if (!hideClass && lesson.clazz != null)
                Text(lesson.clazz!!, Modifier.align(Alignment.BottomStart))
            if (lesson.teacherName?.short != null)
                Text(lesson.teacherName!!.short!!, Modifier.align(Alignment.TopStart))
            if (lesson.classroom != null)
                Text(lesson.classroom!!, Modifier.align(Alignment.TopEnd))
            if (lesson.group != null)
                Text(lesson.group!!, Modifier.align(Alignment.BottomEnd), fontSize = 10.sp)
            if (substitution != null)
                Text(
                    text = substitution,
                    modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 80.dp),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 10.sp,
                )
        }
    }
}

@Composable
private fun DayLabel(
    day: String,
    modifier: Modifier = Modifier
)
{
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).manipulate(1.5f),
        shadowElevation = ElevationLevel.level1,
        shape = RoundedCornerShape(5.dp)
    ) {
        Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            Text(text = day, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LessonDialogContent(
    lesson: Lesson,
    onCloseClick: () -> Unit = {},
    onTeacherClick: (TeacherReference) -> Unit,
    onClassroomClick: (ClassroomReference) -> Unit
)
{
    DialogContainer(
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = lesson.subjectName.full,
                    textAlign = TextAlign.Center,
                )
            }
        },
        buttons = {
            TextButton(onClick = onCloseClick) {
                Text(stringResource(R.string.close))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val teacher = lesson.teacherName
            if (teacher != null && teacher.short == null)
                DialogRow(
                    label = stringResource(R.string.timetable_dialog_teacher),
                    value = teacher.full
                )
            else if (teacher?.short != null)
                DialogRow(
                    label = stringResource(R.string.timetable_dialog_teacher),
                    value = teacher.full,
                    onClick = { onTeacherClick(TeacherReference(teacher.full, teacher.short!!)) }
                )
            if (lesson.classroom != null)
                DialogRow(stringResource(R.string.timetable_dialog_classroom), lesson.classroom!!,
                    { onClassroomClick(ClassroomReference(lesson.classroom!!, lesson.classroom!!)) })
            if (lesson.group != null)
                DialogRow(stringResource(R.string.timetable_dialog_group), lesson.group!!)
        }
    }
}
