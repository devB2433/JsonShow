package com.jsonshow.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

object AppPrefs {
    private val KEY_PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")

    fun privacyAccepted(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_PRIVACY_ACCEPTED] == true }

    suspend fun setPrivacyAccepted(context: Context, accepted: Boolean) {
        context.dataStore.edit { it[KEY_PRIVACY_ACCEPTED] = accepted }
    }
}
