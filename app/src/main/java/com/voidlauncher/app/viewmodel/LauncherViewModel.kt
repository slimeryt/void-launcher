package com.voidlauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voidlauncher.app.data.AppInfo
import com.voidlauncher.app.data.AppRepository
import com.voidlauncher.app.data.HomeFolder
import com.voidlauncher.app.data.HomeItem
import com.voidlauncher.app.data.LauncherPreferences
import com.voidlauncher.app.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class LauncherUiState(
    val apps: List<AppInfo> = emptyList(),
    val appsByKey: Map<String, AppInfo> = emptyMap(),
    val dockApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val pages: List<List<HomeItem>> = listOf(emptyList()),
    val folders: Map<String, HomeFolder> = emptyMap(),
    val searchQuery: String = "",
    val isDrawerOpen: Boolean = false,
    val drawerFocusSearch: Boolean = false,
    val isNotificationCenterOpen: Boolean = false,
    val isControlCenterOpen: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val showLabels: Boolean = true,
    val gridColumns: Int = 4,
    val iconScale: Float = 1f,
    val iconTheme: String = "standard",
    val iconCornerRadiusPercent: Float = 24f,
    val iconTintHue: Float = 210f,
    val iconTintAlpha: Float = 0.55f,
    val iconEditorOpen: Boolean = false,
    val hiddenCount: Int = 0,
    val glassBlurStrength: Float = 1f,
    val glassFrostAmount: Float = 1f,
    val glassRefraction: Boolean = true,
    val glassSheen: Boolean = true,
    val dockLabels: Boolean = false,
    val hapticFeedback: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val widgetIds: List<Int> = emptyList()
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val prefsRepository = PreferencesRepository(application)

    private val allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val isDrawerOpen = MutableStateFlow(false)
    private val drawerFocusSearch = MutableStateFlow(false)
    private val isNotificationCenterOpen = MutableStateFlow(false)
    private val isControlCenterOpen = MutableStateFlow(false)
    private val isEditMode = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(true)
    private val iconEditorOpen = MutableStateFlow(false)

    val state: StateFlow<LauncherUiState> = combine(
        combine(allApps, searchQuery, isDrawerOpen) { a, q, d -> Triple(a, q, d) },
        combine(drawerFocusSearch, isEditMode, isLoading) { f, e, l -> Triple(f, e, l) },
        combine(iconEditorOpen, prefsRepository.preferences) { editor, prefs -> editor to prefs },
        combine(isNotificationCenterOpen, isControlCenterOpen) { n, c -> n to c }
    ) { t1, t2, editorPrefs, shade ->
        val apps = t1.first
        val query = t1.second
        val drawerOpen = t1.third
        val focusSearch = t2.first
        val editMode = t2.second
        val loading = t2.third
        val editorOpen = editorPrefs.first
        val prefs = editorPrefs.second

        val visible = apps.filterNot { it.key in prefs.hidden }
        val byKey = visible.associateBy { it.key }
        val dock = prefs.favorites
            .mapNotNull { key -> byKey[key] }
            .take(5)
            .ifEmpty { visible.take(4) }

        val filtered = if (query.isBlank()) visible
        else visible.filter { it.label.contains(query, ignoreCase = true) }

        LauncherUiState(
            apps = visible,
            appsByKey = byKey,
            dockApps = dock,
            filteredApps = filtered,
            pages = prefs.pages.ifEmpty { listOf(emptyList()) },
            folders = prefs.folders,
            searchQuery = query,
            isDrawerOpen = drawerOpen,
            drawerFocusSearch = focusSearch,
            isNotificationCenterOpen = shade.first,
            isControlCenterOpen = shade.second,
            isEditMode = editMode,
            isLoading = loading,
            showLabels = prefs.showLabels,
            gridColumns = prefs.gridColumns,
            iconScale = prefs.iconScale,
            iconTheme = prefs.iconTheme,
            iconCornerRadiusPercent = prefs.iconCornerRadiusPercent,
            iconTintHue = prefs.iconTintHue,
            iconTintAlpha = prefs.iconTintAlpha,
            iconEditorOpen = editorOpen,
            hiddenCount = prefs.hidden.size,
            glassBlurStrength = prefs.glassBlurStrength,
            glassFrostAmount = prefs.glassFrostAmount,
            glassRefraction = prefs.glassRefraction,
            glassSheen = prefs.glassSheen,
            dockLabels = prefs.dockLabels,
            hapticFeedback = prefs.hapticFeedback,
            autoCheckUpdates = prefs.autoCheckUpdates,
            widgetIds = prefs.widgetIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState()
    )

    private var prefsCache = LauncherPreferences()
    private var seeded = false

    init {
        viewModelScope.launch {
            prefsRepository.preferences.collect { prefs ->
                prefsCache = prefs
                maybeSeedHome(prefs)
            }
        }
        refreshApps()
    }

    private fun maybeSeedHome(prefs: LauncherPreferences) {
        if (seeded) return
        if (prefs.pages.any { it.isNotEmpty() }) {
            seeded = true
            return
        }
        val apps = allApps.value
        if (apps.isEmpty()) return
        seeded = true
        val page = apps.filterNot { it.key in prefs.hidden }
            .take(prefs.gridColumns * 5)
            .map { HomeItem.App(it.key) }
        viewModelScope.launch {
            prefsRepository.setHomeLayout(listOf(page), prefs.folders)
            if (prefs.favorites.isEmpty()) {
                prefsRepository.setFavorites(apps.take(4).map { it.key })
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            isLoading.value = true
            allApps.value = appRepository.loadLaunchableApps()
            isLoading.value = false
            maybeSeedHome(prefsCache)
        }
    }

    fun launchApp(app: AppInfo) {
        if (isEditMode.value) return
        appRepository.launch(app)
        setDrawerOpen(false)
        setSearchQuery("")
    }

    fun toggleFavorite(app: AppInfo) {
        viewModelScope.launch {
            val next = prefsCache.favorites.toMutableList()
            if (next.contains(app.key)) {
                next.removeAll { it == app.key }
            } else if (next.size < 5) {
                next.add(app.key)
            } else {
                // Replace last dock slot when full
                next[next.lastIndex] = app.key
            }
            prefsRepository.setFavorites(next)
        }
    }

    fun swapDockItems(a: Int, b: Int) {
        if (a == b || a < 0 || b < 0) return
        viewModelScope.launch {
            val next = prefsCache.favorites.toMutableList()
            if (a >= next.size || b >= next.size) return@launch
            val tmp = next[a]
            next[a] = next[b]
            next[b] = tmp
            prefsRepository.setFavorites(next)
        }
    }

    /** Remove from dock; place on home if it isn't already there. */
    fun moveDockAppToHome(app: AppInfo, pageIndex: Int = 0) {
        viewModelScope.launch {
            val nextFav = prefsCache.favorites.toMutableList()
            if (!nextFav.removeAll { it == app.key }) return@launch
            val pagesSnapshot = prefsCache.pages.toMutableList().ifEmpty { mutableListOf(emptyList()) }
            val foldersSnapshot = prefsCache.folders
            while (pagesSnapshot.size <= pageIndex) pagesSnapshot.add(emptyList())
            val alreadyOnHome = pagesSnapshot.any { p ->
                p.any { it is HomeItem.App && it.key == app.key }
            }
            prefsRepository.setFavorites(nextFav)
            if (!alreadyOnHome) {
                val page = pagesSnapshot[pageIndex].toMutableList()
                page.add(HomeItem.App(app.key))
                pagesSnapshot[pageIndex] = page
                prefsRepository.setHomeLayout(pagesSnapshot, foldersSnapshot)
            }
        }
    }

    fun hideApp(app: AppInfo) {
        viewModelScope.launch {
            prefsRepository.setHidden(prefsCache.hidden + app.key)
            removeAppFromHome(app.key)
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setDrawerOpen(open: Boolean, focusSearch: Boolean = false) {
        isDrawerOpen.value = open
        drawerFocusSearch.value = open && focusSearch
        if (open) {
            isNotificationCenterOpen.value = false
            isControlCenterOpen.value = false
        }
        if (!open) {
            searchQuery.value = ""
            drawerFocusSearch.value = false
        }
    }

    fun openDrawerSearch() {
        setDrawerOpen(open = true, focusSearch = true)
    }

    fun setNotificationCenterOpen(open: Boolean) {
        isNotificationCenterOpen.value = open
        if (open) {
            isControlCenterOpen.value = false
            isDrawerOpen.value = false
            drawerFocusSearch.value = false
            isEditMode.value = false
        }
    }

    fun setControlCenterOpen(open: Boolean) {
        isControlCenterOpen.value = open
        if (open) {
            isNotificationCenterOpen.value = false
            isDrawerOpen.value = false
            drawerFocusSearch.value = false
            isEditMode.value = false
        }
    }

    fun setEditMode(edit: Boolean) {
        isEditMode.value = edit
        if (edit) {
            isNotificationCenterOpen.value = false
            isControlCenterOpen.value = false
        }
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

    fun setIconAppearance(
        theme: String,
        cornerRadiusPercent: Float,
        tintHue: Float,
        tintAlpha: Float,
        scale: Float
    ) {
        viewModelScope.launch {
            prefsRepository.setIconAppearance(theme, cornerRadiusPercent, tintHue, tintAlpha, scale)
        }
    }

    fun setIconEditorOpen(open: Boolean) {
        iconEditorOpen.value = open
        if (open) {
            isDrawerOpen.value = false
            isEditMode.value = false
        }
    }

    fun setGlassBlurStrength(value: Float) {
        viewModelScope.launch { prefsRepository.setGlassBlurStrength(value) }
    }

    fun setGlassFrostAmount(value: Float) {
        viewModelScope.launch { prefsRepository.setGlassFrostAmount(value) }
    }

    fun setGlassRefraction(value: Boolean) {
        viewModelScope.launch { prefsRepository.setGlassRefraction(value) }
    }

    fun setGlassSheen(value: Boolean) {
        viewModelScope.launch { prefsRepository.setGlassSheen(value) }
    }

    fun setDockLabels(value: Boolean) {
        viewModelScope.launch { prefsRepository.setDockLabels(value) }
    }

    fun setHapticFeedback(value: Boolean) {
        viewModelScope.launch { prefsRepository.setHapticFeedback(value) }
    }

    fun setAutoCheckUpdates(value: Boolean) {
        viewModelScope.launch { prefsRepository.setAutoCheckUpdates(value) }
    }

    fun openAppInfo(app: AppInfo) {
        appRepository.openAppInfo(app.packageName)
    }

    fun addAppToHome(app: AppInfo, pageIndex: Int = 0) {
        viewModelScope.launch {
            val pages = prefsCache.pages.toMutableList().ifEmpty { mutableListOf(emptyList()) }
            while (pages.size <= pageIndex) pages.add(emptyList())
            val page = pages[pageIndex].toMutableList()
            if (pages.any { p -> p.any { it is HomeItem.App && it.key == app.key } }) return@launch
            page.add(HomeItem.App(app.key))
            pages[pageIndex] = page
            prefsRepository.setHomeLayout(pages, prefsCache.folders)
        }
    }

    fun removeItemFromHome(pageIndex: Int, itemIndex: Int) {
        viewModelScope.launch {
            val pages = prefsCache.pages.toMutableList()
            if (pageIndex !in pages.indices) return@launch
            val page = pages[pageIndex].toMutableList()
            if (itemIndex !in page.indices) return@launch
            val removed = page.removeAt(itemIndex)
            pages[pageIndex] = page
            var folders = prefsCache.folders
            if (removed is HomeItem.Folder) folders = folders - removed.id
            while (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
            prefsRepository.setHomeLayout(pages, folders)
        }
    }

    fun moveItem(fromPage: Int, fromIndex: Int, toPage: Int, toIndex: Int) {
        viewModelScope.launch {
            val pages = prefsCache.pages.map { it.toMutableList() }.toMutableList()
            while (pages.size <= maxOf(fromPage, toPage)) pages.add(mutableListOf())
            if (fromPage !in pages.indices || fromIndex !in pages[fromPage].indices) return@launch
            val item = pages[fromPage].removeAt(fromIndex)
            val dest = pages[toPage]
            dest.add(toIndex.coerceIn(0, dest.size), item)
            while (pages.size > 1 && pages.last().isEmpty()) pages.removeAt(pages.lastIndex)
            prefsRepository.setHomeLayout(pages, prefsCache.folders)
        }
    }

    fun swapItems(pageIndex: Int, a: Int, b: Int) {
        viewModelScope.launch {
            val pages = prefsCache.pages.toMutableList()
            if (pageIndex !in pages.indices) return@launch
            val page = pages[pageIndex].toMutableList()
            if (a !in page.indices || b !in page.indices) return@launch
            val tmp = page[a]
            page[a] = page[b]
            page[b] = tmp
            pages[pageIndex] = page
            prefsRepository.setHomeLayout(pages, prefsCache.folders)
        }
    }

    fun createFolderFromDrop(pageIndex: Int, targetIndex: Int, draggedIndex: Int) {
        viewModelScope.launch {
            if (pageIndex !in prefsCache.pages.indices) return@launch
            val page = prefsCache.pages[pageIndex].toMutableList()
            if (targetIndex !in page.indices || draggedIndex !in page.indices) return@launch
            if (targetIndex == draggedIndex) return@launch
            val targetApp = (page[targetIndex] as? HomeItem.App)?.key ?: return@launch
            val draggedApp = (page[draggedIndex] as? HomeItem.App)?.key ?: return@launch
            val id = UUID.randomUUID().toString().take(8)
            val folder = HomeFolder(id, "Folder", listOf(targetApp, draggedApp))
            val high = maxOf(targetIndex, draggedIndex)
            val low = minOf(targetIndex, draggedIndex)
            page.removeAt(high)
            page.removeAt(low)
            page.add(low, HomeItem.Folder(id))
            val pages = prefsCache.pages.toMutableList()
            pages[pageIndex] = page
            prefsRepository.setHomeLayout(pages, prefsCache.folders + (id to folder))
        }
    }

    /** Drop an app onto an existing folder (or fold an app under a dragged folder). */
    fun addAppToFolderFromDrop(pageIndex: Int, folderIndex: Int, appIndex: Int) {
        viewModelScope.launch {
            if (pageIndex !in prefsCache.pages.indices) return@launch
            val page = prefsCache.pages[pageIndex].toMutableList()
            if (folderIndex !in page.indices || appIndex !in page.indices) return@launch
            if (folderIndex == appIndex) return@launch
            val folderItem = page[folderIndex] as? HomeItem.Folder ?: return@launch
            val appItem = page[appIndex] as? HomeItem.App ?: return@launch
            val folder = prefsCache.folders[folderItem.id] ?: return@launch
            val keys = if (appItem.key in folder.appKeys) folder.appKeys
            else folder.appKeys + appItem.key
            page.removeAt(appIndex)
            val pages = prefsCache.pages.toMutableList()
            pages[pageIndex] = page
            prefsRepository.setHomeLayout(
                pages,
                prefsCache.folders + (folder.id to folder.copy(appKeys = keys))
            )
        }
    }

    fun addPage() {
        viewModelScope.launch {
            prefsRepository.setHomeLayout(prefsCache.pages + listOf(emptyList()), prefsCache.folders)
        }
    }

    fun removePage(pageIndex: Int) {
        viewModelScope.launch {
            val pages = prefsCache.pages.toMutableList().ifEmpty { mutableListOf(emptyList()) }
            if (pages.size <= 1 || pageIndex !in pages.indices) return@launch
            val removed = pages.removeAt(pageIndex)
            if (removed.isNotEmpty()) {
                val dest = (pageIndex - 1).coerceAtLeast(0).coerceAtMost(pages.lastIndex)
                pages[dest] = pages[dest] + removed
            }
            // Drop orphaned empty folders that are no longer referenced
            val folderIdsOnHome = pages.flatten()
                .mapNotNull { (it as? HomeItem.Folder)?.id }
                .toSet()
            val folders = prefsCache.folders.filterKeys { it in folderIdsOnHome }
            prefsRepository.setHomeLayout(pages.ifEmpty { listOf(emptyList()) }, folders)
        }
    }

    fun extractAppFromFolder(folderId: String, appKey: String, pageIndex: Int) {
        viewModelScope.launch {
            val folder = prefsCache.folders[folderId] ?: return@launch
            if (appKey !in folder.appKeys) return@launch
            val remaining = folder.appKeys.filterNot { it == appKey }

            val pages = prefsCache.pages.map { it.toMutableList() }.toMutableList()
                .ifEmpty { mutableListOf(mutableListOf()) }
            while (pages.size <= pageIndex) pages.add(mutableListOf())

            var folderPage = -1
            var folderIndex = -1
            for (pi in pages.indices) {
                val idx = pages[pi].indexOfFirst { it is HomeItem.Folder && it.id == folderId }
                if (idx >= 0) {
                    folderPage = pi
                    folderIndex = idx
                    break
                }
            }

            val destPage = pageIndex.coerceIn(0, pages.lastIndex)
            fun placeApp(p: Int, key: String) {
                if (pages[p].none { it is HomeItem.App && it.key == key }) {
                    pages[p].add(HomeItem.App(key))
                }
            }
            placeApp(destPage, appKey)

            val folders = prefsCache.folders.toMutableMap()
            when {
                remaining.isEmpty() -> {
                    if (folderPage >= 0) pages[folderPage].removeAt(folderIndex)
                    folders.remove(folderId)
                }
                remaining.size == 1 -> {
                    val lastKey = remaining[0]
                    if (folderPage >= 0) {
                        pages[folderPage][folderIndex] = HomeItem.App(lastKey)
                    } else {
                        placeApp(destPage, lastKey)
                    }
                    folders.remove(folderId)
                }
                else -> {
                    folders[folderId] = folder.copy(appKeys = remaining)
                }
            }

            prefsRepository.setHomeLayout(pages.map { it.toList() }, folders)
        }
    }

    fun addWidget(widgetId: Int) {
        viewModelScope.launch {
            if (widgetId in prefsCache.widgetIds) return@launch
            prefsRepository.setWidgetIds(prefsCache.widgetIds + widgetId)
        }
    }

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            prefsRepository.setWidgetIds(prefsCache.widgetIds - widgetId)
        }
    }

    private suspend fun removeAppFromHome(appKey: String) {
        val folders = prefsCache.folders.mapValues { (_, f) ->
            f.copy(appKeys = f.appKeys.filterNot { it == appKey })
        }.filterValues { it.appKeys.isNotEmpty() }
        val folderIds = folders.keys
        val cleaned = prefsCache.pages.map { page ->
            page.filterNot {
                (it is HomeItem.App && it.key == appKey) ||
                    (it is HomeItem.Folder && it.id !in folderIds)
            }
        }.toMutableList()
        while (cleaned.size > 1 && cleaned.last().isEmpty()) cleaned.removeAt(cleaned.lastIndex)
        prefsRepository.setHomeLayout(cleaned.ifEmpty { listOf(emptyList()) }, folders)
    }
}
