package com.lamnian.app

object Config {
    const val LAT = 34.2404
    const val LON = 73.7768
    const val REGION = "Azad Kashmir, Pakistan"
    const val ADMIN_EMAIL = "mohzammajeed@gmail.com"

    /**
     * Your Vercel website address (no slash at the end). Used for the share link and the admin
     * panel link in Settings (admin account only).
     */
    const val API_BASE = "https://lamnian.vercel.app"

    /**
     * false = weather comes straight from Open-Meteo's free API (no key needed, each phone has its own limit).
     * true  = weather goes through your Vercel /api/weather function (only useful with a paid Open-Meteo key).
     */
    const val WEATHER_VIA_API = false

    const val PRAYER_METHOD = 1
    val COLS = listOf("news", "gallery", "events", "notices", "directory")
}
