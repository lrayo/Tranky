package com.example.beaconkotlinapp.ui

import BeaconRepository
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.beaconkotlinapp.R
import com.example.beaconkotlinapp.utils.CallUtils
import com.example.beaconkotlinapp.utils.LocationUtils
import com.example.beaconkotlinapp.utils.NotificationHelper
import com.example.beaconkotlinapp.utils.PermissionUtils
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTrigger
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAction
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerType
import com.kkmcn.kbeaconlib2.KBConnState
import com.kkmcn.kbeaconlib2.KBeacon
import com.kkmcn.kbeaconlib2.KBeaconsMgr
import org.json.JSONObject

class DevicePannelActivity : AppCompatActivity(), KBeacon.NotifyDataDelegate {

    private lateinit var beacon: KBeacon
    private lateinit var btnBeep: Button

    // Verifica y solicita permisos en Android 13+
    private fun checkAndRequestNotificationPermission() {
        // Android 13+
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pannel)

        // Solicitar permisos de llamada al iniciar la app si aún no se concedieron
        if (!PermissionUtils.hasCallPermission(this)) {
            PermissionUtils.requestCallPermission(this)
        }

        // Solicitar permisos de ubicación
        if (!PermissionUtils.hasLocationPermission(this)) {
            PermissionUtils.requestLocationPermission(this)
        }

        btnBeep = findViewById(R.id.buttonBeep)

        // Obtener la dirección MAC desde el intent
        val deviceMac = intent.getStringExtra("DEVICE_MAC_ADDRESS")

        if (deviceMac.isNullOrEmpty()) {
            Log.e("BeaconApp", "Error: No se recibió DEVICE_MAC_ADDRESS")
            finish() // Cierra la actividad si no hay dirección MAC
            return
        }

        Log.d("BeaconApp", "📡 Conectando a beacon con MAC: $deviceMac")

        val beaconMgr = KBeaconsMgr.sharedBeaconManager(this)
        beacon = beaconMgr.getBeacon(deviceMac)!!

        NotificationHelper.createNotificationChannel(this)
        checkAndRequestNotificationPermission()

        connectToBeacon()

        btnBeep.setOnClickListener { triggerBeep() }
    }

    private fun connectToBeacon() {
        beacon.connect("0000000000000000", 10000
        ) { _, state, reason ->
            runOnUiThread {
                if (state == KBConnState.Connected) {
                    showToast("Conectado al beacon ✅")
                    Log.d("BeaconApp", "Conectado correctamente al beacon.")

                    // Verificar qué eventos de botón soporta el beacon
                    checkSupportedTriggers()

                    // Habilitar la detección del botón
                    enableButtonTriggerEvent2App()
                } else {
                    showToast("Error de conexión: $reason ❌")
                    Log.e("BeaconApp", "Error de conexión con código: $reason")
                }
            }
        }
    }

    private fun triggerBeep() {
        if (!beacon.isConnected) {
            showToast("Beacon no conectado")
            return
        }

        // Deshabilitar botón para evitar múltiples clics
        btnBeep.isEnabled = false

        val cmdPara = JSONObject()
        try {
            cmdPara.put("msg", "ring")
            cmdPara.put("ringTime", 20000)   // Duración del beep en milisegundos
            cmdPara.put("ringType", 0x1) // 0x0: solo LED, 0x1: solo sonido, 0x2: ambos
            cmdPara.put("ledOn", 200)   // Si usa LED, duración de encendido en ms
            cmdPara.put("ledOff", 1800) // Si usa LED, duración de apagado en ms
        } catch (exception: Exception) {
            exception.printStackTrace()
            btnBeep.isEnabled = true
            return
        }

        beacon.sendCommand(cmdPara) { success, error ->
            runOnUiThread {
                btnBeep.isEnabled = true
                if (success) {
                    showToast("El Beacon está sonando 🔊")
                } else {
                    showToast("Error al enviar comando: ${error?.errorCode}")
                }
            }
        }
    }

    private fun checkSupportedTriggers() {
        val beaconConfig = beacon.commonCfg
        if (beaconConfig == null || !beaconConfig.isSupportButton) {
            Log.e("BeaconApp", "El beacon no soporta eventos de botón ❌")
            showToast("El beacon no soporta eventos de botón ❌")
            return
        }

        val triggers = listOf(
            KBTriggerType.BtnSingleClick to "Clic único",
            KBTriggerType.BtnDoubleClick to "Doble clic",
            KBTriggerType.BtnTripleClick to "Triple clic",
            KBTriggerType.BtnLongPress to "Pulsación larga"
        )

        for ((type, name) in triggers) {
            if (beaconConfig.isSupportTrigger(type)) {
                Log.d("BeaconApp", "✅ El beacon soporta: $name")
                showToast("✅ Soporta: $name")
            } else {
                Log.w("BeaconApp", "❌ El beacon NO soporta: $name")
                showToast("❌ NO soporta: $name")
            }
        }
    }

    private fun enableButtonTriggerEvent2App() {
        if (!beacon.isConnected) {
            showToast("Beacon no conectado ❌")
            return
        }

        val beaconConfig = beacon.commonCfg
        if (beaconConfig == null || !beaconConfig.isSupportButton) {
            showToast("El beacon no soporta eventos de botón ❌")
            return
        }

        // Configurar TODOS los eventos en una sola lista
        val triggers = ArrayList<KBCfgBase>()

        val singleClick = KBCfgTrigger(0, KBTriggerType.BtnSingleClick)
        singleClick.setTriggerAction(KBTriggerAction.Report2App)
        triggers.add(singleClick)

        val doubleClick = KBCfgTrigger(1, KBTriggerType.BtnDoubleClick)
        doubleClick.setTriggerAction(KBTriggerAction.Report2App)
        triggers.add(doubleClick)

        val tripleClick = KBCfgTrigger(2, KBTriggerType.BtnTripleClick)
        tripleClick.setTriggerAction(KBTriggerAction.Report2App)
        triggers.add(tripleClick)

        val longPress = KBCfgTrigger(3, KBTriggerType.BtnLongPress)
        longPress.setTriggerAction(KBTriggerAction.Report2App)
        triggers.add(longPress)

        try {
            beacon.modifyConfig(triggers) { success, error ->
                runOnUiThread {
                    if (success) {
                        showToast("Eventos del botón configurados ✅")
                        Log.d("BeaconApp", "Se configuraron eventos de botón correctamente")

                        // Suscribirse a eventos del botón
                        beacon.subscribeSensorDataNotify(null, this@DevicePannelActivity) { subSuccess, subError ->
                            runOnUiThread {
                                if (subSuccess) {
                                    showToast("Suscripción exitosa a eventos de botón ✅")
                                } else {
                                    showToast("Error en la suscripción ❌")
                                    Log.e("BeaconApp", "Error en la suscripción: ${subError?.errorCode}")
                                }
                            }
                        }
                    } else {
                        showToast("Error en la configuración: ${error?.errorCode}")
                        Log.e("BeaconApp", "Error en modifyConfig: ${error?.errorCode}")
                    }
                }
            }
        } catch (e: Exception) {
            showToast("Excepción en modifyConfig: ${e.message} ❌")
            Log.e("BeaconApp", "Excepción en modifyConfig()", e)
        }
    }

    override fun onNotifyDataReceived(beacon: KBeacon, nEventType: Int, sensorData: ByteArray?) {
        Log.v("BeaconApp", "Se ha presionado el botón del beacon. Evento: $nEventType")

        val deviceMac = beacon.mac // Obtener la dirección MAC del beacon
        val phoneNumber = "+123456789" // Aquí puedes obtener el número dinámicamente
        Log.d("BeaconApp", "📡 Beacon MAC: $deviceMac")

        runOnUiThread {
            when (nEventType) {
                KBTriggerType.BtnSingleClick -> {
                    Log.d("BeaconApp", "🔹 Botón: Clic único detectado.")
                    NotificationHelper.showNotification(this, "Clic Único", "Se detectó un clic único en el beacon.", deviceMac)

                    // Guardar en PostgreSQL
                    BeaconRepository.saveBeaconEvent(deviceMac, null, null, phoneNumber, "Clic Único")
                }

                KBTriggerType.BtnDoubleClick -> {
                    Log.d("BeaconApp", "🔹 Botón: Doble clic detectado.")

                    // Obtener ubicación y guardarla en la base de datos
                    LocationUtils.getCurrentLocation(this) { location ->
                        val lat = location?.latitude
                        val lon = location?.longitude
                        val locationMessage = if (location != null) {
                            "📍 Ubicación en tiempo real: Lat ${lat}, Lng ${lon}"
                        } else {
                            "No se pudo obtener la ubicación en tiempo real."
                        }

                        Log.d("BeaconApp", "📍 Ubicación obtenida: $locationMessage")

                        NotificationHelper.showNotification(
                            context = this,
                            title = "Doble Clic",
                            message = locationMessage,
                            deviceMac = deviceMac,
                            location = location
                        )

                        // Guardar en PostgreSQL
                        BeaconRepository.saveBeaconEvent(deviceMac, lat, lon, phoneNumber, "Doble Clic")
                    }
                }

                KBTriggerType.BtnTripleClick -> {
                    Log.d("BeaconApp", "🔹 Botón: Triple clic detectado.")
                    NotificationHelper.showNotification(this, "Triple Clic", "Se detectó un triple clic en el beacon.", deviceMac)

                    // Guardar en PostgreSQL
                    BeaconRepository.saveBeaconEvent(deviceMac, null, null, phoneNumber, "Triple Clic")
                }

                KBTriggerType.BtnLongPress -> {
                    Log.d("BeaconApp", "🔹 Botón: Pulsación larga detectada. Iniciando llamada...")
                    CallUtils.makePhoneCall(this, phoneNumber)

                    // Guardar en PostgreSQL
                    BeaconRepository.saveBeaconEvent(deviceMac, null, null, phoneNumber, "Pulsación Larga")
                }

                else -> {
                    Log.e("BeaconApp", "🚨 Evento desconocido recibido: $nEventType")
                    NotificationHelper.showNotification(this, "Evento Desconocido", "Se recibió un evento desconocido.", deviceMac)

                    // Guardar en PostgreSQL
                    BeaconRepository.saveBeaconEvent(deviceMac, null, null, phoneNumber, "Evento Desconocido")
                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de llamada concedido ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de llamada denegado ❌", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
