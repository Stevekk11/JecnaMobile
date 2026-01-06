package me.tomasan7.jecnamobile.login

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tomhula.jecnaapi.web.Auth
import javax.inject.Inject

class SharedPreferencesAuthRepository @Inject constructor(
    @ApplicationContext
    appContext: Context
) : AuthRepository
{
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        appContext,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun get(): Auth?
    {
        val username = preferences.getString(USERNAME_KEY, null) ?: return null
        val password = preferences.getString(PASSWORD_KEY, null) ?: return null

        return Auth(username, password)
    }

    override fun set(auth: Auth)
    {
        preferences.edit {
            putString(USERNAME_KEY, auth.username)
            putString(PASSWORD_KEY, auth.password)
        }
    }

    override fun clear()
    {
        preferences.edit {
            clear()
        }
    }

    override fun exists() = preferences.contains(USERNAME_KEY) && preferences.contains(PASSWORD_KEY)

    companion object
    {
        private const val FILE_NAME = "auth"
        private const val USERNAME_KEY = "username"
        private const val PASSWORD_KEY = "password"
    }
}
