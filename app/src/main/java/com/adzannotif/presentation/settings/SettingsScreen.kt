package com.adzannotif.presentation.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adzannotif.R
import com.adzannotif.core.prayer.CalculationMethod
import com.adzannotif.core.prayer.HighLatitudeRule
import com.adzannotif.core.prayer.Madhab
import com.adzannotif.core.prayer.Prayer
import com.adzannotif.domain.model.AdhanVoice
import com.adzannotif.domain.model.AlarmConfig
import com.adzannotif.domain.model.CelestialAlertType
import com.adzannotif.domain.model.LocationInfo
import com.adzannotif.domain.model.ThemeMode
import com.adzannotif.presentation.common.WindowWidthSizeClass
import java.util.TimeZone

private data class MethodItem(val method: CalculationMethod, val label: String)
private data class RuleItem(val rule: HighLatitudeRule, val label: String)
private data class PrayerAdjustmentItem(val prayer: Prayer, val name: String, val minutes: Int)
private data class VoiceItem(val voice: AdhanVoice, val label: String)
private data class CelestialAlertItem(val type: CelestialAlertType, val label: String, val enabled: Boolean)
private data class PrayerAlarmItem(val prayer: Prayer, val label: String, val config: AlarmConfig)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = androidx.compose.ui.platform.LocalContext.current
    var customSoundPrayer by remember { mutableStateOf<Prayer?>(null) }
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val prayer = customSoundPrayer
        if (uri != null && prayer != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(SettingsUiAction.SetCustomSound(prayer, uri.toString()))
        }
        customSoundPrayer = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
        when {
            state.isLoading || state.dataState == SettingsDataState.LOADING -> SettingsLoadingState(
                modifier = Modifier.padding(innerPadding),
            )
            state.dataState == SettingsDataState.ERROR -> SettingsErrorState(
                modifier = Modifier.padding(innerPadding),
                message = state.errorMessage,
            )
            else -> Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(innerPadding)
                        .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.EXPANDED) 24.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
            if (state.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Location Section
            item {
                Text(
                    text = stringResource(R.string.settings_location_method_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                    text = stringResource(R.string.settings_selected_location),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.userSettings.selectedLocation?.name
                                        ?: stringResource(R.string.settings_location_unavailable),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.userSettings.selectedLocation?.let { location ->
                                        "${String.format(java.util.Locale.US, "%.4f", location.latitude)}°, ${String.format(java.util.Locale.US, "%.4f", location.longitude)}° • ${location.timeZoneId}"
                                    } ?: stringResource(R.string.settings_location_setup_hint),
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
                                    contentDescription = stringResource(R.string.settings_change_location),
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
                                Text(stringResource(R.string.settings_gps_searching))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_use_current_gps))
                            }
                        }
                    }
                }
            }

            // Calculation Method Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_calculation_method),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val methods = listOf(
                            MethodItem(CalculationMethod.KEMENAG_RI, stringResource(R.string.settings_method_kemenag)),
                            MethodItem(CalculationMethod.MUSLIM_WORLD_LEAGUE, stringResource(R.string.settings_method_mwl)),
                            MethodItem(CalculationMethod.EGYPTIAN, stringResource(R.string.settings_method_egyptian)),
                            MethodItem(CalculationMethod.UMM_AL_QURA, stringResource(R.string.settings_method_umm_al_qura)),
                            MethodItem(CalculationMethod.KARACHI, stringResource(R.string.settings_method_karachi)),
                            MethodItem(CalculationMethod.NORTH_AMERICA, stringResource(R.string.settings_method_north_america)),
                            MethodItem(CalculationMethod.SINGAPORE_MUIS, stringResource(R.string.settings_method_singapore))
                        )

                        methods.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = state.userSettings.calculationMethod == item.method,
                                    onClick = { viewModel.onAction(SettingsUiAction.SetCalculationMethod(item.method)) },
                                    label = {
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_madhab),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.userSettings.madhab == Madhab.SHAFI,
                                onClick = { viewModel.onAction(SettingsUiAction.SetMadhab(Madhab.SHAFI)) },
                                label = { Text(stringResource(R.string.madhab_shafii_label), maxLines = 2) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            FilterChip(
                                selected = state.userSettings.madhab == Madhab.HANAFI,
                                onClick = { viewModel.onAction(SettingsUiAction.SetMadhab(Madhab.HANAFI)) },
                                label = { Text(stringResource(R.string.madhab_hanafi_label)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // High Latitude Rule Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_high_latitude),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_high_latitude_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val rules = listOf(
                            RuleItem(HighLatitudeRule.MIDDLE_OF_THE_NIGHT, stringResource(R.string.settings_high_latitude_middle)),
                            RuleItem(HighLatitudeRule.SEVENTH_OF_THE_NIGHT, stringResource(R.string.settings_high_latitude_seventh)),
                            RuleItem(HighLatitudeRule.TWILIGHT_ANGLE, stringResource(R.string.settings_high_latitude_angle))
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

            // Prayer alarm controls
            item {
                Text(
                    text = stringResource(R.string.settings_prayer_notifications_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_prayer_notifications_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val alarmItems = Prayer.STANDARD_TIMELINE.map { prayer ->
                            PrayerAlarmItem(
                                prayer = prayer,
                                label = prayer.displayNameId,
                                config = state.alarmSettings.getConfigForPrayer(prayer),
                            )
                        }
                        val voiceOptions = AdhanVoice.entries

                        alarmItems.forEachIndexed { index, alarm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = alarm.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                androidx.compose.material3.Switch(
                                    checked = alarm.config.isEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.onAction(SettingsUiAction.SetPrayerEnabled(alarm.prayer, enabled))
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = alarm.label
                                    },
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(voiceOptions) { voice ->
                                    FilterChip(
                                        selected = alarm.config.adhanVoice == voice,
                                        onClick = {
                                            viewModel.onAction(SettingsUiAction.SetAdhanVoice(alarm.prayer, voice))
                                        },
                                        label = { Text(voice.title) },
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(0, 5, 10, 15)) { minutes ->
                                    FilterChip(
                                        selected = alarm.config.preReminderMinutes == minutes,
                                        onClick = {
                                            viewModel.onAction(SettingsUiAction.SetPreReminder(alarm.prayer, minutes))
                                        },
                                        label = {
                                            Text(
                                                if (minutes == 0) {
                                                    stringResource(R.string.settings_no_reminder)
                                                } else {
                                                    stringResource(R.string.settings_reminder_minutes, minutes)
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = alarm.config.customSoundUri?.let {
                                        stringResource(R.string.settings_custom_audio_saved)
                                    } ?: stringResource(
                                        R.string.settings_builtin_audio,
                                        alarm.config.adhanVoice.title,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            customSoundPrayer = alarm.prayer
                                            audioPicker.launch(arrayOf("audio/*"))
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                                    ) {
                                        Text(stringResource(R.string.settings_choose_audio))
                                    }
                                    if (alarm.config.customSoundUri != null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.onAction(SettingsUiAction.SetCustomSound(alarm.prayer, null))
                                            },
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.settings_remove_custom_audio),
                                            )
                                        }
                                    }
                                }
                            }
                            if (index < alarmItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }
            }

            // Per-Prayer Minute Adjustments
            item {
                Text(
                    text = stringResource(R.string.settings_adjustments_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_adjustments_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_adjustments_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val prayerAdjustments = listOf(
                            PrayerAdjustmentItem(Prayer.FAJR, Prayer.FAJR.displayNameId, state.userSettings.fajrAdjustment),
                            PrayerAdjustmentItem(Prayer.DHUHR, Prayer.DHUHR.displayNameId, state.userSettings.dhuhrAdjustment),
                            PrayerAdjustmentItem(Prayer.ASR, Prayer.ASR.displayNameId, state.userSettings.asrAdjustment),
                            PrayerAdjustmentItem(Prayer.MAGHRIB, Prayer.MAGHRIB.displayNameId, state.userSettings.maghribAdjustment),
                            PrayerAdjustmentItem(Prayer.ISHA, Prayer.ISHA.displayNameId, state.userSettings.ishaAdjustment),
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
                                        modifier = Modifier.size(48.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = stringResource(R.string.settings_decrease_adjustment, item.name),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Text(
                                        text = if (item.minutes >= 0) {
                                            stringResource(R.string.settings_minutes_value_positive, item.minutes)
                                        } else {
                                            stringResource(R.string.settings_minutes_value_negative, item.minutes)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = if (item.minutes != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { viewModel.onAction(SettingsUiAction.SetPrayerAdjustment(item.prayer, item.minutes + 1)) },
                                        modifier = Modifier.size(48.dp),
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = stringResource(R.string.settings_increase_adjustment, item.name),
                                            modifier = Modifier.size(16.dp),
                                        )
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
                    text = stringResource(R.string.settings_audio_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_voice_preview_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val voices = listOf(
                            VoiceItem(AdhanVoice.MAKKAH, stringResource(R.string.settings_voice_makkah)),
                            VoiceItem(AdhanVoice.MADINAH, stringResource(R.string.settings_voice_madinah)),
                            VoiceItem(AdhanVoice.AL_AQSA, stringResource(R.string.settings_voice_al_aqsa)),
                            VoiceItem(AdhanVoice.EGYPT, stringResource(R.string.settings_voice_egypt)),
                            VoiceItem(AdhanVoice.FAJR_SPECIAL, stringResource(R.string.settings_voice_fajr))
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
                                        contentDescription = stringResource(
                                            if (isPlaying) R.string.settings_stop_preview else R.string.settings_play_preview,
                                        ),
                                        tint = if (isPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Celestial notification preferences
            item {
                Text(
                    text = stringResource(R.string.settings_celestial_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_celestial_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_celestial_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val celestialAlerts = listOf(
                            CelestialAlertItem(
                                CelestialAlertType.GOLDEN_HOUR_START,
                                stringResource(R.string.settings_celestial_golden_hour),
                                state.alarmSettings.celestialAlerts.goldenHourStart,
                            ),
                            CelestialAlertItem(
                                CelestialAlertType.BLUE_HOUR_START,
                                stringResource(R.string.settings_celestial_blue_hour),
                                state.alarmSettings.celestialAlerts.blueHourStart,
                            ),
                            CelestialAlertItem(
                                CelestialAlertType.MOONRISE,
                                stringResource(R.string.settings_celestial_moonrise),
                                state.alarmSettings.celestialAlerts.moonrise,
                            ),
                            CelestialAlertItem(
                                CelestialAlertType.MOONSET,
                                stringResource(R.string.settings_celestial_moonset),
                                state.alarmSettings.celestialAlerts.moonset,
                            ),
                            CelestialAlertItem(
                                CelestialAlertType.FULL_MOON,
                                stringResource(R.string.settings_celestial_full_moon),
                                state.alarmSettings.celestialAlerts.fullMoon,
                            ),
                            CelestialAlertItem(
                                CelestialAlertType.NEW_MOON,
                                stringResource(R.string.settings_celestial_new_moon),
                                state.alarmSettings.celestialAlerts.newMoon,
                            ),
                        )
                        celestialAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = alert.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Switch(
                                    checked = alert.enabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.onAction(SettingsUiAction.SetCelestialAlert(alert.type, enabled))
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = alert.label
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_celestial_offset_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(0, 5, 10, 15)) { minutes ->
                                FilterChip(
                                    selected = state.alarmSettings.celestialAlerts.minutesBefore == minutes,
                                    onClick = {
                                        viewModel.onAction(SettingsUiAction.SetCelestialAlertOffset(minutes))
                                    },
                                    label = {
                                        Text(
                                            if (minutes == 0) {
                                                stringResource(R.string.settings_on_time)
                                            } else {
                                                stringResource(R.string.settings_reminder_minutes, minutes)
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Theme Section
            item {
                Text(
                    text = stringResource(R.string.settings_theme_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_theme_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val themeModeChipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.userSettings.themeMode == ThemeMode.SYSTEM,
                                    onClick = { viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.SYSTEM)) },
                                    label = { Text(stringResource(R.string.theme_system)) },
                                    colors = themeModeChipColors,
                                )
                            }
                            item {
                                FilterChip(
                                    selected = state.userSettings.themeMode == ThemeMode.LIGHT,
                                    onClick = { viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.LIGHT)) },
                                    label = { Text(stringResource(R.string.theme_light)) },
                                    colors = themeModeChipColors,
                                )
                            }
                            item {
                                FilterChip(
                                    selected = state.userSettings.themeMode == ThemeMode.DARK,
                                    onClick = { viewModel.onAction(SettingsUiAction.SetThemeMode(ThemeMode.DARK)) },
                                    label = { Text(stringResource(R.string.theme_dark)) },
                                    colors = themeModeChipColors,
                                )
                            }
                        }
                    }
                }
            }

            // Widget Section
            item {
                Text(
                    text = stringResource(R.string.settings_widget_section),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val appWidgetManager = remember { context.getSystemService(android.appwidget.AppWidgetManager::class.java) }
                val isPinSupported = remember {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        appWidgetManager?.isRequestPinAppWidgetSupported == true
                    } else false
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_widget_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_widget_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. Prayer Times Widget
                        Button(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && appWidgetManager != null && isPinSupported) {
                                    val myProvider = android.content.ComponentName(
                                        context,
                                        com.adzannotif.presentation.widget.PrayerTimesWidgetReceiver::class.java,
                                    )
                                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                    android.widget.Toast.makeText(context, context.getString(R.string.widget_pin_requested), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isPinSupported,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.settings_install_prayer_widget))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Astronomy Moon & Sun Widgets
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && appWidgetManager != null && isPinSupported) {
                                        val provider = android.content.ComponentName(context, com.adzannotif.widget.MoonWidgetReceiver::class.java)
                                        appWidgetManager.requestPinAppWidget(provider, null, null)
                                        android.widget.Toast.makeText(context, context.getString(R.string.widget_pin_requested), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isPinSupported,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.settings_install_moon_widget), style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && appWidgetManager != null && isPinSupported) {
                                        val provider = android.content.ComponentName(context, com.adzannotif.widget.SunWidgetReceiver::class.java)
                                        appWidgetManager.requestPinAppWidget(provider, null, null)
                                        android.widget.Toast.makeText(context, context.getString(R.string.widget_pin_requested), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isPinSupported,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.settings_install_sun_widget), style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isPinSupported) {
                                stringResource(R.string.settings_widget_hint)
                            } else {
                                stringResource(R.string.widget_pin_unavailable)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
                }
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
private fun SettingsLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            },
            )
            Text(
                text = stringResource(R.string.settings_loading),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsErrorState(modifier: Modifier = Modifier, message: String?) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = message ?: stringResource(R.string.settings_error_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
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
            text = stringResource(R.string.settings_location_picker_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_location_picker_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            tabs = {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.settings_tab_city), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.settings_tab_gps), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.settings_tab_coordinates), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Saved Favorites Row
        if (state.favoriteLocations.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_saved_locations),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InputChip(
                            modifier = Modifier.weight(1f, fill = false),
                            selected = state.userSettings.selectedLocation?.id == loc.id,
                            onClick = { onSelectLocation(loc) },
                            label = {
                                Text(
                                    text = loc.name,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            },
                        )
                        IconButton(onClick = { onDeleteSaved(loc.id) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.delete_saved_location, loc.name),
                            )
                        }
                    }
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
                    label = { Text(stringResource(R.string.settings_city_search_label)) },
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
                    if (state.searchResults.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.settings_locations_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    items(state.searchResults) { city ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onSelectLocation(city) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.userSettings.selectedLocation?.id == city.id) {
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${city.country} • ${String.format(java.util.Locale.US, "%.2f", city.latitude)}°, ${String.format(java.util.Locale.US, "%.2f", city.longitude)}° • ${city.timeZoneId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                }
                                if (state.userSettings.selectedLocation?.id == city.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.settings_selected),
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        text = stringResource(R.string.settings_device_location_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_device_location_description),
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
                                Text(stringResource(R.string.settings_searching_coordinates))
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_refresh_use_gps))
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
                var customElev by remember { mutableStateOf("") }
                var selectedTz by remember { mutableStateOf(TimeZone.getDefault().id) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(stringResource(R.string.settings_custom_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customLat,
                            onValueChange = { customLat = it },
                            label = { Text(stringResource(R.string.settings_latitude)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customLng,
                            onValueChange = { customLng = it },
                            label = { Text(stringResource(R.string.settings_longitude)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customElev,
                            onValueChange = { customElev = it },
                            label = { Text(stringResource(R.string.settings_elevation)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = selectedTz,
                            onValueChange = { selectedTz = it },
                            label = { Text(stringResource(R.string.settings_timezone_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val lat = customLat.toDoubleOrNull()
                            val lng = customLng.toDoubleOrNull()
                            val elev = customElev.toDoubleOrNull()
                            if (lat != null && lng != null && elev != null) {
                                onSaveCustomCoordinates(customName, lat, lng, elev, selectedTz)
                            }
                        },
                        enabled = customLat.toDoubleOrNull()?.takeIf { it in -90.0..90.0 } != null &&
                            customLng.toDoubleOrNull()?.takeIf { it in -180.0..180.0 } != null &&
                            customElev.toDoubleOrNull() != null &&
                            selectedTz in TimeZone.getAvailableIDs(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.settings_save_coordinates))
                    }
                }
            }
        }
    }
}
