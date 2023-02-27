package me.tomasan7.jecnamobile.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import de.palm.composestateevents.EventEffect
import me.tomasan7.jecnaapi.data.timetable.TimetablePage
import me.tomasan7.jecnaapi.util.SchoolYear
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreenDestination
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.component.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@SubScreensNavGraph(start = true)
@Destination
@Composable
fun TimetableSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: TimetableViewModel = hiltViewModel()
)
{
    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        onDispose {
            viewModel.leftComposition()
        }
    }

    val uiState = viewModel.uiState
    val pullRefreshState = rememberPullRefreshState(uiState.loading, viewModel::reload)
    val snackbarHostState = remember { SnackbarHostState() }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(R.string.sidebar_timetable, navDrawerController) {
                OfflineDataIndicator(
                    modifier = Modifier.padding(end = 16.dp),
                    underlyingIcon = SubScreenDestination.Timetable.iconSelected,
                    lastUpdateTimestamp = uiState.lastUpdateTimestamp,
                    visible = uiState.isCache
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
                    timetablePeriodOptions = uiState.timetablePage?.periodOptions ?: emptyList(),
                    selectedSchoolYear = uiState.selectedSchoolYear,
                    selectedTimetablePeriod = uiState.timetablePage?.periodOptions?.find { it.selected },
                    onChangeSchoolYear = { viewModel.selectSchoolYear(it) },
                    onChangeTimetablePeriod = { viewModel.selectTimetablePeriod(it) }
                )

                if (uiState.timetablePage != null)
                    Timetable(
                        modifier = Modifier.fillMaxSize(),
                        timetable = uiState.timetablePage.timetable,
                        hideClass = true
                    )
            }

            PullRefreshIndicator(
                refreshing = uiState.loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PeriodSelectors(
    timetablePeriodOptions: List<TimetablePage.PeriodOption>,
    selectedSchoolYear: SchoolYear,
    selectedTimetablePeriod: TimetablePage.PeriodOption?,
    onChangeSchoolYear: (SchoolYear) -> Unit,
    onChangeTimetablePeriod: (TimetablePage.PeriodOption) -> Unit,
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
        TimetablePeriodSelector(
            modifier = Modifier.width(160.dp),
            periodOptions = timetablePeriodOptions,
            selectedOption = selectedTimetablePeriod,
            onChange = onChangeTimetablePeriod
        )
    }
}

@Composable
private fun TimetablePeriodSelector(
    periodOptions: List<TimetablePage.PeriodOption>,
    modifier: Modifier = Modifier,
    selectedOption: TimetablePage.PeriodOption?,
    onChange: (TimetablePage.PeriodOption) -> Unit
)
{
    OutlinedDropDownSelector(
        modifier = modifier,
        label = stringResource(R.string.timetable_period),
        options = periodOptions,
        optionStringMap = { it?.toString() ?: "" },
        selectedValue = selectedOption,
        onChange = onChange
    )
}