package com.example.data

import com.example.model.WeatherState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class WeatherRepository {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    private val weatherApi: OpenMeteoWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoWeatherApi::class.java)
    }

    suspend fun fetchWeatherForCity(cityName: String): WeatherState {
        val cleanCity = cityName.trim().ifBlank { "New Delhi" }
        return try {
            val geo = geocodingApi.searchCity(cleanCity)
            val place = geo.results?.firstOrNull()
                ?: return WeatherState(
                    city = cleanCity,
                    conditionLabel = "City not found",
                    errorMessage = "Could not locate \"$cleanCity\""
                )
            val forecast = weatherApi.getCurrentWeather(place.latitude, place.longitude)
            val current = forecast.currentWeather
                ?: return WeatherState(
                    city = place.name,
                    conditionLabel = "Unavailable",
                    errorMessage = "Weather data unavailable"
                )
            WeatherState(
                city = place.name,
                temperatureC = current.temperature,
                windKmh = current.windspeed,
                conditionLabel = mapWeatherCode(current.weathercode),
                isLoading = false,
                errorMessage = null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WeatherState(
                city = cleanCity,
                conditionLabel = "Offline",
                isLoading = false,
                errorMessage = e.localizedMessage ?: "Unable to fetch live weather"
            )
        }
    }

    suspend fun fetchWeatherByCoordinates(lat: Double, lon: Double, label: String): WeatherState {
        return try {
            val forecast = weatherApi.getCurrentWeather(lat, lon)
            val current = forecast.currentWeather
                ?: return WeatherState(city = label, conditionLabel = "Unavailable")
            WeatherState(
                city = label,
                temperatureC = current.temperature,
                windKmh = current.windspeed,
                conditionLabel = mapWeatherCode(current.weathercode),
                isLoading = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WeatherState(
                city = label,
                conditionLabel = "Offline",
                errorMessage = e.localizedMessage ?: "Weather request failed"
            )
        }
    }

    private fun mapWeatherCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Variable Sky"
    }
}
