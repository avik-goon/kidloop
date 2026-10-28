package com.kidloop.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kidloop.app.R

@Composable
fun PinSetupDialog(
    isInitialSetup: Boolean,
    onPinSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val mismatchError = stringResource(R.string.pin_setup_mismatch)
    val lengthError = stringResource(R.string.pin_setup_length_error)

    AlertDialog(
        onDismissRequest = {
            if (!isInitialSetup) onDismiss()
        },
        title = {
            Text(
                text = stringResource(
                    if (isInitialSetup) R.string.pin_setup_title_create
                    else R.string.pin_setup_title_change
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            newPin = input
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.pin_setup_new_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            confirmPin = input
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.pin_setup_confirm_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length < 4 || newPin.length > 6) {
                        errorMessage = lengthError
                    } else if (newPin != confirmPin) {
                        errorMessage = mismatchError
                    } else {
                        onPinSaved(newPin)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            if (!isInitialSetup) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
