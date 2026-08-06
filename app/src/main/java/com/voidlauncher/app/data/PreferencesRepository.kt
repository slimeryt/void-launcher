package com.voidlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("void_prefs")

/** Home grid item: app key or folder id. */
sealed class HomeItem {
    data class App(val key: String) : HomeItem()
    data class Folder(val id: String) : HomeItem()
}

data class HomeFolder(
    val id: String,
    val name: String,
    val appKeys: List<String>
)

data class LauncherPreferences(
    val favorites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val showLabels: Boolean = true,
    val gridColumns: Int = 4,
    val iconScale: Float = 1f,
    /** Each page is an ordered list of home items. */
    val pages: List<List<HomeItem>> = listOf(emptyList()),
    val folders: Map<String, HomeFolder> = emptyMap(),
    val glassBlurStrength: Float = 1f,
    val glassFrostAmount: Float = 1f,
    val glassRefraction: Boolean = true,
    val glassSheen: Boolean = true,
    val dockLabels: Boolean = false,
    val hapticFeedback: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    /** Ids of home-screen widgets (AppWidgetManager appWidgetId), stacked above the app grid. */
    val widgetIds: List<Int> = emptyList()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val Favorites = stringSetPreferencesKey("favorites")
        val Hidden = stringSetPreferencesKey("hidden")
        val ShowLabels = booleanPreferencesKey("show_labels")
        val GridColumns = intPreferencesKey("grid_columns")
        val IconScale = floatPreferencesKey("icon_scale")
        val PagesJson = stringPreferencesKey("home_pages_json")
        val FoldersJson = stringPreferencesKey("home_folders_json")
        val GlassBlur = floatPreferencesKey("glass_blur")
        val GlassFrost = floatPreferencesKey("glass_frost")
        val GlassRefraction = booleanPreferencesKey("glass_refraction")
        val GlassSheen = booleanPreferencesKey("glass_sheen")
        val DockLabels = booleanPreferencesKey("dock_labels")
        val Haptic = booleanPreferencesKey("haptic_feedback")
        val AutoCheckUpdates = booleanPreferencesKey("auto_check_updates")
        val WidgetIdsJson = stringPreferencesKey("widget_ids_json")
    }

    val preferences: Flow<LauncherPreferences> = context.dataStore.data.map { prefs ->
        LauncherPreferences(
            favorites = prefs[Keys.Favorites] ?: emptySet(),
            hidden = prefs[Keys.Hidden] ?: emptySet(),
            showLabels = prefs[Keys.ShowLabels] ?: true,
            gridColumns = prefs[Keys.GridColumns] ?: 4,
            iconScale = prefs[Keys.IconScale] ?: 1f,
            pages = decodePages(prefs[Keys.PagesJson]),
            folders = decodeFolders(prefs[Keys.FoldersJson]),
            glassBlurStrength = prefs[Keys.GlassBlur] ?: 1f,
            glassFrostAmount = prefs[Keys.GlassFrost] ?: 1f,
            glassRefraction = prefs[Keys.GlassRefraction] ?: true,
            glassSheen = prefs[Keys.GlassSheen] ?: true,
            dockLabels = prefs[Keys.DockLabels] ?: false,
            hapticFeedback = prefs[Keys.Haptic] ?: true,
            autoCheckUpdates = prefs[Keys.AutoCheckUpdates] ?: true,
            widgetIds = decodeWidgetIds(prefs[Keys.WidgetIdsJson])
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

    suspend fun setGlassBlurStrength(value: Float) {
        // Must match the slider's valueRange in SettingsScreen.kt (0f..1.6f) — a tighter
        // floor here silently snapped dragged values back up before persisting.
        context.dataStore.edit { it[Keys.GlassBlur] = value.coerceIn(0f, 1.6f) }
    }

    suspend fun setGlassFrostAmount(value: Float) {
        context.dataStore.edit { it[Keys.GlassFrost] = value.coerceIn(0f, 1.5f) }
    }

    suspend fun setGlassRefraction(value: Boolean) {
        context.dataStore.edit { it[Keys.GlassRefraction] = value }
    }

    suspend fun setGlassSheen(value: Boolean) {
        context.dataStore.edit { it[Keys.GlassSheen] = value }
    }

    suspend fun setDockLabels(value: Boolean) {
        context.dataStore.edit { it[Keys.DockLabels] = value }
    }

    suspend fun setHapticFeedback(value: Boolean) {
        context.dataStore.edit { it[Keys.Haptic] = value }
    }

    suspend fun setAutoCheckUpdates(value: Boolean) {
        context.dataStore.edit { it[Keys.AutoCheckUpdates] = value }
    }

    suspend fun setWidgetIds(ids: List<Int>) {
        context.dataStore.edit { it[Keys.WidgetIdsJson] = JSONArray(ids).toString() }
    }

    private fun decodeWidgetIds(json: String?): List<Int> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList { for (i in 0 until arr.length()) add(arr.getInt(i)) }
        }.getOrElse { emptyList() }
    }

    suspend fun setHomeLayout(pages: List<List<HomeItem>>, folders: Map<String, HomeFolder>) {
        context.dataStore.edit {
            it[Keys.PagesJson] = encodePages(pages)
            it[Keys.FoldersJson] = encodeFolders(folders)
        }
    }

    private fun encodePages(pages: List<List<HomeItem>>): String {
        val root = JSONArray()
        pages.forEach { page ->
            val arr = JSONArray()
            page.forEach { item ->
                when (item) {
                    is HomeItem.App -> arr.put("a:${item.key}")
                    is HomeItem.Folder -> arr.put("f:${item.id}")
                }
            }
            root.put(arr)
        }
        return root.toString()
    }

    private fun decodePages(json: String?): List<List<HomeItem>> {
        if (json.isNullOrBlank()) return listOf(emptyList())
        return runCatching {
            val root = JSONArray(json)
            buildList {
                for (i in 0 until root.length()) {
                    val arr = root.getJSONArray(i)
                    add(buildList {
                        for (j in 0 until arr.length()) {
                            val s = arr.getString(j)
                            when {
                                s.startsWith("a:") -> add(HomeItem.App(s.removePrefix("a:")))
                                s.startsWith("f:") -> add(HomeItem.Folder(s.removePrefix("f:")))
                            }
                        }
                    })
                }
            }.ifEmpty { listOf(emptyList()) }
        }.getOrElse { listOf(emptyList()) }
    }

    private fun encodeFolders(folders: Map<String, HomeFolder>): String {
        val root = JSONObject()
        folders.values.forEach { folder ->
            root.put(
                folder.id,
                JSONObject()
                    .put("name", folder.name)
                    .put("apps", JSONArray(folder.appKeys))
            )
        }
        return root.toString()
    }

    private fun decodeFolders(json: String?): Map<String, HomeFolder> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(json)
            buildMap {
                root.keys().forEach { id ->
                    val obj = root.getJSONObject(id)
                    val appsArr = obj.optJSONArray("apps") ?: JSONArray()
                    val apps = buildList {
                        for (i in 0 until appsArr.length()) add(appsArr.getString(i))
                    }
                    put(
                        id,
                        HomeFolder(
                            id = id,
                            name = obj.optString("name", "Folder"),
                            appKeys = apps
                        )
                    )
                }
            }
        }.getOrElse { emptyMap() }
    }
}
