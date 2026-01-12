package me.tomasan7.jecnamobile.testdata

import io.github.tomhula.jecnaapi.web.Auth
import javax.inject.Singleton

/**
 * Singleton that manages test account state.
 * This is checked by repositories and clients to determine if test data should be served.
 */
@Singleton
object TestAccountManager {
    private var currentTestAuth: Auth? = null
    val isTestAccountActive: Boolean get() = currentTestAuth != null
    
    fun setTestAccount(auth: Auth?) {
        if (auth?.username == "test" && auth.password == "test123") {
            currentTestAuth = auth
        } else {
            currentTestAuth = null
        }
    }
    
    fun clearTestAccount() {
        currentTestAuth = null
    }
}

