package com.hmx.shield.features.themes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hmx.shield.core.ThemeController
import com.hmx.shield.core.model.AccentColor
import com.hmx.shield.core.model.ThemeMode
import com.hmx.shield.data.local.repository.SettingsRepository
import com.hmx.shield.ui.components.GlassCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val themeController: ThemeController
) : ViewModel() {
    val theme: StateFlow<com.hmx.shield.data.local.db.entity.ThemeSettingsEntity?> =
        settingsRepository.observeTheme().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode.name)
            val accent = themeController.state.value.accent
            themeController.update(mode, accent)
        }
    }

    fun setAccent(color: AccentColor) {
        viewModelScope.launch {
            settingsRepository.setAccent(color.name)
            val mode = themeController.state.value.mode
            themeController.update(mode, color)
        }
    }
}

@Composable
fun ThemeScreen(nav: androidx.navigation.NavHostController, vm: ThemeViewModel = hiltViewModel()) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val modes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val accents = AccentColor.values()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Themes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        modes.forEach { m ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { vm.setMode(m) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = theme?.themeMode == m.name, onClick = { vm.setMode(m) })
                Spacer(modifier = Modifier.height(8.dp))
                Text(m.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Accent color", style = MaterialTheme.typography.titleMedium)
        accents.forEach { a ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { vm.setAccent(a) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = theme?.accentColor == a.name, onClick = { vm.setAccent(a) })
                Spacer(modifier = Modifier.height(8.dp))
                Text(a.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}
