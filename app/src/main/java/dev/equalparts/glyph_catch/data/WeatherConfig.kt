package dev.equalparts.glyph_catch.data

data class WeatherConfig(val useOpenWeather: Boolean, val apiKey: String?, val latitude: Float, val longitude: Float) {
    val hasValidCoordinates: Boolean
        get() = latitude != 0f && longitude != 0f

    val isConfigured: Boolean
        get() = useOpenWeather && !apiKey.isNullOrBlank() && hasValidCoordinates
}
