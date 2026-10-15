package com.example.utils

import java.util.Locale

/**
 * Utilitaires de formatage de données pour Nexus Tunnel VPN
 */
object FormatUtils {

    /**
     * Formatage de l'identifiant UID avec points de suspension au milieu
     * Exemple : "nx_uid_9774d56d682e549c" -> "nx_uid...549c"
     */
    fun formatMaskedUid(uid: String?): String {
        if (uid.isNullOrBlank()) return "---"
        val clean = uid.trim()
        return when {
            clean.length >= 16 -> {
                "${clean.take(7)}...${clean.takeLast(4)}"
            }
            clean.length >= 10 -> {
                "${clean.take(4)}...${clean.takeLast(3)}"
            }
            clean.length > 6 -> {
                "${clean.take(3)}...${clean.takeLast(2)}"
            }
            else -> "$clean..."
        }
    }

    /**
     * Formatage lisible des débits et des données (Octets, Ko, Mo, Go)
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f Go", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f Mo", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.0f Ko", bytes / 1024.0)
            else -> "$bytes O"
        }
    }

    /**
     * Formatage de vitesse en octets/sec
     */
    fun formatSpeed(bps: Long): String {
        return when {
            bps >= 1024 * 1024 -> String.format(Locale.US, "%.1f Mo/s", bps / (1024.0 * 1024.0))
            bps >= 1024 -> String.format(Locale.US, "%.0f Ko/s", bps / 1024.0)
            else -> "$bps O/s"
        }
    }

    /**
     * Formatage du temps de session en HH:MM:SS ou MM:SS
     */
    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
