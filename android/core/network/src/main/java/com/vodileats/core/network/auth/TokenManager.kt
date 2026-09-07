package com.vodileats.core.network.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    val accessTokenFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val refreshTokenFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }

    val userIdFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val userRoleFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[USER_ROLE_KEY]
    }

    fun getAccessTokenSync(): String? = runBlocking {
        context.tokenDataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    fun getRefreshTokenSync(): String? = runBlocking {
        context.tokenDataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveUserInfo(userId: String, role: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
            prefs[USER_ROLE_KEY] = role
        }
    }

    suspend fun clearAll() {
        context.tokenDataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean {
        return context.tokenDataStore.data.first()[ACCESS_TOKEN_KEY] != null
    }
}
