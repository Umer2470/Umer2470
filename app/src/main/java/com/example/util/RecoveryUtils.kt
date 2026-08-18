package com.example.util

import java.security.SecureRandom

object RecoveryUtils {

    private val RANDOM = SecureRandom()

    private val DICTIONARY = listOf(
        "anchor", "beacon", "castle", "dragon", "eagle", "forest", "harbour", "island",
        "jungle", "kingdom", "lantern", "monument", "nature", "ocean", "planet", "quartz",
        "river", "silver", "timber", "universe", "valley", "wisdom", "crystal", "falcon",
        "galaxy", "harvest", "legacy", "meadow", "nexus", "oasis", "phoenix", "quarry",
        "redwood", "summit", "thunder", "upland", "vortex", "wildlife", "zenith", "alpine",
        "blossom", "compass", "diamond", "emerald", "fountain", "glacier", "horizon", "infinity",
        "jubilee", "keystone", "liberty", "mirage", "nebula", "odyssey", "pioneer", "radiance",
        "solstice", "trinity", "unity", "venture", "wander", "yellow", "zephyr", "beacon",
        "cadence", "destiny", "eclipse", "feather", "granite", "harmony", "ironwood"
    )

    private const val CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // Excludes ambiguous 0, O, 1, I

    fun generate12WordPassphrase(): String {
        val selectedWords = mutableListOf<String>()
        val pool = DICTIONARY.toMutableList()
        repeat(12) {
            val idx = RANDOM.nextInt(pool.size)
            selectedWords.add(pool.removeAt(idx))
        }
        return selectedWords.joinToString(" ")
    }

    fun generate20CharEmergencyCode(): String {
        val blocks = mutableListOf<String>()
        repeat(5) {
            val sb = StringBuilder()
            repeat(4) {
                val idx = RANDOM.nextInt(CODE_CHARS.length)
                sb.append(CODE_CHARS[idx])
            }
            blocks.add(sb.toString())
        }
        return blocks.joinToString("-")
    }

    fun generate6DigitOtp(): String {
        val num = RANDOM.nextInt(900000) + 100000
        return num.toString()
    }

    fun normalizePassphrase(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return input.replace("\n", " ")
            .replace(",", " ")
            .split(" ")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
    }

    fun normalizeEmergencyCode(input: String): String {
        return input.uppercase().replace("-", "").replace(" ", "").trim()
    }
}
