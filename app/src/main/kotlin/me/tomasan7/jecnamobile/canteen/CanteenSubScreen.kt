package me.tomasan7.jecnamobile.canteen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.tomasan7.jecnaapi.data.canteen.DayMenu
import me.tomasan7.jecnaapi.data.canteen.MenuItem
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.mainscreen.NavDrawerController
import me.tomasan7.jecnamobile.mainscreen.SubScreensNavGraph
import me.tomasan7.jecnamobile.ui.ElevationLevel
import me.tomasan7.jecnamobile.ui.component.*
import me.tomasan7.jecnamobile.ui.theme.jm_canteen_disabled
import me.tomasan7.jecnamobile.ui.theme.jm_canteen_ordered
import me.tomasan7.jecnamobile.ui.theme.jm_canteen_ordered_disabled
import me.tomasan7.jecnamobile.util.getWeekDayName
import me.tomasan7.jecnamobile.util.settingsDataStore
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterialApi::class)
@SubScreensNavGraph
@Destination
@Composable
fun CanteenSubScreen(
    navDrawerController: NavDrawerController,
    viewModel: CanteenViewModel = hiltViewModel()
)
{
    val uiState = viewModel.uiState
    val allergensDialogState = rememberObjectDialogState<DayMenu>()
    val helpDialogState = rememberObjectDialogState<Unit>()
    val pullRefreshState = rememberPullRefreshState(uiState.loading, viewModel::reload)
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        viewModel.enteredComposition()
        coroutineScope.launch {
            context.settingsDataStore.data.collect {
                if (!it.canteenHelpSeen)
                {
                    helpDialogState.show(Unit)
                    viewModel.onHelpDialogShownAutomatically()
                }
            }
        }
        onDispose {
            viewModel.leftComposition()
        }
    }

    EventEffect(
        event = uiState.snackBarMessageEvent,
        onConsumed = viewModel::onSnackBarMessageEventConsumed
    ) {
        snackbarHostState.showSnackbar(it)
    }

    Scaffold(
        topBar = {
            SubScreenTopAppBar(
                R.string.sidebar_canteen,
                navDrawerController = navDrawerController,
                actions = {
                    if (uiState.credit != null)
                        Credit(uiState.credit)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            val columnState = remember { LazyListState() }

            LazyColumn(
                state = columnState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.menuSorted, key = { it.day }) { dayMenu ->
                    // TODO: Add animated appearance using AnimatedVisibility
                    DayMenu(
                        dayMenu = dayMenu,
                        onMenuItemClick = { viewModel.orderMenuItem(it, dayMenu.day) },
                        onMenuItemLongClick = { viewModel.putMenuItemOnExchange(it, dayMenu.day) },
                        onInfoClick = { allergensDialogState.show(dayMenu) }
                    )
                }
            }

            InfiniteListHandler(listState = columnState, buffer = 1) {
                viewModel.loadMoreDayMenus(1)
            }

            PullRefreshIndicator(
                refreshing = uiState.loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            ObjectDialog(
                state = allergensDialogState,
                onDismissRequest = { allergensDialogState.hide() },
                content = { dayMenu ->
                    AllergensDialogContent(
                        dayMenu = dayMenu,
                        onCloseCLick = { allergensDialogState.hide() })
                }
            )

            if (uiState.orderInProcess)
                Dialog(onDismissRequest = { }) {
                    CircularProgressIndicator()
                }
        }
    }
}

/* https://dev.to/luismierez/infinite-lazycolumn-in-jetpack-compose-44a4 */
/**
 * Handler to make any lazy column (or lazy row) infinite. Will notify the [onLoadMore]
 * callback once needed
 * @param listState state of the list that needs to also be passed to the LazyColumn composable.
 * Get it by calling rememberLazyListState()
 * @param buffer the number of items before the end of the list to call the onLoadMore callback
 * @param onLoadMore will notify when we need to load more
 */
@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    buffer: Int = 2,
    onLoadMore: () -> Unit
)
{
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .collect {
                onLoadMore()
            }
    }
}

@Composable
private fun DayMenu(
    dayMenu: DayMenu,
    modifier: Modifier = Modifier,
    onInfoClick: () -> Unit = {},
    onMenuItemClick: (MenuItem) -> Unit = {},
    onMenuItemLongClick: (MenuItem) -> Unit = {}
)
{
    val dayName = getWeekDayName(dayMenu.day.dayOfWeek)
    val dayDate = remember { dayMenu.day.format(DATE_FORMATTER) }
    val isSoupSameForAllItems = remember {
        val firstItem = dayMenu.items.firstOrNull() ?: return@remember false
        dayMenu.items.all { it.description?.soup == firstItem.description?.soup }
    }

    Card(
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "$dayName $dayDate",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                )

                val anyMenuItemsWithAllergens = dayMenu.items.any { it.allergens != null }

                if (anyMenuItemsWithAllergens)
                    IconButton(onClick = onInfoClick) {
                        Icon(Icons.Outlined.Info, null)
                    }
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSoupSameForAllItems)
                dayMenu.items.firstOrNull()?.description?.soup?.let { Soup(it) }

            dayMenu.items.forEach { menuItem ->
                key(menuItem) {
                    MenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        menuItem = menuItem,
                        onClick = { onMenuItemClick(menuItem) },
                        onLongClick = { onMenuItemLongClick(menuItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Soup(soup: String)
{
    Surface(
        tonalElevation = ElevationLevel.level3,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_soup_filled),
                contentDescription = null
            )
            Text(soup, Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun Credit(credit: Float)
{
    Icon(
        imageVector = Icons.Filled.AccountBalanceWallet,
        contentDescription = null,
    )

    val creditNumberStr = remember(credit) {
        String.format(if (credit.rem(1) == 0f) "%.0f" else "%.2f", credit)
    }

    Text(
        text = stringResource(R.string.canteen_credit, creditNumberStr),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(10.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuItem(
    menuItem: MenuItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
)
{
    val color = when
    {
        menuItem.isOrdered && !menuItem.isEnabled -> jm_canteen_ordered_disabled
        menuItem.isOrdered                        -> jm_canteen_ordered
        !menuItem.isEnabled                       -> jm_canteen_disabled
        else                                      -> MaterialTheme.colorScheme.surface
    }

    val lunchString = stringResource(R.string.canteen_lunch, menuItem.number)

    val text = remember(menuItem.description?.rest) {
        menuItem.description?.rest?.replaceFirstChar { it.uppercase() }?.replace(" , ", ", ")
            ?: lunchString
    }

    ElevatedTextRectangle(
        text = { Text(text, modifier = Modifier.weight(1f)) },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = color,
        trailingIcon = if (menuItem.isInExchange)
        {
            {
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    tint = Color.Gray.copy(alpha = .5f),
                    contentDescription = null
                )
            }
        }
        else
            null
    )
}

@Composable
private fun AllergensDialogContent(
    dayMenu: DayMenu,
    onCloseCLick: () -> Unit = {}
)
{
    DialogContainer(
        title = {
            Text(stringResource(R.string.canteen_allergens))
        },
        buttons = {
            TextButton(onClick = onCloseCLick) {
                Text(stringResource(R.string.close))
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dayMenu.items.forEach { menuItem ->
                AllergensForMenuItem(menuItem)
            }
        }
    }
}

@Composable
private fun AllergensForMenuItem(menuItem: MenuItem)
{
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.canteen_lunch, menuItem.number))

        ElevatedTextRectangle(
            modifier = Modifier.fillMaxWidth()
        ) {
            val allergensText = remember(menuItem.allergens) {
                menuItem.allergens?.joinToString { it.split(" - ")[0] }
            }

            Text(
                text = allergensText ?: "",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ElevatedTextRectangle(
    modifier: Modifier = Modifier,
    elevation: Dp = ElevationLevel.level5,
    color: Color = MaterialTheme.colorScheme.surface,
    trailingIcon: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    text: @Composable RowScope.() -> Unit
)
{
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = elevation,
        color = color,
    ) {
        Column(modifier = Modifier.padding(top = 5.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)) {
            if (label != null)
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelSmall) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        label()
                    }
                }
            VerticalSpacer(5.dp)
            Row {
                text()
                if (trailingIcon != null)
                {
                    HorizontalSpacer(10.dp)
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
private fun ElevatedTextRectangle(
    modifier: Modifier = Modifier,
    elevation: Dp = ElevationLevel.level5,
    color: Color = MaterialTheme.colorScheme.surface,
    trailingIcon: ImageVector? = null,
    text: String
) = ElevatedTextRectangle(
    modifier = modifier,
    elevation = elevation,
    color = color,
    trailingIcon = trailingIcon?.let { { Icon(it, null) } },
    text = { Text(text, modifier = Modifier.weight(1f)) }
)

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.")