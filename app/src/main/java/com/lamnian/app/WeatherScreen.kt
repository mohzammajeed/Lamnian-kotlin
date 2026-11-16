package com.lamnian.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

fun wxInfo(code: Int, day: Boolean): Triple<String, ImageVector, Color> = when (code) {
    0 -> Triple("Clear", if (day) Icons.Filled.WbSunny else Icons.Filled.NightsStay, if (day) Color(0xFFF5B301) else Color(0xFFF6D86B))
    1 -> Triple("Mostly clear", if (day) Icons.Filled.WbSunny else Icons.Filled.NightsStay, if (day) Color(0xFFF5B301) else Color(0xFFF6D86B))
    2 -> Triple("Partly cloudy", Icons.Filled.WbCloudy, Color(0xFFCFD8DC))
    3 -> Triple("Overcast", Icons.Filled.Cloud, Color(0xFFCFD8DC))
    45, 48 -> Triple("Fog", Icons.Filled.Cloud, Color(0xFFB0BEC5))
    in 51..57 -> Triple("Drizzle", Icons.Filled.WaterDrop, Color(0xFF64B5F6))
    in 61..67, in 80..81 -> Triple("Rain", Icons.Filled.WaterDrop, Color(0xFF64B5F6))
    in 71..77, 85, 86 -> Triple("Snow", Icons.Filled.AcUnit, Color(0xFFBBDEFB))
    82, 95, 96, 99 -> Triple("Thunderstorm", Icons.Filled.FlashOn, Color(0xFFF5B301))
    else -> Triple("—", Icons.Filled.Cloud, Color.Gray)
}

fun skyColors(code: Int, day: Boolean): List<Color> = when {
    code >= 95 -> listOf(Color(0xFF232C3A), Color(0xFF667690))
    code in 71..77 || code in 85..86 -> listOf(Color(0xFF7D95AB), Color(0xFFC1D1DE))
    code >= 51 -> listOf(Color(0xFF34495A), Color(0xFF8AA1AE))
    code == 3 || code in 45..48 -> listOf(Color(0xFF5D7587), Color(0xFF9FB2BF))
    day -> listOf(Color(0xFF3F86D3), Color(0xFF8FC1EE))
    else -> listOf(Color(0xFF081428), Color(0xFF2B4A7A))
}

fun t12(h: Int, m: Int = 0, withMin: Boolean = false): String {
    val hh = if (h % 12 == 0) 12 else h % 12
    val suffix = if (h < 12) "am" else "pm"
    return if (withMin) "%d:%02d %s".format(hh, m, suffix) else "$hh $suffix"
}

private fun clock(iso: String): String {
    return try { t12(iso.substring(11, 13).toInt(), iso.substring(14, 16).toInt(), true) } catch (e: Exception) { iso }
}

@Composable
private fun WCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(vm: AppVM) {
    val w = vm.weather
    PullToRefreshBox(isRefreshing = vm.wxLoading, onRefresh = { vm.refreshWeather() }, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (w == null) {
                item {
                    WCard("Weather") { Text(if (vm.wxLoading) "Loading weather…" else "Weather is unavailable. Pull down to try again.") }
                }
            } else {
                item { HeroCard(w) }
                item { HourlyCard(w) }
                item { TipsCard(w) }
                item { DailyCard(w) }
                item { StatsCard(w) }
                item { SunCard(w) }
                vm.prayer?.let { pr -> item { PrayerCard(pr) } }
                vm.air?.let { a -> item { AirCard(a) } }
                item {
                    Text(
                        "Weather by Open-Meteo · Prayer times by Aladhan\nFor official warnings check PMD / NDMA.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroCard(w: Weather) {
    val (label, icon, tint) = wxInfo(w.code, w.isDay)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(skyColors(w.code, w.isDay))).padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Lamnian · ${Config.REGION}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${w.temp.roundToInt()}°", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(72.dp))
            }
            Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val d = w.days.firstOrNull()
            Text(
                "Feels like ${w.feels.roundToInt()}°" + (if (d != null) " · High ${d.max.roundToInt()}° / Low ${d.min.roundToInt()}°" else ""),
                color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "${w.humidity}%" to "Humidity", "${w.wind.roundToInt()} km/h" to "Wind",
                    "${d?.rain ?: 0}%" to "Rain"
                ).forEach { (v, l) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(v, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(l, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyCard(w: Weather) {
    WCard("Next 24 hours") {
        if (w.hours.size > 1) {
            val temps = w.hours.map { it.temp }
            val mx = temps.max()
            val mn = temps.min()
            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val step = size.width / (temps.size - 1)
                val range = (mx - mn).coerceAtLeast(1.0)
                val path = Path()
                temps.forEachIndexed { i, t ->
                    val x = i * step
                    val y = (size.height - 8f) - ((t - mn) / range).toFloat() * (size.height - 16f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Color(0xFFE0782E), style = Stroke(width = 6f, cap = StrokeCap.Round))
                drawLine(Color(0x22888888), Offset(0f, size.height), Offset(size.width, size.height), 2f)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(w.hours) { h ->
                val hr = h.time.substring(11, 13).toIntOrNull() ?: 0
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t12(hr), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${h.temp.roundToInt()}°", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WaterDrop, null, tint = Color(0xFF3D8FD1), modifier = Modifier.size(12.dp))
                        Text("${h.rain}%", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TipsCard(w: Weather) {
    val tips = ArrayList<Pair<ImageVector, String>>()
    val d = w.days.firstOrNull()
    val rain = w.hours.take(12).maxOfOrNull { it.rain } ?: 0
    if (rain >= 60) tips.add(Icons.Filled.WaterDrop to "Rain is likely today — carry an umbrella.")
    if (d != null) {
        if (d.min <= 3) tips.add(Icons.Filled.AcUnit to "Very cold — dress in warm layers.")
        else if (d.min <= 10) tips.add(Icons.Filled.Thermostat to "Cool mornings and evenings — bring a jacket.")
        if (d.max >= 35) tips.add(Icons.Filled.WbSunny to "Hot afternoon — drink water and avoid midday sun.")
        if (d.code >= 95) tips.add(Icons.Filled.FlashOn to "Thunderstorms possible — avoid open ground and hill roads.")
        if (rain >= 70 && d.code >= 61) tips.add(Icons.Filled.Warning to "Heavy rain can cause landslides — check road conditions before travel.")
    }
    if (w.wind >= 30) tips.add(Icons.Filled.Air to "Strong winds — secure loose items.")
    if (w.uv >= 7) tips.add(Icons.Filled.WbSunny to "High UV — use sun protection.")
    if (tips.isEmpty()) tips.add(Icons.Filled.Check to "Pleasant weather — a good day to be out.")
    WCard("Smart tips") {
        tips.forEach { (ic, text) ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(ic, null, tint = Blue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DailyCard(w: Weather) {
    val lo = w.days.minOf { it.min }
    val hi = w.days.maxOf { it.max }
    val span = (hi - lo).coerceAtLeast(1.0)
    WCard("7-day forecast") {
        w.days.forEachIndexed { i, d ->
            val name = if (i == 0) "Today" else try {
                LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            } catch (e: Exception) { d.date }
            val (_, icon, tint) = wxInfo(d.code, true)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.width(52.dp), fontWeight = FontWeight.SemiBold)
                Icon(icon, null, tint = if (tint == Color(0xFFCFD8DC)) Color(0xFF90A4AE) else tint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text("${d.rain}%", Modifier.width(40.dp), fontSize = 12.sp, color = Color(0xFF3D8FD1))
                Text("${d.min.roundToInt()}°", Modifier.width(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                val left = ((d.min - lo) / span).toFloat().coerceAtLeast(0.001f)
                val mid = ((d.max - d.min) / span).toFloat().coerceAtLeast(0.02f)
                val right = (1f - left - mid).coerceAtLeast(0.001f)
                Row(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Spacer(Modifier.weight(left).fillMaxHeight())
                    Box(Modifier.weight(mid).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF3D8FD1), Color(0xFFE0782E)))))
                    Spacer(Modifier.weight(right).fillMaxHeight())
                }
                Text("${d.max.roundToInt()}°", Modifier.width(36.dp), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
private fun StatsCard(w: Weather) {
    val items = listOf(
        Triple(Icons.Filled.Thermostat, "Feels like", "${w.feels.roundToInt()}°C"),
        Triple(Icons.Filled.WaterDrop, "Humidity", "${w.humidity}%"),
        Triple(Icons.Filled.Air, "Wind", "${w.wind.roundToInt()} km/h"),
        Triple(Icons.Filled.Speed, "Pressure", "${w.pressure.roundToInt()} hPa"),
        Triple(Icons.Filled.WbSunny, "UV index (max)", "${w.uv.roundToInt()}")
    )
    WCard("Right now") {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (ic, label, value) ->
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(ic, null, tint = Blue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SunCard(w: Weather) {
    WCard("Sun") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Sunrise" to w.sunrise, "Sunset" to w.sunset).forEach { (l, t) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.WbTwilight, null, tint = Color(0xFFF5A300), modifier = Modifier.size(28.dp))
                    Text(l, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(clock(t), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PrayerCard(p: Prayer) {
    val now = java.util.Calendar.getInstance()
    val nowMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
    val next = p.times.indexOfFirst {
        val parts = it.second.split(":")
        (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0) > nowMin
    }
    WCard("Prayer times · ${p.hijri}") {
        p.times.forEachIndexed { i, (name, time) ->
            val parts = time.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val isNext = i == next
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (isNext) Blue else Color.Transparent).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, fontWeight = FontWeight.SemiBold, color = if (isNext) Color.White else MaterialTheme.colorScheme.onSurface)
                Text(t12(h, m, true), fontWeight = FontWeight.SemiBold, color = if (isNext) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AirCard(a: Air) {
    val (label, color) = when {
        a.aqi <= 50 -> "Good" to Color(0xFF2E9D5B)
        a.aqi <= 100 -> "Moderate" to Color(0xFFC8961A)
        a.aqi <= 150 -> "Unhealthy for sensitive" to Color(0xFFE0782E)
        a.aqi <= 200 -> "Unhealthy" to Color(0xFFC0392B)
        else -> "Very unhealthy" to Color(0xFF7D2A8A)
    }
    WCard("Air quality") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${a.aqi}", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(
                label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Text(
            "PM2.5: ${a.pm25.roundToInt()} µg/m³ · PM10: ${a.pm10.roundToInt()} µg/m³",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
        )
    }
}
