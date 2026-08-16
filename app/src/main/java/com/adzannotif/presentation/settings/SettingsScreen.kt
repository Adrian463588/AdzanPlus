package com.adzannotif.presentation.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.HighLatitudeRule
import com.adzannotif.core.prayer.Madhab
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanVoice
import com.adzannotif.domain.model.ThemeMode
import com.adzannotif.presentation.common.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Section
            item {
                Text(
                    text = "LOKASI & WILAYAH",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onAction(SettingsUiAction.SetCityPickerVisible(true)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = "Kota",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = state.userSettings.selectedLocation.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Zona: ${state.userSettings.selectedLocation.timeZoneId} • (${state.userSettings.selectedLocation.latitude}, ${state.userSettings.selectedLocation.longitude})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Text(
                            text = "Ganti",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Calculation Method Section
            item {
                Text(
                    text = "METODE PERHITUNGAN HISAB",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CalculationMethod.entries.forEachIndexed { index, method ->
                            val isSelected = state.userSettings.calculationMethod == method
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onAction(SettingsUiAction.SetCalculationMethod(method)) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method.organizationName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Subuh: ${method.fajrAngle}°, Isya: ${if (method.ishaInterval > 0) "${method.ishaInterval}m" else "${method.ishaAngle}°"} • ${method.region}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (index < CalculationMethod.entries.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            // Madhab Selection Section
            item {
                Text(
                    text = "MADHAB PERHITUNGAN ASHAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.userSettings.madhab == Madhab.SHAFI,
                        onClick = { viewModel.onAction(SettingsUiAction.SetMadhab(Madhab.SHAFI)) },
                        label = { Text("Syafi'i / Maliki / Hanbali (1x)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.userSettings.madhab == Madhab.HANAFI,
                        onClick = { viewModel.onAction(SettingsUiAction.SetMadhab(Madhab.HANAFI)) },
                        label = { Text("Hanafi (Bayangan 2x)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Per-Prayer Minute Adjustments (Ihtiyath / Local Mosque Offsets)
            item {
                Text(
                    text = "KOREKSI MENIT SHOLAT (IHTIYATH / MASJID LOKAL)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val adjustments = listOf(
                            Prayer.FAJR to state.userSettings.fajrAdjustment,
                            Prayer.DHUHR to state.userSettings.dhuhrAdjustment,
                            Prayer.ASR to state.userSettings.asrAdjustment,
                            Prayer.MAGHRIB to state.userSettings.maghribAdjustment,
                            Prayer.ISHA to state.userSettings.ishaAdjustment,
                        )

                        adjustments.forEach { (prayer, offset) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prayer.displayNameId,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.onAction(SettingsUiAction.SetPrayerAdjustment(prayer, offset - 1)) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${if (offset >= 0) "+$offset" else "$offset"} m",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(48.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { viewModel.onAction(SettingsUiAction.SetPrayerAdjustment(prayer, offset + 1)) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High Latitude Rule
            item {
                Text(
                    text = "ATURAN LINTANG TINGGI (HIGH LATITUDE)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        HighLatitudeRule.entries.forEach { rule ->
                            val isSelected = state.userSettings.highLatitudeRule == rule
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onAction(SettingsUiAction.SetHighLatitudeRule(rule)) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = rule.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Audio & Voice Selection with Live Audition Player
            item {
                Text(
                    text = "PILIHAN NADA ADZAN & PRATINJAU SUARA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AdhanVoice.entries.forEach { voice ->
                            val isSelected = state.alarmSettings.dhuhr.adhanVoice == voice
                            val isPlaying = state.currentlyPlayingVoice == voice

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onAction(SettingsUiAction.SetAdhanVoice(Prayer.DHUHR, voice))
                                        viewModel.onAction(SettingsUiAction.SetAdhanVoice(Prayer.ASR, voice))
                                        viewModel.onAction(SettingsUiAction.SetAdhanVoice(Prayer.MAGHRIB, voice))
                                        viewModel.onAction(SettingsUiAction.SetAdhanVoice(Prayer.ISHA, voice))
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Column {
                                        Text(
                                            text = voice.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = "Aktif untuk seluruh jadwal adzan",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Play / Stop Preview button
                                    IconButton(
                                        onClick = { viewModel.onAction(SettingsUiAction.ToggleAdhanPreview(voice)) },
                                        modifier = Modifier.size(36.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Hentikan Suara" else "Dengarkan Pratinjau",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Dipilih",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Theme Mode Section
            item {
                Text(
                    text = "TEMA APLIKASI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.userSettings.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.SYSTEM)) },
                        label = { Text("Sistem") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.userSettings.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.LIGHT)) },
                        label = { Text("Terang") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.userSettings.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.DARK)) },
                        label = { Text("Gelap") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // City Search Bottom Sheet
        if (state.isCityPickerVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(SettingsUiAction.SetCityPickerVisible(false)) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Pilih Kota / Wilayah",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onAction(SettingsUiAction.SearchCity(it)) },
                        placeholder = { Text("Cari kota di Indonesia atau dunia...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        items(state.filteredCities) { city ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onAction(SettingsUiAction.SelectCity(city)) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${city.country} • ${city.timeZoneId} • (${city.latitude}, ${city.longitude})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (state.userSettings.selectedLocation.id == city.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}
