package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.Timetable
import me.tomasan7.jecnamobile.util.extractGroupSubstitutionForLesson
import me.tomasan7.jecnamobile.util.extractSubstitutionOverridesForLesson
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.manipulate
import me.tomasan7.jecnamobile.util.resolveSubstitutionOverrides
import java.time.DayOfWeek
import androidx.compose.ui.unit.sp
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonSpot

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
                        TimetableRow(
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

                    val allLessons = timetable.daysSorted
                        .asSequence()
                        .mapNotNull { day -> timetable.getLessonSpotsForDay(day) }
                        .flatten()
                        .flatMap { it.asSequence() }
                        .toList()

                    val resolvedOverrides = resolveSubstitutionOverrides(
                        rawOverrides = rawOverrides,
                        teacherReferences = teacherReferences,
                        allLessons = allLessons
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
    lessonPeriod: io.github.tomhula.jecnaapi.data.timetable.LessonPeriod,
    hourIndex: Int,
    modifier: Modifier = Modifier
)
{
    androidx.compose.material3.Surface(
        modifier = modifier,
        shadowElevation = me.tomasan7.jecnamobile.ui.ElevationLevel.level1,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).manipulate(1.5f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
    ) {
        Box(Modifier.padding(4.dp)) {
            androidx.compose.material3.Text(
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
                text = hourIndex.toString(),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.material3.Text(
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                text = lessonPeriod.toString(),
                fontSize = 12.sp
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
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).manipulate(1.5f),
        shadowElevation = me.tomasan7.jecnamobile.ui.ElevationLevel.level1,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp)
    )
    {
        Box(Modifier.padding(4.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            androidx.compose.material3.Text(text = day, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}
