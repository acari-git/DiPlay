package com.shilapi.xcertplay

import java.util.Locale

/**
 * Best-effort hints from DiPlay's *current* session log, not a root-cause diagnosis.
 * Matches only known failure signatures. No network requests, device changes or raw-log UI.
 */
internal object ConnectionLogTriage {
    enum class Symptom { USB_TRANSPORT, BLUETOOTH, WIRELESS_TIMEOUT }

    /** Most recently seen distinct symptoms first, at most two. */
    fun analyze(lines: Sequence<String>): List<Symptom> {
        val latest = mutableMapOf<Symptom, Int>()
        lines.forEachIndexed { index, raw ->
            val line = raw.lowercase(Locale.ROOT)
            val symptom = when {
                "android could not queue usbmux read request" in line ||
                    "android could not queue the ncm read request" in line ||
                    "usbmux write transferred -1" in line ||
                    "invalid usbmux frame length" in line -> Symptom.USB_TRANSPORT

                "bt socket connect failed" in line ||
                    "bluetooth socket manager missing" in line ||
                    "bluetooth_unusable" in line ||
                    ("rfcomm" in line && "timed out" in line) -> Symptom.BLUETOOTH

                "wireless carplay handoff timed out" in line ||
                    "carplay control service resolution timed out" in line ||
                    "first_tcp_timeout" in line -> Symptom.WIRELESS_TIMEOUT

                else -> null
            }
            if (symptom != null) latest[symptom] = index
        }
        return latest.entries.sortedByDescending { it.value }.take(2).map { it.key }
    }
}
