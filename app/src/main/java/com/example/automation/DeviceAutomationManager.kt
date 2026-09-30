package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import java.util.Calendar

data class PendingAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ActionType,
    val title: String,
    val description: String,
    val target: String,
    val extraData: Map<String, String> = emptyMap()
)

enum class ActionType {
    CALL, SMS, EMAIL, ALARM, TIMER, CALENDAR, OPEN_APP, SYSTEM_SETTING, ACCESSIBILITY
}

class DeviceAutomationManager(private val context: Context) {

    fun findAppByName(appName: String): String? {
        val pm = context.packageManager
        val query = appName.trim().lowercase()

        // Known common mappings
        val common = mapOf(
            "camera" to listOf("camera"),
            "maps" to listOf("maps", "google maps"),
            "youtube" to listOf("youtube"),
            "chrome" to listOf("chrome", "browser"),
            "settings" to listOf("settings"),
            "clock" to listOf("clock", "alarm"),
            "calendar" to listOf("calendar"),
            "calculator" to listOf("calculator"),
            "gallery" to listOf("gallery", "photos"),
            "messages" to listOf("messaging", "messages", "sms"),
            "phone" to listOf("dialer", "phone", "contacts"),
            "whatsapp" to listOf("whatsapp"),
            "spotify" to listOf("spotify")
        )

        for ((key, aliases) in common) {
            if (aliases.any { query.contains(it) }) {
                val launchIntent = pm.getLaunchIntentForPackage("com.google.android.apps.$key")
                    ?: pm.getLaunchIntentForPackage("com.google.android.$key")
                    ?: pm.getLaunchIntentForPackage("com.android.$key")
                if (launchIntent != null) {
                    return launchIntent.component?.packageName
                }
            }
        }

        val launchable = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        )
        for (resolveInfo in launchable) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }

    fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun executeCall(phoneNumber: String, directCall: Boolean = false): Boolean {
        return try {
            val action = if (directCall && hasCallPhonePermission()) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action).apply {
                data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun executeSms(phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun executeEmail(recipient: String, subject: String, body: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(recipient)}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setTimer(seconds: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createCalendarEvent(title: String, startMillis: Long, endMillis: Long, description: String = ""): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSystemSettings(type: String): Boolean {
        return try {
            val action = when (type.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "notification", "notifications" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun searchContacts(query: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return results
        }
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                null
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: ""
                    val number = it.getString(numberIdx) ?: ""
                    if (name.isNotEmpty() && number.isNotEmpty()) {
                        results.add(Pair(name, number))
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun hasCallPhonePermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }
}
