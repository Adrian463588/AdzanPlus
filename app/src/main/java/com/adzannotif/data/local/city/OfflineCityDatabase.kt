package com.adzannotif.data.local.city

import com.adzannotif.domain.model.LocationInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineCityDatabase @Inject constructor() {

    val allCities: List<LocationInfo> = listOf(
        // DKI Jakarta & Java
        LocationInfo("id_jakarta", "Jakarta", "Indonesia", -6.2088, 106.8456, 10.0, "Asia/Jakarta"),
        LocationInfo("id_surabaya", "Surabaya", "Indonesia", -7.2575, 112.7521, 5.0, "Asia/Jakarta"),
        LocationInfo("id_bandung", "Bandung", "Indonesia", -6.9175, 107.6191, 768.0, "Asia/Jakarta"),
        LocationInfo("id_semarang", "Semarang", "Indonesia", -6.9667, 110.4167, 4.0, "Asia/Jakarta"),
        LocationInfo("id_yogyakarta", "Yogyakarta", "Indonesia", -7.7956, 110.3695, 113.0, "Asia/Jakarta"),
        LocationInfo("id_serang", "Serang", "Indonesia", -6.1200, 106.1503, 39.0, "Asia/Jakarta"),
        LocationInfo("id_bogor", "Bogor", "Indonesia", -6.5950, 106.8167, 265.0, "Asia/Jakarta"),
        LocationInfo("id_depok", "Depok", "Indonesia", -6.4025, 106.7942, 91.0, "Asia/Jakarta"),
        LocationInfo("id_tangerang", "Tangerang", "Indonesia", -6.1783, 106.6319, 18.0, "Asia/Jakarta"),
        LocationInfo("id_bekasi", "Bekasi", "Indonesia", -6.2383, 106.9756, 19.0, "Asia/Jakarta"),
        LocationInfo("id_malang", "Malang", "Indonesia", -7.9797, 112.6304, 444.0, "Asia/Jakarta"),
        LocationInfo("id_surakarta", "Surakarta (Solo)", "Indonesia", -7.5755, 110.8243, 92.0, "Asia/Jakarta"),

        // Sumatra
        LocationInfo("id_banda_aceh", "Banda Aceh", "Indonesia", 5.5483, 95.3238, 21.0, "Asia/Jakarta"),
        LocationInfo("id_medan", "Medan", "Indonesia", 3.5952, 98.6722, 26.0, "Asia/Jakarta"),
        LocationInfo("id_padang", "Padang", "Indonesia", -0.9471, 100.4172, 8.0, "Asia/Jakarta"),
        LocationInfo("id_pekanbaru", "Pekanbaru", "Indonesia", 0.5071, 101.4478, 12.0, "Asia/Jakarta"),
        LocationInfo("id_jambi", "Jambi", "Indonesia", -1.6101, 103.6131, 16.0, "Asia/Jakarta"),
        LocationInfo("id_palembang", "Palembang", "Indonesia", -2.9909, 104.7565, 8.0, "Asia/Jakarta"),
        LocationInfo("id_bengkulu", "Bengkulu", "Indonesia", -3.8004, 102.2655, 15.0, "Asia/Jakarta"),
        LocationInfo("id_bandar_lampung", "Bandar Lampung", "Indonesia", -5.4500, 105.2667, 16.0, "Asia/Jakarta"),
        LocationInfo("id_pangkal_pinang", "Pangkal Pinang", "Indonesia", -2.1333, 106.1167, 18.0, "Asia/Jakarta"),
        LocationInfo("id_tanjung_pinang", "Tanjung Pinang", "Indonesia", 0.9167, 104.4500, 18.0, "Asia/Jakarta"),
        LocationInfo("id_batam", "Batam", "Indonesia", 1.1301, 104.0529, 34.0, "Asia/Jakarta"),

        // Kalimantan
        LocationInfo("id_pontianak", "Pontianak", "Indonesia", -0.0263, 109.3425, 3.0, "Asia/Pontianak"),
        LocationInfo("id_palangkaraya", "Palangka Raya", "Indonesia", -2.2167, 113.9167, 15.0, "Asia/Pontianak"),
        LocationInfo("id_banjarmasin", "Banjarmasin", "Indonesia", -3.3167, 114.5900, 1.0, "Asia/Makassar"),
        LocationInfo("id_samarinda", "Samarinda", "Indonesia", -0.5022, 117.1536, 8.0, "Asia/Makassar"),
        LocationInfo("id_balikpapan", "Balikpapan", "Indonesia", -1.2379, 116.8289, 10.0, "Asia/Makassar"),
        LocationInfo("id_tanjung_selor", "Tanjung Selor", "Indonesia", 2.8375, 117.3653, 5.0, "Asia/Makassar"),
        LocationInfo("id_ikn", "Nusantara (IKN)", "Indonesia", -0.9739, 116.7083, 50.0, "Asia/Makassar"),

        // Sulawesi
        LocationInfo("id_manado", "Manado", "Indonesia", 1.4748, 124.8428, 10.0, "Asia/Makassar"),
        LocationInfo("id_gorontalo", "Gorontalo", "Indonesia", 0.5435, 123.0568, 10.0, "Asia/Makassar"),
        LocationInfo("id_palu", "Palu", "Indonesia", -0.9003, 119.8779, 25.0, "Asia/Makassar"),
        LocationInfo("id_mamuju", "Mamuju", "Indonesia", -2.6770, 118.8890, 10.0, "Asia/Makassar"),
        LocationInfo("id_makassar", "Makassar", "Indonesia", -5.1477, 119.4327, 5.0, "Asia/Makassar"),
        LocationInfo("id_kendari", "Kendari", "Indonesia", -3.9985, 122.5126, 12.0, "Asia/Makassar"),

        // Bali & Nusa Tenggara
        LocationInfo("id_denpasar", "Denpasar", "Indonesia", -8.6705, 115.2126, 12.0, "Asia/Makassar"),
        LocationInfo("id_mataram", "Mataram (Lombok)", "Indonesia", -8.5833, 116.1167, 26.0, "Asia/Makassar"),
        LocationInfo("id_kupang", "Kupang", "Indonesia", -10.1772, 123.6070, 32.0, "Asia/Makassar"),

        // Maluku & Papua
        LocationInfo("id_ambon", "Ambon", "Indonesia", -3.6554, 128.1906, 15.0, "Asia/Jayapura"),
        LocationInfo("id_sofifi", "Sofifi", "Indonesia", 0.7410, 127.5680, 10.0, "Asia/Jayapura"),
        LocationInfo("id_jayapura", "Jayapura", "Indonesia", -2.5916, 140.6690, 8.0, "Asia/Jayapura"),
        LocationInfo("id_manokwari", "Manokwari", "Indonesia", -0.8615, 134.0620, 20.0, "Asia/Jayapura"),
        LocationInfo("id_sorong", "Sorong", "Indonesia", -0.8762, 131.2558, 7.0, "Asia/Jayapura"),
        LocationInfo("id_merauke", "Merauke", "Indonesia", -8.4991, 140.4019, 5.0, "Asia/Jayapura"),

        // International Holy Cities & Capitals
        LocationInfo("intl_makkah", "Makkah", "Saudi Arabia", 21.4225, 39.8262, 277.0, "Asia/Riyadh"),
        LocationInfo("intl_madinah", "Madinah", "Saudi Arabia", 24.5247, 39.5692, 608.0, "Asia/Riyadh"),
        LocationInfo("intl_jerusalem", "Jerusalem (Al-Quds)", "Palestine", 31.7683, 35.2137, 754.0, "Asia/Jerusalem"),
        LocationInfo("intl_kuala_lumpur", "Kuala Lumpur", "Malaysia", 3.1390, 101.6869, 21.0, "Asia/Kuala_Lumpur"),
        LocationInfo("intl_singapore", "Singapore", "Singapore", 1.3521, 103.8198, 15.0, "Asia/Singapore"),
        LocationInfo("intl_brunei", "Bandar Seri Begawan", "Brunei", 4.9031, 114.9398, 10.0, "Asia/Brunei"),
        LocationInfo("intl_cairo", "Cairo", "Egypt", 30.0444, 31.2357, 23.0, "Africa/Cairo"),
        LocationInfo("intl_istanbul", "Istanbul", "Turkey", 41.0082, 28.9784, 39.0, "Europe/Istanbul"),
        LocationInfo("intl_london", "London", "United Kingdom", 51.5074, -0.1278, 11.0, "Europe/London"),
        LocationInfo("intl_tokyo", "Tokyo", "Japan", 35.6762, 139.6503, 44.0, "Asia/Tokyo"),
    )

    fun searchCities(query: String): List<LocationInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allCities
        return allCities.filter {
            it.name.lowercase().contains(q) || it.country.lowercase().contains(q)
        }
    }
}
