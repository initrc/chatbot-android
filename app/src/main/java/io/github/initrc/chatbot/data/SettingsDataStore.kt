package io.github.initrc.chatbot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    private val currentModelKey = stringPreferencesKey("current_model")
    private val defaultModel = "llama-3.1-8b-instant"

    suspend fun getCurrentModel(): String {
        return dataStore.data.first()[currentModelKey] ?: defaultModel
    }

    suspend fun setCurrentModel(model: String) {
        dataStore.edit { prefs ->
            prefs[currentModelKey] = model
        }
    }
}
