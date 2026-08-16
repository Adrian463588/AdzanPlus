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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.HighLatitudeRule
import com.adzannotif.core.prayer.Madhab
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanVoice
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.ThemeMode
import com.adzannotif.presentation.common.WindowWidthSizeClass

private data class MethodItem(val method: CalculationMethod, val label: String)
private data class RuleItem(val rule: HighLatitudeRule, val label: String)
private data class PrayerAdjustmentItem(val prayer: Prayer, val name: String, val minutes: Int)
private data class VoiceItem(val voice: AdhanVoice, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 32.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Section
            item {
                Text(
                    text = "LOKASI & METODE HISAB",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lokasi Terpilih",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.userSettings.selectedLocation.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.4f", state.userSettings.selectedLocation.latitude)}°, ${String.format(java.util.Locale.US, "%.4f", state.userSettings.selectedLocation.longitude)}° • ${state.userSettings.selectedLocation.timeZoneId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.onAction(SettingsUiAction.SetLocationPickerVisible(true)) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditLocationAlt,
                                    contentDescription = "Ganti Lokasi",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick GPS button inside card
                        OutlinedButton(
                            onClick = { viewModel.onAction(SettingsUiAction.RefreshGpsLocation) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isRefreshingGps,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isRefreshingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mencari Sinyal GPS Presisi...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gunakan Sinyal GPS Saat Ini")
                            }
                        }
                    }
                }
            }

            // Calculation Method Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Metode Perhitungan Hisab",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val methods = listOf(
                            MethodItem(CalculationMethod.KEMENAG_RI, "Kemenag RI (Standar Indonesia)"),
                            MethodItem(CalculationMethod.MUSLIM_WORLD_LEAGUE, "Muslim World League (MWL)"),
                            MethodItem(CalculationMethod.EGYPTIAN, "Egyptian General Authority"),
                            MethodItem(CalculationMethod.UMM_AL_QURA, "Umm Al-Qura (Makkah)"),
                            MethodItem(CalculationMethod.KARACHI, "University of Islamic Sciences, Karachi"),
                            MethodItem(CalculationMethod.NORTH_AMERICA, "ISNA (Amerika Utara)"),
                            MethodItem(CalculationMethod.SINGAPORE_MUIS, "MUIS (Singapura)")
                        )

                        methods.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onAction(SettingsUiAction.SetCalculationMethod(item.method)) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = state.userSettings.calculationMethod == item.method,
                                    onClick = { viewModel.onAction(SettingsUiAction.SetCalculationMethod(item.method)) },
                                    label = { Text(item.label, style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = if (state.userSettings.calculationMethod == item.method) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Madhab Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Madhab (Penentuan Waktu Ashar)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.userSettings.madhab == Madhab.SHAFI,
                                onClick = { viewModel.onAction(SettingsUiAction.SetMadhab(Madhab.SHAFI)) },
                                label = { Text("Syafi'i / Maliki / Hanbali") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = state.userSettings.madhab == Madhab.HANAFI,
                                onClick = { viewModel.onAction(SettingsUiAction.SetMadhab(Madhab.HANAFI)) },
                                label = { Text("Hanafi") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // High Latitude Rule Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Aturan Lintang Tinggi (High Latitude)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Digunakan saat berada di kutub atau belahan bumi utara/selatan ekstrem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val rules = listOf(
                            RuleItem(HighLatitudeRule.MIDDLE_OF_THE_NIGHT, "Middle of the Night (Tengah Malam)"),
                            RuleItem(HighLatitudeRule.SEVENTH_OF_THE_NIGHT, "1/7th of Night (Sepertujuh Malam)"),
                            RuleItem(HighLatitudeRule.TWILIGHT_ANGLE, "Angle Based (Berdasarkan Sudut)")
                        )

                        rules.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = state.userSettings.highLatitudeRule == item.rule,
                                    onClick = { viewModel.onAction(SettingsUiAction.SetHighLatitudeRule(item.rule)) },
                                    label = { Text(item.label, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Per-Prayer Minute Adjustments
            item {
                Text(
                    text = "KOREKSI WAKTU SHOLAT (MENIT)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Kalibrasi Menit Manual (Ihtiyath)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sesuaikan menit jika jadwal di masjid setempat memiliki selisih waktu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val prayerAdjustments = listOf(
                            PrayerAdjustmentItem(Prayer.FAJR, "Subuh", state.userSettings.fajrAdjustment),
                            PrayerAdjustmentItem(Prayer.DHUHR, "Dzuhur", state.userSettings.dhuhrAdjustment),
                            PrayerAdjustmentItem(Prayer.ASR, "Ashar", state.userSettings.asrAdjustment),
                            PrayerAdjustmentItem(Prayer.MAGHRIB, "Maghrib", state.userSettings.maghribAdjustment),
                            PrayerAdjustmentItem(Prayer.ISHA, "Isya", state.userSettings.ishaAdjustment),
                        )

                        prayerAdjustments.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.onAction(SettingsUiAction.SetPrayerAdjustment(item.prayer, item.minutes - 1)) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = if (item.minutes >= 0) "+${item.minutes} m" else "${item.minutes} m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = if (item.minutes != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { viewModel.onAction(SettingsUiAction.SetPrayerAdjustment(item.prayer, item.minutes + 1)) },
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            if (index < prayerAdjustments.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

            // Audio & Voice Section
            item {
                Text(
                    text = "AUDIO & SUARA ADZAN",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pilihan Muadzin & Pratinjau Suara",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val voices = listOf(
                            VoiceItem(AdhanVoice.MAKKAH, "Adzan Makkah (Masjidil Haram)"),
                            VoiceItem(AdhanVoice.MADINAH, "Adzan Madinah (Masjid Nabawi)"),
                            VoiceItem(AdhanVoice.AL_AQSA, "Adzan Al-Aqsa (Al-Quds)"),
                            VoiceItem(AdhanVoice.EGYPT, "Adzan Mesir (Mishary / Al-Azhar)"),
                            VoiceItem(AdhanVoice.FAJR_SPECIAL, "Adzan Subuh Khusus (As-Salatu Khayrun Minan-Nawm)")
                        )

                        voices.forEach { item ->
                            val isPlaying = state.currentlyPlayingVoice == item.voice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.onAction(SettingsUiAction.ToggleAdhanPreview(item.voice)) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (isPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Hentikan" else "Dengarkan",
                                        tint = if (isPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme Section
            item {
                Text(
                    text = "TAMPILAN & TEMA",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Enhanced Multi-Modal Location Picker BottomSheet
    if (state.isLocationPickerVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(SettingsUiAction.SetLocationPickerVisible(false)) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LocationPickerContent(
                state = state,
                onSearch = { viewModel.onAction(SettingsUiAction.SearchLocation(it)) },
                onSelectLocation = { viewModel.onAction(SettingsUiAction.SelectLocation(it)) },
                onRefreshGps = { viewModel.onAction(SettingsUiAction.RefreshGpsLocation) },
                onSaveCustomCoordinates = { name, lat, lng, elev, tz ->
                    viewModel.onAction(SettingsUiAction.SaveCustomCoordinates(name, lat, lng, elev, tz))
                },
                onDeleteSaved = { viewModel.onAction(SettingsUiAction.DeleteSavedLocation(it)) }
            )
        }
    }
}

@Composable
private fun LocationPickerContent(
    state: SettingsUiState,
    onSearch: (String) -> Unit,
    onSelectLocation: (LocationInfo) -> Unit,
    onRefreshGps: () -> Unit,
    onSaveCustomCoordinates: (String, Double, Double, Double, String) -> Unit,
    onDeleteSaved: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Pilih Lokasi Perhitungan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Pilih dari direktori kota, gunakan GPS real-time, atau masukkan koordinat manual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tabs = {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Cari Kota", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("GPS Sinyal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Koordinat", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Saved Favorites Row
        if (state.favoriteLocations.isNotEmpty()) {
            Text(
                text = "Lokasi Tersimpan / Riwayat:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.favoriteLocations) { loc ->
                    InputChip(
                        selected = state.userSettings.selectedLocation.id == loc.id,
                        onClick = { onSelectLocation(loc) },
                        label = { Text(loc.name) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onDeleteSaved(loc.id) }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (selectedTab) {
            0 -> {
                // Tab 1: Search Directory (Online & Offline)
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari kota di Indonesia atau Dunia...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.searchResults) { city ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLocation(city) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.userSettings.selectedLocation.id == city.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${city.country} • ${String.format(java.util.Locale.US, "%.2f", city.latitude)}°, ${String.format(java.util.Locale.US, "%.2f", city.longitude)}° • ${city.timeZoneId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (state.userSettings.selectedLocation.id == city.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Terpilih",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 2: GPS Auto-Detect
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Deteksi Lokasi GPS Presisi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Menggunakan sinyal Fused Location Provider untuk mendapatkan koordinat dan ketinggian presisi saat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefreshGps,
                            enabled = !state.isRefreshingGps,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isRefreshingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mencari Koordinat...")
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Perbarui & Gunakan GPS Sekarang")
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 3: Custom Coordinates Input
                var customName by remember { mutableStateOf("") }
                var customLat by remember { mutableStateOf("") }
                var customLng by remember { mutableStateOf("") }
                var customElev by remember { mutableStateOf("10") }
                var selectedTz by remember { mutableStateOf("Asia/Jakarta") }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nama Lokasi (misal: Rumah, Kantor)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customLat,
                            onValueChange = { customLat = it },
                            label = { Text("Latitude (-90 s/d 90)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customLng,
                            onValueChange = { customLng = it },
                            label = { Text("Longitude (-180 s/d 180)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customElev,
                            onValueChange = { customElev = it },
                            label = { Text("Elevasi (meter)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = selectedTz,
                            onValueChange = { selectedTz = it },
                            label = { Text("Timezone ID") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val lat = customLat.toDoubleOrNull() ?: 0.0
                            val lng = customLng.toDoubleOrNull() ?: 0.0
                            val elev = customElev.toDoubleOrNull() ?: 0.0
                            onSaveCustomCoordinates(customName, lat, lng, elev, selectedTz)
                        },
                        enabled = customLat.toDoubleOrNull() != null && customLng.toDoubleOrNull() != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan & Terapkan Koordinat")
                    }
                }
            }
        }
    }
}
