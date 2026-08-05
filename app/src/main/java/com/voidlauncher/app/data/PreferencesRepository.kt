package com.voidlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("void_prefs")

data class LauncherPreferences(
    val favorites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val showLabels: Boolean = true,
    val gridColumns: Int = 4,
    val iconScale: Float = 1f
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val Favorites = stringSetPreferencesKey("favorites")
        val Hidden = stringSetPreferencesKey("hidden")
        val ShowLabels = booleanPreferencesKey("show_labels")
        val GridColumns = intPreferencesKey("grid_columns")
        val IconScale = floatPreferencesKey("icon_scale")
    }

    val preferences: Flow<LauncherPreferences> = context.dataStore.data.map { prefs ->
        LauncherPreferences(
            favorites = prefs[Keys.Favorites] ?: emptySet(),
            hidden = prefs[Keys.Hidden] ?: emptySet(),
            showLabels = prefs[Keys.ShowLabels] ?: true,
            gridColumns = prefs[Keys.GridColumns] ?: 4,
            iconScale = prefs[Keys.IconScale] ?: 1f
        )
    }

    suspend fun setFavorites(keys: Set<String>) {
        context.dataStore.edit { it[Keys.Favorites] = keys }
    }

    suspend fun setHidden(keys: Set<String>) {
        context.dataStore.edit { it[Keys.Hidden] = keys }
    }

    suspend fun setShowLabels(value: Boolean) {
        context.dataStore.edit { it[Keys.ShowLabels] = value }
    }

    suspend fun setGridColumns(value: Int) {
        context.dataStore.edit { it[Keys.GridColumns] = value.coerceIn(3, 6) }
    }

    suspend fun setIconScale(value: Float) {
        context.dataStore.edit { it[Keys.IconScale] = value.coerceIn(0.7f, 1.3f) }
    }
}
