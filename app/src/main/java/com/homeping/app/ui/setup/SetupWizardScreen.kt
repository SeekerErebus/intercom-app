package com.homeping.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homeping.app.R
import com.homeping.app.ui.components.LargePrimaryButton

private val nameSuggestions = listOf(
    "Upstairs",
    "Downstairs",
    "Mom's phone",
    "Kitchen",
)

@Composable
fun SetupWizardScreen(
    viewModel: SetupViewModel,
    onSetupFinished: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.step) {
        if (state.step == SetupStep.Done) {
            onSetupFinished()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (state.step) {
            SetupStep.Welcome -> WelcomeStep(onContinue = viewModel::goToName)
            SetupStep.Name -> NameStep(
                name = state.displayName,
                error = state.nameError,
                onNameChange = viewModel::onDisplayNameChange,
                onSuggestion = viewModel::selectSuggestedName,
                onContinue = viewModel::continueFromName,
                onBack = viewModel::goToWelcome,
            )
            SetupStep.Pin -> PinStep(
                pin = state.pin,
                pinConfirm = state.pinConfirm,
                error = state.pinError,
                isSaving = state.isSaving,
                onPinChange = viewModel::onPinChange,
                onPinConfirmChange = viewModel::onPinConfirmChange,
                onContinue = viewModel::finishSetup,
                onBack = viewModel::backFromPin,
            )
            SetupStep.Done -> {
                // Navigation handled by LaunchedEffect
            }
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    WizardScaffold(
        title = stringResource(R.string.setup_welcome_title),
        body = stringResource(R.string.setup_welcome_body),
        primaryLabel = stringResource(R.string.setup_continue),
        onPrimary = onContinue,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NameStep(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.setup_name_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_name_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                label = { Text(stringResource(R.string.setup_name_label)) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_name_suggestions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                nameSuggestions.forEach { suggestion ->
                    FilterChip(
                        selected = name == suggestion,
                        onClick = { onSuggestion(suggestion) },
                        label = {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                    )
                }
            }
        }
        Column {
            LargePrimaryButton(
                text = stringResource(R.string.setup_continue),
                onClick = onContinue,
            )
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.setup_back),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun PinStep(
    pin: String,
    pinConfirm: String,
    error: String?,
    isSaving: Boolean,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.setup_pin_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_pin_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
                label = { Text(stringResource(R.string.setup_pin_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pinConfirm,
                onValueChange = onPinConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
                label = { Text(stringResource(R.string.setup_pin_confirm_label)) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Column {
            LargePrimaryButton(
                text = stringResource(R.string.setup_finish),
                onClick = onContinue,
                enabled = !isSaving,
            )
            TextButton(
                onClick = onBack,
                enabled = !isSaving,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.setup_back),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun WizardScaffold(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        LargePrimaryButton(text = primaryLabel, onClick = onPrimary)
    }
}
