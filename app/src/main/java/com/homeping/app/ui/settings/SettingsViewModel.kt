package com.homeping.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homeping.app.data.PreferencesRepository
import com.homeping.app.data.SetupValidation
import com.homeping.app.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsFormState(
    val displayName: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
    val nameError: String? = null,
    val pinError: String? = null,
    val nameSaved: Boolean = false,
    val pinSaved: Boolean = false,
    val isHydrated: Boolean = false,
)

class SettingsViewModel(
    private val repository: PreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences(
                deviceId = "",
                displayName = "",
                homePin = "",
                setupComplete = false,
            ),
        )

    private val _form = MutableStateFlow(SettingsFormState())
    val form: StateFlow<SettingsFormState> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            repository.preferences.collect { prefs ->
                if (!_form.value.isHydrated && prefs.setupComplete) {
                    _form.update {
                        SettingsFormState(
                            displayName = prefs.displayName,
                            pin = prefs.homePin,
                            pinConfirm = prefs.homePin,
                            isHydrated = true,
                        )
                    }
                }
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _form.update {
            it.copy(
                displayName = value.take(SetupValidation.NAME_MAX),
                nameError = null,
                nameSaved = false,
            )
        }
    }

    fun onPinChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(SetupValidation.PIN_MAX)
        _form.update { it.copy(pin = digits, pinError = null, pinSaved = false) }
    }

    fun onPinConfirmChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(SetupValidation.PIN_MAX)
        _form.update { it.copy(pinConfirm = digits, pinError = null, pinSaved = false) }
    }

    fun saveDisplayName() {
        val name = SetupValidation.normalizeName(_form.value.displayName)
        if (!SetupValidation.isValidDisplayName(name)) {
            _form.update {
                it.copy(nameError = "Enter a short name (1–32 characters).")
            }
            return
        }
        viewModelScope.launch {
            repository.setDisplayName(name)
            _form.update {
                it.copy(displayName = name, nameError = null, nameSaved = true)
            }
        }
    }

    fun savePin() {
        val pin = _form.value.pin
        val confirm = _form.value.pinConfirm
        if (!SetupValidation.isValidPin(pin)) {
            _form.update { it.copy(pinError = "PIN must be 4 to 6 digits.") }
            return
        }
        if (!SetupValidation.pinsMatch(pin, confirm)) {
            _form.update { it.copy(pinError = "Those PINs do not match.") }
            return
        }
        viewModelScope.launch {
            repository.setHomePin(pin)
            _form.update { it.copy(pinError = null, pinSaved = true) }
        }
    }

    class Factory(
        private val repository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
