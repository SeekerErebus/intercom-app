package com.homeping.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.homeping.app.R
import com.homeping.app.ui.theme.HomePingOffline
import com.homeping.app.ui.theme.HomePingOnline
import com.homeping.app.ui.theme.HomePingTheme

@Composable
fun MainScreen(
    peerName: String = stringResource(R.string.peer_placeholder),
    statusText: String = stringResource(R.string.status_not_connected),
    peerOnline: Boolean = false,
    thisDeviceName: String = "",
    pingEnabled: Boolean = false,
    pingResultText: String? = null,
    onPingClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.main_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (thisDeviceName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.main_this_device, thisDeviceName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                PeerStatusCard(
                    peerName = peerName,
                    statusText = statusText,
                    peerOnline = peerOnline,
                )
                if (!pingResultText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = pingResultText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            val pingLabel = stringResource(R.string.ping_button)
            val pingHint = stringResource(R.string.ping_button_hint)
            Button(
                onClick = onPingClick,
                enabled = pingEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .semantics { contentDescription = "$pingLabel. $pingHint" },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                ),
            ) {
                Text(
                    text = pingLabel,
                    style = MaterialTheme.typography.displayLarge,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (pingEnabled) {
                        stringResource(R.string.main_ping_ready_note)
                    } else {
                        stringResource(R.string.main_networking_note)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onSettingsClick) {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeerStatusCard(
    peerName: String,
    statusText: String,
    peerOnline: Boolean,
) {
    val statusColor: Color = if (peerOnline) HomePingOnline else HomePingOffline
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = peerName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = statusColor,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun MainScreenPreview() {
    HomePingTheme {
        MainScreen(
            thisDeviceName = "Upstairs",
            peerName = "Downstairs",
            statusText = "Connected",
            peerOnline = true,
            pingEnabled = true,
            pingResultText = "They said Coming!",
        )
    }
}
