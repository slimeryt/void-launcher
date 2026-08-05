package com.voidlauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.AppRepository
import com.voidlauncher.app.data.LauncherPreferences
import com.voidlauncher.app.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherUiState(
    val apps: List<AppInfo> = emptyList(),
    val dockApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isDrawerOpen: Boolean = false,
    val isLoading: Boolean = true,
    val showLabels: Boolean = true,
    val gridColumns: Int = 4,
    val iconScale: Float = 1f,
    val hiddenCount: Int = 0
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val prefsRepository = PreferencesRepository(application)

    private val allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val isDrawerOpen = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(true)

    private data class TransientUi(
        val apps: List<AppInfo>,
        val query: String,
        val drawerOpen: Boolean,
        val loading: Boolean
    )

    private val transient = combine(
        allApps,
        searchQuery,
        isDrawerOpen,
        isLoading
    ) { apps, query, drawerOpen, loading ->
        TransientUi(apps, query, drawerOpen, loading)
    }

    val state: StateFlow<LauncherUiState> = combine(
        transient,
        prefsRepository.preferences
    ) { ui, prefs ->
        val visible = ui.apps.filterNot { it.key in prefs.hidden }
        val dock = prefs.favorites
            .mapNotNull { key -> visible.find { it.key == key } }
            .take(5)
            .ifEmpty { visible.take(4) }

        val filtered = if (ui.query.isBlank()) {
            visible
        } else {
            visible.filter { it.label.contains(ui.query, ignoreCase = true) }
        }

        LauncherUiState(
            apps = visible,
            dockApps = dock,
            filteredApps = filtered,
            searchQuery = ui.query,
            isDrawerOpen = ui.drawerOpen,
            isLoading = ui.loading,
            showLabels = prefs.showLabels,
            gridColumns = prefs.gridColumns,
            iconScale = prefs.iconScale,
            hiddenCount = prefs.hidden.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState()
    )

    private var prefsCache = LauncherPreferences()

    init {
        viewModelScope.launch {
            prefsRepository.preferences.collect { prefsCache = it }
        }
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            isLoading.value = true
            allApps.value = appRepository.loadLaunchableApps()
            isLoading.value = false
        }
    }

    fun launchApp(app: AppInfo) {
        appRepository.launch(app)
        setDrawerOpen(false)
        setSearchQuery("")
    }

    fun toggleFavorite(app: AppInfo) {
        viewModelScope.launch {
            val next = prefsCache.favorites.toMutableSet()
            if (!next.add(app.key)) next.remove(app.key)
            prefsRepository.setFavorites(next)
        }
    }

    fun hideApp(app: AppInfo) {
        viewModelScope.launch {
            prefsRepository.setHidden(prefsCache.hidden + app.key)
        }
    }

    fun unhideApp(appKey: String) {
        viewModelScope.launch {
            prefsRepository.setHidden(prefsCache.hidden - appKey)
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setDrawerOpen(open: Boolean) {
        isDrawerOpen.value = open
        if (!open) searchQuery.value = ""
    }

    fun setShowLabels(show: Boolean) {
        viewModelScope.launch { prefsRepository.setShowLabels(show) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { prefsRepository.setGridColumns(columns) }
    }

    fun setIconScale(scale: Float) {
        viewModelScope.launch { prefsRepository.setIconScale(scale) }
    }

    fun openAppInfo(app: AppInfo) {
        appRepository.openAppInfo(app.packageName)
    }

    fun uninstallApp(app: AppInfo) {
        appRepository.uninstall(app.packageName)
    }
}
