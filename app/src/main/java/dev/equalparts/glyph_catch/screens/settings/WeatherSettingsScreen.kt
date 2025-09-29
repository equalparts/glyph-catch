package dev.equalparts.glyph_catch.screens.settings

import android.Manifest
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import dev.equalparts.glyph_catch.AppCard
import dev.equalparts.glyph_catch.AppSizes
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.WeatherConnectionStatus
import dev.equalparts.glyph_catch.gameplay.WeatherProviderFactory
import dev.equalparts.glyph_catch.util.LocationHelper
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private data class WeatherUiState(
    val useOpenWeather: Boolean,
    val connectionStatus: WeatherConnectionStatus,
    val lastUpdateMillis: Long,
    val hasSavedApiKey: Boolean,
    val hasSavedLocation: Boolean,
    val hasPendingChanges: Boolean,
    val form: WeatherForm,
    val showApiKey: Boolean,
    val isLoadingLocation: Boolean,
    val locationError: String?,
    val showConfiguration: Boolean,
    val saveEnabled: Boolean
)

private data class WeatherActions(
    val onUseOpenWeatherChange: (Boolean) -> Unit,
    val onFormChange: (WeatherForm) -> Unit,
    val onToggleShowApiKey: () -> Unit,
    val onSearchCity: () -> Unit,
    val onUseCurrentLocation: () -> Unit,
    val onSave: () -> Unit,
    val onScrollToHelp: () -> Unit
)

private data class WeatherForm(
    val apiKey: String,
    val locationName: String,
    val latitude: Float,
    val longitude: Float
) {
    fun trimmed(): WeatherForm = copy(apiKey = apiKey.trim())
    fun hasCoordinates(): Boolean = latitude != 0f && longitude != 0f
}

private fun PreferencesManager.toWeatherForm() = WeatherForm(
    apiKey = openWeatherMapApiKey.orEmpty(),
    locationName = weatherLocationName.orEmpty(),
    latitude = weatherLatitude,
    longitude = weatherLongitude
)

@Composable
fun WeatherSettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var useOpenWeather by remember { mutableStateOf(prefs.useOpenWeatherMap) }
    var savedForm by remember { mutableStateOf(prefs.toWeatherForm()) }
    var form by remember { mutableStateOf(savedForm) }
    var showApiKey by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var connectionStatus by remember { mutableStateOf(prefs.weatherConnectionStatus) }
    var lastUpdateMillis by remember { mutableLongStateOf(prefs.weatherLastUpdateEpochMillis) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            connectionStatus = prefs.weatherConnectionStatus
            lastUpdateMillis = prefs.weatherLastUpdateEpochMillis
        }
        prefs.registerListener(listener)
        onDispose { prefs.unregisterListener(listener) }
    }

    fun resetStatus(syncNow: Boolean) {
        if (!useOpenWeather) {
            prefs.weatherConnectionStatus = WeatherConnectionStatus.DISABLED
            prefs.weatherLastUpdateEpochMillis = 0L
            connectionStatus = WeatherConnectionStatus.DISABLED
            lastUpdateMillis = 0L
            return
        }

        prefs.weatherConnectionStatus = WeatherConnectionStatus.NEVER_CONNECTED
        prefs.weatherLastUpdateEpochMillis = 0L
        connectionStatus = WeatherConnectionStatus.NEVER_CONNECTED
        lastUpdateMillis = 0L

        if (syncNow && prefs.hasValidWeatherConfig()) {
            WeatherProviderFactory.create(context).getCurrentWeather()
        }
    }

    fun saveChanges() {
        val normalized = form.trimmed()
        form = normalized
        savedForm = normalized

        prefs.openWeatherMapApiKey = normalized.apiKey.takeIf { it.isNotEmpty() }
        prefs.weatherLocationName = normalized.locationName.takeIf { it.isNotEmpty() }
        prefs.weatherLatitude = normalized.latitude
        prefs.weatherLongitude = normalized.longitude

        resetStatus(syncNow = useOpenWeather)
        locationError = null
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                isLoadingLocation = true
                locationError = null
                val location = LocationHelper.getCurrentLocation(context)
                if (location != null) {
                    val resolvedName =
                        location.cityName ?: context.getString(R.string.settings_weather_current_location)
                    form = form.copy(
                        locationName = resolvedName,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    locationError = context.getString(R.string.settings_weather_error_current_location)
                }
                isLoadingLocation = false
            }
        } else {
            locationError = context.getString(R.string.settings_weather_error_permission_denied)
        }
    }

    fun searchForCity() {
        val query = form.locationName
        if (query.isBlank()) return

        coroutineScope.launch {
            isLoadingLocation = true
            locationError = null
            val result = LocationHelper.geocodeCity(context, query)
            if (result != null) {
                val resolvedName = result.cityName ?: query
                form = form.copy(
                    locationName = resolvedName,
                    latitude = result.latitude,
                    longitude = result.longitude
                )
            } else {
                locationError = context.getString(R.string.settings_weather_error_city_not_found)
            }
            isLoadingLocation = false
        }
    }

    val scrollState = rememberScrollState()
    val isDirty = form != savedForm

    val uiState = WeatherUiState(
        useOpenWeather = useOpenWeather,
        connectionStatus = connectionStatus,
        lastUpdateMillis = lastUpdateMillis,
        hasSavedApiKey = savedForm.apiKey.isNotBlank(),
        hasSavedLocation = savedForm.hasCoordinates(),
        hasPendingChanges = isDirty,
        form = form,
        showApiKey = showApiKey,
        isLoadingLocation = isLoadingLocation,
        locationError = locationError,
        showConfiguration = useOpenWeather,
        saveEnabled = isDirty
    )

    val actions = WeatherActions(
        onUseOpenWeatherChange = { enabled ->
            useOpenWeather = enabled
            prefs.useOpenWeatherMap = enabled
            resetStatus(syncNow = enabled)
        },
        onFormChange = { form = it },
        onToggleShowApiKey = { showApiKey = !showApiKey },
        onSearchCity = { searchForCity() },
        onUseCurrentLocation = {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onSave = { saveChanges() },
        onScrollToHelp = {
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    )

    Column(modifier = modifier.verticalScroll(scrollState)) {
        WeatherSettingsContent(state = uiState, actions = actions)
    }
}

@Composable
private fun WeatherSettingsContent(state: WeatherUiState, actions: WeatherActions) {
    WeatherProviderToggleCard(
        useOpenWeather = state.useOpenWeather,
        onUseOpenWeatherChange = actions.onUseOpenWeatherChange
    )

    Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

    if (state.showConfiguration) {
        WeatherStatusCard(
            connectionStatus = state.connectionStatus,
            lastUpdateMillis = state.lastUpdateMillis,
            useOpenWeather = state.useOpenWeather,
            hasSavedApiKey = state.hasSavedApiKey,
            hasSavedLocation = state.hasSavedLocation,
            hasPendingChanges = state.hasPendingChanges
        )

        Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
        OpenWeatherConfigurationSection(state = state, actions = actions)
    }
}

@Composable
private fun OpenWeatherConfigurationSection(state: WeatherUiState, actions: WeatherActions) {
    ConfigureOpenWeatherCard(
        apiKey = state.form.apiKey,
        onApiKeyChange = { value -> actions.onFormChange(state.form.copy(apiKey = value)) },
        showApiKey = state.showApiKey,
        onToggleShowApiKey = actions.onToggleShowApiKey,
        locationName = state.form.locationName,
        onLocationNameChange = { value -> actions.onFormChange(state.form.copy(locationName = value)) },
        isLoadingLocation = state.isLoadingLocation,
        locationError = state.locationError,
        onSearchCity = actions.onSearchCity,
        onUseCurrentLocation = actions.onUseCurrentLocation,
        onSave = actions.onSave,
        saveEnabled = state.saveEnabled,
        onScrollToHelp = actions.onScrollToHelp
    )

    Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

    OpenWeatherInfoCard()
}

@Composable
private fun WeatherProviderToggleCard(useOpenWeather: Boolean, onUseOpenWeatherChange: (Boolean) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSizes.spacingLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_weather_use_live_weather_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (useOpenWeather) {
                        stringResource(R.string.settings_weather_provider_openweathermap)
                    } else {
                        stringResource(R.string.settings_weather_provider_random)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = useOpenWeather, onCheckedChange = onUseOpenWeatherChange)
        }
    }
}

@Composable
private fun ConfigureOpenWeatherCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit,
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    isLoadingLocation: Boolean,
    locationError: String?,
    onSearchCity: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
    onScrollToHelp: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingXXLarge)) {
            Text(
                text = stringResource(R.string.settings_weather_section_configure),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
            Text(
                text = stringResource(R.string.settings_weather_setup_info),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = onScrollToHelp,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AppSizes.none, vertical = AppSizes.spacingTiny)
            ) {
                Text(
                    text = stringResource(R.string.settings_weather_help_button),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            Spacer(modifier = Modifier.height(AppSizes.spacingXXLarge))

            ApiKeyField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                showApiKey = showApiKey,
                onToggleShowApiKey = onToggleShowApiKey
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

            LocationField(
                locationName = locationName,
                onLocationNameChange = onLocationNameChange,
                isLoadingLocation = isLoadingLocation,
                locationError = locationError,
                onSearchCity = onSearchCity
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingSmall))

            CurrentLocationButton(
                enabled = !isLoadingLocation,
                onClick = onUseCurrentLocation
            )

            Spacer(modifier = Modifier.height(AppSizes.spacingLarge))

            Button(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_weather_save_and_sync))
            }
        }
    }
}

@Composable
private fun OpenWeatherInfoCard() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppSizes.spacingXXLarge)) {
            Text(
                text = stringResource(R.string.settings_weather_help_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(AppSizes.spacingMedium))
            Text(
                text = stringResource(R.string.settings_weather_help_information),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
            OpenWeatherLink(url = stringResource(R.string.settings_weather_help_url))
            Spacer(modifier = Modifier.height(AppSizes.spacingLarge))
            Text(
                text = stringResource(R.string.settings_weather_help_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OpenWeatherLink(url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = url,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { uriHandler.openUri(url) }
    )
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowApiKey: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.settings_weather_api_key_label)) },
        placeholder = { Text(stringResource(R.string.settings_weather_api_key_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (showApiKey) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onToggleShowApiKey) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = if (showApiKey) {
                        stringResource(R.string.settings_weather_api_key_hide)
                    } else {
                        stringResource(R.string.settings_weather_api_key_show)
                    }
                )
            }
        }
    )
}

@Composable
private fun LocationField(
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    isLoadingLocation: Boolean,
    locationError: String?,
    onSearchCity: () -> Unit
) {
    OutlinedTextField(
        value = locationName,
        onValueChange = onLocationNameChange,
        label = { Text(stringResource(R.string.settings_weather_location_label)) },
        placeholder = { Text(stringResource(R.string.settings_weather_location_placeholder)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = {
            locationError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        },
        trailingIcon = {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppSizes.iconSizeMedium),
                    strokeWidth = AppSizes.strokeWidthThin
                )
            } else {
                IconButton(onClick = onSearchCity, enabled = locationName.isNotBlank()) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.settings_weather_location_search)
                    )
                }
            }
        }
    )
}

@Composable
private fun CurrentLocationButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(AppSizes.iconSizeSmall))
        Spacer(modifier = Modifier.width(AppSizes.spacingSmall))
        Text(stringResource(R.string.settings_weather_use_current_location))
    }
}

@Composable
private fun WeatherStatusCard(
    connectionStatus: WeatherConnectionStatus,
    lastUpdateMillis: Long,
    useOpenWeather: Boolean,
    hasSavedApiKey: Boolean,
    hasSavedLocation: Boolean,
    hasPendingChanges: Boolean
) {
    val headline = when (connectionStatus) {
        WeatherConnectionStatus.CONNECTED -> stringResource(R.string.settings_weather_status_connected)
        WeatherConnectionStatus.FAILED -> stringResource(R.string.settings_weather_status_failed)
        else -> stringResource(R.string.settings_weather_status_waiting)
    }

    val detail = when {
        !useOpenWeather -> stringResource(R.string.settings_weather_provider_random)
        hasPendingChanges -> stringResource(R.string.settings_weather_status_save_changes)
        !hasSavedApiKey -> stringResource(R.string.settings_weather_status_add_api_key)
        !hasSavedLocation -> stringResource(R.string.settings_weather_status_select_location)
        connectionStatus == WeatherConnectionStatus.CONNECTED && lastUpdateMillis > 0L -> stringResource(
            R.string.settings_weather_status_last_update,
            formatLastUpdated(lastUpdateMillis, stringResource(R.string.settings_weather_status_last_update_never))
        )
        connectionStatus == WeatherConnectionStatus.FAILED -> stringResource(
            R.string.settings_weather_status_check_connection
        )
        connectionStatus == WeatherConnectionStatus.NEVER_CONNECTED -> stringResource(
            R.string.settings_weather_status_waiting_to_sync
        )
        else -> null
    }

    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppSizes.spacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(AppSizes.iconSizeTiny)
            )
            Spacer(modifier = Modifier.width(AppSizes.spacingSmall))
            Column {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                detail?.let {
                    Spacer(modifier = Modifier.height(AppSizes.spacingMicro))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatLastUpdated(epochMillis: Long, neverLabel: String): String {
    if (epochMillis <= 0L) return neverLabel
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    return formatter.format(Date(epochMillis))
}
