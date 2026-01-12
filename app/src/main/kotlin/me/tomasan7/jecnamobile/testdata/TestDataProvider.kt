package me.tomasan7.jecnamobile.testdata

import io.github.tomhula.jecnaapi.data.article.Article
import io.github.tomhula.jecnaapi.data.article.NewsPage
import io.github.tomhula.jecnaapi.data.canteen.DayMenu
import io.github.tomhula.jecnaapi.data.canteen.ItemDescription
import io.github.tomhula.jecnaapi.data.canteen.Menu
import io.github.tomhula.jecnaapi.data.canteen.MenuItem
import io.github.tomhula.jecnaapi.data.canteen.MenuPage
import io.github.tomhula.jecnaapi.data.grade.*
import io.github.tomhula.jecnaapi.data.timetable.*
import io.github.tomhula.jecnaapi.util.Name
import io.github.tomhula.jecnaapi.util.SchoolYear
import io.github.tomhula.jecnaapi.util.SchoolYearHalf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import io.github.tomhula.jecnaapi.data.absence.AbsenceInfo
import io.github.tomhula.jecnaapi.data.absence.AbsencesPage
import io.github.tomhula.jecnaapi.data.attendance.Attendance
import io.github.tomhula.jecnaapi.data.attendance.AttendanceType
import io.github.tomhula.jecnaapi.data.attendance.AttendancesPage
import java.time.Month
import io.github.tomhula.jecnaapi.data.student.Student
import io.github.tomhula.jecnaapi.data.student.Locker
import io.github.tomhula.jecnaapi.data.schoolStaff.Teacher
import io.github.tomhula.jecnaapi.data.schoolStaff.TeacherReference
import io.github.tomhula.jecnaapi.data.schoolStaff.TeachersPage
import io.github.tomhula.jecnaapi.data.classroom.Classroom
import io.github.tomhula.jecnaapi.data.classroom.ClassroomReference
import io.github.tomhula.jecnaapi.data.classroom.ClassroomPage

/**
 * Provides realistic mock data for the test account "test"/"test123".
 * All data is fictional but realistic for a Czech secondary school student.
 */
object TestDataProvider {

    fun generateGradesPage(): GradesPage {
        val builder = GradesPage.builder()

        // Subject 1: Matematika (5 grades, mixed sizes)
        val mathGradesBuilder = Grades.builder()
        mathGradesBuilder.addGrade(null, Grade(value = 1, small = false, teacher = Name("Novák", "J."), description = "Písemná práce", receiveDate = LocalDate.now().minusDays(10), gradeId = 1))
        mathGradesBuilder.addGrade(null, Grade(value = 2, small = true, teacher = Name("Novák", "J."), description = "Domácí úkol", receiveDate = LocalDate.now().minusDays(5), gradeId = 2))
        mathGradesBuilder.addGrade(null, Grade(value = 4, small = false, teacher = Name("Novák", "J."), description = "Vědomostní test", receiveDate = LocalDate.now().minusDays(2), gradeId = 3))
        mathGradesBuilder.addGrade(null, Grade(value = 2, small = true, teacher = Name("Novák", "J."), receiveDate = LocalDate.now().minusDays(1), gradeId = 4))
        mathGradesBuilder.addGrade(null, Grade(value = 3, small = false, teacher = Name("Novák", "J."), description = "Příprava", receiveDate = LocalDate.now(), gradeId = 5))
        
        builder.addSubject(Subject(
            name = Name("Matematika", "M"),
            grades = mathGradesBuilder.build(),
            finalGrade = FinalGrade.Grade(value = 1, subject = Name("Matematika", "M"))
        ))

        // Subject 2: Anglický jazyk (4 grades)
        val englishGradesBuilder = Grades.builder()
        englishGradesBuilder.addGrade(null, Grade(value = 2, small = false, teacher = Name("Kučerová", "L."), description = "Konverzace", receiveDate = LocalDate.now().minusDays(8), gradeId = 6))
        englishGradesBuilder.addGrade(null, Grade(value = 1, small = true, teacher = Name("Kučerová", "L."), receiveDate = LocalDate.now().minusDays(3), gradeId = 7))
        englishGradesBuilder.addGrade(null, Grade(value = 2, small = false, teacher = Name("Kučerová", "L."), description = "Gramatika", receiveDate = LocalDate.now().minusDays(1), gradeId = 8))
        englishGradesBuilder.addGrade(null, Grade(value = 2, small = true, teacher = Name("Kučerová", "L."), receiveDate = LocalDate.now(), gradeId = 9))
        
        builder.addSubject(Subject(
            name = Name("Anglický jazyk", "AJ"),
            grades = englishGradesBuilder.build(),
            finalGrade = FinalGrade.Grade(value = 3)
        ))

        // Subject 3: Tělesná výchova (3 grades, mostly small)
        val peGradesBuilder = Grades.builder()
        peGradesBuilder.addGrade(null, Grade(value = 1, small = true, teacher = Name("Verner", "M."), receiveDate = LocalDate.now().minusDays(7), gradeId = 10))
        peGradesBuilder.addGrade(null, Grade(value = 1, small = true, teacher = Name("Verner", "M."), receiveDate = LocalDate.now().minusDays(4), gradeId = 11))
        peGradesBuilder.addGrade(null, Grade(value = 1, small = true, teacher = Name("Verner", "M."), receiveDate = LocalDate.now().minusDays(1), gradeId = 12))
        
        builder.addSubject(Subject(
            name = Name("Tělesná výchova", "TV"),
            grades = peGradesBuilder.build(),
            finalGrade = FinalGrade.Grade(value = 1)
        ))

        builder.setBehaviour(Behaviour(emptyList(), FinalGrade.Grade(value = 2)))
        builder.setSelectedSchoolYear(SchoolYear.current())
        builder.setSelectedSchoolYearHalf(SchoolYearHalf.FIRST)

        return builder.build()
    }

    fun generateTimetablePage(): TimetablePage {
        val timetableBuilder = Timetable.builder()

        // Create lesson periods (typical school day)
        val periods = listOf(
            LessonPeriod(LocalTime.of(8, 0), LocalTime.of(8, 45)),
            LessonPeriod(LocalTime.of(8, 55), LocalTime.of(9, 40)),
            LessonPeriod(LocalTime.of(10, 0), LocalTime.of(10, 45)),
            LessonPeriod(LocalTime.of(10, 55), LocalTime.of(11, 40)),
            LessonPeriod(LocalTime.of(11, 50), LocalTime.of(12, 35)),
            LessonPeriod(LocalTime.of(13, 0), LocalTime.of(13, 45))
        )

        timetableBuilder.setLessonPeriods(periods)

        // Create lessons for each day
        val mathematicsLesson = Lesson(
            subjectName = Name("Matematika", "M"),
            clazz = "4.A",
            teacherName = Name("Novák Jan", "No"),
            classroom = "125",
            group = null
        )
        
        val englishLesson = Lesson(
            subjectName = Name("Anglický jazyk", "AJ"),
            clazz = "4.A",
            teacherName = Name("Kučerová Lenka", "Ku"),
            classroom = "213",
            group = null
        )
        
        val physicsLesson = Lesson(
            subjectName = Name("Fyzika", "F"),
            clazz = "4.A",
            teacherName = Name("Svoboda Pavel", "Sv"),
            classroom = "305",
            group = null
        )
        
        val peLesson = Lesson(
            subjectName = Name("Tělesná výchova", "TV"),
            clazz = "4.A",
            teacherName = Name("Verner Miroslav", "Ve"),
            classroom = "TV",
            group = null
        )
        
        val czechLesson = Lesson(
            subjectName = Name("Čeština", "Č"),
            clazz = "4.A",
            teacherName = Name("Horváthová Petra", "Ho"),
            classroom = "207",
            group = null
        )

        // Monday
        timetableBuilder.addLessonSpot(DayOfWeek.MONDAY, LessonSpot(listOf(mathematicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.MONDAY, LessonSpot(listOf(englishLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.MONDAY, LessonSpot(listOf(physicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.MONDAY, LessonSpot(listOf(peLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.MONDAY, LessonSpot(listOf(czechLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.MONDAY, LessonSpot(emptyList(), 1))

        // Tuesday
        timetableBuilder.addLessonSpot(DayOfWeek.TUESDAY, LessonSpot(listOf(englishLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.TUESDAY, LessonSpot(listOf(mathematicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.TUESDAY, LessonSpot(listOf(czechLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.TUESDAY, LessonSpot(listOf(physicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.TUESDAY, LessonSpot(listOf(peLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.TUESDAY, LessonSpot(emptyList(), 1))

        // Wednesday
        timetableBuilder.addLessonSpot(DayOfWeek.WEDNESDAY, LessonSpot(listOf(physicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.WEDNESDAY, LessonSpot(listOf(czechLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.WEDNESDAY, LessonSpot(listOf(mathematicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.WEDNESDAY, LessonSpot(listOf(englishLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.WEDNESDAY, LessonSpot(emptyList(), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.WEDNESDAY, LessonSpot(emptyList(), 1))

        // Thursday
        timetableBuilder.addLessonSpot(DayOfWeek.THURSDAY, LessonSpot(listOf(peLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.THURSDAY, LessonSpot(listOf(mathematicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.THURSDAY, LessonSpot(listOf(englishLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.THURSDAY, LessonSpot(listOf(czechLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.THURSDAY, LessonSpot(listOf(physicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.THURSDAY, LessonSpot(emptyList(), 1))

        // Friday
        timetableBuilder.addLessonSpot(DayOfWeek.FRIDAY, LessonSpot(listOf(czechLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.FRIDAY, LessonSpot(listOf(englishLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.FRIDAY, LessonSpot(listOf(peLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.FRIDAY, LessonSpot(listOf(mathematicsLesson), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.FRIDAY, LessonSpot(emptyList(), 1))
        timetableBuilder.addLessonSpot(DayOfWeek.FRIDAY, LessonSpot(emptyList(), 1))
        
        val pageBuilder = TimetablePage.builder()
        pageBuilder.setTimetable(timetableBuilder.build())
        pageBuilder.setSelectedSchoolYear(SchoolYear.current())
        pageBuilder.setPeriodOptions(emptyList())

        return pageBuilder.build()
    }

    fun generateStudent(): Student {
        return Student(
            fullName = "Jan Testovací",
            username = "test",
            schoolMail = "test@spsejecna.cz",
            privateMail = "jan.test@gmail.com",
            phoneNumbers = listOf("+420 123 456 789"),
            className = "4.A",
            classGroups = "1/2, 2/2",
            classRegistryId = 15,
            birthDate = LocalDate.of(2005, 5, 20),
            birthPlace = "Praha",
            permanentAddress = "Nerudova 15, Praha 1",
            age = 18
        )
    }

    fun generateLocker(): Locker {
        return Locker("123", "v šatně vlevo")
    }

    fun generateTeachersPage(): TeachersPage {
        val builder = TeachersPage.builder()
        builder.addTeacherReference(TeacherReference("Novák Jan", "No"))
        builder.addTeacherReference(TeacherReference("Kučerová Lenka", "Ku"))
        builder.addTeacherReference(TeacherReference("Svoboda Pavel", "Sv"))
        return builder.build()
    }

            fun generateTeacher(tag: String): Teacher {
                return when (tag) {
                    "No" -> Teacher(
                        fullName = "Jan Novák",
                        username = "novak",
                        schoolMail = "novak@spsejecna.cz",
                        tag = "No",
                        cabinet = "125"
                    )
                    "Ku" -> Teacher(
                        fullName = "Lenka Kučerová",
                        username = "kucerova",
                        schoolMail = "kucerova@spsejecna.cz",
                        tag = "Ku",
                        cabinet = "213"
                    )
                    else -> Teacher(
                        fullName = "Pavel Svoboda",
                        username = "svoboda",
                        schoolMail = "svoboda@spsejecna.cz",
                        tag = "Sv",
                        cabinet = "305"
                    )
                }
            }

    fun generateClassroomsPage(): ClassroomPage {
        return ClassroomPage(setOf(
            ClassroomReference("125", "125"),
            ClassroomReference("213", "213"),
            ClassroomReference("305", "305")
        ))
    }

    fun generateClassroom(ref: ClassroomReference): Classroom {
        return Classroom("Učebna pro teoretickou výuku", "2. patro", "Třída 4.C")
    }

    fun generateMenuPage(): MenuPage {
        val builder = Menu.builder()
        
        // Generate 3 days of menu
        for (i in 0..2) {
            val date = LocalDate.now().plusDays(i.toLong())
            
            // Create menu items for this day
            val dayMenuBuilder = DayMenu.builder(date)
            
            when (i) {
                0 -> {
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 1,
                        description = ItemDescription(soup = "Drůbeží polévka s nudlemi", rest = "Hovězí guláš s knedlíkem"),
                        allergens = null,
                        price = 85f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=1"
                    ))
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 2,
                        description = ItemDescription(soup = "Drůbeží polévka s nudlemi", rest = "Pstruh na másle"),
                        allergens = null,
                        price = 90f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=2"
                    ))
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 3,
                        description = ItemDescription(soup = "Drůbeží polévka s nudlemi", rest = "Vařená zelenina"),
                        allergens = null,
                        price = 60f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=3"
                    ))
                }
                1 -> {
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 1,
                        description = ItemDescription(soup = "Rajská polévka", rest = "Vepřové řízečko"),
                        allergens = null,
                        price = 95f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=1"
                    ))
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 2,
                        description = ItemDescription(soup = "Rajská polévka", rest = "Kuřecí nugetky"),
                        allergens = null,
                        price = 75f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=2"
                    ))
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 3,
                        description = ItemDescription(soup = "Rajská polévka", rest = "Bramborová kaše"),
                        allergens = null,
                        price = 65f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=3"
                    ))
                }
                else -> {
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 1,
                        description = ItemDescription(soup = "Bramborová polévka s houbami", rest = "Kančí maso se sladkou omáčkou"),
                        allergens = null,
                        price = 100f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=1"
                    ))
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 2,
                        description = ItemDescription(soup = "Bramborová polévka s houbami", rest = "Ryba s citrónovou omáčkou"),
                        allergens = null,
                        price = 95f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=2"
                    ))
                    dayMenuBuilder.addMenuItem(MenuItem(
                        number = 3,
                        description = ItemDescription(soup = "Bramborová polévka s houbami", rest = "Glazovaná zelenina"),
                        allergens = null,
                        price = 70f,
                        isEnabled = true,
                        isOrdered = false,
                        isInExchange = false,
                        orderPath = "/canteen/order?day=${date}&item=3"
                    ))
                }
            }
            
            builder.addDayMenu(date, dayMenuBuilder.build())
        }

        return MenuPage(builder.build(), 150f)
    }

    fun generateAttendancesPage(): AttendancesPage {
        val builder = AttendancesPage.builder()

        // Generate attendance data for the last 3 school days
        val today = LocalDate.now()
        val schoolDays = (0..2).map { today.minusDays(it.toLong()) }
            .filter { it.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }

        schoolDays.forEach { day ->
            // Morning entry around 8:00-8:30
            val entryTime = LocalTime.of(8, (10..30).random())
            builder.addAttendance(day, Attendance(AttendanceType.ENTER, entryTime))

            // Lunch break exit around 11:30-12:00
            val lunchExitTime = LocalTime.of(11, (30..59).random())
            builder.addAttendance(day, Attendance(AttendanceType.EXIT, lunchExitTime))

            // Lunch break entry around 12:30-13:00
            val lunchEntryTime = LocalTime.of(12, (30..59).random())
            builder.addAttendance(day, Attendance(AttendanceType.ENTER, lunchEntryTime))

            // End of day exit around 14:00-15:00
            val exitTime = LocalTime.of(14, (0..59).random())
            builder.addAttendance(day, Attendance(AttendanceType.EXIT, exitTime))
        }

        builder.setSelectedSchoolYear(SchoolYear.current())
        builder.setSelectedMonth(today.month)

        return builder.build()
    }

    fun generateAttendancesPage(schoolYear: SchoolYear, month: Month): AttendancesPage {
        val builder = AttendancesPage.builder()

        // Generate attendance data for the specified month
        val daysInMonth = month.length(java.time.Year.of(schoolYear.firstCalendarYear).isLeap)
        val schoolDays = (1..daysInMonth).map { LocalDate.of(schoolYear.getCalendarYear(month), month, it) }
            .filter { it.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }

        schoolDays.take(5).forEach { day: LocalDate ->
            // Morning entry around 8:00-8:30
            val entryTime = LocalTime.of(8, (10..30).random())
            builder.addAttendance(day, Attendance(AttendanceType.ENTER, entryTime))

            // Lunch break exit around 11:30-12:00
            val lunchExitTime = LocalTime.of(11, (30..59).random())
            builder.addAttendance(day, Attendance(AttendanceType.EXIT, lunchExitTime))

            // Lunch break entry around 12:30-13:00
            val lunchEntryTime = LocalTime.of(12, (30..59).random())
            builder.addAttendance(day, Attendance(AttendanceType.ENTER, lunchEntryTime))

            // End of day exit around 14:00-15:00
            val exitTime = LocalTime.of(14, (0..59).random())
            builder.addAttendance(day, Attendance(AttendanceType.EXIT, exitTime))
        }

        builder.setSelectedSchoolYear(schoolYear)
        builder.setSelectedMonth(month)

        return builder.build()
    }

    fun generateAbsencesPage(): AbsencesPage {
        val builder = AbsencesPage.builder()

        // Generate absence data for 3 days
        val today = LocalDate.now()
        val absenceDays = listOf(
            today.minusDays(5) to AbsenceInfo(hoursAbsent = 2, unexcusedHours = 0, lateEntryCount = 1),
            today.minusDays(10) to AbsenceInfo(hoursAbsent = 4, unexcusedHours = 1, lateEntryCount = 0),
            today.minusDays(15) to AbsenceInfo(hoursAbsent = 1, unexcusedHours = 0, lateEntryCount = 2)
        )

        absenceDays.forEach { (day, info) ->
            builder.setAbsence(day, info)
        }

        builder.setSelectedSchoolYear(SchoolYear.current())

        return builder.build()
    }

    fun generateAbsencesPage(schoolYear: SchoolYear): AbsencesPage {
        val builder = AbsencesPage.builder()

        // Generate absence data for the specified school year
        val absenceDays = listOf(
            LocalDate.of(schoolYear.firstCalendarYear, 9, 5) to AbsenceInfo(hoursAbsent = 2, unexcusedHours = 0, lateEntryCount = 1),
            LocalDate.of(schoolYear.firstCalendarYear, 10, 12) to AbsenceInfo(hoursAbsent = 4, unexcusedHours = 1, lateEntryCount = 0),
            LocalDate.of(schoolYear.firstCalendarYear, 11, 8) to AbsenceInfo(hoursAbsent = 1, unexcusedHours = 0, lateEntryCount = 2)
        )

        absenceDays.forEach { (day, info) ->
            builder.setAbsence(day, info)
        }

        builder.setSelectedSchoolYear(schoolYear)

        return builder.build()
    }
}
