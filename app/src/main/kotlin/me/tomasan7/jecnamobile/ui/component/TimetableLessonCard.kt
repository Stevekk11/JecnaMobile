package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.util.extractSubstitutionOverridesForLesson
import me.tomasan7.jecnamobile.util.getSpojTeacherText

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun TimetableLessonCard(
    lesson: Lesson,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    current: Boolean = false,
    next: Boolean = false,
    hideClass: Boolean = false,
    substitution: String? = null,
    isSpoj: Boolean = false
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp)
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
                    color = if (isRoomChanged) MaterialTheme.colorScheme.error else Color.Unspecified,
                    fontWeight = if (isRoomChanged) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                )
            }

            /* TEACHER (Top Left) */
            if (isSpoj && substitution != null) {
                val spojText = remember(substitution) { substitution.getSpojTeacherText() }
                if (spojText != null) {
                    Text(
                        text = spojText,
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
                        fontSize = 14.sp
                    )
                }
            }

            /* CLASS (Bottom Left) */
            if (!hideClass && lesson.clazz != null) {
                Text(
                    text = lesson.clazz!!,
                    modifier = Modifier.align(Alignment.BottomStart),
                    fontSize = 11.sp
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
