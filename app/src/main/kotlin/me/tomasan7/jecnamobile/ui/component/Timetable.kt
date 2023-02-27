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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.tomasan7.jecnaapi.data.timetable.*
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.manipulate
import java.time.DayOfWeek

@Composable
fun Timetable(
    timetable: Timetable,
    modifier: Modifier = Modifier,
    hideClass: Boolean = false
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
        Column(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row {
                timetable.lessonPeriods.forEachIndexed { i, lessonPeriod ->
                    if (i == 0)
                        HorizontalSpacer(30.dp)

                    HorizontalSpacer(5.dp)

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
                    HorizontalSpacer(5.dp)
                    timetable.getLessonSpotsForDay(day)!!.forEach { lessonSpot ->
                        LessonSpot(
                            lessonSpot = lessonSpot,
                            onLessonClick = { dialogState.show(it) },
                            current = timetable.getCurrentLessonSpot() === lessonSpot,
                            next = timetable.getCurrentNextLessonSpot(takeEmpty = true) === lessonSpot,
                            hideClass = hideClass
                        )
                        HorizontalSpacer(5.dp)
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
                    onCloseClick = { dialogState.hide() }
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
    hideClass: Boolean = false
)
{
    var lessonSpotModifier = Modifier.width(100.dp)

    if (lessonSpot.size <= 2)
        lessonSpotModifier = lessonSpotModifier.fillMaxHeight()

    Column(lessonSpotModifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lessonSpot.forEach { lesson ->
            /* If there is < 2 lessons, they are stretched to  */
            var lessonModifier = Modifier.fillMaxWidth()
            lessonModifier = if (lessonSpot.size <= 2)
                lessonModifier.weight(1f)
            else
                lessonModifier.height(50.dp)

            Lesson(
                modifier = lessonModifier,
                onClick = { onLessonClick(lesson) },
                lesson = lesson,
                current = current,
                next = next,
                hideClass = hideClass
            )
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
    hideClass: Boolean = false
)
{
    val shape = RoundedCornerShape(5.dp)
    Surface(
        modifier = if (next) modifier.border(1.dp, MaterialTheme.colorScheme.inverseSurface, shape) else modifier,
        tonalElevation = ElevationLevel.level2,
        shadowElevation = ElevationLevel.level1,
        shape = shape,
        color = if (current) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Box(Modifier.padding(4.dp)) {
            if (lesson.subjectName.short != null)
                Text(lesson.subjectName.short!!, Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
            if (!hideClass && lesson.clazz != null)
                Text(lesson.clazz!!, Modifier.align(Alignment.BottomStart))
            if (lesson.teacherName?.short != null)
                Text(lesson.teacherName!!.short!!, Modifier.align(Alignment.TopStart))
            if (lesson.classroom != null)
                Text(lesson.classroom!!, Modifier.align(Alignment.TopEnd))
            if (lesson.group != null)
                Text(lesson.group!!, Modifier.align(Alignment.BottomEnd), fontSize = 10.sp)
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
    onCloseClick: () -> Unit = {}
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
            if (lesson.teacherName != null)
                DialogRow(stringResource(R.string.timetable_dialog_teacher), lesson.teacherName!!.full)
            if (lesson.classroom != null)
                DialogRow(stringResource(R.string.timetable_dialog_classroom), lesson.classroom!!)
            if (lesson.group != null)
                DialogRow(stringResource(R.string.timetable_dialog_group), lesson.group!!)
        }
    }
}