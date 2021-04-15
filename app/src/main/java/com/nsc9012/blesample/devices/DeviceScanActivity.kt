package com.nsc9012.blesample.devices

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.nsc9012.blesample.R
import com.nsc9012.blesample.extensions.toast
import java.util.*

class DeviceScanActivity : AppCompatActivity() {

    companion object {
        val bt_Nordic_UART_Service = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        const val REQUEST_ENABLE_BT = 1
        const val PERMISSION_REQUEST_COARSE_LOCATION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted
                } else {
                    toast("Without location access, this app cannot discover beacons.")
                }
            }
        }
    }
}
