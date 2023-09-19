package me.tomasan7.jecnamobile.canteen

import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tomasan7.jecnaapi.CanteenClient
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnaapi.data.canteen.DayMenu
import me.tomasan7.jecnaapi.data.canteen.MenuItem
import me.tomasan7.jecnaapi.parser.ParseException
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.util.createBroadcastReceiver
import me.tomasan7.jecnamobile.util.settingsDataStore
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CanteenViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val canteenClient: CanteenClient,
) : ViewModel()
{
    var uiState by mutableStateOf(CanteenState())
        private set
    private var loadMenuJob: Job? = null
    private var loginInProcess = false
    private val awaitedDays = mutableSetOf<LocalDate>()

    private val networkAvailableBroadcastReceiver = createBroadcastReceiver { _, _ ->
        if (!loginInProcess)
            if (canteenClient.lastSuccessfulLoginAuth == null)
                loginCanteenClient()
            else if (uiState.menu.size < getDays().size)
                loadMenu()

        showMessage(R.string.back_online)
    }

    init
    {
        loginCanteenClient()
    }

    private fun loginCanteenClient()
    {
        loginInProcess = true
        viewModelScope.launch {
            changeUiState(loading = true)

            val auth = authRepository.get()

            if (auth != null)
                try
                {
                    canteenClient.login(auth)
                }
                catch (e: Exception)
                {
                    showMessage(R.string.canteen_login_error)
                    e.printStackTrace()
                }
                finally
                {
                    loginInProcess = false
                }
            else
            {
                loginInProcess = false
                showMessage(R.string.canteen_login_error)
            }

            if (uiState.menu.size < getDays().size)
                loadMenu()
            else
                changeUiState(loading = false)
        }
    }

    fun enteredComposition()
    {
        appContext.registerReceiver(
            networkAvailableBroadcastReceiver,
            IntentFilter(JecnaMobileApplication.NETWORK_AVAILABLE_ACTION)
        )
    }

    fun leftComposition()
    {
        loadMenuJob?.cancel()
        appContext.unregisterReceiver(networkAvailableBroadcastReceiver)
    }

    fun orderMenuItem(menuItem: MenuItem, dayMenuDate: LocalDate)
    {
        if (uiState.orderInProcess)
            return

        if (!menuItem.isEnabled)
            return

        changeUiState(orderInProcess = true)

        viewModelScope.launch {
            try
            {
                canteenClient.order(menuItem)
                val newDayMenu = canteenClient.getDayMenu(dayMenuDate)
                updateMenu(newDayMenu)
                changeUiState(loading = false)
            }
            catch (e: UnresolvedAddressException)
            {
                e.printStackTrace()
                showMessage(R.string.no_internet_connection)
            }
            catch (e: CancellationException)
            {
                /* To not catch cancellation exception with the following catch block.  */
                throw e
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                showMessage(R.string.error_order)
            }

            changeUiState(orderInProcess = false)
        }
    }

    fun putMenuItemOnExchange(menuItem: MenuItem, dayMenuDate: LocalDate)
    {
        if (uiState.orderInProcess)
            return

        if (!menuItem.isOrdered)
            return

        if (menuItem.isEnabled)
            return

        changeUiState(orderInProcess = true)

        viewModelScope.launch {
            try
            {
                canteenClient.putOnExchange(menuItem)
                val newDayMenu = canteenClient.getDayMenu(dayMenuDate)
                updateMenu(newDayMenu)
                changeUiState(loading = false)
            }
            catch (e: UnresolvedAddressException)
            {
                showMessage(R.string.no_internet_connection)
            }
            catch (e: CancellationException)
            {
                /* To not catch cancellation exception with the following catch block.  */
                throw e
            }
            catch (e: Exception)
            {
                e.printStackTrace()
                showMessage(R.string.error_order)
            }

            changeUiState(orderInProcess = false)
        }
    }

    fun loadMenu()
    {
        changeUiState(loading = true)

        loadMenuJob?.cancel()

        val days = getDays()
        awaitedDays.addAll(days)
        loadMenuJob = canteenClient.getMenuAsync(days)
            .onEach { addDayMenu(it) }
            .catch { e -> showMenuLoadErrorMessage(e, R.string.error_load) }
            .onCompletion {
                changeUiState(loading = false)
                awaitedDays.removeAll(days.toSet())
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try
            {
                val credit = canteenClient.getCredit()
                changeUiState(credit = credit)
            }
            catch (e: ParseException)
            {
                showMessage(R.string.error_unsupported_credit)
            }
            catch (e: Exception)
            {
                showMessage(R.string.error_load_credit)
            }
        }
    }

    private fun getDays(): List<LocalDate>
    {
        val result = mutableListOf<LocalDate>()
        var cursor = LocalDate.now()
        while (cursor.dayOfWeek != DayOfWeek.SATURDAY)
        {
            result.add(cursor)
            cursor = cursor.plusDays(1)
        }

        return result
    }

    fun showMenuLoadErrorMessage(e: Throwable, @StringRes default: Int) = when (e)
    {
        is UnresolvedAddressException -> showMessage(R.string.no_internet_connection)
        is ParseException             -> showMessage(R.string.error_unsupported_menu)
        else                          -> showMessage(default)
    }

    fun showMessage(@StringRes message: Int) =
        changeUiState(snackBarMessageEvent = triggered(appContext.getString(message)))

    fun loadMoreDayMenus(count: Int)
    {
        if (uiState.loading)
            return

        if (uiState.menu.isEmpty())
            return

        val currentLatestDay = uiState.menuSorted.lastOrNull()?.day ?: LocalDate.now()

        val newDays = generateSequence(currentLatestDay.plusDays(1)) { it.plusDays(1) }
            .filterNot { it.isWeekend() }
            .take(count)
            .toList()

        val newNewDays = newDays
            // Filters any days, that are already loaded.
            .filter { day -> uiState.menu.none { it.day == day } }
            // Filters any days, that are already loading.
            .filter { day -> day !in awaitedDays }

        awaitedDays.addAll(newNewDays)

        loadMenuJob = canteenClient.getMenuAsync(newNewDays)
            .onEach { addDayMenu(it) }
            .catch { e -> showMenuLoadErrorMessage(e, R.string.error_load) }
            .onCompletion { awaitedDays.removeAll(newNewDays.toSet()) }
            .launchIn(viewModelScope)
    }

    fun LocalDate.isWeekend() = dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun reload() = loadMenu()

    fun onSnackBarMessageEventConsumed() = changeUiState(snackBarMessageEvent = consumed())

    fun onHelpDialogShownAutomatically() = viewModelScope.launch {
        appContext.settingsDataStore.updateData {
            it.copy(
                canteenHelpSeen = true
            )
        }
    }

    private fun updateMenu(newDayMenu: DayMenu)
    {
        val menu = uiState.menu.map { if (it.day == newDayMenu.day) newDayMenu else it }.toMutableSet()
        changeUiState(menu = menu)
    }

    private fun addDayMenu(newDayMenu: DayMenu)
    {
        Log.d("CanteenViewModel", "addDayMenu: $newDayMenu")
        awaitedDays.remove(newDayMenu.day)
        val menu = uiState.menu.toMutableSet()
        menu.removeIf { it.day == newDayMenu.day }
        menu.add(newDayMenu)
        changeUiState(menu = menu)
    }

    private fun changeUiState(
        loading: Boolean = uiState.loading,
        orderInProcess: Boolean = uiState.orderInProcess,
        menu: Set<DayMenu> = uiState.menu,
        credit: Float? = uiState.credit,
        snackBarMessageEvent: StateEventWithContent<String> = uiState.snackBarMessageEvent,
    )
    {
        uiState = uiState.copy(
            loading = loading,
            menu = menu,
            credit = credit,
            orderInProcess = orderInProcess,
            snackBarMessageEvent = snackBarMessageEvent,
        )
    }
}