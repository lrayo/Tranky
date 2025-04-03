package com.example.beaconkotlinapp.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object CallUtils {
    private const val CALL_PERMISSION_REQUEST_CODE = 1001

    fun makePhoneCall(activity: Activity, phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")

        if (PermissionUtils.hasCallPermission(activity)) {
            activity.startActivity(callIntent)
        } else {
            Toast.makeText(activity, "No tienes permisos para realizar llamadas ❌", Toast.LENGTH_SHORT).show()
        }
    }
}