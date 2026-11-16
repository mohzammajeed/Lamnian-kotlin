package com.lamnian.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Hour(val time: String, val temp: Double, val rain: Int)
data class Day(val date: String, val code: Int, val max: Double, val min: Double, val rain: Int)
data class Weather(
    val temp: Double, val feels: Double, val humidity: Int, val code: Int, val wind: Double,
    val pressure: Double, val isDay: Boolean, val hours: List<Hour>, val days: List<Day>,
    val sunrise: String, val sunset: String, val uv: Double
)
data class Air(val aqi: Int, val pm25: Double, val pm10: Double)
data class Prayer(val times: List<Pair<String, String>>, val hijri: String)

object WeatherApi {
    private const val FORECAST =
        "current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,pressure_msl,is_day" +
            "&hourly=temperature_2m,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max" +
            "&timezone=auto&forecast_days=7"
    private const val AIR = "current=us_aqi,pm2_5,pm10"

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 10000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun viaSite(type: String): String? {
        if (!Config.WEATHER_VIA_API || Config.API_BASE.isBlank()) return null
        return try {
            get("${Config.API_BASE}/api/weather?type=$type&lat=${Config.LAT}&lon=${Config.LON}")
        } catch (e: Exception) {
            null
        }
    }

    fun forecastRaw(): String = viaSite("forecast")
        ?: get("https://api.open-meteo.com/v1/forecast?latitude=${Config.LAT}&longitude=${Config.LON}&$FORECAST")

    fun airRaw(): String = viaSite("air")
        ?: get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${Config.LAT}&longitude=${Config.LON}&$AIR")

    fun prayerRaw(): String =
        get("https://api.aladhan.com/v1/timings?latitude=${Config.LAT}&longitude=${Config.LON}&method=${Config.PRAYER_METHOD}")

    fun parseWeather(raw: String): Weather {
        val j = JSONObject(raw)
        val c = j.getJSONObject("current")
        val h = j.getJSONObject("hourly")
        val d = j.getJSONObject("daily")
        val ht = h.getJSONArray("time")
        val hv = h.getJSONArray("temperature_2m")
        val hp = h.getJSONArray("precipitation_probability")
        val now = c.getString("time").take(13)
        var start = 0
        for (i in 0 until ht.length()) {
            if (ht.getString(i).take(13) >= now) { start = i; break }
        }
        val hours = (start until minOf(start + 24, ht.length())).map {
            Hour(ht.getString(it), hv.getDouble(it), hp.optInt(it, 0))
        }
        val dt = d.getJSONArray("time")
        val dc = d.getJSONArray("weather_code")
        val dmax = d.getJSONArray("temperature_2m_max")
        val dmin = d.getJSONArray("temperature_2m_min")
        val dr = d.getJSONArray("precipitation_probability_max")
        val days = (0 until dt.length()).map {
            Day(dt.getString(it), dc.optInt(it, 0), dmax.getDouble(it), dmin.getDouble(it), dr.optInt(it, 0))
        }
        return Weather(
            temp = c.getDouble("temperature_2m"), feels = c.getDouble("apparent_temperature"),
            humidity = c.optInt("relative_humidity_2m", 0), code = c.optInt("weather_code", 0),
            wind = c.optDouble("wind_speed_10m", 0.0), pressure = c.optDouble("pressure_msl", 0.0),
            isDay = c.optInt("is_day", 1) == 1, hours = hours, days = days,
            sunrise = d.getJSONArray("sunrise").getString(0), sunset = d.getJSONArray("sunset").getString(0),
            uv = d.getJSONArray("uv_index_max").optDouble(0, 0.0)
        )
    }

    fun parseAir(raw: String): Air {
        val c = JSONObject(raw).getJSONObject("current")
        return Air(c.optInt("us_aqi", 0), c.optDouble("pm2_5", 0.0), c.optDouble("pm10", 0.0))
    }

    fun parsePrayer(raw: String): Prayer {
        val d = JSONObject(raw).getJSONObject("data")
        val t = d.getJSONObject("timings")
        val hj = d.getJSONObject("date").getJSONObject("hijri")
        val names = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        return Prayer(
            names.map { it to t.getString(it).take(5) },
            "${hj.getString("day")} ${hj.getJSONObject("month").getString("en")} ${hj.getString("year")}"
        )
    }
}
