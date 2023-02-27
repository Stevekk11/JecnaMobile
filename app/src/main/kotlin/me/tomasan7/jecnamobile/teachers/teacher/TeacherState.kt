package me.tomasan7.jecnamobile.teachers.teacher

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import me.tomasan7.jecnaapi.data.schoolStaff.Teacher

data class TeacherState(
    val loading: Boolean = false,
    val teacher: Teacher? = null,
    val snackBarMessageEvent: StateEventWithContent<String> = consumed()
)