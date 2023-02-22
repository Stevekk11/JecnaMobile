package me.tomasan7.jecnamobile.login

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import me.tomasan7.jecnaapi.JecnaClient
import me.tomasan7.jecnaapi.web.Auth
import me.tomasan7.jecnamobile.util.showShortToast
import javax.inject.Inject
import kotlin.Exception

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val jecnaClient: JecnaClient
) : ViewModel()
{
    var uiState by mutableStateOf(LoginState())
        private set

    fun onUsernameFieldValueChange(newValue: String)
    {
        changeUiState(username = newValue.trim())

        if (uiState.username.isBlank())
            changeUiState(usernameBlankError = true)
        else if (uiState.usernameBlankError && uiState.username.isNotBlank())
            changeUiState(usernameBlankError = false)

        checkCanHitLogin()
    }

    fun onPasswordFieldValueChange(newValue: String)
    {
        changeUiState(password = newValue.trim())

        if (uiState.password.isBlank())
            changeUiState(passwordBlankError = true)
        else if (uiState.passwordBlankError && uiState.password.isNotBlank())
            changeUiState(passwordBlankError = false)

        checkCanHitLogin()
    }

    private fun checkCanHitLogin()
    {
        val canHitLogin = uiState.username.isNotBlank() && uiState.password.isNotBlank()

        /* So the update only happens when the value actually changed. */
        if (uiState.canHitLogin != canHitLogin)
            changeUiState(canHitLogin = canHitLogin)
    }


    fun onRememberAuthCheckedChange(newValue: Boolean) = changeUiState(rememberAuth = newValue)

    fun onLoginClick()
    {
        if (uiState.canHitLogin)
            login()
    }

    private fun login(username: String = uiState.username, password: String = uiState.password)
    {
        changeUiState(isLoading = true)

        val auth = Auth(username, password)

        viewModelScope.launch {
            val result = try
            {
                if (jecnaClient.login(auth))
                    LoginResult.Success
                else
                    LoginResult.Error.InvalidCredentials
            }
            catch (e: UnresolvedAddressException)
            {
                LoginResult.Error.NoInternetConnection
            }
            catch (e: CancellationException)
            {
                /* To not catch cancellation exception with the following catch block.  */
                throw e
            }
            catch (e: Exception)
            {
                LoginResult.Error.Unknown
            }

            if (result is LoginResult.Success)
            {
                //changeUiState(loginEvent = triggered)
                changeUiState(loginResult = LoginResult.Success)

                if (uiState.rememberAuth)
                    saveAuth(auth)
            }
            else
                changeUiState(isLoading = false, username = "", password = "", loginResult = result)
        }
    }

    private fun saveAuth(auth: Auth) = authRepository.set(auth)


    private fun changeUiState(
        isLoading: Boolean = uiState.isLoading,
        username: String = uiState.username,
        password: String = uiState.password,
        usernameBlankError: Boolean = uiState.usernameBlankError,
        passwordBlankError: Boolean = uiState.passwordBlankError,
        rememberAuth: Boolean = uiState.rememberAuth,
        canHitLogin: Boolean = uiState.canHitLogin,
        loginResult: LoginResult? = uiState.loginResult,
    )
    {
        uiState = uiState.copy(
            isLoading = isLoading,
            username = username,
            password = password,
            usernameBlankError = usernameBlankError,
            passwordBlankError = passwordBlankError,
            rememberAuth = rememberAuth,
            canHitLogin = canHitLogin,
            loginResult = loginResult
        )
    }
}