package me.tomasan7.jecnamobile.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.timetable.Lesson
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.util.SubstitutionOverrides

@Composable
internal fun LessonDialogContent(
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

