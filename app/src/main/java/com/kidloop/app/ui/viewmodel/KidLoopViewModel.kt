package com.kidloop.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidloop.app.data.model.DailyUsage
import com.kidloop.app.data.model.KidLoopSettings
import com.kidloop.app.data.model.PlaylistItem
import com.kidloop.app.data.repository.DataStoreSettingsRepository
import com.kidloop.app.data.repository.SettingsRepository
import com.kidloop.app.lock.PinLockoutManager
import com.kidloop.app.player.PlaybackStatus
import com.kidloop.app.player.PlayerError
import com.kidloop.app.timer.TimeLimitManager
import com.kidloop.app.timer.TimerState
import com.kidloop.app.util.AndroidNetworkMonitor
import com.kidloop.app.util.Clock
import com.kidloop.app.util.NetworkMonitor
import com.kidloop.app.util.SecurityUtils
import com.kidloop.app.util.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScreenState {
    SETTINGS,
    EXPLANATION,
    KID_MODE
}

data class KidLoopUiState(
    val screen: ScreenState = ScreenState.SETTINGS,
    val settings: KidLoopSettings = KidLoopSettings(),
    val dailyUsage: DailyUsage = DailyUsage(),
    val currentItemIndex: Int = 0,
    val currentItem: PlaylistItem? = null,
    val isPlaying: Boolean = false,
    val isOffline: Boolean = false,
    val offlineRetrySeconds: Int = 10,
    val isAllVideosFailed: Boolean = false,
    val showPinDialog: Boolean = false,
    val isPinningDeclined: Boolean = false,
    val timerState: TimerState = TimerState(
        sessionRemainingSeconds = 1200L,
        sessionLimitSeconds = 1200L,
        dailyRemainingSeconds = 3600L,
        dailyLimitSeconds = 3600L,
        isSessionExpired = false,
        isDailyExpired = false
    )
)

class KidLoopViewModel(
    private val repository: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock = SystemClock()
) : ViewModel() {

    private val _uiState = MutableStateFlow(KidLoopUiState())
    val uiState: StateFlow<KidLoopUiState> = _uiState.asStateFlow()

    val lockoutManager = PinLockoutManager(clock = clock)

    private var timeLimitManager: TimeLimitManager? = null
    private var timerJob: Job? = null
    private var skipErrorJob: Job? = null
    private var offlineRetryJob: Job? = null
    private var consecutiveErrors = 0
    private var isInitialized = false

    init {
        observeDataStore()
        observeNetwork()
    }

    private fun observeDataStore() {
        viewModelScope.launch {
            combine(
                repository.settingsFlow,
                repository.dailyUsageFlow,
                repository.currentItemIndexFlow
            ) { settings, dailyUsage, savedIndex ->
                Triple(settings, dailyUsage, savedIndex)
            }.collect { (settings, dailyUsage, savedIndex) ->
                val items = settings.items
                val safeIndex = if (items.isNotEmpty()) {
                    savedIndex.coerceIn(0, items.size - 1)
                } else 0
                val activeItem = items.getOrNull(safeIndex)

                if (!isInitialized) {
                    isInitialized = true
                    timeLimitManager = TimeLimitManager(
                        clock = clock,
                        sessionLimitMinutes = settings.sessionLimitMinutes,
                        dailyLimitMinutes = settings.dailyLimitMinutes,
                        initialDailyUsedSeconds = dailyUsage.usedSecondsToday,
                        initialDateString = dailyUsage.dateString,
                        onPersistUsage = { dateStr, usedSecs ->
                            repository.saveDailyUsage(dateStr, usedSecs)
                        }
                    )

                    // Process death recovery: if Kid Mode was active, restore directly into KID_MODE
                    if (settings.isKidModeActive && settings.canStartKidMode) {
                        _uiState.update {
                            it.copy(
                                screen = ScreenState.KID_MODE,
                                settings = settings,
                                dailyUsage = dailyUsage,
                                currentItemIndex = safeIndex,
                                currentItem = activeItem,
                                isPlaying = true,
                                timerState = timeLimitManager!!.getTimerState()
                            )
                        }
                        startPlaybackTimerLoop()
                        return@collect
                    }
                } else {
                    timeLimitManager?.updateLimits(
                        sessionMinutes = settings.sessionLimitMinutes,
                        dailyMinutes = settings.dailyLimitMinutes
                    )
                }

                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        dailyUsage = dailyUsage,
                        currentItemIndex = safeIndex,
                        currentItem = activeItem,
                        timerState = timeLimitManager?.getTimerState() ?: current.timerState
                    )
                }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
                if (isOnline) {
                    offlineRetryJob?.cancel()
                    // Resume playback when network is restored
                    if (_uiState.value.screen == ScreenState.KID_MODE && !_uiState.value.timerState.isAnyLimitReached) {
                        _uiState.update { it.copy(isPlaying = true) }
                    }
                } else {
                    startOfflineRetryCountdown()
                }
            }
        }
    }

    private fun startOfflineRetryCountdown() {
        offlineRetryJob?.cancel()
        offlineRetryJob = viewModelScope.launch {
            while (_uiState.value.isOffline) {
                for (sec in 10 downTo 1) {
                    _uiState.update { it.copy(offlineRetrySeconds = sec) }
                    delay(1000L)
                }
                _uiState.update { it.copy(offlineRetrySeconds = 0) }
                delay(1000L)
            }
        }
    }

    private fun startPlaybackTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                if (state.screen == ScreenState.KID_MODE && state.isPlaying && !state.isOffline && !state.isAllVideosFailed) {
                    val updatedTimer = timeLimitManager?.tickPlaybackSecond()
                    if (updatedTimer != null) {
                        _uiState.update { it.copy(timerState = updatedTimer) }
                        if (updatedTimer.isAnyLimitReached) {
                            _uiState.update { it.copy(isPlaying = false) }
                        }
                    }
                }
            }
        }
    }

    fun onStartKidModeClicked() {
        val settings = _uiState.value.settings
        if (timeLimitManager?.isDailyLimitExhausted() == true) {
            return
        }
        if (settings.canStartKidMode) {
            _uiState.update { it.copy(screen = ScreenState.EXPLANATION) }
        }
    }

    fun onConfirmLaunchKidMode() {
        timeLimitManager?.resetSession()
        consecutiveErrors = 0
        viewModelScope.launch {
            repository.setKidModeActive(true)
        }
        _uiState.update {
            it.copy(
                screen = ScreenState.KID_MODE,
                isPlaying = true,
                isAllVideosFailed = false,
                timerState = timeLimitManager?.getTimerState() ?: it.timerState
            )
        }
        startPlaybackTimerLoop()
    }

    fun onCancelExplanation() {
        _uiState.update { it.copy(screen = ScreenState.SETTINGS) }
    }

    fun setPinningDeclined(declined: Boolean) {
        _uiState.update { it.copy(isPinningDeclined = declined) }
    }

    fun onPlayerReady() {
        consecutiveErrors = 0
        _uiState.update { it.copy(isPlaying = true) }
    }

    fun onPlayerStateChange(status: PlaybackStatus) {
        when (status) {
            PlaybackStatus.PLAYING -> {
                consecutiveErrors = 0
                _uiState.update { it.copy(isPlaying = true) }
            }
            PlaybackStatus.ENDED -> {
                // Loop to next video seamlessly
                nextVideo()
            }
            PlaybackStatus.PAUSED -> {
                // Transient PAUSED states happen internally in the IFrame player during video switches
                // or buffering. In Kid Mode, continuous loop playback is preserved.
            }
            else -> {}
        }
    }

    fun onPlayerError(error: PlayerError) {
        consecutiveErrors++
        val itemsCount = _uiState.value.settings.items.size
        if (itemsCount > 0 && consecutiveErrors >= itemsCount) {
            _uiState.update {
                it.copy(
                    isAllVideosFailed = true,
                    isPlaying = false
                )
            }
            return
        }

        // Automatically skip to the next video within 2 seconds
        skipErrorJob?.cancel()
        skipErrorJob = viewModelScope.launch {
            delay(2000L)
            nextVideo()
        }
    }

    /**
     * Advances to next video in circular queue: (i + 1) % size
     */
    fun nextVideo() {
        val items = _uiState.value.settings.items
        if (items.isEmpty()) return
        val nextIndex = (uiState.value.currentItemIndex + 1) % items.size
        selectIndex(nextIndex)
    }

    /**
     * Moves to previous video in circular queue: (i - 1 + size) % size
     */
    fun previousVideo() {
        val items = _uiState.value.settings.items
        if (items.isEmpty()) return
        val prevIndex = (uiState.value.currentItemIndex - 1 + items.size) % items.size
        selectIndex(prevIndex)
    }

    private fun selectIndex(index: Int) {
        val items = _uiState.value.settings.items
        if (items.isEmpty()) return
        val safeIndex = index.coerceIn(0, items.size - 1)
        val nextItem = items[safeIndex]
        _uiState.update {
            it.copy(
                currentItemIndex = safeIndex,
                currentItem = nextItem,
                isPlaying = true
            )
        }
        viewModelScope.launch {
            repository.saveCurrentItemIndex(safeIndex)
        }
    }

    fun onUnlockCornerTriggered() {
        lockoutManager.recordActivity()
        _uiState.update { it.copy(showPinDialog = true) }
    }

    fun onDismissPinDialog() {
        _uiState.update { it.copy(showPinDialog = false) }
    }

    fun verifyPin(inputPin: String): Boolean {
        val salt = _uiState.value.settings.pinSalt
        val hash = _uiState.value.settings.pinHash
        return SecurityUtils.verifyPin(inputPin, salt, hash)
    }

    fun onPinUnlockSuccess() {
        _uiState.update {
            it.copy(
                showPinDialog = false,
                screen = ScreenState.SETTINGS,
                isPlaying = false
            )
        }
        timerJob?.cancel()
        viewModelScope.launch {
            timeLimitManager?.forcePersist()
            repository.setKidModeActive(false)
        }
    }

    fun addLink(item: PlaylistItem) {
        viewModelScope.launch {
            repository.addItem(item)
        }
    }

    fun removeLink(itemId: String) {
        viewModelScope.launch {
            repository.removeItem(itemId)
        }
    }

    fun reorderLink(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorderItems(fromIndex, toIndex)
        }
    }

    fun savePin(pin: String) {
        val (salt, hash) = SecurityUtils.hashPin(pin)
        viewModelScope.launch {
            repository.savePin(salt, hash)
        }
    }

    fun updateSessionLimit(minutes: Int) {
        viewModelScope.launch {
            repository.updateSessionLimit(minutes)
        }
    }

    fun updateDailyLimit(minutes: Int) {
        viewModelScope.launch {
            repository.updateDailyLimit(minutes)
        }
    }

    fun resetTodayUsage() {
        viewModelScope.launch {
            val todayStr = timeLimitManager?.getLocalDateString().orEmpty()
            timeLimitManager?.resetTodayUsage()
            repository.resetDailyUsage(todayStr)
            _uiState.update {
                it.copy(
                    dailyUsage = DailyUsage(todayStr, 0L),
                    timerState = timeLimitManager?.getTimerState() ?: it.timerState
                )
            }
        }
    }

    fun toggleAllowVolumeKeys(allow: Boolean) {
        viewModelScope.launch {
            repository.setAllowVolumeKeys(allow)
        }
    }

    fun retryOnlineNow() {
        // Force retry trigger
        _uiState.update { it.copy(offlineRetrySeconds = 10) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        skipErrorJob?.cancel()
        offlineRetryJob?.cancel()
    }
}

class KidLoopViewModelFactory(
    private val context: Context,
    private val clock: Clock = SystemClock()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KidLoopViewModel::class.java)) {
            val repository = DataStoreSettingsRepository(context.applicationContext)
            val networkMonitor = AndroidNetworkMonitor(context.applicationContext)
            return KidLoopViewModel(repository, networkMonitor, clock) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
