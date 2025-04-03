package com.example.beaconkotlinapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.beaconkotlinapp.R
import com.example.beaconkotlinapp.ui.DevicePannelActivity

object NotificationHelper {

    private const val CHANNEL_ID = "beacon_alerts"

    // Crear el canal de notificaciones (solo es necesario en Android 8.0+)
    fun createNotificationChannel(context: Context) {
        val name = "Beacon Notifications"
        val descriptionText = "Canal para notificaciones de eventos del beacon"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, title: String, message: String, deviceMac: String, location: Location? = null) {
        val pendingIntent: PendingIntent

        if (location != null) {
            // 📍 Si hay ubicación, abrir Google Maps con la posición
            val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                setPackage("com.google.android.apps.maps") // Intent explícito para Maps
            }

            pendingIntent = PendingIntent.getActivity(
                context,
                1001, // ID único para la ubicación
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("BeaconApp", "🗺️ Notificación con ubicación → Abriendo Google Maps")
        } else {
            // 📲 Si NO hay ubicación, abrir DevicePannelActivity
            val appIntent = Intent(context, DevicePannelActivity::class.java).apply {
                putExtra("DEVICE_MAC_ADDRESS", deviceMac) // Pasa la dirección MAC
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            pendingIntent = PendingIntent.getActivity(
                context,
                1002, // ID único para abrir la app
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("BeaconApp", "📲 Notificación sin ubicación → Abriendo la app")
        }

        // Generar mensaje de notificación
        val notificationMessage = location?.let {
            "📍 Ubicación en tiempo real: Lat ${it.latitude}, Lng ${it.longitude}\n" +
                    "➡ Ver en Google Maps: https://www.google.com/maps/search/?api=1&query=${it.latitude},${it.longitude}"
        } ?: message

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Asociamos el `PendingIntent` correcto

        // Enviar la notificación
        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return // No enviar la notificación si no hay permiso
            }
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }


}
