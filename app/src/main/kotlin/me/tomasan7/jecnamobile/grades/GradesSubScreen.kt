package me.tomasan7.jecnamobile.grades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import de.palm.composestateevents.EventEffect
import me.tomasan7.jecnaapi.data.grade.Behaviour
import me.tomasan7.jecnaapi.data.grade.FinalGrade
import me.tomasan7.jecnaapi.data.grade.Grade
import me.tomasan7.jecnaapi.data.grade.Subject
import me.tomasan7.jecnaapi.data.schoolStaff.TeacherReference
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnaapi.util.SchoolYearHalf
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.destinations.TeacherScreenDestination
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.settings.Settings
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.ui.component.DialogRow
import me.tomasan7.jecnamobile.ui.component.HorizontalSpacer
import me.tomasan7.jecnamobile.ui.component.ObjectDialog
import me.tomasan7.jecnamobile.ui.component.OfflineDataIndicator
import me.tomasan7.jecnamobile.ui.component.SchoolYearHalfSelector
import me.tomasan7.jecnamobile.ui.component.SchoolYearSelector
import me.tomasan7.jecnamobile.ui.component.SubScreenTopAppBar
import me.tomasan7.jecnamobile.ui.component.VerticalSpacer
import me.tomasan7.jecnamobile.ui.component.rememberObjectDialogState
import me.tomasan7.jecnamobile.ui.theme.grade_absence_warning
import me.tomasan7.jecnamobile.ui.theme.grade_grades_warning
import me.tomasan7.jecnamobile.ui.theme.jm_label
import me.tomasan7.jecnamobile.util.getGradeColor
import me.tomasan7.jecnamobile.util.rememberMutableStateOf
import me.tomasan7.jecnamobile.util.settingsAsState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@SubScreensNavGraph
@Destination
@Composable
fun GradesSubScreen(
    navDrawerController: NavDrawerController,
    navigator: DestinationsNavigator,
    viewModel: GradesViewModel = hiltViewModel()
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose {
            viewModel.leftComposition()
        }
    }

    val uiState = viewModel.uiState
    val objectDialogState = rememberObjectDialogState<Grade>()
    val pullRefreshState = rememberPullRefreshState(uiState.loading, viewModel::reload)
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by settingsAsState()

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_grades, navDrawerController) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SubScreenDestination.Grades.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
                )
                ViewModeButton(
                    viewMode = settings.gradesViewMode,
                    onViewModeChange = viewModel::setViewMode
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PeriodSelectors(
                    modifier = Modifier.fillMaxWidth(),
                    selectedSchoolYear = uiState.selectedSchoolYear,
                    selectedSchoolYearHalf = uiState.selectedSchoolYearHalf,
                    onChangeSchoolYear = { viewModel.selectSchoolYear(it) },
                    onChangeSchoolYearHalf = { viewModel.selectSchoolYearHalf(it) }
                )

                if (uiState.gradesPage != null)
                {
                    uiState.subjectsSorted!!.forEach { subject ->
                        key(subject) {
                            when (settings.gradesViewMode)
                            {
                                Settings.GradesViewMode.LIST -> ListSubject(
                                    subject = subject,
                                    onGradeClick = { objectDialogState.show(it) }
                                )

                                Settings.GradesViewMode.GRID -> Subject(
                                    subject = subject,
                                    onGradeClick = { objectDialogState.show(it) }
                                )
                            }
                        }
                    }

                    Behaviour(uiState.gradesPage.behaviour)
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            ObjectDialog(
                state = objectDialogState,
                onDismissRequest = { objectDialogState.hide() },
                content = { grade ->
                    GradeDialogContent(
                        grade = grade,
                        onTeacherClick = { navigator.navigate(TeacherScreenDestination(it)) },
                        onCloseClick = { objectDialogState.hide() }
                    )
                }
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    viewMode: Settings.GradesViewMode,
    onViewModeChange: (Settings.GradesViewMode) -> Unit,
)
{
    val icon = remember(viewMode) {
        when (viewMode)
        {
            Settings.GradesViewMode.LIST -> Icons.Filled.ViewModule
            Settings.GradesViewMode.GRID -> Icons.AutoMirrored.Filled.ViewList
        }
    }

    val nextViewMode = remember(viewMode) {
        when (viewMode)
        {
            Settings.GradesViewMode.LIST -> Settings.GradesViewMode.GRID
            Settings.GradesViewMode.GRID -> Settings.GradesViewMode.LIST
        }
    }

    IconButton(onClick = { onViewModeChange(nextViewMode) }) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            //tint = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
        )
    }
}

@Composable
private fun PeriodSelectors(
    selectedSchoolYear: SchoolYear,
    selectedSchoolYearHalf: SchoolYearHalf,
    onChangeSchoolYear: (SchoolYear) -> Unit,
    onChangeSchoolYearHalf: (SchoolYearHalf) -> Unit,
    modifier: Modifier = Modifier
)
{
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SchoolYearSelector(
            modifier = Modifier.width(160.dp),
            selectedSchoolYear = selectedSchoolYear,
            onChange = onChangeSchoolYear
        )
        SchoolYearHalfSelector(
            modifier = Modifier.width(160.dp),
            selectedSchoolYearHalf = selectedSchoolYearHalf,
            onChange = onChangeSchoolYearHalf
        )
    }
}

@Composable
private fun Container(
    title: @Composable () -> Unit = {},
    rightColumnVisible: Boolean = true,
    rightColumnContent: @Composable ColumnScope.() -> Unit = {},
    onRightColumnClick: (() -> Unit)? = null,
    modifier: Modifier,
    content: @Composable () -> Unit = {}
)
{
    Surface(
        modifier = modifier,
        tonalElevation = ElevationLevel.level1,
        shadowElevation = ElevationLevel.level1,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(20.dp)
        ) {
            Column(Modifier.weight(1f, true)) {
                title()

                Spacer(Modifier.height(15.dp))

                content()
            }

            if (rightColumnVisible)
            {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            enabled = onRightColumnClick != null,
                            onClick = { onRightColumnClick?.invoke() },
                            indication = null,
                            interactionSource = MutableInteractionSource()
                        ),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    content = rightColumnContent
                )
            }
        }
    }
}

@Composable
private fun Container(
    title: String,
    rightColumnVisible: Boolean = true,
    rightColumnContent: @Composable ColumnScope.() -> Unit = {},
    onRightColumnClick: (() -> Unit)? = null,
    modifier: Modifier,
    content: @Composable () -> Unit = {}
) = Container(
    title = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
    },
    rightColumnVisible = rightColumnVisible,
    rightColumnContent = rightColumnContent,
    onRightColumnClick = onRightColumnClick,
    modifier = modifier,
    content = content
)

@Composable
private fun Subject(
    subject: Subject,
    onGradeClick: (Grade) -> Unit = {}
)
{
    val average = remember { subject.grades.average() }
    var showAverage by rememberMutableStateOf(false)

    Container(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Column {
                Text(subject.name.full, style = MaterialTheme.typography.titleMedium)
                if (subject.grades.count != 0)
                    Text(
                        text = pluralStringResource(R.plurals.grades_count, subject.grades.count, subject.grades.count),
                        color = jm_label,
                        style = MaterialTheme.typography.labelSmall
                    )
            }
        },
        rightColumnVisible = subject.finalGrade != null || average != null,
        rightColumnContent = {
            if (subject.finalGrade == null)
                GradeAverage(average!!)
            else
                if (showAverage)
                    GradeAverage(average!!)
                else
                    FinalGrade(finalGrade = subject.finalGrade!!)

        },
        onRightColumnClick = { showAverage = !showAverage }
    ) {
        if (subject.grades.isEmpty())
            Text(
                text = stringResource(R.string.no_grades),
                style = MaterialTheme.typography.bodyMedium
            )
        else
            Column {
                subject.grades.subjectParts.forEach { subjectPart ->
                    SubjectPart(subjectPart, subject.grades[subjectPart]!!, onGradeClick)
                }
            }
    }
}

@Composable
private fun ListSubject(
    subject: Subject,
    onGradeClick: (Grade) -> Unit = {}
)
{
    val average = remember { subject.grades.average() }
    var showAverage by rememberMutableStateOf(false)
    var showGrades by rememberMutableStateOf(false)

    Container(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showGrades = !showGrades },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(subject.name.full, style = MaterialTheme.typography.titleMedium)
                    if (subject.grades.count != 0)
                        Text(
                            text = pluralStringResource(
                                R.plurals.grades_count, subject.grades.count, subject.grades.count
                            ),
                            color = jm_label,
                            style = MaterialTheme.typography.labelSmall
                        )
                }

                if (subject.finalGrade != null || average != null)
                    if (subject.finalGrade == null)
                        GradeAverage(average!!, onClick = { showAverage = !showAverage })
                    else
                        if (showAverage)
                            GradeAverage(average!!, onClick = { showAverage = !showAverage })
                        else
                            FinalGrade(finalGrade = subject.finalGrade!!, onClick = { showAverage = !showAverage })
            }
        },
        rightColumnVisible = false,
        rightColumnContent = {},
    ) {
        if (subject.grades.isEmpty())
            Text(
                text = stringResource(R.string.no_grades),
                style = MaterialTheme.typography.bodyMedium
            )
        else
        {
            if (showGrades)
                Column {
                    subject.grades.subjectParts.forEach { subjectPart ->
                        ListSubjectPart(subjectPart, subject.grades[subjectPart]!!, onGradeClick)
                    }
                }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectPart(
    subjectPart: String? = null,
    grades: List<Grade>,
    onGradeClick: (Grade) -> Unit = {}
)
{
    if (subjectPart != null)
        Text(
            text = "$subjectPart:",
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge
        )

    FlowRow(
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start),
    ) {
        grades.forEach { grade ->
            Grade(
                grade = grade,
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { onGradeClick(grade) }
            )
        }
    }
}

@Composable
private fun ListSubjectPart(
    subjectPart: String? = null,
    grades: List<Grade>,
    onGradeClick: (Grade) -> Unit = {}
)
{
    if (subjectPart != null)
        Text(
            text = "$subjectPart:",
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge
        )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
    ) {
        grades.forEachIndexed { i, grade ->
            ListGrade(
                grade = grade,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onGradeClick(grade) }
            )
            if (i != grades.lastIndex)
                HorizontalDivider(
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
                )
        }
    }
}

@Composable
private fun ListGrade(
    grade: Grade,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
{
    val newModifier = if (onClick != null)
        modifier.clickable(onClick = onClick)
    else
        modifier

    val gradeColor = remember { getGradeColor(grade) }

    val gradeSizeBig = 30.dp
    val gradeSizeSmall = 20.dp

    Row(
        modifier = newModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val size = if (grade.small) gradeSizeSmall else gradeSizeBig

            Box(
                modifier = Modifier.size(gradeSizeBig),
                contentAlignment = Alignment.Center
            ) {
                GradeBox(
                    text = grade.valueChar().toString(),
                    color = gradeColor,
                    modifier = Modifier.size(size)
                )
            }
            HorizontalSpacer(10.dp)
            Text(grade.description ?: "")
        }

        if (grade.receiveDate != null)
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = grade.receiveDate!!.format(Constants.gradeDateFormatter)
            )
    }
}

@Composable
private fun GradeBox(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = Constants.gradeWidth,
    onClick: (() -> Unit)? = null
)
{
    var newModifier = modifier
        .size(Constants.gradeWidth, height)
        .clip(Constants.gradeShape)
        .background(color)

    if (onClick != null)
        newModifier = newModifier.clickable(onClick = onClick)

    Box(
        modifier = newModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun Grade(
    grade: Grade,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
{
    val gradeHeight = if (grade.small) Constants.gradeSmallHeight else Constants.gradeWidth
    val gradeColor = remember { getGradeColor(grade) }

    GradeBox(
        modifier = modifier,
        text = grade.valueChar().toString(),
        color = gradeColor,
        height = gradeHeight,
        onClick = onClick
    )
}

@Composable
private fun GradeAverage(
    value: Float,
    onClick: () -> Unit = {}
)
{
    val valueString = remember { Constants.gradeAverageDecimalFormat.format(value) }

    Surface(
        modifier = Modifier.size(Constants.gradeWidth),
        border = BorderStroke(1.dp, getGradeColor(value.roundToInt())),
        tonalElevation = 10.dp,
        onClick = onClick,
        shape = Constants.gradeShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = valueString,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun FinalGrade(
    finalGrade: FinalGrade,
    onClick: () -> Unit = {}
) = when (finalGrade)
{
    is FinalGrade.Grade                   -> Grade(Grade(finalGrade.value, false), onClick = onClick)
    is FinalGrade.GradesWarning           -> GradesWarning(onClick = onClick)
    is FinalGrade.AbsenceWarning          -> AbsenceWarning(onClick = onClick)
    is FinalGrade.GradesAndAbsenceWarning -> GradesAndAbsenceWarning(onClick = onClick)
}


@Composable
private fun GradesWarning(onClick: () -> Unit = {})
{
    GradeBox(
        text = stringResource(R.string.grade_grades_warning_content),
        color = grade_grades_warning,
        onClick = onClick
    )
}

@Composable
private fun AbsenceWarning(onClick: () -> Unit = {})
{
    GradeBox(
        text = stringResource(R.string.grade_absence_warning_content),
        color = grade_absence_warning,
        onClick = onClick
    )
}

@Composable
private fun GradesAndAbsenceWarning(onClick: () -> Unit = {})
{
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        GradesWarning()
        AbsenceWarning()
    }
}

@Composable
private fun GradeDialogContent(
    grade: Grade,
    onTeacherClick: (TeacherReference) -> Unit = {},
    onCloseClick: () -> Unit = {}
)
{
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.width(300.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(2.dp, getGradeColor(grade))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(getGradeColor(grade))
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getGradeWord(grade),
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DialogRow(
                        stringResource(R.string.grade_receive_date),
                        grade.receiveDate?.format(Constants.gradeDateFormatter) ?: ""
                    )
                    DialogRow(stringResource(R.string.grade_description), grade.description ?: "")
                    val teacher = grade.teacher
                    if (teacher?.short == null)
                        DialogRow(stringResource(R.string.grade_teacher), teacher?.full ?: "")
                    else
                        DialogRow(
                            label = stringResource(R.string.grade_teacher),
                            value = teacher.full,
                            onClick = { onTeacherClick(TeacherReference(teacher.full, teacher.short!!)) }
                        )
                }

                VerticalSpacer(24.dp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCloseClick) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Behaviour(behaviour: Behaviour)
{
    Container(
        modifier = Modifier.fillMaxWidth(),
        title = Behaviour.SUBJECT_NAME,
        rightColumnContent = {
            if (behaviour.finalGrade is FinalGrade.Grade)
                Grade(Grade((behaviour.finalGrade as FinalGrade.Grade).value, false))
        }
    ) {
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start)
        ) {
            behaviour.notifications.forEach {
                BehaviourNotification(it)
            }
        }
    }
}

@Composable
private fun BehaviourNotification(behaviourNotification: Behaviour.Notification)
{
    Surface(
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(7.dp)
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = 5.dp),
                text = behaviourNotification.message
            )
            when (behaviourNotification.type)
            {
                Behaviour.NotificationType.GOOD ->
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = getGradeColor(1)
                    )

                Behaviour.NotificationType.BAD  ->
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = getGradeColor(5)
                    )
            }
        }
    }
}

@Composable
private fun getGradeWord(grade: Grade) = when (grade.value)
{
    0    -> stringResource(R.string.grade_word_0)
    1    -> stringResource(R.string.grade_word_1)
    2    -> stringResource(R.string.grade_word_2)
    3    -> stringResource(R.string.grade_word_3)
    4    -> stringResource(R.string.grade_word_4)
    5    -> stringResource(R.string.grade_word_5)
    else -> throw IllegalArgumentException("Grade value must be between 0 and 5. (got ${grade.value})")
}

private object Constants
{
    val gradeWidth = 40.dp
    val gradeSmallHeight = 25.dp
    val gradeAverageDecimalFormat = DecimalFormat("#.##").apply {
        roundingMode = RoundingMode.HALF_UP
    }
    val gradeDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")
    val gradeShape = RoundedCornerShape(7.dp)
}
