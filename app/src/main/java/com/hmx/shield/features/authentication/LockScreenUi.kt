package com.hmx.shield.features.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LockScreen(
    appName: String,
    viewModel: LockViewModel,
    biometricAvailable: Boolean,
    onBiometric: () -> Unit,
    onUnlocked: () -> Unit,
    onIntruder: (Int) -> Unit
) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val failed by viewModel.failedAttempts.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val lockType = viewModel.lockType

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("$appName Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        when (lockType) {
            com.hmx.shield.core.model.LockType.PIN,
            com.hmx.shield.core.model.LockType.BIOMETRIC -> {
                PinDots(input.length)
                Spacer(Modifier.height(24.dp))
                PinPad { digit ->
                    val newInput = if (digit == "back") input.dropLast(1) else input + digit
                    viewModel.setInput(newInput)
                    if (newInput.length >= 4) {
                        viewModel.submit(onUnlocked, onIntruder)
                    }
                }
            }
            com.hmx.shield.core.model.LockType.PASSWORD -> {
                OutlinedTextField(
                    value = input,
                    onValueChange = viewModel::setInput,
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { viewModel.submit(onUnlocked, onIntruder) }) {
                    Text("Unlock")
                }
            }
            com.hmx.shield.core.model.LockType.PATTERN -> {
                PatternGrid { sequence ->
                    viewModel.setInput(sequence)
                    if (sequence.length >= 4) viewModel.submit(onUnlocked, onIntruder)
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error ?: "", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        if (failed > 0) {
            Spacer(Modifier.height(8.dp))
            Text("Failed attempts: $failed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        if (biometricAvailable) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBiometric) { Text("Use biometric") }
        }
    }
}

@Composable
private fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { index ->
            Box(
                Modifier.size(14.dp).clip(CircleShape)
                    .background(if (index < count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    Box(
                        Modifier.size(64.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = key.isNotEmpty()) {
                                if (key == "⌫") onDigit("back") else onDigit(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) {
                            Text(key, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternGrid(onComplete: (String) -> Unit) {
    val selected = remember { mutableStateListOf<Int>() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    val active = selected.contains(index)
                    Box(
                        Modifier.size(56.dp).clip(CircleShape)
                            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (!selected.contains(index)) {
                                    selected.add(index)
                                    if (selected.size >= 4) onComplete(selected.joinToString(""))
                                }
                            }
                    )
                }
            }
        }
        TextButton(onClick = { selected.clear() }) { Text("Reset pattern") }
    }
}
