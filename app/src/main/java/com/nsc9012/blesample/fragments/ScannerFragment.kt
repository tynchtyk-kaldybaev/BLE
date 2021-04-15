package com.nsc9012.blesample.fragments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nsc9012.blesample.R
import com.nsc9012.blesample.devices.DeviceScanActivity
import com.nsc9012.blesample.devices.DeviceConnectActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.ArrayList

class ScannerFragment : Fragment() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner


    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //Log.e("Main", result.device?.address + " " + result.device?.name)
            processResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            for (result in results!!) {
                processResult(result)
            }
        }

        private fun processResult(result: ScanResult) {
            val devInfo = result.toString()

            if (devInfo.contains("Young&be")) {
                stopScanning()
                val intent = Intent(activity, DeviceConnectActivity::class.java)
                intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_NAME, result.device?.name)
                intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_ADDRESS, result.device?.address)
                startActivity(intent)
            }
            else {
               //Log.e("Main", result.device?.address + " " + result.device?.name)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initBLE()
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onResume() {
        super.onResume()
        startScanning()
    }

    private fun initBLE() {
        bluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, DeviceScanActivity.REQUEST_ENABLE_BT)
        }

        if (activity?.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                DeviceScanActivity.PERMISSION_REQUEST_COARSE_LOCATION
            )
        }
    }

    private fun startScanning() {
        /*
        val filters: MutableList<ScanFilter> = ArrayList()
        val scan_filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(DeviceScanActivity.bt_Nordic_UART_Service)) //.setDeviceName("Young&be")
            .build()
        filters.add(scan_filter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()*/

        bluetoothAdapter.getProfileConnectionState(BluetoothAdapter.STATE_CONNECTED)

        GlobalScope.launch {
            bluetoothLeScanner.startScan(leScanCallback)
        }
    }

    private fun stopScanning() {
        GlobalScope.launch {
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

}