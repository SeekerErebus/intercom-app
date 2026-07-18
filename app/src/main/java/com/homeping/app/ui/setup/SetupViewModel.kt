package com.homeping.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homeping.app.data.PreferencesRepository
import com.homeping.app.data.SetupValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupStep {
    Welcome,
    Name,
    Pin,
    Done,
}

data class SetupUiState(
    val step: SetupStep = SetupStep.Welcome,
    val displayName: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
    val nameError: String? = null,
    val pinError: String? = null,
    val isSaving: Boolean = false,
)

class SetupViewModel(
    private val repository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDeviceId()
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update {
            it.copy(displayName = value.take(SetupValidation.NAME_MAX), nameError = null)
        }
    }

    fun onPinChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(SetupValidation.PIN_MAX)
        _uiState.update { it.copy(pin = digits, pinError = null) }
    }

    fun onPinConfirmChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(SetupValidation.PIN_MAX)
        _uiState.update { it.copy(pinConfirm = digits, pinError = null) }
    }

    fun goToName() {
        _uiState.update { it.copy(step = SetupStep.Name) }
    }

    fun goToWelcome() {
        _uiState.update { it.copy(step = SetupStep.Welcome) }
    }

    fun continueFromName() {
        val name = SetupValidation.normalizeName(_uiState.value.displayName)
        if (!SetupValidation.isValidDisplayName(name)) {
            _uiState.update {
                it.copy(nameError = "Enter a short name for this phone (1–32 characters).")
            }
            return
        }
        _uiState.update {
            it.copy(displayName = name, step = SetupStep.Pin, nameError = null)
        }
    }

    fun backFromPin() {
        _uiState.update { it.copy(step = SetupStep.Name, pinError = null) }
    }

    fun finishSetup() {
        val state = _uiState.value
        if (!SetupValidation.isValidPin(state.pin)) {
            _uiState.update {
                it.copy(pinError = "PIN must be 4 to 6 digits.")
            }
            return
        }
        if (!SetupValidation.pinsMatch(state.pin, state.pinConfirm)) {
            _uiState.update {
                it.copy(pinError = "Those PINs do not match. Try again.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, pinError = null) }
            repository.completeSetup(
                displayName = state.displayName,
                homePin = state.pin,
            )
            _uiState.update {
                it.copy(isSaving = false, step = SetupStep.Done)
            }
        }
    }

    fun selectSuggestedName(name: String) {
        onDisplayNameChange(name)
    }

    class Factory(
        private val repository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
                return SetupViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
