package com.homeping.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homeping.app.R
import com.homeping.app.discovery.DiscoveredPeer
import com.homeping.app.service.NotificationPermission
import com.homeping.app.ui.components.LargePrimaryButton
import com.homeping.app.ui.theme.HomePingOnline

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    discoveredPeers: List<DiscoveredPeer> = emptyList(),
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationsOk = NotificationPermission.hasPostNotifications(context)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_this_phone, prefs.displayName.ifBlank { "—" }),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.settings_name_section),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = form.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                label = { Text(stringResource(R.string.setup_name_label)) },
                isError = form.nameError != null,
                supportingText = {
                    when {
                        form.nameError != null -> Text(form.nameError!!)
                        form.nameSaved -> Text(stringResource(R.string.settings_saved))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LargePrimaryButton(
                text = stringResource(R.string.settings_save_name),
                onClick = viewModel::saveDisplayName,
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_pin_section),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_pin_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = form.pin,
                onValueChange = viewModel::onPinChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
                label = { Text(stringResource(R.string.setup_pin_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = form.pinConfirm,
                onValueChange = viewModel::onPinConfirmChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
                label = { Text(stringResource(R.string.setup_pin_confirm_label)) },
                isError = form.pinError != null,
                supportingText = {
                    when {
                        form.pinError != null -> Text(form.pinError!!)
                        form.pinSaved -> Text(stringResource(R.string.settings_saved))
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LargePrimaryButton(
                text = stringResource(R.string.settings_save_pin),
                onClick = viewModel::savePin,
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_service_section),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_service_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (notificationsOk) {
                    stringResource(R.string.settings_service_running)
                } else {
                    stringResource(R.string.settings_enable_notifications)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!notificationsOk) {
                Spacer(modifier = Modifier.height(12.dp))
                LargePrimaryButton(
                    text = stringResource(R.string.settings_enable_notifications),
                    onClick = onRequestNotificationPermission,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.settings_peers_section),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_peers_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (discoveredPeers.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_peers_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                discoveredPeers.forEach { peer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = peer.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = HomePingOnline,
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_peer_detail,
                                    peer.host,
                                    peer.port,
                                    peer.shortId,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.settings_about, prefs.deviceId.take(8).ifBlank { "—" }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
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
