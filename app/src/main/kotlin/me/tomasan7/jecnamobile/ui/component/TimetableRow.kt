package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import io.github.tomhula.jecnaapi.data.timetable.LessonSpot
import me.tomasan7.jecnamobile.util.extractGroupSubstitutionForLesson

@Composable
internal fun TimetableRow(
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
            TimetableLessonCard(
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

                TimetableLessonCard(
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
