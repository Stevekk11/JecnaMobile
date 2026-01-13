package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import me.tomasan7.jecnamobile.util.SubstitutionOverrides
import me.tomasan7.jecnamobile.util.extractGroupSubstitutionForLesson
import me.tomasan7.jecnamobile.util.extractSubstitutionOverridesForLesson
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.manipulate
import java.time.DayOfWeek

@Composable
fun Timetable(
    timetable: Timetable,
    modifier: Modifier = Modifier,
    hideClass: Boolean = false,
    showSubstitutions: Boolean = true,
    teacherReferences: Set<TeacherReference>? = null,
    onTeacherClick: (TeacherReference) -> Unit = {},
    onClassroomClick: (ClassroomReference) -> Unit = { },
    isCurrentSchoolYear: Boolean = true
)
{
    val effectiveShowSubstitutions = showSubstitutions && isCurrentSchoolYear

    val (mostLessonsInLessonSpotInEachDay, hasExpandedSubstitutionInDay) = remember(timetable) {
        timetable.run {
            val most = mutableMapOf<DayOfWeek, Int>()
            val expanded = mutableMapOf<DayOfWeek, Boolean>()

            for (day in days)
            {
                var dayMost = 0
                var dayHasExpandableSub = false
                
                for (lessonSpot in getLessonSpotsForDay(day)!!) {
                    if (lessonSpot.size > dayMost) dayMost = lessonSpot.size

                    val isSpoj = lessonSpot.substitution?.lowercase()?.contains("spoj") == true

                    if (effectiveShowSubstitutions &&
                        lessonSpot.substitution != null &&
                        lessonSpot.size > 1 &&
                        !isSpoj) {
                        dayHasExpandableSub = true
                    }
                }

                most[day] = dayMost
                expanded[day] = dayHasExpandableSub
            }

            most to expanded
        }
    }

    data class LessonDialogPayload(
        val lessonSpot: LessonSpot,
        val lesson: Lesson
    )

    val dialogState = rememberObjectDialogState<LessonDialogPayload>()

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
                val baseHeight = if (mostLessonsInLessonSpotInEachDay[day]!! <= 2) 100.dp else (mostLessonsInLessonSpotInEachDay[day]!! * 50.dp + (mostLessonsInLessonSpotInEachDay[day]!! - 1) * 2.dp)
                val actualHeight = if (hasExpandedSubstitutionInDay[day] == true) (baseHeight.value * 1.7f).dp else baseHeight
                val rowModifier = Modifier.height(actualHeight)
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
                            onLessonClick = { clickedLesson -> dialogState.show(LessonDialogPayload(lessonSpot, clickedLesson)) },
                            current = timetable.getCurrentLessonSpot() === lessonSpot,
                            next = timetable.getCurrentNextLessonSpot(takeEmpty = true) === lessonSpot,
                            hideClass = hideClass,
                            showSubstitutions = effectiveShowSubstitutions,
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
            content = { payload ->
                val lesson = payload.lesson
                val clickedSpot = payload.lessonSpot

                
                key(clickedSpot.substitution, lesson) {
                    val substitutionText = if (effectiveShowSubstitutions)
                        clickedSpot.substitution?.extractGroupSubstitutionForLesson(lesson)
                    else null

                    val rawOverrides = substitutionText?.extractSubstitutionOverridesForLesson(lesson)

                    // Resolve subject full name from shorts using data already present in the timetable.
                    val allLessons = timetable.daysSorted
                        .asSequence()
                        .mapNotNull { day -> timetable.getLessonSpotsForDay(day) }
                        .flatten()
                        .flatMap { it.asSequence() }
                        .toList()

                    val resolvedSubjectFull = rawOverrides?.subjectFull?.let { subjShort ->
                        allLessons.firstOrNull { it.subjectName.short?.equals(subjShort, ignoreCase = true) == true }
                            ?.subjectName?.full
                            ?: subjShort
                    }

                    
                    val resolvedTeacherRef: TeacherReference? = rawOverrides?.teacherTag?.let { tag ->
                        teacherReferences
                            ?.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                            ?: run {
                                val name = allLessons.firstOrNull { it.teacherName?.short?.equals(tag, ignoreCase = true) == true }
                                    ?.teacherName
                                if (name?.short != null) TeacherReference(name.full, name.short!!) else null
                            }
                    }

                    val resolvedOverrides = rawOverrides?.copy(
                        subjectFull = resolvedSubjectFull,
                        teacherFull = resolvedTeacherRef?.fullName,
                        teacherTag = resolvedTeacherRef?.tag ?: rawOverrides.teacherTag
                    )

                    LessonDialogContent(
                        lesson = lesson,
                        onCloseClick = { dialogState.hide() },
                        onTeacherClick = { onTeacherClick(it) },
                        onClassroomClick = { onClassroomClick(it) },
                        substitutionOverrides = resolvedOverrides
                    )
                }
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
        if (showSubstitutions && lessonSpot.substitution?.lowercase()?.contains("spoj") == true && lessonSpot.size > 1) {
            val mergedLesson = lessonSpot.first().copy(group = "spoj")
            Lesson(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                onClick = { onLessonClick(mergedLesson) },
                lesson = mergedLesson,
                current = current,
                next = next,
                hideClass = hideClass,
                substitution = lessonSpot.substitution,
                isSpoj = true
            )
        } else {
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

                    // For split lessons, try to route substitutions marked with 1/2 or 2/2 or thirds to the correct group.
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
    substitution: String? = null,
    isSpoj: Boolean = false
) {
    val shape = RoundedCornerShape(5.dp)
    val substitutionLower = substitution?.lowercase()
    
    val greenOutline = Color(0xFF4CAF50)
    val yellowOutline = Color(0xFFFFC107)
    val borderColor = when {
        substitutionLower == null -> if (next) MaterialTheme.colorScheme.inverseSurface else null
        substitutionLower.contains("0") || substitutionLower.contains("odpadá") || substitutionLower.contains("oběd") -> greenOutline
        substitutionLower.contains("spoj") -> yellowOutline
        else -> MaterialTheme.colorScheme.error
    }
    
    val overrides = remember(substitution, lesson) {
        if (substitution == null || isSpoj) null
        else substitution.extractSubstitutionOverridesForLesson(lesson)
    }

    val displayClassroom = overrides?.classroom ?: lesson.classroom
    val isRoomChanged = overrides?.classroom != null && overrides.classroom != lesson.classroom

    val outlinedModifier = if (borderColor != null) modifier.border(1.dp, borderColor, shape) else modifier

    Surface(
        modifier = outlinedModifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = if (current) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Box(Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {

            /* SUBJECT NAME */
            if (lesson.subjectName.short != null) {
                Text(
                    text = lesson.subjectName.short!!,
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (substitution != null && !isSpoj) TextDecoration.LineThrough else null
                )
            }

            /* CLASSROOM (Top Right) */
            if (displayClassroom != null) {
                Text(
                    text = displayClassroom,
                    modifier = Modifier.align(Alignment.TopEnd),
                    // Highlight red if the room is different from the original
                    color = if (isRoomChanged) MaterialTheme.colorScheme.error else Color.Unspecified,
                    fontWeight = if (isRoomChanged) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                )
            }

            /* TEACHER (Top Left) */
            if (isSpoj && substitution != null) {
                // Extract "Missing(Subbing)" pattern for merged lessons
                val regex = Regex("(\\w+)\\(([^)]+)\\)")
                val match = regex.find(substitution)
                if (match != null) {
                    Text(
                        text = "${match.groupValues[2]} | ${match.groupValues[1]}",
                        modifier = Modifier.align(Alignment.TopStart),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
            } else {
                val displayTeacher = overrides?.teacherTag ?: lesson.teacherName?.short
                if (displayTeacher != null) {
                    Text(
                        text = displayTeacher,
                        modifier = Modifier.align(Alignment.TopStart),
                        color = if (overrides?.teacherTag != null) MaterialTheme.colorScheme.error else Color.Unspecified,
                        fontSize = 12.sp
                    )
                }
            }

            /* CLASS (Bottom Left) */
            if (!hideClass && lesson.clazz != null) {
                Text(
                    text = lesson.clazz!!,
                    modifier = Modifier.align(Alignment.BottomStart),
                    fontSize = 10.sp
                )
            }

            /* GROUP (Bottom Right) */
            if (lesson.group != null) {
                Text(
                    text = lesson.group!!,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    fontSize = 10.sp
                )
            }

            /* SUBSTITUTION FOOTNOTE (Bottom Left/Center) */
            if (substitution != null) {
                Text(
                    text = substitution,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (!hideClass && lesson.clazz != null) 12.dp else 0.dp)
                        .widthIn(max = 85.dp),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 10.sp,
                )
            }
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
    )
    {
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
    onClassroomClick: (ClassroomReference) -> Unit,
    substitutionOverrides: SubstitutionOverrides? = null
)
{
    val red = MaterialTheme.colorScheme.error

    val substitutionSubject = substitutionOverrides?.subjectFull
    val substitutionTeacherFull = substitutionOverrides?.teacherFull
    val substitutionTeacherTag = substitutionOverrides?.teacherTag
    val substitutionClassroom = substitutionOverrides?.classroom

    val titleText = substitutionSubject?.takeIf { it.isNotBlank() } ?: lesson.subjectName.full
    val titleColor = if (!substitutionSubject.isNullOrBlank() && !substitutionSubject.equals(lesson.subjectName.full, ignoreCase = true)) red else Color.Unspecified

    DialogContainer(
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = titleText,
                    textAlign = TextAlign.Center,
                    color = titleColor,
                )
            }
        },
        buttons = {
            TextButton(onClick = onCloseClick) {
                Text(stringResource(R.string.close))
            }
        }
    )
    {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        )
        {
            if (!substitutionTeacherFull.isNullOrBlank() && !substitutionTeacherTag.isNullOrBlank())
            {
                DialogRow(
                    label = stringResource(R.string.timetable_dialog_teacher),
                    value = substitutionTeacherFull,
                    valueColor = red,
                    onClick = { onTeacherClick(TeacherReference(substitutionTeacherFull, substitutionTeacherTag)) }
                )
            }
            else
            {
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
            }
            if (!substitutionClassroom.isNullOrBlank())
            {
                DialogRow(
                    label = stringResource(R.string.timetable_dialog_classroom),
                    value = substitutionClassroom,
                    valueColor = red,
                    onClick = { onClassroomClick(ClassroomReference(substitutionClassroom, substitutionClassroom)) }
                )
            }
            else if (lesson.classroom != null)
            {
                DialogRow(
                    stringResource(R.string.timetable_dialog_classroom),
                    lesson.classroom!!,
                    { onClassroomClick(ClassroomReference(lesson.classroom!!, lesson.classroom!!)) }
                )
            }

            if (lesson.group != null)
                DialogRow(stringResource(R.string.timetable_dialog_group), lesson.group!!)
        }
    }
}
