package me.tomasan7.jecnamobile.mainscreen

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tomasan7.jecnaapi.CanteenClient
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnamobile.JecnaMobileApplication
import me.tomasan7.jecnamobile.R
import me.tomasan7.jecnamobile.login.AuthRepository
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val jecnaClient: JecnaClient,
    private val canteenClient: CanteenClient
) : ViewModel()
{
    var navigateToLoginEvent: StateEvent by mutableStateOf(consumed)
        private set

    private val connectivityManager =
        getSystemService(appContext, ConnectivityManager::class.java) as ConnectivityManager
    private val networkAvailabilityCallback = NetworkAvailabilityCallback()

    init
    {
        registerNetworkAvailabilityListener()
    }

    fun tryLogin()
    {
        val hasBeenLoggedIn = jecnaClient.lastSuccessfulLoginAuth != null
        val auth = jecnaClient.lastSuccessfulLoginAuth ?: authRepository.get()

        if (auth == null)
        {
            navigateToLoginEvent = triggered
            return
        }

        viewModelScope.launch {
            val loginResult = try
            {
                jecnaClient.login(auth).also {
                    if (!it)
                        Toast.makeText(appContext, R.string.login_error_credentials_saved, Toast.LENGTH_SHORT).show()
                }
            }
            /* This method (tryLogin()) should only run, when internet is available, but if it fails anyway,
            * just leave it as is and try again on new broadcast. */
            catch (e: UnresolvedAddressException)
            {
                return@launch
            }
            catch (e: CancellationException)
            {
                throw e
            }
            catch (e: Exception)
            {
                Toast.makeText(appContext, R.string.login_error_unknown, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                false
            }

            if (loginResult)
                broadcastSuccessfulLogin(!hasBeenLoggedIn)
        }
    }

    private fun onNetworkAvailable()
    {
        broadcastNetworkAvailable()
        tryLogin()
    }

    private fun broadcastSuccessfulLogin(first: Boolean = false)
    {
        val intent = Intent(JecnaMobileApplication.SUCCESSFUL_LOGIN_ACTION)
        intent.putExtra(JecnaMobileApplication.SUCCESSFUL_LOGIN_FIRST_EXTRA, first)
        appContext.sendBroadcast(intent)
    }

    private fun broadcastNetworkAvailable()
    {
        val intent = Intent(JecnaMobileApplication.NETWORK_AVAILABLE_ACTION)
        appContext.sendBroadcast(intent)
    }

    fun logout()
    {
        runBlocking {
            launch {
                jecnaClient.logout()
            }
            launch {
                canteenClient.logout()
            }
        }

        authRepository.clear()
    }

    fun onLoginEventConsumed()
    {
        navigateToLoginEvent = consumed
    }

    private fun registerNetworkAvailabilityListener()
    {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkAvailabilityCallback)
    }

    private fun unregisterNetworkAvailabilityListener()
    {
        connectivityManager.unregisterNetworkCallback(networkAvailabilityCallback)
    }

    override fun onCleared() = unregisterNetworkAvailabilityListener()

    inner class NetworkAvailabilityCallback : ConnectivityManager.NetworkCallback()
    {
        override fun onAvailable(network: android.net.Network) = onNetworkAvailable()
    }
}