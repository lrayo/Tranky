package com.example.beaconkotlinapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.beaconkotlinapp.R
import com.kkmcn.kbeaconlib2.KBeacon
import com.kkmcn.kbeaconlib2.KBeaconsMgr

class DeviceScanActivity : AppCompatActivity(), KBeaconsMgr.KBeaconMgrDelegate {

    private lateinit var beaconsMgr: KBeaconsMgr
    private lateinit var beaconListView: ListView
    private lateinit var scanButton: Button
    private lateinit var statusTextView: TextView
    private val beaconsList = mutableListOf<KBeacon>()
    private lateinit var beaconAdapter: ArrayAdapter<String>
    private var isNavigating = false // Evitar múltiples aperturas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        scanButton = findViewById(R.id.btnScan)
        beaconListView = findViewById(R.id.listViewBeacons)
        statusTextView = findViewById(R.id.statusTextView)

        beaconsMgr = KBeaconsMgr.sharedBeaconManager(this)
        beaconsMgr.delegate = this

        beaconAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        beaconListView.adapter = beaconAdapter

        scanButton.setOnClickListener { startScanning() }

        beaconListView.setOnItemClickListener { _, _, position, _ ->
            val beacon = beaconsList[position]
            navigateToDevicePanel(beacon)
        }
    }

    private fun startScanning() {
        if (!checkPermissions()) {
            return
        }

        beaconsList.clear()
        beaconAdapter.clear()
        statusTextView.text = "Buscando dispositivos..."
        beaconsMgr.startScanning()
    }

    override fun onBeaconDiscovered(beacons: Array<out KBeacon>?) {
        runOnUiThread {
            if (!beacons.isNullOrEmpty()) {
                for (beacon in beacons) {
                    if (!beaconsList.contains(beacon)) {
                        beaconsList.add(beacon)
                        beaconAdapter.add("${beacon.name} - ${beacon.mac}")
                    }
                }

                statusTextView.text = "Beacons encontrados: ${beaconsList.size}"

                // 🚀 Abre la siguiente pantalla automáticamente si encuentra un beacon
                if (!isNavigating && beaconsList.isNotEmpty()) {
                    isNavigating = true
                    navigateToDevicePanel(beaconsList.first())
                }
            } else {
                statusTextView.text = "No se encontraron beacons"
            }
        }
    }

    private fun navigateToDevicePanel(beacon: KBeacon) {
        val intent = Intent(this, DevicePannelActivity::class.java)
        intent.putExtra("DEVICE_MAC_ADDRESS", beacon.mac)
        startActivity(intent)
    }

    private fun checkPermissions(): Boolean {
        return true // Aquí puedes validar permisos si es necesario
    }

    override fun onScanFailed(errorCode: Int) {
        runOnUiThread {
            statusTextView.text = "Error en escaneo: $errorCode"
        }
    }

    override fun onCentralBleStateChang(nNewState: Int) {
        runOnUiThread {
            when (nNewState) {
                KBeaconsMgr.BLEStatePowerOff -> statusTextView.text = "Bluetooth apagado"
                KBeaconsMgr.BLEStatePowerOn -> statusTextView.text = "Bluetooth activado"
                else -> statusTextView.text = "Estado desconocido de Bluetooth"
            }
        }
    }
}
