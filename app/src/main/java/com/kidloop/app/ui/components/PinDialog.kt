package com.kidloop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kidloop.app.R
import com.kidloop.app.lock.PinLockoutManager
import kotlinx.coroutines.delay

const val PIN_DIALOG_TEST_TAG = "PinDialog"
const val PIN_SUBMIT_TEST_TAG = "PinSubmitButton"
const val PIN_ERROR_TEST_TAG = "PinErrorMessage"

@Composable
fun PinDialog(
    lockoutManager: PinLockoutManager,
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLockedOut by remember { mutableStateOf(lockoutManager.isLockedOut()) }
    var lockoutSeconds by remember { mutableIntStateOf(lockoutManager.lockoutSecondsRemaining()) }
    var inactivitySeconds by remember { mutableIntStateOf(lockoutManager.inactivitySecondsRemaining()) }

    // 1-second ticker for inactivity and lockout countdowns
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            isLockedOut = lockoutManager.isLockedOut()
            lockoutSeconds = lockoutManager.lockoutSecondsRemaining()
            inactivitySeconds = lockoutManager.inactivitySecondsRemaining()

            if (lockoutManager.isInactivityTimedOut()) {
                onDismiss()
                break
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag(PIN_DIALOG_TEST_TAG),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header & Auto-dismiss countdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pin_unlock_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.pin_unlock_auto_dismiss, inactivitySeconds),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.pin_unlock_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Masked PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val isFilled = index < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error / Lockout Messages
                if (isLockedOut) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.pin_unlock_locked, lockoutSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag(PIN_ERROR_TEST_TAG)
                        )
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(PIN_ERROR_TEST_TAG)
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numeric Keypad
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )

                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (digit in row) {
                                KeypadDigitButton(
                                    digit = digit,
                                    enabled = !isLockedOut && pinInput.length < 6,
                                    onClick = {
                                        lockoutManager.recordActivity()
                                        if (pinInput.length < 6) {
                                            pinInput += digit
                                            errorMessage = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Row: Backspace, 0, Done
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Backspace button
                        Button(
                            onClick = {
                                lockoutManager.recordActivity()
                                if (pinInput.isNotEmpty()) {
                                    pinInput = pinInput.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            enabled = !isLockedOut && pinInput.isNotEmpty(),
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.action_clear),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // '0' digit button
                        KeypadDigitButton(
                            digit = "0",
                            enabled = !isLockedOut && pinInput.length < 6,
                            onClick = {
                                lockoutManager.recordActivity()
                                if (pinInput.length < 6) {
                                    pinInput += "0"
                                    errorMessage = null
                                }
                            }
                        )

                        // Cancel button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Unlock Button
                val wrongPinTemplate = stringResource(R.string.pin_unlock_error)
                Button(
                    onClick = {
                        lockoutManager.recordActivity()
                        val isCorrect = onVerifyPin(pinInput)
                        val attemptSuccess = lockoutManager.recordAttempt(isCorrect)
                        if (attemptSuccess) {
                            onSuccess()
                        } else {
                            isLockedOut = lockoutManager.isLockedOut()
                            lockoutSeconds = lockoutManager.lockoutSecondsRemaining()
                            val remaining = lockoutManager.remainingAttempts()
                            errorMessage = String.format(wrongPinTemplate, remaining)
                            pinInput = ""
                        }
                    },
                    enabled = !isLockedOut && pinInput.length >= 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(PIN_SUBMIT_TEST_TAG),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_done),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = digit,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
